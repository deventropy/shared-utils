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

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests creating zip and jar files.
 * 
 * @author Bindul Bhowmik
 */
public class DirectoryArchiverUtilTest {
	
	private static final String MANIFEST_FILE_ENTRY_NAME = "META-INF/MANIFEST.MF";
	private static final int MAX_FILE_SIZE = 8 * 1024; // 8K
	
	private static final String ZIP_FILE_SUFFIX = ".zip";
	private static final String JAR_FILE_SUFFIX = ".jar";
	private static final String TAR_FILE_SUFFIX = ".tar";
	private static final String TAR_GZ_FILE_SUFFIX = ".tar.gz";
	
	private static final String VALID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-=+_{}|"
			+ "\\;:'\"/?.>,<~!@#$%^&*()~` \n";
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	
	private final String[] testFileStructure01 = new String[] {
		"temp/",
		"temp/test1/",
		"temp/test1/file1.txt",
		"temp/test2/file1.txt",
		"temp/test2/file2.bin",
		"tmp/test3/test4/test5/file3.bin",
		"tmp/test3/test4/test5/file3.txt"
	};
	
	private final Random random = new Random();
	
	@Test
	public void testZipArchiveNullPrefix () throws IOException {
		final String archiveFilePath = testZipArchive(null, testFileStructure01);
		assertTrue("Should test as .zip file (testZipArchiveNullPrefix)", archiveFilePath.endsWith(ZIP_FILE_SUFFIX));
	}
	
	@Test
	public void testZipArchiveEmptyPrefix () throws IOException {
		final String archiveFilePath = testZipArchive("", testFileStructure01);
		assertTrue("Should test as .zip file (testZipArchiveEmptyPrefix)", archiveFilePath.endsWith(ZIP_FILE_SUFFIX));
	}
	
	@Test
	public void testZipArchivePrefix () throws IOException {
		final String archiveFilePath = testZipArchive("prefix/path", testFileStructure01);
		assertTrue("Should test as .zip file (testZipArchivePrefix)", archiveFilePath.endsWith(ZIP_FILE_SUFFIX));
	}
	
	@Test
	public void testZipArchiveWinPrefix () throws IOException {
		final String archiveFilePath = testZipArchive("prefix\\path\\win", testFileStructure01);
		assertTrue("Should test as .zip file (testZipArchiveWinPrefix)", archiveFilePath.endsWith(ZIP_FILE_SUFFIX));
	}
	
