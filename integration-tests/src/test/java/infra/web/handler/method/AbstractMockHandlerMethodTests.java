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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.function.Consumer;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.web.RequestContextUtils;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.MockDispatcherHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for tests using on the DispatcherHandler and HandlerMethod infrastructure classes:
 * <ul>
 * <li>RequestMappingHandlerMapping
 * <li>RequestMappingHandlerAdapter
 * <li>ExceptionHandlerExceptionResolver
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public abstract class AbstractMockHandlerMethodTests {

  @Nullable
  protected MockDispatcherHandler handler;

  protected MockDispatcherHandler getMockHandler() {
    assertThat(handler).as("MockDispatcherHandler not initialized").isNotNull();
    return handler;
  }

  @AfterEach
  public void tearDown() {
    this.handler = null;
  }

  protected ApplicationContext initDispatcher() {
    return initDispatcher(null, null);
  }

  protected ApplicationContext initDispatcher(@Nullable Consumer<GenericApplicationContext> initializer) {
    return initDispatcher(null, initializer);
  }

  /**
   * Initialize a DispatcherHandler instance registering zero or more controller classes.
   */
  protected ApplicationContext initDispatcher(@Nullable Class<?> controllerClass) {
    return initDispatcher(controllerClass, null);
  }

  ApplicationContext initDispatcher(@Nullable Class<?> controllerClass,
          @Nullable Consumer<GenericApplicationContext> initializer) {

    final GenericApplicationContext context = new GenericApplicationContext();

    if (controllerClass != null) {
      context.registerBeanDefinition(
              controllerClass.getSimpleName(), new RootBeanDefinition(controllerClass));
    }

    if (initializer != null) {
      initializer.accept(context);
    }

//        register("handlerAdapter", RequestMappingHandlerAdapter.class, wac);

    register("testConfig", TestConfig.class, context);
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    RequestContextUtils.registerScopes(context.getBeanFactory());

    context.refresh();

    handler = new MockDispatcherHandler(context);
    handler.start();
    return context;
  }

  protected BeanDefinition register(String beanName, Class<?> beanType, GenericApplicationContext wac) {
    if (wac.containsBeanDefinition(beanName)) {
      return wac.getBeanDefinition(beanName);
    }
    RootBeanDefinition beanDef = new RootBeanDefinition(beanType);
    wac.registerBeanDefinition(beanName, beanDef);
    return beanDef;
  }

  protected BeanDefinition register(GenericApplicationContext wac, Class<?> beanType) {
    if (wac.containsBeanDefinition(beanType)) {
      return wac.getBeanDefinition(beanType);
    }
    RootBeanDefinition beanDef = new RootBeanDefinition(beanType);
    wac.registerBeanDefinition(beanType.getSimpleName(), beanDef);
    return beanDef;
  }

  @EnableWebMvc
  @Configuration(proxyBeanMethods = false)
  static class TestConfig implements WebMvcConfigurer {
    private final List<HttpMessageConverter<?>> converters;

    TestConfig(List<HttpMessageConverter<?>> converters) {
      this.converters = converters;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
      if (!converters.isEmpty()) {
        builder.disableDefaults()
                .addCustomConverters(converters);
      }
    }

  }

}
