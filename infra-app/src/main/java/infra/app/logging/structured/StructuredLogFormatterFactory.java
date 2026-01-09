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

package infra.app.logging.structured;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import infra.core.GenericTypeResolver;
import infra.core.env.Environment;
import infra.lang.Assert;
import infra.util.Instantiator;
import infra.util.Instantiator.AvailableParameters;
import infra.util.Instantiator.FailureHandler;

/**
 * Factory that can be used to create a fully instantiated {@link StructuredLogFormatter}
 * for either a {@link CommonStructuredLogFormat#getId() common format} or a
 * fully-qualified class name.
 *
 * @param <E> the log even type
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see StructuredLogFormatter
 * @since 5.0
 */
public class StructuredLogFormatterFactory<E> {

  private static final FailureHandler failureHandler = (type, implementationName, failure) -> {
    if (!(failure instanceof ClassNotFoundException)) {
      throw new IllegalArgumentException(
              "Unable to instantiate %s [%s]".formatted(implementationName, type.getName()), failure);
    }
  };

  private final Class<E> logEventType;

  private final Instantiator<StructuredLogFormatter<E>> instantiator;

  private final CommonFormatters<E> commonFormatters;

  /**
   * Create a new {@link StructuredLogFormatterFactory} instance.
   *
   * @param logEventType the log event type
   * @param environment the Infra {@link Environment}
   * @param availableParameters callback used to configure available parameters for the
   * specific logging system
   * @param commonFormatters callback used to define supported common formatters
   */
  public StructuredLogFormatterFactory(Class<E> logEventType, Environment environment,
          @Nullable Consumer<AvailableParameters> availableParameters, Consumer<CommonFormatters<E>> commonFormatters) {
    this.logEventType = logEventType;
    this.instantiator = new Instantiator<>(StructuredLogFormatter.class, (allAvailableParameters) -> {
      allAvailableParameters.add(Environment.class, environment);
      if (availableParameters != null) {
        availableParameters.accept(allAvailableParameters);
      }
    }, failureHandler);
    this.commonFormatters = new CommonFormatters<>();
    commonFormatters.accept(this.commonFormatters);
  }

  /**
   * Get a new {@link StructuredLogFormatter} instance for the specified format.
   *
   * @param format the format requested (either a {@link CommonStructuredLogFormat} ID
   * or a fully-qualified class name)
   * @return a new {@link StructuredLogFormatter} instance
   * @throws IllegalArgumentException if the format is unknown
   */
  public StructuredLogFormatter<E> get(String format) {
    StructuredLogFormatter<E> formatter = this.commonFormatters.get(this.instantiator, format);
    formatter = (formatter != null) ? formatter : getUsingClassName(format);
    if (formatter != null) {
      return formatter;
    }
    throw new IllegalArgumentException(
            "Unknown format '%s'. Values can be a valid fully-qualified class name or one of the common formats: %s"
                    .formatted(format, this.commonFormatters.getCommonNames()));
  }

  @Nullable
  private StructuredLogFormatter<E> getUsingClassName(String className) {
    StructuredLogFormatter<E> formatter = this.instantiator.instantiate(className);
    if (formatter != null) {
      checkTypeArgument(formatter);
    }
    return formatter;
  }

  private void checkTypeArgument(Object formatter) {
    Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(formatter.getClass(),
            StructuredLogFormatter.class);
    Assert.isTrue(this.logEventType.equals(typeArgument),
            () -> "Type argument of %s must be %s but was %s".formatted(formatter.getClass().getName(),
                    this.logEventType.getName(), (typeArgument != null) ? typeArgument.getName() : "null"));

  }

  /**
   * Callback used for configure the {@link CommonFormatterFactory} to use for a given
   * {@link CommonStructuredLogFormat}.
   *
   * @param <E> the log event type
   */
  public static class CommonFormatters<E> {

    private final Map<CommonStructuredLogFormat, CommonFormatterFactory<E>> factories = new TreeMap<>();

    /**
     * Add the factory that should be used for the given
     * {@link CommonStructuredLogFormat}.
     *
     * @param format the common structured log format
     * @param factory the factory to use
     */
    public void add(CommonStructuredLogFormat format, CommonFormatterFactory<E> factory) {
      this.factories.put(format, factory);
    }

    Collection<String> getCommonNames() {
      return this.factories.keySet().stream().map(CommonStructuredLogFormat::getId).toList();
    }

    @Nullable
    StructuredLogFormatter<E> get(Instantiator<StructuredLogFormatter<E>> instantiator, String format) {
      CommonStructuredLogFormat commonFormat = CommonStructuredLogFormat.forId(format);
      CommonFormatterFactory<E> factory = (commonFormat != null) ? this.factories.get(commonFormat) : null;
      return (factory != null) ? factory.createFormatter(instantiator) : null;
    }

  }

  /**
   * Factory used to create a {@link StructuredLogFormatter} for a given
   * {@link CommonStructuredLogFormat}.
   *
   * @param <E> the log event type
   */
  @FunctionalInterface
  public interface CommonFormatterFactory<E> {

    /**
     * Create the {@link StructuredLogFormatter} instance.
     *
     * @param instantiator instantiator that can be used to obtain arguments
     * @return a new {@link StructuredLogFormatter} instance
     */
    StructuredLogFormatter<E> createFormatter(Instantiator<StructuredLogFormatter<E>> instantiator);

  }

}
