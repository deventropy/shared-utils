/* 
 * Copyright 2015 Development Entropy (deventropy.org) Contributors
 * Copyright 2002-2014 the original author or authors at Spring Framework.
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
package org.deventropy.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A utility class to provide uniform resource loading capability from real and <em>pseudo</em> URLs, like classpath.
 * 
 * <p>This utility can be used to allow consumers the same interface to load resources from classpaths, file system,
 * HTTP, etc. This class is inspired by the Spring Framework
 * <a href="http://tinyurl.com/gp4eagg">DefaultResourceLoader</a>.
 * 
 * <p>Supported formats or URLs by this util:
 * <ul>
 * <li><em>Classpath</em> Example: <code><b>classpath:</b>/org/deventropy/junithelper/utils/file.ext</code></li>
 * <li><em>File</em> Example: <code><b>file:</b>/path/to/file/file.ext</code></li>
 * <li><em>HTTP</em> Example: <code><b>http://</b>example.com/file.ext</code></li>
 * <li><em>HTTPS</em> Example: <code><b>https://</b>example.com/file.ext</code></li>
 * </ul>
 * 
 * @author Bindul Bhowmik
 */
public final class UrlResourceUtil {
	
	/**
	 * Classpath URL prefix.
	 */
	public static final String URL_PROTOCOL_CLASSPATH = "classpath:";
	
	/**
	 * File url prefix.
	 */
	public static final String URL_PROTOCOL_FILE = "file:";
	
	/**
	 * HTTP url prefix.
	 */
	public static final String URL_PROTOCOL_HTTP = "http://";
	
	/**
	 * HTTPS url prefix.
	 */
	public static final String URL_PROTOCOL_HTTPS = "https://";
	
	/**
	 * known protocols.
	 */
	public static final Set<String> KNOWN_PROTOCOLS;
	
	static {
		final Set<String> knownProtocols = new HashSet<String> ();
		knownProtocols.add(URL_PROTOCOL_FILE);
		knownProtocols.add(URL_PROTOCOL_CLASSPATH);
		knownProtocols.add(URL_PROTOCOL_HTTP);
		knownProtocols.add(URL_PROTOCOL_HTTPS);

		KNOWN_PROTOCOLS = Collections.unmodifiableSet(knownProtocols);
	}
	
	private UrlResourceUtil () {
		// Util class
	}
	
	/**
	 * @param resource Resource to checkl
	 * @param cl Classpath to use
	 * @return A URL format
	 * @throws MalformedURLException Malformed URL
	 * @throws IOException Resource not found
	 */
	public static URL getUrl (final String resource, final ClassLoader cl)
			throws MalformedURLException, IOException {

		ArgumentCheck.notNull(resource, "Resource cannot be null");
		URL url = null;

		if (resource.startsWith(URL_PROTOCOL_CLASSPATH)) {
			String resourcePart = resource.substring(URL_PROTOCOL_CLASSPATH.length());
	
			// System and non system classloaders behave different with the starting '/', so try both
			if (resourcePart.length() >= 1 && resourcePart.charAt(0) != '/') {
				resourcePart = "/" + resourcePart;
			}
			url = cl.getResource(resourcePart);
	
			if (null == url) {
				url = cl.getResource(resourcePart.substring(1)); // Try without the '/'
			}
	
			if (null == url) {
				throw new FileNotFoundException("The requested classpath resource " + resource
						+ " does not exist");
			}
		} else {
	
			try {
				url = new URL(resource);
			} catch (MalformedURLException e) {
				// Try to resolve it as a file
				url = new File(resource).toURI().toURL();
			}
		}

		return url;
	}
	
	/**
	 * @param resource Resource to check
	 * @return URL format
	 * @throws IOException Exception getting the URL
	 */
	public static URL getUrl (final String resource) throws IOException {
		return getUrl(resource, ClassUtil.getApplicableClassloader(null));
	}
	
	/**
	 * @param resource If the resource is a file
	 * @return <code>true</code> if file on mounted drive
	 */
	public static boolean isFile (final String resource) {
		return resource.startsWith(URL_PROTOCOL_FILE) || new File(resource).exists();
	}
	
	/**
	 * @param resource OPen resource as a file to write
	 * @return File if the resource is a file and can write
	 * @throws IOException Exception trying to open a file
	 */
	public static File getFileForWrite (final String resource) throws IOException {
		if (!isFile(resource)) {
			throw new IOException("Not a file");
		}
		if (resource.startsWith(URL_PROTOCOL_FILE)) {
			return new File(resource.substring(URL_PROTOCOL_FILE.length()));
		}
		return new File(resource);
	}

}
