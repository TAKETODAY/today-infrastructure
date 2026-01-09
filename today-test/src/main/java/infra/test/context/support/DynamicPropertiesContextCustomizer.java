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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import infra.context.ConfigurableApplicationContext;
import infra.core.env.PropertySources;
import infra.lang.Assert;
import infra.test.context.ContextCustomizer;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.test.context.MergedContextConfiguration;
import infra.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} to support
 * {@link DynamicPropertySource @DynamicPropertySource} methods.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see DynamicPropertiesContextCustomizerFactory
 * @since 4.0
 */
class DynamicPropertiesContextCustomizer implements ContextCustomizer {

  private static final String PROPERTY_SOURCE_NAME = "Dynamic Test Properties";

  private final Set<Method> methods;

  DynamicPropertiesContextCustomizer(Set<Method> methods) {
    methods.forEach(this::assertValid);
    this.methods = methods;
  }

  private void assertValid(Method method) {
    Assert.state(Modifier.isStatic(method.getModifiers()),
            () -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
    Class<?>[] types = method.getParameterTypes();
    Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
            () -> "@DynamicPropertySource method '" + method.getName() + "' must accept a single DynamicPropertyRegistry argument");
  }

  @Override
  public void customizeContext(ConfigurableApplicationContext context,
          MergedContextConfiguration mergedConfig) {

    PropertySources sources = context.getEnvironment().getPropertySources();
    sources.addFirst(new DynamicValuesPropertySource(PROPERTY_SOURCE_NAME, buildDynamicPropertiesMap()));
  }

  private Map<String, Supplier<Object>> buildDynamicPropertiesMap() {
    Map<String, Supplier<Object>> map = new LinkedHashMap<>();
    DynamicPropertyRegistry dynamicPropertyRegistry = (name, valueSupplier) -> {
      Assert.hasText(name, "'name' must not be null or blank");
      Assert.notNull(valueSupplier, "'valueSupplier' is required");
      map.put(name, valueSupplier);
    };
    this.methods.forEach(method -> {
      ReflectionUtils.makeAccessible(method);
      ReflectionUtils.invokeMethod(method, null, dynamicPropertyRegistry);
    });
    return Collections.unmodifiableMap(map);
  }

  Set<Method> getMethods() {
    return this.methods;
  }

  @Override
  public int hashCode() {
    return this.methods.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.methods.equals(((DynamicPropertiesContextCustomizer) obj).methods);
  }

}
