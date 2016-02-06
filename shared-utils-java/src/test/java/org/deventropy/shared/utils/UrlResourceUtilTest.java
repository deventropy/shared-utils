/* 
 * Copyright 2015 Development Entropy (deventropy.org) Contributors
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Bindul Bhowmik
 *
 */
public class UrlResourceUtilTest {

	/**
	 * Used to store the temporary test data.
	 */
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder ();
	
	/**
	 * Test method for {@link org.deventropy.shared.utils.UrlResourceUtil#getUrl(java.lang.String)}.
	 * @throws IOException on error
	 */
	@Test
	public void testGetUrlFile () throws IOException {
		final File tempFile = tempFolder.newFile();
		final URL url = tempFile.toURI().toURL();
		final String urlStr = url.toExternalForm();

//		log.debug("Temp file url = {}", url);

		URL resolved = UrlResourceUtil.getUrl(urlStr);
		assertEquals(url, resolved);

		checkInputStream(resolved);

		resolved = UrlResourceUtil.getUrl(tempFile.getPath());
		assertNotNull(resolved);

		checkInputStream(resolved);
	}
	
	@Test
	public void testGetUrlClasspath () throws IOException {
		// CHECK 1
		String url = "classpath:/org/deventropy/shared/utils/classpath-url-test.file";

		URL resolved = UrlResourceUtil.getUrl(url);

//		log.debug("Resolved: {}", resolved);
		assertNotNull(resolved);

		checkInputStream(resolved);

		// CHECK 2
		url = "classpath:org/deventropy/shared/utils/classpath-url-test.file";

		resolved = UrlResourceUtil.getUrl(url);

//		log.debug("Resolved: {}", resolved);
		assertNotNull(resolved);

		checkInputStream(resolved);
	}

	private void checkInputStream (final URL resolved) throws IOException {
		InputStream is = null;
		try {
			is = resolved.openStream();
			assertNotNull(is);
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
//					log.catching(e);
				}
			}
		}
	}
	
	@Test(expected = FileNotFoundException.class)
	public void testGetUrlClasspathNonExisting () throws IOException {
		final String url = "classpath:/some/missing/resource/file";
		UrlResourceUtil.getUrl(url);
	}
	
	@Test
	public void testIsFile () throws IOException {
		final File tempFile = tempFolder.newFile();
		final URL url = tempFile.toURI().toURL();
		final String urlStr = url.toExternalForm();

		assertTrue(UrlResourceUtil.isFile(urlStr));
		assertFalse(UrlResourceUtil.isFile("http://google.com/"));
	}
	
	@Test
	public void testGetFileForWrite () throws IOException {
		final File tempFile = tempFolder.newFile();
		final URL url = tempFile.toURI().toURL();
		final String urlStr = url.toExternalForm();

		File tempFileResolved = UrlResourceUtil.getFileForWrite(urlStr);

		assertNotNull(tempFileResolved);
		assertEquals(tempFile.getPath(), tempFileResolved.getPath());


		tempFileResolved = UrlResourceUtil.getFileForWrite(tempFile.getPath());

		assertNotNull(tempFileResolved);
		assertEquals(tempFile.getPath(), tempFileResolved.getPath());
	}
	
	@Test(expected = IOException.class)
	public void testGetFileForWriteNonExistantFile () throws IOException {
		UrlResourceUtil.getFileForWrite("/file/does/not/exist");
	}

}
