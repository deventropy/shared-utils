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

# Resource Location Formats

This document lists the Resource URL formats supported by the `UrlResourceUtil` class that is used by multiple
Development Entropy projects.

## Classpath

Loads resoruces from the classpath. Resource Locations should be prefixed with `classpath:` and be fully qualified paths
in the classpath, with `.` replaced by `/`.

*Example:* `classpath:/org/deventropy/junithelper/utils/file.ext`

## File

Refers to a resource on a local or mounted file system. Resource Locations should be prefixed with `file:` and be fully
qualified paths.

*Example:* `file:/path/to/file/file.ext`

## HTTP / HTTPS

A remote HTTP / HTTPS resource. Regular HTTP/HTTPs URL format should be used.

*Example:* `http://example.com/file.ext`
