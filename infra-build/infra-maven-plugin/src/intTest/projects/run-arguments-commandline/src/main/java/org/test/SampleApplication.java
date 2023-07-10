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

import java.util.Arrays;

public class SampleApplication {

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("Missing arguments " + Arrays.toString(args));
		}
		if (!args[0].startsWith("--management.endpoints.web.exposure.include=")) {
			throw new IllegalArgumentException("Invalid argument " + args[0]);
		}
		if (!args[1].startsWith("--infra.profiles.active=")) {
			throw new IllegalArgumentException("Invalid argument " + args[1]);
		}
		String endpoints = args[0].split("=")[1];
		String profile = args[1].split("=")[1];
		System.out.println("I haz been run with profile(s) '" + profile + "' and endpoint(s) '" + endpoints + "'");
	}

}
