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

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.context.properties.bind.BindException;
import infra.context.properties.bind.UnboundConfigurationPropertiesException;
import infra.context.properties.bind.validation.BindValidationException;
import infra.context.properties.source.ConfigurationProperty;
import infra.core.conversion.ConversionFailedException;
import infra.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BindException} excluding {@link BindValidationException} and
 * {@link UnboundConfigurationPropertiesException}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BindFailureAnalyzer extends AbstractFailureAnalyzer<BindException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, BindException cause) {
    Throwable rootCause = cause.getCause();
    if (rootCause instanceof BindValidationException
            || rootCause instanceof UnboundConfigurationPropertiesException) {
      return null;
    }
    return analyzeGenericBindException(rootFailure, cause);
  }

  private FailureAnalysis analyzeGenericBindException(Throwable rootFailure, BindException cause) {
    FailureAnalysis missingParametersAnalysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(rootFailure);
    StringBuilder description = new StringBuilder(String.format("%s:%n", cause.getMessage()));
    ConfigurationProperty property = cause.getProperty();
    buildDescription(description, property);
    description.append(String.format("%n    Reason: %s", getMessage(cause)));
    if (missingParametersAnalysis != null) {
      MissingParameterNamesFailureAnalyzer.appendPossibility(description);
    }
    return getFailureAnalysis(description.toString(), cause, missingParametersAnalysis);
  }

  private void buildDescription(StringBuilder description, @Nullable ConfigurationProperty property) {
    if (property != null) {
      description.append(String.format("%n    Property: %s", property.getName()));
      description.append(String.format("%n    Value: \"%s\"", property.getValue()));
      description.append(String.format("%n    Origin: %s", property.getOrigin()));
    }
  }

  private String getMessage(BindException cause) {
    Throwable rootCause = getRootCause(cause.getCause());
    ConversionFailedException conversionFailure = findCause(cause, ConversionFailedException.class);
    if (conversionFailure != null) {
      String message = "failed to convert " + conversionFailure.getSourceType() + " to "
              + conversionFailure.getTargetType();
      if (rootCause != null) {
        message += " (caused by " + getExceptionTypeAndMessage(rootCause) + ")";
      }
      return message;
    }
    if (rootCause != null && StringUtils.hasText(rootCause.getMessage())) {
      return getExceptionTypeAndMessage(rootCause);
    }
    return getExceptionTypeAndMessage(cause);
  }

  @Nullable
  private Throwable getRootCause(Throwable cause) {
    Throwable rootCause = cause;
    while (rootCause != null && rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  private String getExceptionTypeAndMessage(Throwable ex) {
    String message = ex.getMessage();
    return ex.getClass().getName() + (StringUtils.hasText(message) ? ": " + message : "");
  }

  private FailureAnalysis getFailureAnalysis(String description,
          BindException cause, @Nullable FailureAnalysis missingParametersAnalysis) {
    StringBuilder action = new StringBuilder("Update your application's configuration");
    Collection<String> validValues = findValidValues(cause);
    if (!validValues.isEmpty()) {
      action.append(String.format(". The following values are valid:%n"));
      validValues.forEach((value) -> action.append(String.format("%n    %s", value)));
    }
    if (missingParametersAnalysis != null) {
      action.append(String.format("%n%n%s", missingParametersAnalysis.getAction()));
    }
    return new FailureAnalysis(description, action.toString(), cause);
  }

  private Collection<String> findValidValues(BindException ex) {
    ConversionFailedException conversionFailure = findCause(ex, ConversionFailedException.class);
    if (conversionFailure != null) {
      Object[] enumConstants = conversionFailure.getTargetType().getType().getEnumConstants();
      if (enumConstants != null) {
        return Stream.of(enumConstants).map(Object::toString).collect(Collectors.toCollection(TreeSet::new));
      }
    }
    return Collections.emptySet();
  }

}
