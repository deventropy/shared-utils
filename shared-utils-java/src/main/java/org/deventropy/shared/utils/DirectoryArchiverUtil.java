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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.Charsets;
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
	private static final String UTF_8_NAME = Charsets.UTF_8.name();
	
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

		createArchiveOfDirectory(archiveFile, srcDirectory, rootPathPrefix, ArchiveStreamFactory.ZIP, UTF_8_NAME, null);
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

		createArchiveOfDirectory(archiveFile, srcDirectory, rootPathPrefix, ArchiveStreamFactory.JAR, UTF_8_NAME,
				new JarArchiverCreateProcessor());
	}
	
	/**
	 * Create a tar archive with all the contents of the directory. Optionally push the contents down a directory level
	 * or two.
	 * 
	 * @param archiveFile The final archive file location. The file location must be writable.
	 * @param srcDirectory The source directory.
	 * @param rootPathPrefix The root prefix. Multiple directory parts should be separated by <code>/</code>.
	 * @throws IOException Exception reading the source directory or writing to the destination file.
	 */
	public static void createTarArchiveOfDirectory (final String archiveFile, final File srcDirectory,
		final String rootPathPrefix) throws IOException {

		createArchiveOfDirectory(archiveFile, srcDirectory, rootPathPrefix, ArchiveStreamFactory.TAR, null,
				new TarArchiverCreateProcessor(null));
	}
	
	/**
	 * Create a GZipped tar archive with all the contents of the directory. Optionally push the contents down a
	 * directory level or two.
	 * 
	 * @param archiveFile The final archive file location. The file location must be writable.
	 * @param srcDirectory The source directory.
	 * @param rootPathPrefix The root prefix. Multiple directory parts should be separated by <code>/</code>.
	 * @throws IOException Exception reading the source directory or writing to the destination file.
	 */
	public static void createGZippedTarArchiveOfDirectory (final String archiveFile, final File srcDirectory,
		final String rootPathPrefix) throws IOException {

		createArchiveOfDirectory(archiveFile, srcDirectory, rootPathPrefix, ArchiveStreamFactory.TAR, null,
				new TarArchiverCreateProcessor(CompressorStreamFactory.GZIP));
	}
	
	private static void createArchiveOfDirectory (final String archiveFile, final File srcDirectory,
			final String rootPathPrefix, final String archiveStreamFactoryConstant, final String encoding,
			final ArchiverCreateProcessor archiverCreateProcessorIn) throws IOException {

		/*
		 * NOTE ON CHARSET ENCODING: Traditionally the ZIP archive format uses CodePage 437 as encoding for file name,
		 * which is not sufficient for many international character sets.
		 * Over time different archivers have chosen different ways to work around the limitation - the java.util.zip
		 * packages simply uses UTF-8 as its encoding for example.
		 * Ant has been offering the encoding attribute of the zip and unzip task as a way to explicitly specify the
		 * encoding to use (or expect) since Ant 1.4. It defaults to the platform's default encoding for zip and UTF-8
		 * for jar and other jar-like tasks (war, ear, ...) as well as the unzip family of tasks.
		 */
		final ArchiverCreateProcessor archiveCreateProcessor = (null != archiverCreateProcessorIn)
				? archiverCreateProcessorIn : new ArchiverCreateProcessor();
		ArchiveOutputStream aos = null;
		try {

			final ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory(encoding);
			final FileOutputStream archiveFileOutputStream = new FileOutputStream(archiveFile);
			final OutputStream decoratedArchiveFileOutputStream = archiveCreateProcessor
					.decorateFileOutputStream(archiveFileOutputStream);
			aos = archiveStreamFactory.createArchiveOutputStream(archiveStreamFactoryConstant,
					decoratedArchiveFileOutputStream);
			archiveCreateProcessor.processArchiverPostCreate(aos, encoding);

			final String normalizedRootPathPrefix = (null == rootPathPrefix || rootPathPrefix.isEmpty()) ? ""
					: normalizeName(rootPathPrefix, true);
			if (!normalizedRootPathPrefix.isEmpty()) {
				final ArchiveEntry archiveEntry = aos.createArchiveEntry(srcDirectory, normalizedRootPathPrefix);
				aos.putArchiveEntry(archiveEntry);
				aos.closeArchiveEntry();
			}

			final Path srcRootPath = Paths.get(srcDirectory.toURI());
			final ArchiverFileVisitor visitor = new ArchiverFileVisitor(srcRootPath, normalizedRootPathPrefix, aos);
			Files.walkFileTree(srcRootPath, visitor);

			aos.flush();
		} catch (ArchiveException e) {
			throw new IOException("Error creating archive", e);
		} finally {
			if (null != aos) {
				aos.close();
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
		private final ArchiveOutputStream archiveOutputStream;

		private ArchiverFileVisitor (final Path sourceRootPath, final String normalizedRootPathPrefix,
				final ArchiveOutputStream archiveOutputStream) {
			this.normalizedRootPathPrefix = normalizedRootPathPrefix;
			this.sourceRootPath = sourceRootPath;
			this.archiveOutputStream = archiveOutputStream;
		}

		/* (non-Javadoc)
		 * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object,
		 * 		java.nio.file.attribute.BasicFileAttributes)
		 */
		@Override
		public FileVisitResult preVisitDirectory (final Path dir, final BasicFileAttributes attrs) throws IOException {

			// Create a zip entry for the directory
			final Path relativeSourcePath = sourceRootPath.relativize(dir);
			if (relativeSourcePath.toString().isEmpty()) { // Per documentation in Path, the relative path is not NULL
				// Special case for the root
				return FileVisitResult.CONTINUE;
			}

			final String relativeDestinationPath = normalizeName(normalizedRootPathPrefix + relativeSourcePath, true);
			LOG.trace("Creating zip / jar entry for directory {} at {}", dir, relativeDestinationPath);

			final ArchiveEntry archiveEntry = archiveOutputStream.createArchiveEntry(dir.toFile(),
					relativeDestinationPath);
			archiveOutputStream.putArchiveEntry(archiveEntry);
			archiveOutputStream.closeArchiveEntry();

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

			final ArchiveEntry archiveEntry = archiveOutputStream.createArchiveEntry(file.toFile(),
					relativeDestinationPath);
			archiveOutputStream.putArchiveEntry(archiveEntry);
			Files.copy(file, archiveOutputStream);
			archiveOutputStream.closeArchiveEntry();

			return FileVisitResult.CONTINUE;
		}
	}
	
	private static class ArchiverCreateProcessor {

		protected OutputStream decorateFileOutputStream (final FileOutputStream archiveFileOutputStream)
				throws IOException {
			return archiveFileOutputStream;
		}

		protected void processArchiverPostCreate (final ArchiveOutputStream archiveOutputStream, final String charset)
				throws IOException {
			// Default implementation does nothing.
		}
	}
	
	private static class JarArchiverCreateProcessor extends ArchiverCreateProcessor {

		@Override
		protected void processArchiverPostCreate (final ArchiveOutputStream archiveOutputStream, final String charset)
				throws IOException {
			super.processArchiverPostCreate(archiveOutputStream, charset);

			// Wrute the Jar Manifest file META-INF/MANIFEST.MF
			archiveOutputStream.putArchiveEntry(new JarArchiveEntry(JarFile.MANIFEST_NAME));
			final Manifest manifest = new Manifest();
			// Manifest-Version: 1.0
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, DEFAULT_MANIFEST_VERSION);
			manifest.write(new BufferedOutputStream(archiveOutputStream));
			archiveOutputStream.closeArchiveEntry();
		}
	}
	
	private static class TarArchiverCreateProcessor extends ArchiverCreateProcessor {

		private final String compressor;

		protected TarArchiverCreateProcessor (final String compressor) {
			this.compressor = compressor;
		}

		@Override
		protected OutputStream decorateFileOutputStream (final FileOutputStream archiveFileOutputStream)
				throws IOException {
			OutputStream returnStream = super.decorateFileOutputStream(archiveFileOutputStream);

			if (null != compressor) {
				try {
					returnStream = new CompressorStreamFactory().createCompressorOutputStream(compressor,
							new BufferedOutputStream(archiveFileOutputStream));
				} catch (CompressorException e) {
					throw new IOException("Error wrapping the file into a Compressed stream", e);
				}
			}

			return returnStream;
		}

		@Override
		protected void processArchiverPostCreate (final ArchiveOutputStream archiveOutputStream, final String charset)
				throws IOException {
			super.processArchiverPostCreate(archiveOutputStream, charset);
			final TarArchiveOutputStream tarArchiveOutputStream = (TarArchiveOutputStream) archiveOutputStream;
			tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarArchiveOutputStream.setAddPaxHeadersForNonAsciiNames(true);
		}
	}
}