	private String testZipArchive (final String prefix, final String[] fileStructure) throws IOException {
		final File rootFolder = tempFolder.newFolder();
		createDirectoryTree(rootFolder, fileStructure);
		final String testArchiveName = "archive-test-" + random.nextInt() + ZIP_FILE_SUFFIX;
		final File archiveFile = tempFolder.newFile(testArchiveName);
		DirectoryArchiverUtil.createZipArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, prefix);
		assertTrue("Zip file should not be zero sized", archiveFile.length() > 0);
		checkZipArchive(archiveFile, rootFolder, prefix);
		return archiveFile.getPath();
	}
	
	private void checkZipArchive (final File archiveFile, final File sourceDirectory, final String pathPrefix)
			throws IOException {

		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(archiveFile);
			final ArchiveEntries archiveEntries = createArchiveEntries(sourceDirectory, pathPrefix);
	
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry ze = entries.nextElement();
				if (ze.isDirectory()) {
					assertTrue("Directory in zip should be from us [" + ze.getName() + "]",
							archiveEntries.dirs.contains(ze.getName()));
					archiveEntries.dirs.remove(ze.getName());
				} else {
					assertTrue("File in zip should be from us [" + ze.getName() + "]",
							archiveEntries.files.containsKey(ze.getName()));
					final byte[] inflatedMd5 = getMd5Digest(zipFile.getInputStream(ze), true);
					assertArrayEquals("MD5 hash of files should equal [" + ze.getName() + "]",
							archiveEntries.files.get(ze.getName()), inflatedMd5);
					archiveEntries.files.remove(ze.getName());
				}
			}
	
			// Check that all files and directories have been accounted for
			assertTrue("All directories should be in the zip", archiveEntries.dirs.isEmpty());
			assertTrue("All files should be in the zip", archiveEntries.files.isEmpty());
		} finally {
			if (null != zipFile) {
				zipFile.close();
			}
		}
	}
	
	@Test
	public void testJarArchiveNullPrefix () throws IOException {
		final String archiveFilePath = testJarArchive(null, testFileStructure01);
		assertTrue("Should test as .jar file (testJarArchiveNullPrefix)", archiveFilePath.endsWith(JAR_FILE_SUFFIX));
	}
	
	@Test
	public void testJarArchiveEmptyPrefix () throws IOException {
		final String archiveFilePath = testJarArchive("", testFileStructure01);
		assertTrue("Should test as .jar file (testJarArchiveEmptyPrefix)", archiveFilePath.endsWith(JAR_FILE_SUFFIX));
	}
	
	@Test
	public void testJarArchivePrefix () throws IOException {
		final String archiveFilePath = testJarArchive("prefix/path", testFileStructure01);
		assertTrue("Should test as .jar file (testJarArchivePrefix)", archiveFilePath.endsWith(JAR_FILE_SUFFIX));
	}
	
	@Test
	public void testJarArchiveWinPrefix () throws IOException {
		final String archiveFilePath = testJarArchive("prefix\\path\\win", testFileStructure01);
		assertTrue("Should test as .jar file (testJarArchiveWinPrefix)", archiveFilePath.endsWith(JAR_FILE_SUFFIX));
	}
	
	private String testJarArchive (final String prefix, final String[] fileStructure) throws IOException {
		final File rootFolder = tempFolder.newFolder();
		createDirectoryTree(rootFolder, fileStructure);
		final String testArchiveName = "archive-test-" + random.nextInt() + JAR_FILE_SUFFIX;
		final File archiveFile = tempFolder.newFile(testArchiveName);
		DirectoryArchiverUtil.createJarArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, prefix);
		assertTrue("Jar file should not be zero sized", archiveFile.length() > 0);
		checkJarArchive(archiveFile, rootFolder, prefix);
		return archiveFile.getPath();
	}
	
	private void checkJarArchive (final File archiveFile, final File sourceDirectory, final String pathPrefix)
			throws IOException {

		JarFile jarFile = null;
		try {
			jarFile = new JarFile(archiveFile);

			final Manifest manifest = jarFile.getManifest();
			assertNotNull("Manifest should be present", manifest);
			assertEquals("Manifest version should be 1.0", "1.0",
					manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION));

			final ArchiveEntries archiveEntries = createArchiveEntries(sourceDirectory, pathPrefix);
	
			final Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {
				final JarEntry jarEntry = entries.nextElement();
				if (MANIFEST_FILE_ENTRY_NAME.equalsIgnoreCase(jarEntry.getName())) {
					// It is the manifest file, not added by use
					continue;
				}
				if (jarEntry.isDirectory()) {
					assertTrue("Directory in jar should be from us [" + jarEntry.getName() + "]",
							archiveEntries.dirs.contains(jarEntry.getName()));
					archiveEntries.dirs.remove(jarEntry.getName());
				} else {
					assertTrue("File in jar should be from us [" + jarEntry.getName() + "]",
							archiveEntries.files.containsKey(jarEntry.getName()));
					final byte[] inflatedMd5 = getMd5Digest(jarFile.getInputStream(jarEntry), false);
					assertArrayEquals("MD5 hash of files should equal [" + jarEntry.getName() + "]",
							archiveEntries.files.get(jarEntry.getName()), inflatedMd5);
					archiveEntries.files.remove(jarEntry.getName());
				}
			}
	
			// Check that all files and directories have been accounted for
			assertTrue("All directories should be in the jar", archiveEntries.dirs.isEmpty());
			assertTrue("All files should be in the jar", archiveEntries.files.isEmpty());
		} finally {
			if (null != jarFile) {
				jarFile.close();
			}
		}
	}
	
	@Test
	public void testTarArchiveNullPrefix () throws IOException {
		final String archiveFilePath = testTarArchive(null, testFileStructure01);
		assertTrue("Should test as .tar file (testZipArchiveNullPrefix)", archiveFilePath.endsWith(TAR_FILE_SUFFIX));
	}
	
	@Test
	public void testTarArchiveEmptyPrefix () throws IOException {
		final String archiveFilePath = testTarArchive("", testFileStructure01);
		assertTrue("Should test as .tar file (testZipArchiveEmptyPrefix)", archiveFilePath.endsWith(TAR_FILE_SUFFIX));
	}
	
	@Test
	public void testTarArchivePrefix () throws IOException {
		final String archiveFilePath = testTarArchive("prefix/path", testFileStructure01);
		assertTrue("Should test as .tar file (testZipArchivePrefix)", archiveFilePath.endsWith(TAR_FILE_SUFFIX));
	}
	
	@Test
	public void testTarArchiveWinPrefix () throws IOException {
		final String archiveFilePath = testTarArchive("prefix\\path\\win", testFileStructure01);
		assertTrue("Should test as .tar file (testZipArchiveWinPrefix)", archiveFilePath.endsWith(TAR_FILE_SUFFIX));
	}
	
	private String testTarArchive (final String prefix, final String[] fileStructure) throws IOException {
		final File rootFolder = tempFolder.newFolder();
		createDirectoryTree(rootFolder, fileStructure);
		final String testArchiveName = "archive-test-" + random.nextInt() + TAR_FILE_SUFFIX;
		final File archiveFile = tempFolder.newFile(testArchiveName);
		DirectoryArchiverUtil.createTarArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, prefix);
		assertTrue("Tar file should not be zero sized", archiveFile.length() > 0);
		checkTarArchive(archiveFile, rootFolder, prefix);
		return archiveFile.getPath();
	}
	
	private void checkTarArchive (final File archiveFile, final File sourceDirectory, final String pathPrefix)
			throws IOException {

		ArchiveInputStream tarInputStream = null;
		try {
			tarInputStream = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.TAR,
					new BufferedInputStream(new FileInputStream(archiveFile)));
			final ArchiveEntries archiveEntries = createArchiveEntries(sourceDirectory, pathPrefix);
	
			ArchiveEntry tarEntry = null;
			while ((tarEntry = tarInputStream.getNextEntry()) != null) {
				if (tarEntry.isDirectory()) {
					assertTrue("Directory in tar should be from us [" + tarEntry.getName() + "]",
							archiveEntries.dirs.contains(tarEntry.getName()));
					archiveEntries.dirs.remove(tarEntry.getName());
				} else {
					assertTrue("File in tar should be from us [" + tarEntry.getName() + "]",
							archiveEntries.files.containsKey(tarEntry.getName()));
					final byte[] inflatedMd5 = getMd5Digest(new BoundedInputStream(tarInputStream, tarEntry.getSize()),
							false);
					assertArrayEquals("MD5 hash of files should equal [" + tarEntry.getName() + "]",
							archiveEntries.files.get(tarEntry.getName()), inflatedMd5);
					archiveEntries.files.remove(tarEntry.getName());
				}
			}
	
			// Check that all files and directories have been accounted for
			assertTrue("All directories should be in the tar", archiveEntries.dirs.isEmpty());
			assertTrue("All files should be in the tar", archiveEntries.files.isEmpty());
		} catch (ArchiveException e) {
			throw new IOException(e);
		} finally {
			if (null != tarInputStream) {
				tarInputStream.close();
			}
		}
	}
	
	@Test
	public void testTarGzArchiveNullPrefix () throws IOException {
		final String archiveFilePath = testTarGzArchive(null, testFileStructure01);
		assertTrue("Should test as .tar.gz file (testZipArchiveNullPrefix)",
				archiveFilePath.endsWith(TAR_GZ_FILE_SUFFIX));
	}
	
	@Test
	public void testTarGzArchiveEmptyPrefix () throws IOException {
		final String archiveFilePath = testTarGzArchive("", testFileStructure01);
		assertTrue("Should test as .tar.gz file (testZipArchiveEmptyPrefix)",
				archiveFilePath.endsWith(TAR_GZ_FILE_SUFFIX));
	}
	
	@Test
	public void testTarGzArchivePrefix () throws IOException {
		final String archiveFilePath = testTarGzArchive("prefix/path", testFileStructure01);
		assertTrue("Should test as .tar.gz file (testZipArchivePrefix)", archiveFilePath.endsWith(TAR_GZ_FILE_SUFFIX));
	}
	
	@Test
	public void testTarGzArchiveWinPrefix () throws IOException {
		final String archiveFilePath = testTarGzArchive("prefix\\path\\win", testFileStructure01);
		assertTrue("Should test as .tar.gz file (testZipArchiveWinPrefix)",
				archiveFilePath.endsWith(TAR_GZ_FILE_SUFFIX));
	}
	
	private String testTarGzArchive (final String prefix, final String[] fileStructure) throws IOException {
		final File rootFolder = tempFolder.newFolder();
		createDirectoryTree(rootFolder, fileStructure);
		final String testArchiveName = "archive-test-" + random.nextInt() + TAR_GZ_FILE_SUFFIX;
		final File archiveFile = tempFolder.newFile(testArchiveName);
		DirectoryArchiverUtil.createGZippedTarArchiveOfDirectory(archiveFile.getAbsolutePath(), rootFolder, prefix);
		assertTrue("Tar GZ file should not be zero sized", archiveFile.length() > 0);
		checkTarGzArchive(archiveFile, rootFolder, prefix);
		return archiveFile.getPath();
	}
	
	private void checkTarGzArchive (final File archiveFile, final File sourceDirectory, final String pathPrefix)
			throws IOException {

		FileInputStream fin = null;
		CompressorInputStream gzIn = null;
		FileOutputStream out = null;

		final File unGzippedTar = tempFolder.newFile("archive-test-" + random.nextInt() + TAR_FILE_SUFFIX);

		try {
			fin = new FileInputStream(archiveFile);
			final BufferedInputStream in = new BufferedInputStream(fin);
			out = new FileOutputStream(unGzippedTar);
			gzIn = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, in);

			IOUtils.copy(gzIn, out);

		} catch (CompressorException e) {
			throw new IOException(e);
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(gzIn);
			IOUtils.closeQuietly(fin);
		}

		checkTarArchive(unGzippedTar, sourceDirectory, pathPrefix);
	}

	private ArchiveEntries createArchiveEntries (final File sourceDirectory, final String pathPrefix)
			throws IOException {
		final ArchiveEntries archiveEntries = new ArchiveEntries();
		final Path sourcePath = Paths.get(sourceDirectory.toURI());
		final StringBuilder normalizedPathPrefix = new StringBuilder();
		if (null != pathPrefix && !pathPrefix.isEmpty()) {
			normalizedPathPrefix.append(pathPrefix.replace("\\", "/"));
			if (normalizedPathPrefix.charAt(normalizedPathPrefix.length() - 1) != '/') {
				normalizedPathPrefix.append('/');
			}
		}

		Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path> () {

			@Override
			public FileVisitResult preVisitDirectory (final Path dir, final BasicFileAttributes attrs)
					throws IOException {
				final Path relativeSourcePath = sourcePath.relativize(dir);
				String normalizedPath = normalizedPathPrefix.toString() + relativeSourcePath;
				if (!normalizedPath.isEmpty()) {
					if (!normalizedPath.endsWith("/")) {
						normalizedPath += "/";
					}
					archiveEntries.dirs.add(normalizedPath.replace("\\", "/"));
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile (final Path file, final BasicFileAttributes attrs) throws IOException {
				final Path relativeSourcePath = sourcePath.relativize(file);
				final String normalizedPath = normalizedPathPrefix.toString() + relativeSourcePath;
				final byte[] md5Digest = getMd5Digest(Files.newInputStream(file), true);
				archiveEntries.files.put(normalizedPath.replace("\\", "/"), md5Digest);
				return FileVisitResult.CONTINUE;
			}
		});
		return archiveEntries;
	}

	private void createDirectoryTree (final File parentDir, final String... fileEntries) throws IOException {
		final String parentPath = parentDir.getAbsolutePath();

		for (String fileEntry : fileEntries) {
			final File entry = new File (parentPath + "/" + fileEntry);
			if (fileEntry.endsWith("/")) {
				ensureDirectoryExists(entry);
			} else {
				// Create file
				ensureDirectoryExists(entry.getParentFile());
				if (!entry.createNewFile()) {
					throw new IOException("Error creating file : " + entry.getAbsolutePath());
				}
				if (fileEntry.endsWith(".txt")) {
					FileWriter fileWriter = null;
					try {
						fileWriter = new FileWriter(entry);
						// Write random characters
						final int fileSize = random.nextInt(MAX_FILE_SIZE);
						for (int i = 0; i < fileSize; i++) {
							fileWriter.write((char) (VALID_CHARS.charAt(random.nextInt(VALID_CHARS.length()))));
						}
					} finally {
						if (null != fileWriter) {
							try {
								fileWriter.flush();
							} catch (IOException e) {
								// Ignore
							}
							try {
								fileWriter.close();
							} catch (IOException e) {
								// Ignore
							}
						}
					}
				} else {
					FileOutputStream fos = null;
					try {
						fos = new FileOutputStream(entry);
						// Write random bytes
						final byte[] buf = new byte[random.nextInt(MAX_FILE_SIZE)];
						random.nextBytes(buf);
						fos.write(buf);
					} finally {
						if (null != fos) {
							try {
								fos.flush();
							} catch (IOException e) {
								// Ignore
							}
							try {
								fos.close();
							} catch (IOException e) {
								// Ignore
							}
						}
					}
				}
			}
		}
	}
	
	private byte[] getMd5Digest (final InputStream inputStream, final boolean closeStream) throws IOException {
		try {
			final MessageDigest md = MessageDigest.getInstance("MD5");
			final DigestInputStream dis = new DigestInputStream(inputStream, md);

			final byte[] buf = new byte[MAX_FILE_SIZE];
			int len = 0;
			while (len != -1) {
				len = dis.read(buf);
			}
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		} finally {
			if (closeStream && null != inputStream) {
				inputStream.close();
			}
		}
	}

	private void ensureDirectoryExists (final File directory) throws IOException {
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				throw new IOException("Error creating file: " + directory.getAbsolutePath());
			}
		} else if (!directory.isDirectory()) {
			throw new IOException("Requested directory does not exist: " + directory.getAbsolutePath());
		}
	}
	
	private class ArchiveEntries {
		private Set<String> dirs = new HashSet<>();
		private Map<String, byte[]> files = new HashMap<>();
	}
}
