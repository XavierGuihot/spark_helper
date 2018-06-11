package com.spark_helper

import org.apache.hadoop.io.compress.GzipCodec

import com.holdenkarau.spark.testing.SharedSparkContext

import org.scalatest.FunSuite

/** Testing facility for the HdfsHelper.
  *
  * @author Xavier Guihot
  * @since 2017-02
  */
class HdfsHelperTest extends FunSuite with SharedSparkContext {

  val resourceFolder = "src/test/resources"

  test("Delete file/folder") {

    // Let's try to delete a file:

    HdfsHelper.writeToHdfsFile("", "src/test/resources/file_to_delete.txt")

    // 1: Let's try to delete it with the deleteFolder method:
    var messageThrown = intercept[IllegalArgumentException] {
      HdfsHelper.deleteFolder("src/test/resources/file_to_delete.txt")
    }
    var expectedMessage =
      "requirement failed: to delete a file, prefer using the " +
        "deleteFile() method."
    assert(messageThrown.getMessage === expectedMessage)
    assert(HdfsHelper.fileExists("src/test/resources/file_to_delete.txt"))

    // 2: Let's delete it with the deleteFile method:
    HdfsHelper.deleteFile("src/test/resources/file_to_delete.txt")
    assert(!HdfsHelper.fileExists("src/test/resources/file_to_delete.txt"))

    // Let's try to delete a folder:

    HdfsHelper
      .writeToHdfsFile("", "src/test/resources/folder_to_delete/file.txt")

    // 3: Let's try to delete it with the deleteFile method:
    messageThrown = intercept[IllegalArgumentException] {
      HdfsHelper.deleteFile("src/test/resources/folder_to_delete")
    }
    expectedMessage =
      "requirement failed: to delete a folder, prefer using the " +
        "deleteFolder() method."
    assert(messageThrown.getMessage === expectedMessage)
    assert(HdfsHelper.folderExists("src/test/resources/folder_to_delete"))

    // 4: Let's delete it with the deleteFolder method:
    HdfsHelper.deleteFolder("src/test/resources/folder_to_delete")
    assert(!HdfsHelper.folderExists("src/test/resources/folder_to_delete"))
  }

  test("File/folder exists") {

    HdfsHelper.deleteFile("src/test/resources/file_to_check.txt")
    HdfsHelper.deleteFolder("src/test/resources/folder_to_check")

    // Let's try to check if a file exists:

    assert(!HdfsHelper.fileExists("src/test/resources/file_to_check.txt"))

    HdfsHelper.writeToHdfsFile("", "src/test/resources/file_to_check.txt")

    // 1: Let's try to check it exists with the folderExists method:
    var messageThrown = intercept[IllegalArgumentException] {
      HdfsHelper.folderExists("src/test/resources/file_to_check.txt")
    }
    var expectedMessage =
      "requirement failed: to check if a file exists, prefer using the " +
        "fileExists() method."
    assert(messageThrown.getMessage === expectedMessage)

    // 2: Let's try to check it exists with the fileExists method:
    assert(HdfsHelper.fileExists("src/test/resources/file_to_check.txt"))

    // Let's try to check if a folder exists:

    assert(!HdfsHelper.folderExists("src/test/resources/folder_to_check"))

    HdfsHelper
      .writeToHdfsFile("", "src/test/resources/folder_to_check/file.txt")

    // 3: Let's try to check it exists with the fileExists method:
    messageThrown = intercept[IllegalArgumentException] {
      HdfsHelper.fileExists("src/test/resources/folder_to_check")
    }
    expectedMessage =
      "requirement failed: to check if a folder exists, prefer using " +
        "the folderExists() method."
    assert(messageThrown.getMessage === expectedMessage)

    // 2: Let's try to check it exists with the folderExists method:
    assert(HdfsHelper.folderExists("src/test/resources/folder_to_check"))

    HdfsHelper.deleteFile("src/test/resources/file_to_check.txt")
    HdfsHelper.deleteFolder("src/test/resources/folder_to_check")
  }

