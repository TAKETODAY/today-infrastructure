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

package infra.app.diagnostics.analyzer;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.context.properties.bind.BindException;
import infra.context.properties.bind.UnboundConfigurationPropertiesException;
import infra.context.properties.source.ConfigurationProperty;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by any
 * {@link UnboundConfigurationPropertiesException}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class UnboundConfigurationPropertyFailureAnalyzer
        extends AbstractFailureAnalyzer<UnboundConfigurationPropertiesException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, UnboundConfigurationPropertiesException cause) {
    BindException exception = findCause(rootFailure, BindException.class);
    return analyzeUnboundConfigurationPropertiesException(exception, cause);
  }

  private FailureAnalysis analyzeUnboundConfigurationPropertiesException(BindException cause,
                                                                         UnboundConfigurationPropertiesException exception) {
    StringBuilder description = new StringBuilder(
            String.format("Binding to target %s failed:%n", cause.getTarget()));
    for (ConfigurationProperty property : exception.getUnboundProperties()) {
      buildDescription(description, property);
      description.append(String.format("%n    Reason: %s", exception.getMessage()));
    }
    return getFailureAnalysis(description, cause);
  }

  private void buildDescription(StringBuilder description, ConfigurationProperty property) {
    if (property != null) {
      description.append(String.format("%n    Property: %s", property.getName()));
      description.append(String.format("%n    Value: %s", property.getValue()));
      description.append(String.format("%n    Origin: %s", property.getOrigin()));
    }
  }

  private FailureAnalysis getFailureAnalysis(Object description, BindException cause) {
    return new FailureAnalysis(description.toString(), "Update your application's configuration", cause);
  }

}
