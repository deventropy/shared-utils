<!--
Copyright 2015 Development Entropy (deventropy.org) Contributors

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

# Home

Development Entropy Shared Utils is a shared library of utility classes used by various Deventropy projects. It does not
contain any public use APIs in itself.

This library aims to have a minimal set of dependencies to keep the *import impact* minimal. Or it will have imports
marked optional.

The library has the following utilities (all in the package `org.deventropy.shared.utils`):

| Utility | Summary | Additional Documentation |
|---------|---------|--------------------------|
| **ArgumentCheck** | Methods to validate parameters to methods (`null` checks, etc.) | |
| **ClassUtil** | Utility to find appropriate class loaders / resources in the classpath. | |
| **UrlResourceUtil** | Methods to normalize access to resources across multiple sources (classpath, file system, etc.). The formats supported by this class are documented in [Resource Location Formats](./resource-location-formats.html) | [Resource Location Formats](./resource-location-formats.html) |
| **DirectoryArchiveUtil** | Rudimentary methods to create zip or jar files for entire contents of a directory. | [Directory Archive Util Guide](./guide-directory-archive-util.html) |

## Attributions

`UrlResourceUtil` is inspired by the Spring Framework [DefaultResourceLoader](http://tinyurl.com/gp4eagg) licensed
under the Apache Software License ver. 2.0.

`DirectoryArchiverUtil` is influenced by ideas by user **Gili** on a
[Stackoverflow discussion](http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file)
