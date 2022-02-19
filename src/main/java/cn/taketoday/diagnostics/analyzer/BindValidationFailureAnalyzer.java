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

import cn.taketoday.boot.context.properties.bind.BindException;
import cn.taketoday.boot.context.properties.bind.validation.BindValidationException;
import cn.taketoday.boot.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.boot.diagnostics.FailureAnalysis;
import cn.taketoday.boot.origin.Origin;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.ObjectError;

import java.util.List;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of any bind validation
 * failures caused by {@link BindValidationException} or
 * {@link cn.taketoday.validation.BindException}.
 *
 * @author Madhura Bhave
 */
class BindValidationFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
		ExceptionDetails details = getBindValidationExceptionDetails(rootFailure);
		if (details == null) {
			return null;
		}
		return analyzeBindValidationException(details);
	}

	private ExceptionDetails getBindValidationExceptionDetails(Throwable rootFailure) {
		BindValidationException validationException = findCause(rootFailure, BindValidationException.class);
		if (validationException != null) {
			BindException target = findCause(rootFailure, BindException.class);
			List<ObjectError> errors = validationException.getValidationErrors().getAllErrors();
			return new ExceptionDetails(errors, target, validationException);
		}
		cn.taketoday.validation.BindException bindException = findCause(rootFailure,
				cn.taketoday.validation.BindException.class);
		if (bindException != null) {
			List<ObjectError> errors = bindException.getAllErrors();
			return new ExceptionDetails(errors, bindException.getTarget(), bindException);
		}
		return null;
	}

	private FailureAnalysis analyzeBindValidationException(ExceptionDetails details) {
		StringBuilder description = new StringBuilder(
				String.format("Binding to target %s failed:%n", details.getTarget()));
		for (ObjectError error : details.getErrors()) {
			if (error instanceof FieldError) {
				appendFieldError(description, (FieldError) error);
			}
			description.append(String.format("%n    Reason: %s%n", error.getDefaultMessage()));
		}
		return getFailureAnalysis(description, details.getCause());
	}

	private void appendFieldError(StringBuilder description, FieldError error) {
		Origin origin = Origin.from(error);
		description.append(String.format("%n    Property: %s", error.getObjectName() + "." + error.getField()));
		description.append(String.format("%n    Value: %s", error.getRejectedValue()));
		if (origin != null) {
			description.append(String.format("%n    Origin: %s", origin));
		}
	}

	private FailureAnalysis getFailureAnalysis(Object description, Throwable cause) {
		return new FailureAnalysis(description.toString(), "Update your application's configuration", cause);
	}

	private static class ExceptionDetails {

		private List<ObjectError> errors;

		private Object target;

		private Throwable cause;

		ExceptionDetails(List<ObjectError> errors, Object target, Throwable cause) {
			this.errors = errors;
			this.target = target;
			this.cause = cause;
		}

		Object getTarget() {
			return this.target;
		}

		List<ObjectError> getErrors() {
			return this.errors;
		}

		Throwable getCause() {
			return this.cause;
		}

	}

}
