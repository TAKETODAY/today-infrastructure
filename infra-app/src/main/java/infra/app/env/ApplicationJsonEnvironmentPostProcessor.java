/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.env;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import infra.app.Application;
import infra.app.json.JsonParser;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.origin.PropertySourceOrigin;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationJsonEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  /**
   * Name of the {@code application.json} property.
   */
  public static final String APPLICATION_JSON_PROPERTY = "infra.application.json";

  /**
   * Name of the {@code APPLICATION_JSON} environment variable.
   */
  public static final String APPLICATION_JSON_ENVIRONMENT_VARIABLE = "INFRA_APPLICATION_JSON";

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

  private void flatten(@Nullable String prefix, Map<String, Object> result, Map<String, Object> map) {
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

    @Nullable
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
