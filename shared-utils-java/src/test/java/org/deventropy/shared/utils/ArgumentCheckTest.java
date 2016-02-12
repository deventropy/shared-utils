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

import org.junit.Test;

/**
 * @author Bindul Bhowmik
 *
 */
public class ArgumentCheckTest {

	@Test
	public void testNotNull () {
		try {
			ArgumentCheck.notNull (new Object (), "test");
		} catch (IllegalArgumentException e) {
			fail ("Should not have an exception here.");
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNotNullFail () {
		try {
			ArgumentCheck.notNull (null, "test");
		} catch (IllegalArgumentException e) {
			assertEquals ("test is required and cannot be null", e.getMessage ());
			throw e;
		}
	}
	
	@Test
	public void testNotNullOrEmpty () {
		try {
			ArgumentCheck.notNullOrEmpty ("test", "test");
		} catch (IllegalArgumentException e) {
			fail ("Should not have an exception here.");
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNotNullOrEmptyFail () {
		try {
			ArgumentCheck.notNullOrEmpty ("", "test");
		} catch (IllegalArgumentException e) {
			assertEquals ("test is required and cannot be null or empty", e.getMessage ());
			throw e;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotNullOrEmptyFailWhitespace () {
		try {
			ArgumentCheck.notNullOrEmpty (" ", "test");
		} catch (IllegalArgumentException e) {
			assertEquals ("test is required and cannot be null or empty", e.getMessage ());
			throw e;
		}
	}
}