  test("Create an empty file on hdfs") {

    HdfsHelper.deleteFile("src/test/resources/empty_file.token")

    HdfsHelper.createEmptyHdfsFile("src/test/resources/empty_file.token")

    assert(HdfsHelper.fileExists("src/test/resources/empty_file.token"))

    val tokenContent = sc
      .textFile("src/test/resources/empty_file.token")
      .collect()
      .sorted
      .mkString("\n")

    assert(tokenContent === "")

    HdfsHelper.deleteFile("src/test/resources/empty_file.token")
  }

  test(
    "Save text in HDFS file with the fileSystem API instead of the Spark API") {

    // 1: Stores using a "\n"-joined string:

    HdfsHelper.deleteFile("src/test/resources/folder/small_file.txt")

    val contentToStore = "Hello World\nWhatever"

    HdfsHelper.writeToHdfsFile(
      contentToStore,
      "src/test/resources/folder/small_file.txt")

    assert(HdfsHelper.fileExists("src/test/resources/folder/small_file.txt"))

    var storedContent = sc
      .textFile("src/test/resources/folder/small_file.txt")
      .collect()
      .sorted
      .mkString("\n")

    assert(storedContent === contentToStore)

    HdfsHelper.deleteFolder("src/test/resources/folder")

    // 2: Stores using a list of strings to be "\n"-joined:

    HdfsHelper.deleteFile("src/test/resources/folder/small_file.txt")

    val listToStore = List("Hello World", "Whatever")

    HdfsHelper
      .writeToHdfsFile(listToStore, "src/test/resources/folder/small_file.txt")

    assert(HdfsHelper.fileExists("src/test/resources/folder/small_file.txt"))

    storedContent = sc
      .textFile("src/test/resources/folder/small_file.txt")
      .collect()
      .sorted
      .mkString("\n")

    assert(storedContent === listToStore.mkString("\n"))

    HdfsHelper.deleteFolder("src/test/resources/folder")
  }

  test("List file names in Hdfs folder") {

    HdfsHelper.writeToHdfsFile("", "src/test/resources/folder_1/file_1.txt")
    HdfsHelper.writeToHdfsFile("", "src/test/resources/folder_1/file_2.csv")
    HdfsHelper
      .writeToHdfsFile("", "src/test/resources/folder_1/folder_2/file_3.txt")

    // 1: Not recursive, names only:
    var fileNames =
      HdfsHelper.listFileNamesInFolder("src/test/resources/folder_1")
    var expectedFileNames = List("file_1.txt", "file_2.csv")
    assert(fileNames === expectedFileNames)

    // 2: Not recursive, full paths:
    fileNames = HdfsHelper
      .listFileNamesInFolder("src/test/resources/folder_1", onlyName = false)
    expectedFileNames = List(
      "src/test/resources/folder_1/file_1.txt",
      "src/test/resources/folder_1/file_2.csv"
    )
    assert(fileNames === expectedFileNames)

    // 3: Recursive, names only:
    fileNames = HdfsHelper
      .listFileNamesInFolder("src/test/resources/folder_1", recursive = true)
    expectedFileNames = List("file_1.txt", "file_2.csv", "file_3.txt")
    assert(fileNames === expectedFileNames)

    // 4: Recursive, full paths:
    fileNames = HdfsHelper.listFileNamesInFolder(
      "src/test/resources/folder_1",
      recursive = true,
      onlyName = false)
    expectedFileNames = List(
      "src/test/resources/folder_1/file_1.txt",
      "src/test/resources/folder_1/file_2.csv",
      "src/test/resources/folder_1/folder_2/file_3.txt"
    )
    assert(fileNames === expectedFileNames)

    HdfsHelper.deleteFolder("src/test/resources/folder_1")
  }

  test("List folder names in Hdfs folder") {

    HdfsHelper.writeToHdfsFile("", "src/test/resources/folder_1/file_1.txt")
    HdfsHelper
      .writeToHdfsFile("", "src/test/resources/folder_1/folder_2/file_2.txt")
    HdfsHelper
      .writeToHdfsFile("", "src/test/resources/folder_1/folder_3/file_3.txt")

    val folderNames = HdfsHelper.listFolderNamesInFolder(
      "src/test/resources/folder_1"
    )
    val expectedFolderNames = List("folder_2", "folder_3")

    assert(folderNames === expectedFolderNames)

    HdfsHelper.deleteFolder("src/test/resources/folder_1")
  }

