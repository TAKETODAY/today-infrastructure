/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.env;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.json.JsonParser;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;
import cn.taketoday.origin.PropertySourceOrigin;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.servlet.support.StandardServletEnvironment;

/**
 * An {@link EnvironmentPostProcessor} that parses JSON from
 * {@code application.json} or equivalently {@code APPLICATION_JSON} and
 * adds it as a map property source to the {@link Environment}. The new properties are
 * added with higher priority than the system properties.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @since 4.0
 */
public class ApplicationJsonEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  /**
   * Name of the {@code application.json} property.
   */
  public static final String APPLICATION_JSON_PROPERTY = "application.json";

  /**
   * Name of the {@code APPLICATION_JSON} environment variable.
   */
  public static final String APPLICATION_JSON_ENVIRONMENT_VARIABLE = "APPLICATION_JSON";

  private static final String SERVLET_ENVIRONMENT_CLASS = "cn.taketoday.web.servlet.support.StandardServletEnvironment";

  private static final LinkedHashSet<String> SERVLET_ENVIRONMENT_PROPERTY_SOURCES = CollectionUtils.newLinkedHashSet(
          StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME,
          StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
          StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME
  );

  /**
   * The default order for the processor.
   */
  public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

  private int order = DEFAULT_ORDER;

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
    PropertySources propertySources = environment.getPropertySources();
    propertySources.stream()
            .map(JsonPropertyValue::get)
            .filter(Objects::nonNull)
            .findFirst()
            .ifPresent((v) -> processJson(environment, v));
  }

  private void processJson(ConfigurableEnvironment environment, JsonPropertyValue propertyValue) {
    JsonParser parser = JsonParser.lookup();
    Map<String, Object> map = parser.parseMap(propertyValue.getJson());
    if (!map.isEmpty()) {
      addJsonPropertySource(environment, new JsonPropertySource(propertyValue, flatten(map)));
    }
  }

  /**
   * Flatten the map keys using period separator.
   *
   * @param map the map that should be flattened
   * @return the flattened map
   */
  private Map<String, Object> flatten(Map<String, Object> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    flatten(null, result, map);
    return result;
  }

  private void flatten(String prefix, Map<String, Object> result, Map<String, Object> map) {
    String namePrefix = (prefix != null) ? prefix + "." : "";
    map.forEach((key, value) -> extract(namePrefix + key, result, value));
  }

  @SuppressWarnings("unchecked")
  private void extract(String name, Map<String, Object> result, Object value) {
    if (value instanceof Map) {
      if (CollectionUtils.isEmpty((Map<?, ?>) value)) {
        result.put(name, value);
        return;
      }
      flatten(name, result, (Map<String, Object>) value);
    }
    else if (value instanceof Collection) {
      if (CollectionUtils.isEmpty((Collection<?>) value)) {
        result.put(name, value);
        return;
      }
      int index = 0;
      for (Object object : (Collection<Object>) value) {
        extract(name + "[" + index + "]", result, object);
        index++;
      }
    }
    else {
      result.put(name, value);
    }
  }

  private void addJsonPropertySource(ConfigurableEnvironment environment, PropertySource<?> source) {
    PropertySources sources = environment.getPropertySources();
    String name = findPropertySource(sources);
    if (sources.contains(name)) {
      sources.addBefore(name, source);
    }
    else {
      sources.addFirst(source);
    }
  }

  private String findPropertySource(PropertySources sources) {
    if (ClassUtils.isPresent(SERVLET_ENVIRONMENT_CLASS, getClass().getClassLoader())) {
      PropertySource<?> servletPropertySource = sources.stream()
              .filter(source -> SERVLET_ENVIRONMENT_PROPERTY_SOURCES.contains(source.getName()))
              .findFirst()
              .orElse(null);
      if (servletPropertySource != null) {
        return servletPropertySource.getName();
      }
    }
    return StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;
  }

  private static class JsonPropertySource extends MapPropertySource implements OriginLookup<String> {

    private final JsonPropertyValue propertyValue;

    JsonPropertySource(JsonPropertyValue propertyValue, Map<String, Object> source) {
      super(APPLICATION_JSON_PROPERTY, source);
      this.propertyValue = propertyValue;
    }

    @Override
    public Origin getOrigin(String key) {
      return this.propertyValue.getOrigin();
    }

  }

  private record JsonPropertyValue(PropertySource<?> propertySource, String propertyName, String json) {

    String getJson() {
      return this.json;
    }

    Origin getOrigin() {
      return PropertySourceOrigin.get(this.propertySource, this.propertyName);
    }

    static JsonPropertyValue get(PropertySource<?> propertySource) {
      String[] candidates = {
              APPLICATION_JSON_PROPERTY,
              APPLICATION_JSON_ENVIRONMENT_VARIABLE
      };
      for (String candidate : candidates) {
        Object value = propertySource.getProperty(candidate);
        if (value instanceof String stringValue && StringUtils.isNotEmpty(stringValue)) {
          return new JsonPropertyValue(propertySource, candidate, stringValue);
        }
      }
      return null;
    }

  }

}
