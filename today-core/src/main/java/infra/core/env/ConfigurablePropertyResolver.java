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
package infra.core.env;

import infra.core.conversion.ConversionService;
import infra.core.conversion.ConverterRegistry;
import infra.core.conversion.support.ConfigurableConversionService;
import infra.lang.Nullable;

/**
 * Configuration interface to be implemented by most if not all {@link PropertyResolver}
 * types. Provides facilities for accessing and customizing the
 * {@link ConversionService ConversionService}
 * used when converting property values from one type to another.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

  /**
   * Return the {@link ConfigurableConversionService} used when performing type
   * conversions on properties.
   * <p>The configurable nature of the returned conversion service allows for
   * the convenient addition and removal of individual {@code Converter} instances:
   * <pre>{@code
   * ConfigurableConversionService cs = env.getConversionService();
   * cs.addConverter(new FooConverter());
   * }</pre>
   *
   * @see PropertyResolver#getProperty(String, Class)
   * @see ConverterRegistry#addConverter
   */
  ConfigurableConversionService getConversionService();

  /**
   * Set the {@link ConfigurableConversionService} to be used when performing type
   * conversions on properties.
   * <p><strong>Note:</strong> as an alternative to fully replacing the
   * {@code ConversionService}, consider adding or removing individual
   * {@code Converter} instances by drilling into {@link #getConversionService()}
   * and calling methods such as {@code #addConverter}.
   *
   * @see PropertyResolver#getProperty(String, Class)
   * @see #getConversionService()
   * @see ConverterRegistry#addConverter
   */
  void setConversionService(ConfigurableConversionService conversionService);

  /**
   * Set the prefix that placeholders replaced by this resolver must begin with.
   */
  void setPlaceholderPrefix(String placeholderPrefix);

  /**
   * Set the suffix that placeholders replaced by this resolver must end with.
   */
  void setPlaceholderSuffix(String placeholderSuffix);

  /**
   * Specify the separating character between the placeholders replaced by this
   * resolver and their associated default value, or {@code null} if no such
   * special character should be processed as a value separator.
   */
  void setValueSeparator(@Nullable String valueSeparator);

  /**
   * Specify the escape character to use to ignore placeholder prefix
   * or value separator, or {@code null} if no escaping should take
   * place.
   */
  void setEscapeCharacter(@Nullable Character escapeCharacter);

  /**
   * Set whether to throw an exception when encountering an unresolvable placeholder
   * nested within the value of a given property. A {@code false} value indicates strict
   * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
   * that unresolvable nested placeholders should be passed through in their unresolved
   * ${...} form.
   * <p>Implementations of {@link #getProperty(String)} and its variants must inspect
   * the value set here to determine correct behavior when property values contain
   * unresolvable placeholders.
   */
  void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

  /**
   * Specify which properties must be present, to be verified by
   * {@link #validateRequiredProperties()}.
   */
  void setRequiredProperties(String... requiredProperties);

  /**
   * Specify which properties must be present, to be verified by
   * {@link #validateRequiredProperties()}.
   */
  void addRequiredProperties(String... requiredProperties);

  /**
   * Validate that each of the properties specified by
   * {@link #setRequiredProperties} is present and resolves to a
   * non-{@code null} value.
   *
   * @throws MissingRequiredPropertiesException if any of the required
   * properties are not resolvable.
   */
  void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
