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
