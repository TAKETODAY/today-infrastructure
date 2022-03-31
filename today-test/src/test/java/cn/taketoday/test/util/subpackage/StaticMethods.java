/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.util.subpackage;

/**
 * Simple class with static methods; intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class StaticMethods {

	public static String publicMethodValue = "public";

	private static String privateMethodValue = "private";


	public static void publicMethod(String value) {
		publicMethodValue = value;
	}

	public static String publicMethod() {
		return publicMethodValue;
	}

	@SuppressWarnings("unused")
	private static void privateMethod(String value) {
		privateMethodValue = value;
	}

	@SuppressWarnings("unused")
	private static String privateMethod() {
		return privateMethodValue;
	}

	public static void reset() {
		publicMethodValue = "public";
		privateMethodValue = "private";
	}

	public static String getPublicMethodValue() {
		return publicMethodValue;
	}

	public static String getPrivateMethodValue() {
		return privateMethodValue;
	}

}
