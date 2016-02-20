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
package org.deventropy.shared.utils;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Bindul Bhowmik
 *
 */
public class DirectoryArchiverUtilExceptionTest {
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	// Compression methods
	
	@Test(expected = IOException.class)
	public void testZipArchiveForceIOException () throws IOException {
		final File rootFolder = tempFolder.newFolder();
		final File archiveFile = tempFolder.newFolder();
		DirectoryArchiverUtil.createZipArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, null);
	}
	
	@Test(expected = IOException.class)
	public void testJarArchiveForceIOException () throws IOException {
		final File rootFolder = tempFolder.newFolder();
		final File archiveFile = tempFolder.newFolder();
		DirectoryArchiverUtil.createJarArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, null);
	}
	
	@Test(expected = IOException.class)
	public void testTarArchiveForceIOException () throws IOException {
		final File rootFolder = tempFolder.newFolder();
		final File archiveFile = tempFolder.newFolder();
		DirectoryArchiverUtil.createTarArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, null);
	}
	
	@Test(expected = IOException.class)
	public void testTarGzArchiveForceIOException () throws IOException {
		final File rootFolder = tempFolder.newFolder();
		final File archiveFile = tempFolder.newFolder();
		DirectoryArchiverUtil.createGZippedTarArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, null);
	}
	
	@Test (expected = IOException.class)
	public void testArchiveException () throws Throwable {
		// private static void createArchiveOfDirectory (final String archiveFile, final File srcDirectory,
		// final String rootPathPrefix, final String archiveStreamFactoryConstant, final String encoding,
		// final MetaInformationProcessor metaInformationProcessor) throws IOException {
		final Class<DirectoryArchiverUtil> dirArchiverUtilClass = DirectoryArchiverUtil.class;
		final Method privateArchiverMethod = getPrivateAccessibleArchiverMethod(dirArchiverUtilClass);
		assertNotNull("Unable to find target method", privateArchiverMethod);

		invokePrivateArchiverMethod(privateArchiverMethod, null);
	}

	@Test (expected = IOException.class)
	public void testTarGzArchiveForceIOExceptionInvalidCompression () throws Throwable {
		final Class<DirectoryArchiverUtil> dirArchiverUtilClass = DirectoryArchiverUtil.class;

		Class<?> innerTarProcessorClass = null;
		for (Class<?> candidate : dirArchiverUtilClass.getDeclaredClasses()) {
			if ("org.deventropy.shared.utils.DirectoryArchiverUtil$TarArchiverCreateProcessor"
					.equals(candidate.getName())) {
				innerTarProcessorClass = candidate;
				break;
			}
		}
		assertNotNull("Unable to find inner tar processing class", innerTarProcessorClass);
		final Constructor<?> innerTarProcessorConstructor = innerTarProcessorClass.getDeclaredConstructor(String.class);
		final Object innerTarProcessor = innerTarProcessorConstructor.newInstance("INVALID");

		final Method privateArchiverMethod = getPrivateAccessibleArchiverMethod(dirArchiverUtilClass);
		assertNotNull("Unable to find target method", privateArchiverMethod);

		invokePrivateArchiverMethod(privateArchiverMethod, innerTarProcessor);
	}
	
	private Method getPrivateAccessibleArchiverMethod (final Class<DirectoryArchiverUtil> dirArchiverUtilClass) {
		Method targetMethod = null;
		for (Method candidate : dirArchiverUtilClass.getDeclaredMethods()) {
			if ("createArchiveOfDirectory".equals(candidate.getName())) {
				targetMethod = candidate;
				break;
			}
		}
		targetMethod.setAccessible(true);
		return targetMethod;
	}
	
	private void invokePrivateArchiverMethod (final Method privateArchiverMethod, final Object archiverProcessor)
			throws IOException, IllegalAccessException, Throwable {
		final File rootFolder = tempFolder.newFolder();
		final File tempDestinFile = tempFolder.newFile("archive-exception-test.archive");
		try {
			privateArchiverMethod.invoke(null, tempDestinFile.getAbsolutePath(), rootFolder, null, "DOES_NOT_EXIST",
					"UTF-8", archiverProcessor);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
}
