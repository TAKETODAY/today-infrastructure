/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.app.diagnostics.FailureAnalyzer;
import jakarta.validation.NoProviderFoundException;
import jakarta.validation.ValidationException;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link ValidationException}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Andy Wilkinson
 * @since 4.0
 */
class ValidationExceptionFailureAnalyzer extends AbstractFailureAnalyzer<ValidationException> {

  private static final String JAVAX_MISSING_IMPLEMENTATION_MESSAGE = "Unable to create a "
          + "Configuration, because no Bean Validation provider could be found";

  private static final String JAKARTA_MISSING_IMPLEMENTATION_MESSAGE = "Unable to create a "
          + "Configuration, because no Jakarta Bean Validation provider could be found";

  @Override
  protected @Nullable FailureAnalysis analyze(Throwable rootFailure, ValidationException cause) {
    String message = cause.getMessage();
    if (message == null) {
      message = "";
    }
    if (cause instanceof NoProviderFoundException || message.startsWith(JAVAX_MISSING_IMPLEMENTATION_MESSAGE)
            || message.startsWith(JAKARTA_MISSING_IMPLEMENTATION_MESSAGE)) {
      return new FailureAnalysis(
              "The Bean Validation API is on the classpath but no implementation could be found",
              "Add an implementation, such as Hibernate Validator, to the classpath", cause);
    }
    return null;
  }

}
