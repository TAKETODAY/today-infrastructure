/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.support;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.EnumerablePropertySource;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.lang.Assert;
import infra.test.context.DynamicPropertyRegistry;
import infra.util.function.SupplierUtils;

/**
 * {@link EnumerablePropertySource} backed by a map with dynamically supplied
 * values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DynamicValuesPropertySource extends MapPropertySource {

  static final String PROPERTY_SOURCE_NAME = "Dynamic Test Properties";

  final DynamicPropertyRegistry dynamicPropertyRegistry;

  DynamicValuesPropertySource() {
    this(Collections.synchronizedMap(new LinkedHashMap<>()));
  }

  DynamicValuesPropertySource(Map<String, Supplier<Object>> valueSuppliers) {
    super(PROPERTY_SOURCE_NAME, Collections.unmodifiableMap(valueSuppliers));
    this.dynamicPropertyRegistry = (name, valueSupplier) -> {
      Assert.hasText(name, "'name' must not be null or blank");
      Assert.notNull(valueSupplier, "'valueSupplier' must not be null");
      valueSuppliers.put(name, valueSupplier);
    };
  }

  @Override
  public @Nullable Object getProperty(String name) {
    return SupplierUtils.resolve(super.getProperty(name));
  }

  /**
   * Get the {@code DynamicValuesPropertySource} registered in the environment
   * or create and register a new {@code DynamicValuesPropertySource} in the
   * environment.
   */
  static DynamicValuesPropertySource getOrCreate(ConfigurableEnvironment environment) {
    PropertySources propertySources = environment.getPropertySources();
    PropertySource<?> propertySource = propertySources.get(PROPERTY_SOURCE_NAME);
    if (propertySource instanceof DynamicValuesPropertySource dynamicValuesPropertySource) {
      return dynamicValuesPropertySource;
    }
    else if (propertySource == null) {
      DynamicValuesPropertySource dynamicValuesPropertySource = new DynamicValuesPropertySource();
      propertySources.addFirst(dynamicValuesPropertySource);
      return dynamicValuesPropertySource;
    }
    else {
      throw new IllegalStateException("PropertySource with name '%s' must be a DynamicValuesPropertySource"
              .formatted(PROPERTY_SOURCE_NAME));
    }
  }

}
