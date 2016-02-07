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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Bindul Bhowmik
 *
 */
public class ClassUtilTest {
	
	@Rule
	public TemporaryFolder workingFolder = new TemporaryFolder();

	/**
	 * Test method for {@link org.deventropy.shared.utils.ClassUtil#getApplicableClassloader(java.lang.Object)}.
	 * @throws InterruptedException Error running worker
	 */
	@Test
	public void testGetApplicableClassloaderNullContextClassLoader () throws InterruptedException {

		final ContextClNullingObject testObj = new ContextClNullingObject(null);
		final Thread worker = new Thread(testObj);
		worker.start();
		worker.join();

		assertEquals(ClassUtil.class.getClassLoader(), testObj.getReturnedCl());
	}
	
	/**
	 * Test method for {@link org.deventropy.shared.utils.ClassUtil#getApplicableClassloader(java.lang.Object)}.
	 * @throws InterruptedException Error running worker
	 */
	@Test
	public void testGetApplicableClassloaderSecurityException () throws InterruptedException {

		final ClassLoaderGetterRunnable testRunner = new ClassLoaderGetterRunnable(null);
		final NoContextClassLoaderThread worker = new NoContextClassLoaderThread(testRunner);
//		final Thread worker = new Thread(testRunner);
		worker.start();
		worker.join();

		assertEquals(ClassUtil.class.getClassLoader(), testRunner.getReturnedCl());
	}
	
	@Test
	public void testCallerClassloader () throws Exception {

		// Write the source file - see
		// http://stackoverflow.com/questions/2946338/how-do-i-programmatically-compile-and-instantiate-a-java-class
		final File sourceFolder = workingFolder.newFolder();
		// Prepare source somehow.
		final String source = "package test; public class Test { }";
		// Save source in .java file.
		final File sourceFile = new File(sourceFolder, "test/Test.java");
		sourceFile.getParentFile().mkdirs();
		FileUtils.writeStringToFile(sourceFile, source, "UTF-8");

		// Compile the file
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		final Iterable<? extends JavaFileObject> compilationUnit = fileManager.getJavaFileObjectsFromFiles(
				Arrays.asList(sourceFile));
		compiler.getTask(null, fileManager, null, null, null, compilationUnit).call();

		// Create the class loader
		final URLClassLoader urlcl = new URLClassLoader(new URL[] {sourceFolder.toURI().toURL()});
		final Class<?> loadedClass = urlcl.loadClass("test.Test");

		final ContextClNullingObject testObj = new ContextClNullingObject(loadedClass.newInstance());
		final Thread worker = new Thread(testObj);
		worker.start();
		worker.join();

		assertEquals(urlcl, testObj.getReturnedCl());

		urlcl.close();
	}
	
	/**
	 * Worker runnable
	 * 
	 * @author Bindul Bhowmik
	 */
	private class ClassLoaderGetterRunnable implements Runnable {
		private ClassLoader returnedCl;
		private Object callingObj;

		protected ClassLoaderGetterRunnable (final Object callingObj) {
			this.callingObj = callingObj;
		}

		@Override
		public void run () {
			returnedCl = ClassUtil.getApplicableClassloader(callingObj);
		}

		public ClassLoader getReturnedCl () {
			return returnedCl;
		}
	}

	/**
	 * Worker object.
	 * 
	 * @author Bindul Bhowmik
	 */
	private final class ContextClNullingObject extends ClassLoaderGetterRunnable {

		private ContextClNullingObject (final Object callingObj) {
			super (callingObj);
		}

		@Override
		public void run () {
			final Thread currentThread = Thread.currentThread();
			final ClassLoader savedCl = currentThread.getContextClassLoader();

			currentThread.setContextClassLoader(null);
			super.run();

			if (null != savedCl) {
				currentThread.setContextClassLoader(savedCl);
			}
		}
	}
	
	/**
	 * Thread that denies access to the context class loader
	 * @author Bindul Bhowmik
	 */
	private final class NoContextClassLoaderThread extends Thread {

		NoContextClassLoaderThread (final Runnable target) {
			super (target);
		}

		@Override
		public ClassLoader getContextClassLoader () {
			final StackTraceElement[] stackTraces  = getStackTrace();
			if ("org.deventropy.shared.utils.ClassUtil".equals(stackTraces[2].getClassName())
					&& "getApplicableClassloader".equals(stackTraces[2].getMethodName())) {
				// Log4J (among others) also calls this method, and if a security exception is thrown the tests fail;
				// so fail the test only when called from the test method
				throw new SecurityException();
			}
			return super.getContextClassLoader();
		}
	}
}
