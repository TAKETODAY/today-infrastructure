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

import java.util.stream.Collectors;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.BeanCreationException;
import infra.context.properties.source.InvalidConfigurationPropertyNameException;
import infra.lang.Nullable;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by
 * {@link InvalidConfigurationPropertyNameException}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class InvalidConfigurationPropertyNameFailureAnalyzer
        extends AbstractFailureAnalyzer<InvalidConfigurationPropertyNameException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationPropertyNameException cause) {
    BeanCreationException exception = findCause(rootFailure, BeanCreationException.class);
    String action = String.format("Modify '%s' so that it conforms to the canonical names requirements.",
            cause.getName());
    return new FailureAnalysis(buildDescription(cause, exception), action, cause);
  }

  private String buildDescription(InvalidConfigurationPropertyNameException cause, @Nullable BeanCreationException exception) {
    StringBuilder description = new StringBuilder(
            String.format("Configuration property name '%s' is not valid:%n", cause.getName()));
    String invalid = cause.getInvalidCharacters().stream().map(this::quote).collect(Collectors.joining(", "));
    description.append(String.format("%n    Invalid characters: %s", invalid));
    if (exception != null) {
      description.append(String.format("%n    Bean: %s", exception.getBeanName()));
    }
    description.append(String.format("%n    Reason: Canonical names should be "
            + "kebab-case ('-' separated), lowercase alpha-numeric characters and must start with a letter"));
    return description.toString();
  }

  private String quote(Character c) {
    return "'" + c + "'";
  }

}
