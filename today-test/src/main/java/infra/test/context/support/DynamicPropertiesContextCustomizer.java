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
