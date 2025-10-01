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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.app.diagnostics.FailureAnalyzer;
import infra.context.properties.source.ConfigurationPropertySources;
import infra.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by an
 * {@link MutuallyExclusiveConfigurationPropertiesException}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class MutuallyExclusiveConfigurationPropertiesFailureAnalyzer
        extends AbstractFailureAnalyzer<MutuallyExclusiveConfigurationPropertiesException> {

  @Nullable
  private final ConfigurableEnvironment environment;

  public MutuallyExclusiveConfigurationPropertiesFailureAnalyzer(@Nullable ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  @Nullable
  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, MutuallyExclusiveConfigurationPropertiesException cause) {
    List<Descriptor> descriptors = new ArrayList<>();
    for (String name : cause.getConfiguredNames()) {
      List<Descriptor> descriptorsForName = getDescriptors(name);
      if (descriptorsForName.isEmpty()) {
        return null;
      }
      descriptors.addAll(descriptorsForName);
    }
    StringBuilder description = new StringBuilder();
    appendDetails(description, cause, descriptors);
    return new FailureAnalysis(description.toString(),
            "Update your configuration so that only one of the mutually exclusive properties is configured.",
            cause);
  }

  private List<Descriptor> getDescriptors(String propertyName) {
    return getPropertySources()
            .filter(source -> source.containsProperty(propertyName))
            .map(source -> Descriptor.get(source, propertyName))
            .collect(Collectors.toList());
  }

  private Stream<PropertySource<?>> getPropertySources() {
    if (this.environment == null) {
      return Stream.empty();
    }
    return this.environment.getPropertySources().stream()
            .filter(source -> !ConfigurationPropertySources.isAttachedConfigurationPropertySource(source));
  }

  private void appendDetails(StringBuilder message, MutuallyExclusiveConfigurationPropertiesException cause, List<Descriptor> descriptors) {
    descriptors.sort(Comparator.comparing(d -> d.propertyName));
    message.append(String.format("The following configuration properties are mutually exclusive:%n%n"));
    for (String name : sortedStrings(cause.getMutuallyExclusiveNames())) {
      message.append(String.format("\t%s%n", name));
    }
    message.append(String.format("%n"));
    message.append(String.format("However, more than one of those properties has been configured at the same time:%n%n"));
    Set<String> configuredDescriptions = sortedStrings(descriptors,
            descriptor -> String.format("\t%s%s%n", descriptor.propertyName,
                    descriptor.origin != null ? " (originating from '" + descriptor.origin + "')" : ""));
    configuredDescriptions.forEach(message::append);
  }

  private Set<String> sortedStrings(Collection<String> input) {
    return sortedStrings(input, Function.identity());
  }

  private <S> Set<String> sortedStrings(Collection<S> input, Function<S, String> converter) {
    TreeSet<String> results = new TreeSet<>();
    for (S item : input) {
      results.add(converter.apply(item));
    }
    return results;
  }

  private record Descriptor(String propertyName, @Nullable Origin origin) {

    static Descriptor get(PropertySource<?> source, String propertyName) {
      Origin origin = OriginLookup.getOrigin(source, propertyName);
      return new Descriptor(propertyName, origin);
    }

  }

}
