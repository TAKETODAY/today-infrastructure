/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.context.properties.bind.BindException;
import infra.context.properties.bind.validation.BindValidationException;
import infra.lang.Assert;
import infra.origin.Origin;
import infra.validation.FieldError;
import infra.validation.ObjectError;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of any bind validation
 * failures caused by {@link BindValidationException} or
 * {@link infra.validation.BindException}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BindValidationFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

  @Nullable
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
      Assert.state(bindException != null, "BindException not found");
      List<ObjectError> errors = validationException.getValidationErrors().getAllErrors();
      return new ExceptionDetails(errors, bindException.getTarget().getType(), validationException);
    }
    infra.validation.BindException bindException = findCause(rootFailure,
            infra.validation.BindException.class);
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

    private final @Nullable Object target;

    public final Throwable cause;

    ExceptionDetails(List<ObjectError> errors, @Nullable Object target, Throwable cause) {
      this.errors = errors;
      this.target = target;
      this.cause = cause;
    }

  }

}