  test("Move file") {

    // Let's remove possible previous stuff:
    HdfsHelper.deleteFile("src/test/resources/some_file.txt")
    HdfsHelper.deleteFile("src/test/resources/renamed_file.txt")

    // Let's create the file to rename:
    HdfsHelper.writeToHdfsFile("whatever", "src/test/resources/some_file.txt")

    // 1: Let's try to move the file on a file which already exists without
    // the overwrite option:

    assert(HdfsHelper.fileExists("src/test/resources/some_file.txt"))
    assert(!HdfsHelper.fileExists("src/test/resources/renamed_file.txt"))

    // Let's create the existing file where we want to move our file:
    HdfsHelper.writeToHdfsFile("", "src/test/resources/renamed_file.txt")

    // Let's rename the file to the path where a file already exists:
    val ioExceptionThrown = intercept[IllegalArgumentException] {
      HdfsHelper.moveFile(
        "src/test/resources/some_file.txt",
        "src/test/resources/renamed_file.txt")
    }
    var expectedMessage =
      "requirement failed: overwrite option set to false, but a file " +
        "already exists at target location src/test/resources/renamed_file.txt"
    assert(ioExceptionThrown.getMessage === expectedMessage)

    assert(HdfsHelper.fileExists("src/test/resources/some_file.txt"))
    assert(HdfsHelper.fileExists("src/test/resources/renamed_file.txt"))

    HdfsHelper.deleteFile("src/test/resources/renamed_file.txt")

    // 2: Let's fail to move the file with the moveFolder() method:

    assert(HdfsHelper.fileExists("src/test/resources/some_file.txt"))
    assert(!HdfsHelper.fileExists("src/test/resources/renamed_file.txt"))

    // Let's rename the file:
    val illegalArgExceptionThrown = intercept[IllegalArgumentException] {
      HdfsHelper.moveFolder(
        "src/test/resources/some_file.txt",
        "src/test/resources/renamed_file.txt")
    }
    expectedMessage =
      "requirement failed: to move a file, prefer using the " +
        "moveFile() method."
    assert(illegalArgExceptionThrown.getMessage === expectedMessage)

    assert(HdfsHelper.fileExists("src/test/resources/some_file.txt"))
    assert(!HdfsHelper.fileExists("src/test/resources/renamed_file.txt"))

    // 3: Let's successfuly move the file with the moveFile() method:

    // Let's rename the file:
    HdfsHelper.moveFile(
      "src/test/resources/some_file.txt",
      "src/test/resources/renamed_file.txt")

    assert(!HdfsHelper.fileExists("src/test/resources/some_file.txt"))
    assert(HdfsHelper.fileExists("src/test/resources/renamed_file.txt"))

    val newContent = sc.textFile("src/test/resources/renamed_file.txt").collect

    assert(Array("whatever") === newContent)

    HdfsHelper.deleteFile("src/test/resources/renamed_file.txt")
  }

