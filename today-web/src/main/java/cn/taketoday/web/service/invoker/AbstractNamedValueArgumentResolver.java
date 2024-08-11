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

package cn.taketoday.web.service.invoker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;

/**
 * Base class for arguments that resolve to a named request value such as a
 * request header, path variable, cookie, and others.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractNamedValueArgumentResolver implements HttpServiceArgumentResolver {
  private static final TypeDescriptor STRING_TARGET_TYPE = TypeDescriptor.valueOf(String.class);

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private final ConversionService conversionService;

  private final Map<MethodParameter, NamedValueInfo> namedValueInfoCache = new ConcurrentHashMap<>(256);

  /**
   * Constructor for a resolver to a String value.
   *
   * @param conversionService the {@link ConversionService} to use to format
   * Object to String values
   */
  protected AbstractNamedValueArgumentResolver(ConversionService conversionService) {
    Assert.notNull(conversionService, "ConversionService is required");
    this.conversionService = conversionService;
  }

  /**
   * Constructor for a resolver to an Object value, without conversion.
   */
  protected AbstractNamedValueArgumentResolver() {
    this.conversionService = null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean resolve(@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    NamedValueInfo info = getNamedValueInfo(parameter, requestValues);

    if (info == null) {
      return false;
    }

    if (Map.class.isAssignableFrom(parameter.getParameterType())) {
      Assert.isInstanceOf(Map.class, argument);
      if (argument != null) {
        parameter = parameter.nested(1);
        for (var entry : ((Map<String, ?>) argument).entrySet()) {
          addSingleOrMultipleValues(entry.getKey(), entry.getValue(), false,
                  null, info.label, info.multiValued, parameter, requestValues);
        }
      }
    }
    else {
      addSingleOrMultipleValues(info.name, argument, info.required, info.defaultValue,
              info.label, info.multiValued, parameter, requestValues);
    }

    return true;
  }

  @Nullable
  private NamedValueInfo getNamedValueInfo(MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    NamedValueInfo info = this.namedValueInfoCache.get(parameter);
    if (info == null) {
      info = createNamedValueInfo(parameter, requestValues);
      if (info == null) {
        return null;
      }
      info = updateNamedValueInfo(parameter, info);
      this.namedValueInfoCache.put(parameter, info);
    }
    return info;
  }

  /**
   * Return information about the request value, or {@code null} if the
   * parameter does not represent a request value of interest.
   */
  @Nullable
  protected abstract NamedValueInfo createNamedValueInfo(MethodParameter parameter);

  /**
   * Variant of {@link #createNamedValueInfo(MethodParameter)} that also provides
   * access to the static values set from {@code @HttpExchange} attributes.
   *
   * @since 5.0
   */
  @Nullable
  protected NamedValueInfo createNamedValueInfo(
          MethodParameter parameter, HttpRequestValues.Metadata metadata) {

    return createNamedValueInfo(parameter);
  }

  private NamedValueInfo updateNamedValueInfo(MethodParameter parameter, NamedValueInfo info) {
    String name = info.name;
    if (info.name.isEmpty()) {
      name = parameter.getParameterName();
      if (name == null) {
        throw new IllegalArgumentException(
                "Name for argument of type [%s] not specified, and parameter name information not found in class file either."
                        .formatted(parameter.getParameterType().getName()));
      }
    }
    boolean required = (info.required && !parameter.getParameterType().equals(Optional.class));
    String defaultValue = (Constant.DEFAULT_NONE.equals(info.defaultValue) ? null : info.defaultValue);
    return info.update(name, required, defaultValue);
  }

  private void addSingleOrMultipleValues(String name, @Nullable Object value, boolean required,
          @Nullable Object defaultValue, String valueLabel, boolean supportsMultiValues,
          MethodParameter parameter, HttpRequestValues.Builder requestValues) {

    if (supportsMultiValues) {
      if (ObjectUtils.isArray(value)) {
        value = Arrays.asList((Object[]) value);
      }
      if (value instanceof Collection<?> elements) {
        parameter = parameter.nested();
        boolean hasValues = false;
        for (Object element : elements) {
          if (element != null) {
            hasValues = true;
            addSingleValue(name, element, false, null, valueLabel, parameter, requestValues);
          }
        }
        if (hasValues) {
          return;
        }
        value = null;
      }
    }

    addSingleValue(name, value, required, defaultValue, valueLabel, parameter, requestValues);
  }

  private void addSingleValue(String name, @Nullable Object value,
          boolean required, @Nullable Object defaultValue, String valueLabel,
          MethodParameter parameter, HttpRequestValues.Builder requestValues) {

    if (value instanceof Optional<?> optionalValue) {
      value = optionalValue.orElse(null);
    }

    if (value == null && defaultValue != null) {
      value = defaultValue;
    }

    if (conversionService != null && !(value instanceof String)) {
      parameter = parameter.nestedIfOptional();
      Class<?> type = parameter.getNestedParameterType();
      value = (type != Object.class && !type.isArray() ?
              this.conversionService.convert(value, new TypeDescriptor(parameter), STRING_TARGET_TYPE) :
              this.conversionService.convert(value, String.class));
    }

    if (value == null) {
      if (required) {
        throw new IllegalArgumentException("Missing %s value '%s'".formatted(valueLabel, name));
      }
      return;
    }

    if (logger.isTraceEnabled()) {
      logger.trace("Resolved {} value '{}:{}", valueLabel, name, value);
    }

    addRequestValue(name, value, parameter, requestValues);
  }

  /**
   * Add the given, single request value. This may be called multiples times
   * if the request value is multivalued.
   * <p>If the resolver was created with a {@link ConversionService}, the value
   * will have been converted to a String and may be cast down.
   *
   * @param name the request value name
   * @param value the value
   * @param parameter the method parameter type, nested if Map, List/array, or Optional
   * @param requestValues builder to add the request value to
   */
  protected abstract void addRequestValue(String name, Object value,
          MethodParameter parameter, HttpRequestValues.Builder requestValues);

  /**
   * Info about a request value, typically extracted from a method parameter annotation.
   */
  protected static class NamedValueInfo {

    private final String name;

    private final boolean required;

    @Nullable
    private final String defaultValue;

    private final String label;

    private final boolean multiValued;

    /**
     * Create an instance.
     *
     * @param name the name to use, possibly empty if not specified
     * @param required whether it is marked as required
     * @param defaultValue fallback value, possibly {@link Constant#DEFAULT_NONE}
     * @param label how it should appear in error messages, e.g. "path variable", "request header"
     */
    public NamedValueInfo(String name, boolean required,
            @Nullable String defaultValue, String label, boolean multiValued) {
      this.name = name;
      this.required = required;
      this.defaultValue = defaultValue;
      this.label = label;
      this.multiValued = multiValued;
    }

    public NamedValueInfo update(String name, boolean required, @Nullable String defaultValue) {
      return new NamedValueInfo(name, required, defaultValue, this.label, this.multiValued);
    }

  }

}
