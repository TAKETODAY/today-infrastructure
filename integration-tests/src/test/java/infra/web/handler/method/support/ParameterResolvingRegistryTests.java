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

package infra.web.handler.method.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Import;
import infra.core.MethodParameter;
import infra.web.ServerHttpExchange;
import infra.web.MockHandlerParameter;
import infra.web.annotation.RequestParam;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.handler.method.HandlerParameter;
import infra.web.handler.method.ParameterResolverNotFoundException;
import infra.web.handler.method.ParameterResolvingRegistry;
import infra.web.handler.method.ParameterResolvingStrategy;
import infra.web.handler.method.RegistryHandlerParameterFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/12 22:29
 */
class ParameterResolvingRegistryTests {

  @Test
  void explicitlyRegisteredRegistryIsNotPopulatedByFallback() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(ParameterResolvingRegistry.class);
      context.refresh();

      var registry = context.getBean(ParameterResolvingRegistry.class);
      assertThat(ParameterResolvingRegistryFactory.getOrCreate(context)).isSameAs(registry);
      assertThat(registry.getDefaultStrategies()).isEmpty();
    }
  }

  void p(String data) {

  }

  @Test
  void emptyRegistryDoesNotResolveParameters() throws NoSuchMethodException {
    var registry = new ParameterResolvingRegistry();
    HandlerParameter parameter = stringParameter();
    assertThat(registry.findStrategy(parameter)).isNull();
    assertThatThrownBy(() -> registry.obtainStrategy(parameter))
            .isInstanceOf(ParameterResolverNotFoundException.class)
            .hasMessageStartingWith("There isn't have a parameter resolver to resolve parameter");
  }

  @Test
  void factoryPreservesBuiltInOrder() throws NoSuchMethodException {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      var registry = ParameterResolvingRegistryFactory.create(context);
      assertThat(registry.getDefaultStrategies().getStrategies())
              .map(Object::getClass)
              .startsWith(RequestParamMethodArgumentResolver.class, RequestParamMapMethodArgumentResolver.class)
              .endsWith(RequestParamMethodArgumentResolver.class, ModelAttributeMethodProcessor.class);
      assertThat(registry.obtainStrategy(stringParameter())).isInstanceOf(RequestParamMethodArgumentResolver.class);
      assertThat(context.getBeansOfType(ParameterResolvingRegistry.class)).isEmpty();
    }
  }

  private HandlerParameter stringParameter() throws NoSuchMethodException {
    Method method = getClass().getDeclaredMethod("p", String.class);
    return new MockHandlerParameter(MethodParameter.forExecutable(method, 0), "data");
  }

  @Test
  void optionalParameterCanMatchStrategyInDefaultGroup() throws NoSuchMethodException {
    var registry = new ParameterResolvingRegistry();
    var strategy = new ParameterResolvingStrategy0();
    registry.addDefaultStrategies(strategy);

    assertThat(registry.findStrategy(new HandlerParameter(optionalParameter()))).isSameAs(strategy);
  }

  @Test
  void customStrategyCanResolveOptionalParameter() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
      var registry = context.getBean(ParameterResolvingRegistry.class);
      var factory = new RegistryHandlerParameterFactory(registry);
      var parameter = factory.createParameter(optionalParameter());

      assertThat(registry.obtainStrategy(parameter)).isSameAs(context.getBean(ParameterResolvingStrategy0.class));
      assertThat(parameter.resolveArgument(mock(ServerHttpExchange.class))).isEqualTo(Optional.of(42));
    }
  }

  private MethodParameter optionalParameter() throws NoSuchMethodException {
    return MethodParameter.forExecutable(getClass().getDeclaredMethod("optionalQuery", Optional.class), 0);
  }

  void optionalQuery(@RequestParam("value") Optional<Integer> value) {
  }

  @Test
  void fallbackRegistryUsesBuiltInStrategiesAndIsReused() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();

      ParameterResolvingRegistry fallback = ParameterResolvingRegistryFactory.getOrCreate(context);

      assertThat(fallback.contains(RequestParamMethodArgumentResolver.class)).isTrue();
      assertThat(ParameterResolvingRegistryFactory.getOrCreate(context)).isSameAs(fallback);
      assertThat(context.getBean("parameterResolvingRegistry")).isSameAs(fallback);
    }
  }

  @Test
  void ambiguousRegistryBeansDoNotTriggerFallback() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean("firstRegistry", ParameterResolvingRegistry.class);
      context.registerBean("secondRegistry", ParameterResolvingRegistry.class);
      context.refresh();

      assertThatThrownBy(() -> ParameterResolvingRegistryFactory.getOrCreate(context))
              .isInstanceOf(NoUniqueBeanDefinitionException.class);
      assertThat(context.getBeanNamesForType(ParameterResolvingRegistry.class))
              .containsExactlyInAnyOrder("firstRegistry", "secondRegistry");
    }
  }

  @Test
  void primaryRegistryBeanIsReused() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean("firstRegistry", ParameterResolvingRegistry.class);
      context.registerBean("primaryRegistry", ParameterResolvingRegistry.class, definition -> definition.setPrimary(true));
      context.refresh();

      assertThat(ParameterResolvingRegistryFactory.getOrCreate(context)).isSameAs(context.getBean("primaryRegistry"));
      assertThat(context.containsBean("parameterResolvingRegistry")).isFalse();
    }
  }

  @Test
  void singletonWithReservedNameAndWrongTypeIsNotReplaced() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.getBeanFactory().registerSingleton("parameterResolvingRegistry", "not a registry");
      context.refresh();

      assertThatThrownBy(() -> ParameterResolvingRegistryFactory.getOrCreate(context))
              .isInstanceOf(BeanNotOfRequiredTypeException.class);
      assertThat(context.getBean("parameterResolvingRegistry")).isEqualTo("not a registry");
    }
  }

  @Test
  void concurrentFirstLookupsShareFallbackRegistry() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.refresh();
      int callers = 8;
      var start = new CyclicBarrier(callers);
      var lookups = new ArrayList<Future<ParameterResolvingRegistry>>(callers);
      var executor = Executors.newFixedThreadPool(callers);
      try {
        for (int i = 0; i < callers; i++) {
          lookups.add(executor.submit(() -> {
            start.await(10, TimeUnit.SECONDS);
            return ParameterResolvingRegistryFactory.getOrCreate(context);
          }));
        }

        var registry = lookups.get(0).get(10, TimeUnit.SECONDS);
        for (var lookup : lookups) {
          assertThat(lookup.get(10, TimeUnit.SECONDS)).isSameAs(registry);
        }
        assertThat(context.getBeansOfType(ParameterResolvingRegistry.class))
                .containsOnlyKeys("parameterResolvingRegistry").containsValue(registry);
      }
      finally {
        executor.shutdownNow();
      }
    }
  }

  @Test
  void mvcRegistryUsesCustomStrategyBeforeBuiltInStrategies() throws NoSuchMethodException {
    try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
      var registry = context.getBean(ParameterResolvingRegistry.class);
      assertThat(registry.contains(RequestParamMethodArgumentResolver.class)).isTrue();
      assertThat(registry.obtainStrategy(stringParameter())).isSameAs(context.getBean(ParameterResolvingStrategy0.class));
      assertThat(ParameterResolvingRegistryFactory.getOrCreate(context)).isSameAs(registry);
    }
  }

  @EnableWebMvc
  @Import(ParameterResolvingStrategy0.class)
  static class AppConfig {

  }

  static class ParameterResolvingStrategy0 implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(HandlerParameter parameter) {
      return parameter.is(String.class) || parameter.is(Optional.class);
    }

    @Nullable
    @Override
    public Object resolveArgument(ServerHttpExchange exchange, HandlerParameter parameter) {
      return parameter.is(Optional.class) ? Optional.of(42) : null;
    }
  }

}
