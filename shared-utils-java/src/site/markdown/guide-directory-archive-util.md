<!--
Copyright 2016 Development Entropy (deventropy.org) Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Directory Archive Util Guide

This utility class (`DirectoryArchiverUtil`) providing convenience methods to create Zip/Jar/Tar/GZipped Tar archives
for entire directories. The functionality implemented in this class is rudimentary at this time, and does not support:

* Compression Level
* Compression Method
* Advanced manifest manipulation (Jar files)
* Jar signing
* Filtering files out

### Apache Commons Compress Dependency

Using this class requires an additional dependency _org.apache.commons:commons-compress_ to be declared in
your project. At the time of writing, the project has been tested with version `1.10` of Commons Compress. All versions
of Commons Compress should work, except of course the different compression behaviors have changed in Commons Compress
across its versions. See the following pages for such specific behaviors:
* [Tar package](http://commons.apache.org/proper/commons-compress/tar.html)
* [Zip package](http://commons.apache.org/proper/commons-compress/zip.html)

## Creating a Zip Archive

To create a zip archive from a directory `/project/data/source` into a file `/project/data/source.zip`, use the 
[createZipArchiveOfDirectory(String, File, String)](./apidocs/org/deventropy/shared/utils/DirectoryArchiverUtil.html#createJarArchiveOfDirectory-java.lang.String-java.io.File-java.lang.String-)
method as:

```java
DirectoryArchiverUtil.createJarArchiveOfDirectory("/project/data/source", "/project/data/source.zip", null);
```

This will archive all the contents of the source folder recursively in the zip file created. Immediate children
of the source folder will be at the root of the zip file. The current implementation does not traverse down symbolic
links, and they will be excluded.

The Zip file will be created with `UTF-8` encoding.

### Nesting contents in the archive

The implementation supports nesting contents from the source one or more directories down in the archive file
created. Working on the example above, if the code were invoked as:

```java
DirectoryArchiverUtil.createJarArchiveOfDirectory("/project/data/source", "/project/data/source.zip", "test/one");
```

This will cause any files or directories which were immediate children of the source folder to appear nested under
directories `test/one` in the archive (or when the archive is inflated). So `/project/data/source/file.txt` will appear
at `test/one/file.txt` in the archive.

## Creating a Jar Archive

Jar files are created almost identical to the Zip files above, with the additional functionality of a very
rudimentary Manifest (`META-INF/MANIFEST.MF`) file is added to the archive with just the Manifest Version property set.

To create Jar files, use the
[createJarArchiveOfDirectory(String, File, String)](./apidocs/org/deventropy/shared/utils/DirectoryArchiverUtil.html#createZipArchiveOfDirectory-java.lang.String-java.io.File-java.lang.String-)
method instead.

## Creating a Tar Archive

To create a tar archive from a directory `/project/data/source` into a file `/project/data/source.tar`, use the 
[createTarArchiveOfDirectory(String, File, String)](./apidocs/org/deventropy/shared/utils/DirectoryArchiverUtil.html#createTarArchiveOfDirectory-java.lang.String-java.io.File-java.lang.String-)
method as:

```java
DirectoryArchiverUtil.createTarArchiveOfDirectory("/project/data/source", "/project/data/source.tar", null);
```

This will archive all the contents of the source folder recursively in the tar file created. Immediate children
of the source folder will be at the root of the tar file. The current implementation does not traverse down symbolic
links, and they will be excluded.

The tar file will be created with the following additional tar features:
* Uses the platform default character encoding
* Uses a PAX extended header as defined by POSIX 1003.1 for long file names
* PAX extension headers will be written for non-ASCII file names

### Nesting contents in the archive

The implementation supports nesting contents from the source one or more directories down in the archive file
created. Working on the example above, if the code were invoked as:

```java
DirectoryArchiverUtil.createTarArchiveOfDirectory("/project/data/source", "/project/data/source.tar", "test/one");
```

This will cause any files or directories which were immediate children of the source folder to appear nested under
directories `test/one` in the archive (or when the archive is inflated). So `/project/data/source/file.txt` will appear
at `test/one/file.txt` in the archive.

## Creating a GZipped Tar Archive

The utility can optionally `GZip` compress a tar file created as above. To get GZip compressed tar files, use the
[createGZippedTarArchiveOfDirectory(String, File, String)](./apidocs/org/deventropy/shared/utils/DirectoryArchiverUtil.html#createGZippedTarArchiveOfDirectory-java.lang.String-java.io.File-java.lang.String-)
method as:

```java
// Without nesting content
DirectoryArchiverUtil.createGZippedTarArchiveOfDirectory("/project/data/source", "/project/data/source.tar", null);
// Nesting content in sub dirs
DirectoryArchiverUtil.createGZippedTarArchiveOfDirectory("/project/data/source", "/project/data/source.tar", "test/one");
```
