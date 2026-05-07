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

package infra.testcontainers.context;

import org.testcontainers.lifecycle.Startable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.Supplier;

import infra.beans.factory.config.ConstructorArgumentValues;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.core.MethodIntrospector;
import infra.core.annotation.MergedAnnotations;
import infra.lang.Assert;
import infra.test.context.DynamicPropertyRegistrar;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import
 * {@link DynamicPropertySource @DynamicPropertySource} through a
 * {@link DynamicPropertyRegistrar}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class DynamicPropertySourceMethodsImporter {

  void registerDynamicPropertySources(BeanDefinitionRegistry beanDefinitionRegistry, Class<?> definitionClass, Set<Startable> importedContainers) {
    Set<Method> methods = MethodIntrospector.filterMethods(definitionClass, this::isAnnotated);
    if (methods.isEmpty()) {
      return;
    }
    methods.forEach(this::assertValid);
    RootBeanDefinition registrarDefinition = new RootBeanDefinition();
    registrarDefinition.setBeanClass(DynamicPropertySourcePropertyRegistrar.class);
    ConstructorArgumentValues arguments = new ConstructorArgumentValues();
    arguments.addGenericArgumentValue(methods);
    arguments.addGenericArgumentValue(importedContainers);
    registrarDefinition.setConstructorArgumentValues(arguments);
    beanDefinitionRegistry.registerBeanDefinition(definitionClass.getName() + ".dynamicPropertyRegistrar",
            registrarDefinition);
  }

  private boolean isAnnotated(Method method) {
    return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
  }

  private void assertValid(Method method) {
    Assert.state(Modifier.isStatic(method.getModifiers()),
            () -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
    Class<?>[] types = method.getParameterTypes();
    Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
            () -> "@DynamicPropertySource method '" + method.getName()
                    + "' must accept a single DynamicPropertyRegistry argument");
  }

  static class DynamicPropertySourcePropertyRegistrar implements DynamicPropertyRegistrar {

    private final Set<Method> methods;

    private final Set<Startable> containers;

    DynamicPropertySourcePropertyRegistrar(Set<Method> methods, Set<Startable> containers) {
      this.methods = methods;
      this.containers = containers;
    }

    @Override
    public void accept(DynamicPropertyRegistry registry) {
      DynamicPropertyRegistry containersBackedRegistry = new ContainersBackedDynamicPropertyRegistry(registry,
              this.containers);
      this.methods.forEach((method) -> {
        ReflectionUtils.makeAccessible(method);
        ReflectionUtils.invokeMethod(method, null, containersBackedRegistry);
      });
    }

  }

  static class ContainersBackedDynamicPropertyRegistry implements DynamicPropertyRegistry {

    private final DynamicPropertyRegistry delegate;

    private final Set<Startable> containers;

    ContainersBackedDynamicPropertyRegistry(DynamicPropertyRegistry delegate, Set<Startable> containers) {
      this.delegate = delegate;
      this.containers = containers;
    }

    @Override
    public void add(String name, Supplier<Object> valueSupplier) {
      this.delegate.add(name, () -> {
        startContainers();
        return valueSupplier.get();
      });
    }

    private void startContainers() {
      this.containers.forEach(Startable::start);
    }

  }

}