  test("Move folder") {

    // Let's remove possible previous stuff:
    HdfsHelper.deleteFolder("src/test/resources/some_folder_to_move")
    HdfsHelper.deleteFolder("src/test/resources/renamed_folder")

    // Let's create the folder to rename:
    HdfsHelper.writeToHdfsFile(
      "whatever",
      "src/test/resources/some_folder_to_move/file_1.txt")
    HdfsHelper.writeToHdfsFile(
      "something",
      "src/test/resources/some_folder_to_move/file_2.txt")

    // 1: Let's fail to move the folder with the moveFile() method:

    assert(
      HdfsHelper.fileExists(
        "src/test/resources/some_folder_to_move/file_1.txt"))
    assert(
      HdfsHelper.fileExists(
        "src/test/resources/some_folder_to_move/file_2.txt"))
    assert(!HdfsHelper.folderExists("src/test/resources/renamed_folder"))

    // Let's rename the folder:
    val messageThrown = intercept[IllegalArgumentException] {
      HdfsHelper.moveFile(
        "src/test/resources/some_folder_to_move",
        "src/test/resources/renamed_folder")
    }
    val expectedMessage =
      "requirement failed: to move a folder, prefer using the " +
        "moveFolder() method."
    assert(messageThrown.getMessage === expectedMessage)

    assert(
      HdfsHelper.fileExists(
        "src/test/resources/some_folder_to_move/file_1.txt"))
    assert(
      HdfsHelper.fileExists(
        "src/test/resources/some_folder_to_move/file_2.txt"))
    assert(!HdfsHelper.folderExists("src/test/resources/renamed_folder"))

    // 2: Let's successfuly move the folder with the moveFolder() method:

    // Let's rename the folder:
    HdfsHelper.moveFolder(
      "src/test/resources/some_folder_to_move",
      "src/test/resources/renamed_folder")

    assert(!HdfsHelper.folderExists("src/test/resources/some_folder_to_move"))
    assert(
      HdfsHelper.fileExists("src/test/resources/renamed_folder/file_1.txt"))
    assert(
      HdfsHelper.fileExists("src/test/resources/renamed_folder/file_2.txt"))

    val newContent =
      sc.textFile("src/test/resources/renamed_folder").collect().sorted

    assert(newContent === Array("something", "whatever"))

    HdfsHelper.deleteFolder("src/test/resources/renamed_folder")
  }

  test("Append header and footer to file") {

    // 1: Without the tmp/working folder:

    HdfsHelper.deleteFile("src/test/resources/header_footer_file.txt")

    // Let's create the file for which to add header and footer:
    HdfsHelper.writeToHdfsFile(
      "whatever\nsomething else\n",
      "src/test/resources/header_footer_file.txt")

    HdfsHelper.appendHeaderAndFooter(
      "src/test/resources/header_footer_file.txt",
      "my_header",
      "my_footer")

    var newContent = sc
      .textFile("src/test/resources/header_footer_file.txt")
      .collect
      .mkString("\n")

    var expectedNewContent = (
      "my_header\n" +
        "whatever\n" +
        "something else\n" +
        "my_footer"
    )

    assert(newContent === expectedNewContent)

    HdfsHelper.deleteFile("src/test/resources/header_footer_file.txt")

    // 2: With the tmp/working folder:

    // Let's create the file for which to add header and footer:
    HdfsHelper.writeToHdfsFile(
      "whatever\nsomething else\n",
      "src/test/resources/header_footer_file.txt")

    HdfsHelper.appendHeaderAndFooter(
      "src/test/resources/header_footer_file.txt",
      "my_header",
      "my_footer",
      workingFolderPath = "src/test/resources/header_footer_tmp")

    assert(HdfsHelper.folderExists("src/test/resources/header_footer_tmp"))
    assert(
      !HdfsHelper.fileExists("src/test/resources/header_footer_tmp/xml.tmp"))

    newContent = sc
      .textFile("src/test/resources/header_footer_file.txt")
      .collect
      .mkString("\n")

    expectedNewContent = (
      "my_header\n" +
        "whatever\n" +
        "something else\n" +
        "my_footer"
    )

    assert(newContent === expectedNewContent)

    HdfsHelper.deleteFile("src/test/resources/header_footer_file.txt")
    HdfsHelper.deleteFolder("src/test/resources/header_footer_tmp")
  }

  test("Validate Xml Hdfs file with Xsd") {

    // 1: Valid xml:
    HdfsHelper.deleteFile("src/test/resources/xml_file.txt")
    HdfsHelper.writeToHdfsFile(
      "<Customer>\n" +
        "	<Age>24</Age>\n" +
        "	<Address>34 thingy street, someplace, sometown</Address>\n" +
        "</Customer>",
      "src/test/resources/xml_file.txt"
    )

    var xsdFile = getClass.getResource("/some_xml.xsd")

    var isValid = HdfsHelper
      .isHdfsXmlCompliantWithXsd("src/test/resources/xml_file.txt", xsdFile)

    assert(isValid)

    // 2: Invalid xml:
    HdfsHelper.deleteFile("src/test/resources/xml_file.txt")
    HdfsHelper.writeToHdfsFile(
      "<Customer>\n" +
        "	<Age>trente</Age>\n" +
        "	<Address>34 thingy street, someplace, sometown</Address>\n" +
        "</Customer>",
      "src/test/resources/xml_file.txt"
    )

    xsdFile = getClass.getResource("/some_xml.xsd")

    isValid = HdfsHelper
      .isHdfsXmlCompliantWithXsd("src/test/resources/xml_file.txt", xsdFile)

    assert(!isValid)

    HdfsHelper.deleteFile("src/test/resources/xml_file.txt")
  }

