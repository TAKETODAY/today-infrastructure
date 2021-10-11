/*
 * Copyright 2002-2020 the original author or authors.
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

package cn.taketoday.core.env;

import java.util.Collections;
import java.util.LinkedHashSet;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.core.conversion.ConfigurableConversionService;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.PlaceholderResolver;
import cn.taketoday.util.PropertyPlaceholderHandler;
import cn.taketoday.util.SystemPropertyUtils;

/**
 * Abstract base class for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver, PlaceholderResolver {

  @Nullable
  private volatile ConfigurableConversionService conversionService;

  @Nullable
  private PropertyPlaceholderHandler nonStrictHelper;

  @Nullable
  private PropertyPlaceholderHandler strictHelper;

  private boolean ignoreUnresolvableNestedPlaceholders = false;

  private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;

  private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

  @Nullable
  private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

  @Nullable
  private LinkedHashSet<String> requiredProperties;

  @Override
  public ConfigurableConversionService getConversionService() {
    // Need to provide an independent DefaultConversionService, not the
    // shared DefaultConversionService used by PropertySourcesPropertyResolver.
    ConfigurableConversionService cs = this.conversionService;
    if (cs == null) {
      synchronized(this) {
        cs = this.conversionService;
        if (cs == null) {
          cs = new DefaultConversionService();
          DefaultConversionService.addDefaultConverters(cs);
          this.conversionService = cs;
        }
      }
    }
    return cs;
  }

  @Override
  public void setConversionService(ConfigurableConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService must not be null");
    this.conversionService = conversionService;
  }

  /**
   * Set the prefix that placeholders replaced by this resolver must begin with.
   * <p>The default is "${".
   *
   * @see cn.taketoday.util.SystemPropertyUtils#PLACEHOLDER_PREFIX
   */
  @Override
  public void setPlaceholderPrefix(String placeholderPrefix) {
    Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
    this.placeholderPrefix = placeholderPrefix;
  }

  /**
   * Set the suffix that placeholders replaced by this resolver must end with.
   * <p>The default is "}".
   *
   * @see cn.taketoday.util.SystemPropertyUtils#PLACEHOLDER_SUFFIX
   */
  @Override
  public void setPlaceholderSuffix(String placeholderSuffix) {
    Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
    this.placeholderSuffix = placeholderSuffix;
  }

  /**
   * Specify the separating character between the placeholders replaced by this
   * resolver and their associated default value, or {@code null} if no such
   * special character should be processed as a value separator.
   * <p>The default is ":".
   *
   * @see cn.taketoday.util.SystemPropertyUtils#VALUE_SEPARATOR
   */
  @Override
  public void setValueSeparator(@Nullable String valueSeparator) {
    this.valueSeparator = valueSeparator;
  }

  /**
   * Set whether to throw an exception when encountering an unresolvable placeholder
   * nested within the value of a given property. A {@code false} value indicates strict
   * resolution, i.e. that an exception will be thrown. A {@code true} value indicates
   * that unresolvable nested placeholders should be passed through in their unresolved
   * ${...} form.
   * <p>The default is {@code false}.
   */
  @Override
  public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
    this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
  }

  @Override
  public void setRequiredProperties(String... requiredProperties) {
    if (this.requiredProperties == null) {
      this.requiredProperties = new LinkedHashSet<>();
    }
    else {
      this.requiredProperties.clear();
    }
    Collections.addAll(this.requiredProperties, requiredProperties);
  }

  @Override
  public void addRequiredProperties(String... requiredProperties) {
    if (this.requiredProperties == null) {
      this.requiredProperties = new LinkedHashSet<>();
    }
    Collections.addAll(this.requiredProperties, requiredProperties);
  }

  @Override
  public void validateRequiredProperties() {
    if (CollectionUtils.isNotEmpty(requiredProperties)) {
      LinkedHashSet<String> missingRequiredProperties = new LinkedHashSet<>();
      for (String key : requiredProperties) {
        if (getProperty(key) == null) {
          missingRequiredProperties.add(key);
        }
      }
      if (!missingRequiredProperties.isEmpty()) {
        throw new MissingRequiredPropertiesException(missingRequiredProperties);
      }
    }
  }

  @Override
  public boolean containsProperty(String key) {
    return getProperty(key) != null;
  }

  @Override
  @Nullable
  public String getProperty(String key) {
    return getProperty(key, String.class);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    String value = getProperty(key);
    return value != null ? value : defaultValue;
  }

  @Override
  public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
    T value = getProperty(key, targetType);
    return value != null ? value : defaultValue;
  }

  @Override
  public String getRequiredProperty(String key) throws IllegalStateException {
    String value = getProperty(key);
    if (value == null) {
      throw new IllegalStateException("Required key '" + key + "' not found");
    }
    return value;
  }

  @Override
  public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
    T value = getProperty(key, valueType);
    if (value == null) {
      throw new IllegalStateException("Required key '" + key + "' not found");
    }
    return value;
  }

  @Override
  public String resolvePlaceholders(String text) {
    if (nonStrictHelper == null) {
      nonStrictHelper = createPlaceholderHelper(true);
    }
    return nonStrictHelper.replacePlaceholders(text, this);
  }

  @Override
  public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
    if (strictHelper == null) {
      strictHelper = createPlaceholderHelper(false);
    }
    return strictHelper.replacePlaceholders(text, this);
  }

  /**
   * Resolve placeholders within the given string, deferring to the value of
   * {@link #setIgnoreUnresolvableNestedPlaceholders} to determine whether any
   * unresolvable placeholders should raise an exception or be ignored.
   * <p>Invoked from {@link #getProperty} and its variants, implicitly resolving
   * nested placeholders. In contrast, {@link #resolvePlaceholders} and
   * {@link #resolveRequiredPlaceholders} do <i>not</i> delegate
   * to this method but rather perform their own handling of unresolvable
   * placeholders, as specified by each of those methods.
   *
   * @see #setIgnoreUnresolvableNestedPlaceholders
   */
  protected String resolveNestedPlaceholders(String value) {
    if (value.isEmpty()) {
      return value;
    }
    return ignoreUnresolvableNestedPlaceholders
           ? resolvePlaceholders(value)
           : resolveRequiredPlaceholders(value);
  }

  private PropertyPlaceholderHandler createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
    return new PropertyPlaceholderHandler(
            placeholderPrefix, placeholderSuffix,
            valueSeparator, ignoreUnresolvablePlaceholders);
  }

  @Nullable
  @Override
  public String resolvePlaceholder(String placeholderName) {
    return getPropertyAsRawString(placeholderName);
  }

  /**
   * Convert the given value to the specified target type, if necessary.
   *
   * @param value
   *         the original property value
   * @param targetType
   *         the specified target type for property retrieval
   *
   * @return the converted value, or the original value if no conversion
   * is necessary
   */
  @SuppressWarnings("unchecked")
  @Nullable
  protected <T> T convertValueIfNecessary(Object value, @Nullable Class<T> targetType) {
    if (targetType == null) {
      return (T) value;
    }
    ConversionService conversionServiceToUse = this.conversionService;
    if (conversionServiceToUse == null) {
      // Avoid initialization of shared DefaultConversionService if
      // no standard type conversion is needed in the first place...
      if (ClassUtils.isAssignableValue(targetType, value)) {
        return (T) value;
      }
      conversionServiceToUse = DefaultConversionService.getSharedInstance();
    }
    return conversionServiceToUse.convert(value, targetType);
  }

  /**
   * Retrieve the specified property as a raw String,
   * i.e. without resolution of nested placeholders.
   *
   * @param key
   *         the property name to resolve
   *
   * @return the property value or {@code null} if none found
   */
  @Nullable
  protected abstract String getPropertyAsRawString(String key);

}
