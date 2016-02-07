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

/**
 * Utility methods to check arguments for validity.
 * 
 * @author Bindul Bhowmik
 */
public final class ArgumentCheck {
	
	private ArgumentCheck () {
		// Utility class
	}

	/**
	 * Checks to make sure a string is not null or empty.
	 * Throws <code>IllegalArgumentException</code> if expectation is not met.
	 * @param arg Argument to check
	 * @param name Name of the argument
	 * @throws IllegalArgumentException if expectation is not met
	 */
	public static void notNullOrEmpty (final String arg, final String name) {
		if (null == arg || arg.isEmpty() || arg.trim().isEmpty()) {
			throw new IllegalArgumentException (name + " is required and cannot be null or empty");
		}
	}
	
	/**
	 * Checks to make sure the argument object is not null.
	 * @param arg The argument to check
	 * @param name The name of the argument
	 * @param <T> The type of the object
	 * @throws IllegalArgumentException if expectation is not met (<code>arg</code> is null).
	 */
	public static <T> void notNull (final T arg, final String name) {
		if (null == arg) {
			throw new IllegalArgumentException (name + " is required and cannot be null");
		}
	}
}
