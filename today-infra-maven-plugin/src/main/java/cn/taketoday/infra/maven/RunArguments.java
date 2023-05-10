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

package cn.taketoday.infra.maven;

import org.codehaus.plexus.util.cli.CommandLineUtils;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Parse and expose arguments specified in a single string.
 *
 * @author Stephane Nicoll
 */
class RunArguments {

	private static final String[] NO_ARGS = {};

	private final Deque<String> args = new LinkedList<>();

	RunArguments(String arguments) {
		this(parseArgs(arguments));
	}

	RunArguments(String[] args) {
		if (args != null) {
			Arrays.stream(args).filter(Objects::nonNull).forEach(this.args::add);
		}
	}

	Deque<String> getArgs() {
		return this.args;
	}

	String[] asArray() {
		return this.args.toArray(new String[0]);
	}

	private static String[] parseArgs(String arguments) {
		if (arguments == null || arguments.trim().isEmpty()) {
			return NO_ARGS;
		}
		try {
			arguments = arguments.replace('\n', ' ').replace('\t', ' ');
			return CommandLineUtils.translateCommandline(arguments);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("Failed to parse arguments [" + arguments + "]", ex);
		}
	}

}
