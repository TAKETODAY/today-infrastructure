/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.diagnostics.analyzer;

import java.util.List;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.validation.BindValidationException;
import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.Origin;
import cn.taketoday.validation.FieldError;
import cn.taketoday.validation.ObjectError;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of any bind validation
 * failures caused by {@link BindValidationException} or
 * {@link cn.taketoday.validation.BindException}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
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

  @Nullable
  private ExceptionDetails getBindValidationExceptionDetails(Throwable rootFailure) {
    BindValidationException validationException = findCause(rootFailure, BindValidationException.class);
    if (validationException != null) {
      BindException bindException = findCause(rootFailure, BindException.class);
      List<ObjectError> errors = validationException.getValidationErrors().getAllErrors();
      return new ExceptionDetails(errors, bindException.getTarget().getType(), validationException);
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
            String.format("Binding to target %s failed:%n", details.target));
    for (ObjectError error : details.errors) {
      if (error instanceof FieldError) {
        appendFieldError(description, (FieldError) error);
      }
      description.append(String.format("%n    Reason: %s%n", error.getDefaultMessage()));
    }
    return getFailureAnalysis(description, details.cause);
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

    public final List<ObjectError> errors;

    public final Object target;

    public final Throwable cause;

    ExceptionDetails(List<ObjectError> errors, Object target, Throwable cause) {
      this.errors = errors;
      this.target = target;
      this.cause = cause;
    }

  }

}
