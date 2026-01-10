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
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.mock.web.MockMockConfig;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.config.annotation.WebMvcConfigurer;
import infra.web.mock.MockDispatcher;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.support.GenericWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for tests using on the DispatcherServlet and HandlerMethod infrastructure classes:
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
  protected MockDispatcher mockapi;

  protected MockDispatcher getMockApi() {
    assertThat(mockapi).as("DispatcherMockApi not initialized").isNotNull();
    return mockapi;
  }

  @AfterEach
  public void tearDown() {
    this.mockapi = null;
  }

  protected WebApplicationContext initDispatcher() {
    return initDispatcher(null, null);
  }

  protected WebApplicationContext initDispatcher(@Nullable Consumer<GenericWebApplicationContext> initializer) {
    return initDispatcher(null, initializer);
  }

  /**
   * Initialize a DispatcherServlet instance registering zero or more controller classes.
   */
  protected WebApplicationContext initDispatcher(@Nullable Class<?> controllerClass) {
    return initDispatcher(controllerClass, null);
  }

  WebApplicationContext initDispatcher(@Nullable Class<?> controllerClass,
          @Nullable Consumer<GenericWebApplicationContext> initializer) {

    final GenericWebApplicationContext wac = new GenericWebApplicationContext();

    mockapi = new MockDispatcher() {

      @Override
      protected ApplicationContext createApplicationContext(@Nullable ApplicationContext parent) {

        if (controllerClass != null) {
          wac.registerBeanDefinition(
                  controllerClass.getSimpleName(), new RootBeanDefinition(controllerClass));
        }

        if (initializer != null) {
          initializer.accept(wac);
        }

//        register("handlerAdapter", RequestMappingHandlerAdapter.class, wac);

        register("testConfig", TestConfig.class, wac);

        AnnotationConfigUtils.registerAnnotationConfigProcessors(wac);

        wac.refresh();
        return wac;
      }
    };

    MockMockConfig config = new MockMockConfig();
    config.addInitParameter("infra.mock.api.http.legacyDoHead", "true");
    wac.setMockConfig(config);
    wac.setMockContext(config.getMockContext());

    mockapi.init(config);
    return wac;
  }

  protected BeanDefinition register(String beanName, Class<?> beanType, GenericWebApplicationContext wac) {
    if (wac.containsBeanDefinition(beanName)) {
      return wac.getBeanDefinition(beanName);
    }
    RootBeanDefinition beanDef = new RootBeanDefinition(beanType);
    wac.registerBeanDefinition(beanName, beanDef);
    return beanDef;
  }

  protected BeanDefinition register(GenericWebApplicationContext wac, Class<?> beanType) {
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
        builder.unregisterDefaults()
                .addCustomConverters(converters);
      }
    }

  }

}
