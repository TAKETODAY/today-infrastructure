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

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.context.properties.bind.BindException;
import infra.context.properties.bind.UnboundConfigurationPropertiesException;
import infra.context.properties.source.ConfigurationProperty;
import infra.lang.Assert;

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
    Assert.state(exception != null, "BindException not found");
    return analyzeUnboundConfigurationPropertiesException(exception, cause);
  }

  private FailureAnalysis analyzeUnboundConfigurationPropertiesException(
          BindException cause, UnboundConfigurationPropertiesException exception) {
    StringBuilder description = new StringBuilder(
            String.format("Binding to target %s failed:%n", cause.getTarget()));
    for (ConfigurationProperty property : exception.getUnboundProperties()) {
      buildDescription(description, property);
      description.append(String.format("%n    Reason: %s", exception.getMessage()));
    }
    return getFailureAnalysis(description, cause);
  }

  private void buildDescription(StringBuilder description, @Nullable ConfigurationProperty property) {
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
