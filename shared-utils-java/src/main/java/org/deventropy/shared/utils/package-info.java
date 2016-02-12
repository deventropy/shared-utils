/* 
 * Copyright 2016 Development Entropy (deventropy.org) Contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Deventropy Shared Java Utils is a shared library of utility classes used by various JUnit Helper libraries. It does
 * not contain any public use APIs in itself.
 * 
 * <p>This library aims to have a minimal set of dependencies to keep the *import impact* minimal.
 * 
 * <p>The library has the following utilities:
 * <ul>
 * <li><em>{@link org.deventropy.shared.utils.ArgumentCheck ArgumentCheck}:</em> Methods to validate parameters to
 * methods (`null` checks, etc.)</li>
 * <li><em>{@link org.deventropy.shared.utils.ClassUtil ClassUtil}:</em> Utility to find appropriate class loaders/
 * resources in the classpath.</li>
 * <li><em>{@link org.deventropy.shared.utils.UrlResourceUtil UrlResourceUtil}:</em> Methods to normalize access to
 * resources across multiple sources (classpath, file system, etc.).</li>
 * <li><em>{@link org.deventropy.shared.utils.DirectoryArchiverUtil DirectoryArchiverUtil}:</em> Methods to create
 * zip or jar files for entire contents of a directory.</li>
 * </ul>
 * 
 * @author Bindul Bhowmik
 */
package org.deventropy.shared.utils;