  test("Load Typesafe Config from Hdfs") {

    HdfsHelper.deleteFile("src/test/resources/typesafe_config.conf")
    HdfsHelper.writeToHdfsFile(
      "config {\n" +
        "	something = something_else\n" +
        "}",
      "src/test/resources/typesafe_config.conf"
    )

    val config = HdfsHelper
      .loadTypesafeConfigFromHdfs("src/test/resources/typesafe_config.conf")

    assert(config.getString("config.something") === "something_else")

    HdfsHelper.deleteFile("src/test/resources/typesafe_config.conf")
  }

  test("Load Xml file from Hdfs") {

    HdfsHelper.deleteFile("src/test/resources/folder/xml_to_load.xml")

    HdfsHelper.writeToHdfsFile(
      "<toptag>\n" +
        "	<sometag value=\"something\">whatever</sometag>\n" +
        "</toptag>",
      "src/test/resources/folder/xml_to_load.xml"
    )

    val xmlContent = HdfsHelper
      .loadXmlFileFromHdfs("src/test/resources/folder/xml_to_load.xml")

    assert((xmlContent \ "sometag" \ "@value").text === "something")
    assert((xmlContent \ "sometag").text === "whatever")

    HdfsHelper.deleteFolder("src/test/resources/folder/")
  }

  test("Purge folder from too old files/folders") {

    HdfsHelper.deleteFolder("src/test/resources/folder_to_purge")
    HdfsHelper
      .createEmptyHdfsFile("src/test/resources/folder_to_purge/file.txt")
    HdfsHelper
      .createEmptyHdfsFile("src/test/resources/folder_to_purge/folder/file.txt")
    assert(HdfsHelper.fileExists("src/test/resources/folder_to_purge/file.txt"))
    assert(HdfsHelper.folderExists("src/test/resources/folder_to_purge/folder"))

    HdfsHelper.purgeFolder("src/test/resources/folder_to_purge", 63)

    assert(HdfsHelper.fileExists("src/test/resources/folder_to_purge/file.txt"))
    assert(HdfsHelper.folderExists("src/test/resources/folder_to_purge/folder"))

    HdfsHelper.purgeFolder("src/test/resources/folder_to_purge", 1)

    assert(HdfsHelper.fileExists("src/test/resources/folder_to_purge/file.txt"))
    assert(HdfsHelper.folderExists("src/test/resources/folder_to_purge/folder"))

    val messageThrown = intercept[IllegalArgumentException] {
      HdfsHelper.purgeFolder("src/test/resources/folder_to_purge", -3)
    }
    val expectedMessage =
      "requirement failed: the purgeAge provided \"-3\" must be superior to 0."
    assert(messageThrown.getMessage === expectedMessage)

    HdfsHelper.purgeFolder("src/test/resources/folder_to_purge", 0)

    assert(
      !HdfsHelper.fileExists("src/test/resources/folder_to_purge/file.txt"))
    assert(
      !HdfsHelper.folderExists("src/test/resources/folder_to_purge/folder"))

    HdfsHelper.deleteFolder("src/test/resources/folder_to_purge")
  }

  test("Compress hdfs file") {

    val testFolder = s"$resourceFolder/folder"
    val filePath = s"$testFolder/file.txt"

    HdfsHelper.deleteFile(filePath)

    HdfsHelper.writeToHdfsFile("hello\nworld", filePath)
    HdfsHelper.compressFile(filePath, classOf[GzipCodec], true)

    assert(HdfsHelper.fileExists(s"$filePath.gz"))

    // Easy to test with spark, as reading a file with the ".gz" extention
    // forces the read with the compression codec:
    val content = sc.textFile(s"$filePath.gz").collect.sorted
    assert(content === Array("hello", "world"))

    HdfsHelper.deleteFolder(testFolder)
  }
}
