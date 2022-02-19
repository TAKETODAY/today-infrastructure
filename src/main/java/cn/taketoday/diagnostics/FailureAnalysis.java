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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.diagnostics;

/**
 * The result of analyzing a failure.
 *
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class FailureAnalysis {

	private final String description;

	private final String action;

	private final Throwable cause;

	/**
	 * Creates a new {@code FailureAnalysis} with the given {@code description} and
	 * {@code action}, if any, that the user should take to address the problem. The
	 * failure had the given underlying {@code cause}.
	 * @param description the description
	 * @param action the action
	 * @param cause the cause
	 */
	public FailureAnalysis(String description, String action, Throwable cause) {
		this.description = description;
		this.action = action;
		this.cause = cause;
	}

	/**
	 * Returns a description of the failure.
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Returns the action, if any, to be taken to address the failure.
	 * @return the action or {@code null}
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * Returns the cause of the failure.
	 * @return the cause
	 */
	public Throwable getCause() {
		return this.cause;
	}

}
