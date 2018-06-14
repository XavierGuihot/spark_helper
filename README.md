
# SparkHelper [![Build Status](https://travis-ci.org/xavierguihot/spark_helper.svg?branch=master)](https://travis-ci.org/xavierguihot/spark_helper) [![Coverage Status](https://coveralls.io/repos/github/xavierguihot/spark_helper/badge.svg?branch=master)](https://coveralls.io/github/xavierguihot/spark_helper?branch=master) [![Release](https://jitpack.io/v/xavierguihot/spark_helper.svg)](https://jitpack.io/#xavierguihot/spark_helper)


## Overview


Version: 1.1.1

API Scaladoc: [SparkHelper](http://xavierguihot.com/spark_helper/#com.spark_helper.SparkHelper$)

This library contains a bunch of low-level basic methods for data processing
with Scala Spark.

The goal is to remove the maximum of highly used and highly duplicated low-level
code from the spark job code and replace it with methods fully tested whose
names are self-explanatory and readable.

This also provides a monitoring/logger tool.

This is a set of 4 modules:

* [HdfsHelper](http://xavierguihot.com/spark_helper/#com.spark_helper.HdfsHelper$): Wrapper around the [apache Hadoop FileSystem API](https://hadoop.apache.org/docs/r2.6.1/api/org/apache/hadoop/fs/FileSystem.html) for file manipulations on hdfs.
* [SparkHelper](http://xavierguihot.com/spark_helper/#com.spark_helper.SparkHelper$): Hdfs file manipulations through the Spark API (pimped RDDs and SparkContext).
* [DateHelper](http://xavierguihot.com/spark_helper/#com.spark_helper.DateHelper$): Wrapper around [joda-time](http://www.joda.org/joda-time/apidocs/) for usual data mining dates manipulations.
* [Monitor](http://xavierguihot.com/spark_helper/#com.spark_helper.Monitor$): Spark custom monitoring/logger and kpi validator.

Compatible with Spark 2.x


### HdfsHelper:

The full list of methods is available at
[HdfsHelper](http://xavierguihot.com/spark_helper/#com.spark_helper.HdfsHelper$).

Contains basic file-related methods mostly based on hdfs apache Hadoop
FileSystem API [org.apache.hadoop.fs.FileSystem](https://hadoop.apache.org/docs/r2.6.1/api/org/apache/hadoop/fs/FileSystem.html).

A non-exhaustive list of exemples:

```scala
import com.spark_helper.HdfsHelper

// A bunch of methods wrapping the FileSystem API, such as:
HdfsHelper.fileExists("my/hdfs/file/path.txt") // HdfsHelper.folderExists("my/hdfs/folder")
assert(HdfsHelper.listFileNamesInFolder("my/folder/path") == List("file_name_1.txt", "file_name_2.csv"))
assert(HdfsHelper.fileModificationDate("my/hdfs/file/path.txt") == "20170306")
assert(HdfsHelper.nbrOfDaysSinceFileWasLastModified("my/hdfs/file/path.txt") == 3)
HdfsHelper.deleteFile("my/hdfs/file/path.csv") // HdfsHelper.deleteFolder("my/hdfs/folder")
HdfsHelper.moveFolder("my/hdfs/folder") // HdfsHelper.moveFile("my/hdfs/file.txt")
HdfsHelper.createEmptyHdfsFile("/some/hdfs/file/path.token") // HdfsHelper.createFolder("my/hdfs/folder")

// File content helpers:
HdfsHelper.compressFile("hdfs/path/to/uncompressed_file.txt", classOf[GzipCodec])
HdfsHelper.appendHeader("my/hdfs/file/path.csv", "colum0,column1")

// Some Xml/Typesafe helpers for hadoop as well:
HdfsHelper.isHdfsXmlCompliantWithXsd("my/hdfs/file/path.xml", getClass.getResource("/some_xml.xsd"))
HdfsHelper.loadXmlFileFromHdfs("my/hdfs/file/path.xml")

// Very handy to load a config (typesafe format) stored on hdfs at the begining of a spark job:
HdfsHelper.loadTypesafeConfigFromHdfs("my/hdfs/file/path.conf"): Config

// In order to write small amount of data in a file on hdfs without the whole spark stack:
HdfsHelper.writeToHdfsFile(Array("some", "relatively small", "text"), "/some/hdfs/file/path.txt")
// or:
import com.spark_helper.HdfsHelper._
Array("some", "relatively small", "text").writeToHdfs("/some/hdfs/file/path.txt")
"hello world".writeToHdfs("/some/hdfs/file/path.txt")

// Deletes all files/folders in "hdfs/path/to/folder" for which the timestamp is older than 10 days:
HdfsHelper.purgeFolder("hdfs/path/to/folder", 10)
```

In case a specific configuration is needed to access the file system, these
setters are available:

```scala
// To use a specific conf FileSystem.get(whateverConf) instead of FileSystem.get(new Configuration()):
HdfsHelper.setConf(whateverConf)
// Or directly the FileSystem:
HdfsHelper.setFileSystem(whateverFileSystem)
```

### SparkHelper:

The full list of methods is available at
[SparkHelper](http://xavierguihot.com/spark_helper/#com.spark_helper.SparkHelper$).

Contains basic RRD-related methods.

A non-exhaustive list of exemples:

```scala
import com.spark_helper.SparkHelper._

// Same as rdd.saveAsTextFile("path"), but the result is a single file (while
// keeping the processing parallelized):
rdd.saveAsSingleTextFile("/my/output/file/path.txt")
rdd.saveAsSingleTextFile("/my/output/file/path.txt", classOf[BZip2Codec])

// Same as sc.textFile("path"), but instead of reading one record per line (by
// splitting the input with \n), it splits the file in records based on a custom
// delimiter. This way, xml, json, yml or any multi-line record file format can
// be used with Spark:
sc.textFile("/my/input/folder/path", "---\n")

// Equivalent to rdd.flatMap(identity) for RDDs of Seqs or Options:
rdd.flatten

// Equivalent to sparkContext.textFile(), but for each line is tupled with its
// file path:
SparkHelper.textFileWithFileName("folder", sparkContext)
// which produces:
// RDD(("folder/file_1.txt", "record1fromfile1"), ("folder/file_1.txt", "record2fromfile1"),
//    ("folder/file_2.txt", "record1fromfile2"), ...)

// In the given folder, this generates one file per key in the given key/value
// RDD. Within each file (named from the key) are all values for this key:
rdd.saveAsTextFileByKey("/my/output/folder/path")

// Concept mapper (the following exemple transforms RDD(1, 3, 2, 7, 8) into RDD(1, 3, 4, 7, 16)):
rdd.partialMap { case a if a % 2 == 0 => 2 * a }

// For when input files contain commas and textFile can't handle it:
sc.textFile(Seq("path/hello,world.txt", "path/hello_world.txt"))
```

### DateHelper:

The full list of methods is available at
[DateHelper](http://xavierguihot.com/spark_helper/#com.spark_helper.DateHelper$).

Wrapper around [joda-time](http://www.joda.org/joda-time/apidocs/) for
data-mining classic dates manipulations and job scheduling.

A non-exhaustive list of exemples:

```scala
import com.spark_helper.DateHelper

DateHelper.daysBetween("20161230", "20170101") // List("20161230", "20161231", "20170101")
DateHelper.today // "20170310"
DateHelper.yesterday // "20170309"
DateHelper.reformatDate("20170327", "yyyyMMdd", "yyMMdd") // "170327"
DateHelper.now("HH:mm") // "10:24"
DateHelper.currentTimestamp // "1493105229736"
DateHelper.nDaysBefore(3) // "20170307"
DateHelper.nDaysAfterDate(3, "20170307") // "20170310"
DateHelper.nextDay("20170310") // "20170311"
DateHelper.nbrOfDaysSince("20170302") // 8
DateHelper.nbrOfDaysBetween("20170327", "20170401") // 5
DateHelper.dayOfWeek("20160614") // 2

import com.spark_helper.DateHelper._

2.daysAgo // "20170308"
"20161230" to "20170101" // List("20161230", "20161231", "20170101")
3.daysBefore("20170310") // "20170307"
5.daysAfter // "20170315"
4.daysAfter("20170310") // "20170314"
"20170302".isCompliantWith("yyyyMMdd")
"20170310".nextDay // "20170311"
"20170310".previousDay // "20170309"
```

The default format (when no format is specified) is "yyyyMMdd" (20170327). It
can be modified globally with:

```scala
DateHelper.setFormat("ddMMMyy")
```

### Monitor:

The full list of methods is available at
[Monitor](http://xavierguihot.com/spark_helper/#com.spark_helper.Monitor$)

It's a simple logger/report which contains a report that one can update from
the driver and a success state. The idea is to persist job executions logs and
errors (and forget about grepping unreadable yarn logs).

It's designed for perdiodic spark jobs (handles storage and purge of logs) and
provides a way to handle kpis validation.

Logs are stored on the go which means one can have a direct real time access of
the job logs/status and it's current state (which can overwise be a pain if it
means going through yarn logs, or even for certain production environments going
through additional layers of software logs to get to yarn logs).

One of the issues this logger aims at tackling is the handling of exceptions
reported from executors. An exception within a Spark pipeline doesn't always
mean one want to make the job fail. Or if it's the case, one might still want to
perform a few actions before letting the job crash. The idea is thus to surround
(driver side) a Spark pipeline within a try catch and redirect the exception to
the logger for a clean logging.

This is a "driver-only" logger and is not intended at logging concurrent actions
from executors.

Produced reports can easily be inserted in a notification email whenerver the
job fails, which saves a lot of time to maintainers operating on heavy
production environements.

The produced persisted report is also a way for downstream jobs to know the
status of their input data.

```scala
import com.spark_helper.Monitor

Monitor.setTitle("My job title")
Monitor.addDescription(
  "My job description (whatever you want); for instance:\n" +
  "Documentation: https://github.com/xavierguihot/spark_helper")
Monitor.setLogFolder("path/to/log/folder")

try {

  // Let's perform a spark pipeline which might go wrong:
  val processedData = sc.textFile("file.json").map(/**whatever*/)

  // Let's say you want to get some KPIs on your output before storing it:
  val outputIsValid = Monitor.kpis(
    List(
      Test("Nbr of output records", processedData.count(), SUPERIOR_THAN, 10e6d, NBR),
      Test("Some pct of invalid output", your_complex_kpi, INFERIOR_THAN, 3, PCT)
    ),
    "My pipeline descirption"
  )

  if (outputIsValid)
    processedData.saveAsTextFile("wherever.csv")

} catch {
  case iie: InvalidInputException =>
    Monitor.error(iie, "My pipeline descirption", diagnostic = "No input data!")
  case e: Throwable =>
    Monitor.error(e, "My pipeline descirption") // whatever unexpected error
}

if (Monitor.isSuccess()) {
  val doMore = "Let's do some more stuff!"
  Monitor.log("My second pipeline description: success")
}

// At the end of the different steps of the job, we can store the report in
// HDFS (this saves the logs in the folder set with Monitor.setLogFolder):
Monitor.store()

// At the end of the job, if the job isn't successfull, you might want to
// crash it (for instance to get a notification from your scheduler):
if (!Monitor.isSuccess()) throw new Exception() // or send an email, or ...
```

At any time during the job, logs can be accessed from file
`path/to/log/folder/current.ongoing`

Here are some possible reports generated by the previous pipeline:

```
           My job title

My job description (whatever you want); for instance:
Documentation: https://github.com/xavierguihot/spark_helper
[10:23] Begining
[10:23-10:23] My pipeline descirption: failed
  Diagnostic: No input data!
    org.apache.hadoop.mapred.InvalidInputException: Input path does not exist: hdfs://my/hdfs/input/path
    at org.apache.hadoop.mapred.FileInputFormat.singleThreadedListStatus(FileInputFormat.java:285)
    at org.apache.hadoop.mapred.FileInputFormat.listStatus(FileInputFormat.java:228)
    ...
[10:23] Duration: 00:00:00
```

or

```
           My job title

My job description (whatever you want); for instance:
Documentation: https://github.com/xavierguihot/spark_helper
[10:23] Begining
[10:23-10:41] My pipeline descirption: success
  KPI: Nbr of output records
    Value: 14669071.0
    Must be superior than 10000000.0
    Validated: true
  KPI: Some pct of invalid output
    Value: 0.06%
    Must be inferior than 3.0%
    Validated: true
[10:41-10:42] My second pipeline description: success
[10:42] Duration: 00:19:23
```

## Including spark_helper to your dependencies:


With sbt, add these lines to your build.sbt:

```scala
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.xavierguihot" % "spark_helper" % "v1.1.1"
```

With maven, add these lines to your pom.xml:

```xml
<repositories>
	<repository>
		<id>jitpack.io</id>
		<url>https://jitpack.io</url>
	</repository>
</repositories>

<dependency>
	<groupId>com.github.xavierguihot</groupId>
	<artifactId>spark_helper</artifactId>
	<version>v1.1.1</version>
</dependency>
```

With gradle, add these lines to your build.gradle:

```groovy
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}

dependencies {
	compile 'com.github.xavierguihot:spark_helper:v1.1.1'
}
```
