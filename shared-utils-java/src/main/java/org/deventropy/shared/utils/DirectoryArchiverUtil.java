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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class providing convenience methods to create Zip/Jar archives for entire directories.
 * 
 * <p>The functionality implemented in this class is rudimentary at this time, and does not support:
 * <ul>
 * 	<li>Compression Level</li>
 * 	<li>Compression Method</li>
 * 	<li>Advanced manifest manipulation (Jar files)</li>
 * 	<li>Jar signing</li>
 * 	<li>Filtering files out</li>
 * </ul>
 * 
 * <h2>Creating a Zip Archive</h2>
 * To create a zip archive from a directory <code>/project/data/source</code> into a file
 * <code>/project/data/source.zip</code>, use the {@link #createZipArchiveOfDirectory(String, File, String)} as:
 * <pre>
 * DirectoryArchiverUtil.createJarArchiveOfDirectory("/project/data/source", "/project/data/source.zip", null);
 * </pre>
 * 
 * <p>This will archive all the contents of the source folder recursively in the zip file created. Immediate children
 * of the source folder will be at the root of the zip file. The current implementation does not traverse down symbolic
 * links, and they will be excluded.
 * 
 * <h3>Nesting contents in the archive</h3>
 * The implementation supports nesting contents from the source one or more directories down in the archive file
 * created. Working on the example above, if the code were invoked as:
 * <pre>
 * DirectoryArchiverUtil.createJarArchiveOfDirectory("/project/data/source", "/project/data/source.zip", "test/one");
 * </pre>
 * 
 * <p>This will cause any files or directories which were immediate children of the source folder to appear nested under
 * directories <code>test/one</code> in the archive (or when the archive is inflated). So
 * <code>/project/data/source/file.txt</code> will appear at <code>test/one/file.txt</code> in the archive.
 * 
 * <h2>Creating a Jar Archive</h2>
 * Jar files are created almost identical to the Zip files above, with the additional functionality of a very
 * rudimentary Manifest (<code>META-INF/MANIFEST.MF</code>) file is added to the archive with just the Manifest Version
 * property set.
 * 
 * <p>To create Jar files, use the {@link #createJarArchiveOfDirectory(String, File, String)} method instead.
 * 
 * <p>This class uses ideas expressed by user Gili on a StackOverflow question:
 * <a href="http://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file">
 * How to use JarOutputStream to create a JAR file?</a>
 * 
 * @author Bindul Bhowmik
 */
public final class DirectoryArchiverUtil {
	
	private static final String DEFAULT_MANIFEST_VERSION = "1.0";
	private static final String ARCHIVE_PATH_SEPARATOR = "/";
	private static final String WIN_PATH_SEPARATOR = "\\";
	
	private static final Logger LOG = LogManager.getLogger(DirectoryArchiverUtil.class);

	private DirectoryArchiverUtil () {
		// Utility class
	}
	
	/**
	 * Create a zip archive with all the contents of the directory. Optionally push the contents down a directory level
	 * or two.
	 * 
	 * @param archiveFile The final archive file location. The file location must be writable.
	 * @param srcDirectory The source directory.
	 * @param rootPathPrefix The root prefix. Multiple directory parts should be separated by <code>/</code>.
	 * @throws IOException Exception reading the source directory or writing to the destination file.
	 */
	public static void createZipArchiveOfDirectory (final String archiveFile, final File srcDirectory,
		final String rootPathPrefix) throws IOException {

		createArchiveOfDirectory(archiveFile, srcDirectory, rootPathPrefix, false);
	}
	
	/**
	 * Create a Jar archive with all the contents of the directory. Optionally push the contents down a directory level
	 * or two. A Manifest file is automatically added.
	 * 
	 * @param archiveFile The final archive file location. The file location must be writable.
	 * @param srcDirectory The source directory.
	 * @param rootPathPrefix The root prefix. Multiple directory parts should be separated by <code>/</code>.
	 * @throws IOException Exception reading the source directory or writing to the destination file.
	 */
	public static void createJarArchiveOfDirectory (final String archiveFile, final File srcDirectory,
		final String rootPathPrefix) throws IOException {

		createArchiveOfDirectory(archiveFile, srcDirectory, rootPathPrefix, true);
	}
	
	private static void createArchiveOfDirectory (final String archiveFile, final File srcDirectory,
			final String rootPathPrefix, final boolean isJar) throws IOException {

			ZipOutputStream zos = null;
			try {

				final FileOutputStream archiveFileOutputStream = new FileOutputStream(archiveFile);
				if (isJar) {
					final Manifest manifest = new Manifest();
					manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, DEFAULT_MANIFEST_VERSION);
					zos = new JarOutputStream(archiveFileOutputStream, manifest);
				} else {
					zos = new ZipOutputStream(archiveFileOutputStream);
				}

				final String normalizedRootPathPrefix = (null == rootPathPrefix || rootPathPrefix.isEmpty()) ? ""
						: normalizeName(rootPathPrefix, true);
				if (!normalizedRootPathPrefix.isEmpty()) {
					final ZipEntry entry = (isJar) ? new JarEntry(normalizedRootPathPrefix)
							: new ZipEntry(normalizedRootPathPrefix);
					zos.putNextEntry(entry);
					zos.closeEntry();
				}

				final Path srcRootPath = Paths.get(srcDirectory.toURI());
				final ArchiverFileVisitor visitor = new ArchiverFileVisitor(srcRootPath, normalizedRootPathPrefix, zos,
						isJar);
				Files.walkFileTree(srcRootPath, visitor);

			} finally {
				if (null != zos) {
					zos.close();
				}
			}
		}

	private static String normalizeName (final String path, final boolean isDirectory) {
		String normalizedPath = path.replace(WIN_PATH_SEPARATOR, ARCHIVE_PATH_SEPARATOR);
		if (isDirectory && !normalizedPath.endsWith(ARCHIVE_PATH_SEPARATOR)) {
			normalizedPath += ARCHIVE_PATH_SEPARATOR;
		}
		return normalizedPath;
	}
	
	private static final class ArchiverFileVisitor extends SimpleFileVisitor<Path> {
		private final Path sourceRootPath;
		private final String normalizedRootPathPrefix;
		private final ZipOutputStream zipOutputStream;
		private final boolean isJar;

		private ArchiverFileVisitor (final Path sourceRootPath, final String normalizedRootPathPrefix,
				final ZipOutputStream zipOutputStream, final boolean isJar) {
			this.normalizedRootPathPrefix = normalizedRootPathPrefix;
			this.sourceRootPath = sourceRootPath;
			this.zipOutputStream = zipOutputStream;
			this.isJar = isJar;
		}

		/* (non-Javadoc)
		 * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object,
		 * 		java.nio.file.attribute.BasicFileAttributes)
		 */
		@Override
		public FileVisitResult preVisitDirectory (final Path dir, final BasicFileAttributes attrs) throws IOException {

			// Create a zip entry for the directory
			final Path relativeSourcePath = sourceRootPath.relativize(dir);
			if (null == relativeSourcePath || relativeSourcePath.toString().isEmpty()) {
				// Special case for the root
				return FileVisitResult.CONTINUE;
			}

			final String relativeDestinationPath = normalizeName(normalizedRootPathPrefix + relativeSourcePath, true);
			LOG.trace("Creating zip / jar entry for directory {} at {}", dir, relativeDestinationPath);

			final ZipEntry zipEntry = (isJar) ? new JarEntry(relativeDestinationPath)
					: new ZipEntry(relativeDestinationPath);
			zipEntry.setTime(dir.toFile().lastModified());
			zipOutputStream.putNextEntry(zipEntry);
			zipOutputStream.closeEntry();

			return FileVisitResult.CONTINUE;
		}

		/* (non-Javadoc)
		 * @see java.nio.file.FileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
		 */
		@Override
		public FileVisitResult visitFile (final Path file, final BasicFileAttributes attrs) throws IOException {

			// Add the file to the zip
			final Path relativeSourcePath = sourceRootPath.relativize(file);
			final String relativeDestinationPath = normalizeName(normalizedRootPathPrefix + relativeSourcePath, false);
			LOG.trace("Creating zip / jar entry for file {} at {}", file, relativeDestinationPath);

			final ZipEntry zipEntry = (isJar) ? new JarEntry(relativeDestinationPath)
					: new ZipEntry(relativeDestinationPath);
			zipEntry.setTime(file.toFile().lastModified());
			zipOutputStream.putNextEntry(zipEntry);
			Files.copy(file, zipOutputStream);
			zipOutputStream.closeEntry();

			return FileVisitResult.CONTINUE;
		}
	}
}
