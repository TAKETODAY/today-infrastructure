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

package cn.taketoday.diagnostics.analyzer;

import cn.taketoday.boot.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.boot.diagnostics.FailureAnalysis;
import cn.taketoday.boot.diagnostics.FailureAnalyzer;

import jakarta.validation.ValidationException;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link ValidationException}.
 *
 * @author Andy Wilkinson
 */
class ValidationExceptionFailureAnalyzer extends AbstractFailureAnalyzer<ValidationException> {

	private static final String JAVAX_MISSING_IMPLEMENTATION_MESSAGE = "Unable to create a "
			+ "Configuration, because no Bean Validation provider could be found";

	private static final String JAKARTA_MISSING_IMPLEMENTATION_MESSAGE = "Unable to create a "
			+ "Configuration, because no Jakarta Bean Validation provider could be found";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ValidationException cause) {
		if (cause.getMessage().startsWith(JAVAX_MISSING_IMPLEMENTATION_MESSAGE)
				|| cause.getMessage().startsWith(JAKARTA_MISSING_IMPLEMENTATION_MESSAGE)) {
			return new FailureAnalysis(
					"The Bean Validation API is on the classpath but no implementation could be found",
					"Add an implementation, such as Hibernate Validator, to the classpath", cause);
		}
		return null;
	}

}
