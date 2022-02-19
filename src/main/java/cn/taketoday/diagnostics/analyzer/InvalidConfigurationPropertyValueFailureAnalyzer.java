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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.boot.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import cn.taketoday.boot.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.boot.diagnostics.FailureAnalysis;
import cn.taketoday.boot.diagnostics.FailureAnalyzer;
import cn.taketoday.boot.origin.Origin;
import cn.taketoday.boot.origin.OriginLookup;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.util.StringUtils;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by an
 * {@link InvalidConfigurationPropertyValueException}.
 *
 * @author Stephane Nicoll
 */
class InvalidConfigurationPropertyValueFailureAnalyzer
        extends AbstractFailureAnalyzer<InvalidConfigurationPropertyValueException> implements EnvironmentAware {

  private ConfigurableEnvironment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = (ConfigurableEnvironment) environment;
  }

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationPropertyValueException cause) {
    List<Descriptor> descriptors = getDescriptors(cause.getName());
    if (descriptors.isEmpty()) {
      return null;
    }
    StringBuilder description = new StringBuilder();
    appendDetails(description, cause, descriptors);
    appendReason(description, cause);
    appendAdditionalProperties(description, descriptors);
    return new FailureAnalysis(description.toString(), getAction(cause), cause);
  }

  private List<Descriptor> getDescriptors(String propertyName) {
    return getPropertySources().filter((source) -> source.containsProperty(propertyName))
            .map((source) -> Descriptor.get(source, propertyName)).collect(Collectors.toList());
  }

  private Stream<PropertySource<?>> getPropertySources() {
    if (this.environment == null) {
      return Stream.empty();
    }
    return this.environment.getPropertySources().stream()
            .filter((source) -> !ConfigurationPropertySources.isAttachedConfigurationPropertySource(source));
  }

  private void appendDetails(StringBuilder message, InvalidConfigurationPropertyValueException cause,
                             List<Descriptor> descriptors) {
    Descriptor mainDescriptor = descriptors.get(0);
    message.append("Invalid value '").append(mainDescriptor.getValue()).append("' for configuration property '");
    message.append(cause.getName()).append("'");
    mainDescriptor.appendOrigin(message);
    message.append(".");
  }

  private void appendReason(StringBuilder message, InvalidConfigurationPropertyValueException cause) {
    if (StringUtils.hasText(cause.getReason())) {
      message.append(String.format(" Validation failed for the following reason:%n%n"));
      message.append(cause.getReason());
    }
    else {
      message.append(" No reason was provided.");
    }
  }

  private void appendAdditionalProperties(StringBuilder message, List<Descriptor> descriptors) {
    List<Descriptor> others = descriptors.subList(1, descriptors.size());
    if (!others.isEmpty()) {
      message.append(
              String.format("%n%nAdditionally, this property is also set in the following property %s:%n%n",
                      (others.size() > 1) ? "sources" : "source"));
      for (Descriptor other : others) {
        message.append("\t- In '").append(other.getPropertySource()).append("'");
        message.append(" with the value '").append(other.getValue()).append("'");
        other.appendOrigin(message);
        message.append(String.format(".%n"));
      }
    }
  }

  private String getAction(InvalidConfigurationPropertyValueException cause) {
    StringBuilder action = new StringBuilder();
    action.append("Review the value of the property");
    if (cause.getReason() != null) {
      action.append(" with the provided reason");
    }
    action.append(".");
    return action.toString();
  }

  private static final class Descriptor {

    private final String propertySource;

    private final Object value;

    private final Origin origin;

    private Descriptor(String propertySource, Object value, Origin origin) {
      this.propertySource = propertySource;
      this.value = value;
      this.origin = origin;
    }

    String getPropertySource() {
      return this.propertySource;
    }

    Object getValue() {
      return this.value;
    }

    void appendOrigin(StringBuilder message) {
      if (this.origin != null) {
        message.append(" (originating from '").append(this.origin).append("')");
      }
    }

    static Descriptor get(PropertySource<?> source, String propertyName) {
      Object value = source.getProperty(propertyName);
      Origin origin = OriginLookup.getOrigin(source, propertyName);
      return new Descriptor(source.getName(), value, origin);
    }

  }

}
