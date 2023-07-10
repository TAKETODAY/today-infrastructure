/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		assertEnvValue("ENV1", "5000");
		assertEnvValue("ENV2", "Some Text");
		assertEnvValue("ENV3", "");
		assertEnvValue("ENV4", "");

		System.out.println("I haz been run");
	}

	private static void assertEnvValue(String envKey, String expectedValue) {
		String actual = System.getenv(envKey);
		if (!expectedValue.equals(actual)) {
			throw new IllegalStateException("env property [" + envKey + "] mismatch "
					+ "(got [" + actual + "], expected [" + expectedValue + "]");
		}
	}

}
