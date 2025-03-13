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

package infra.web.handler.method;

import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.function.Consumer;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.annotation.Configuration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.lang.Nullable;
import infra.mock.web.MockMockConfig;
import infra.stereotype.Component;
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

    @Autowired
    private HttpMessageConverters messageConverters;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
      converters.addAll(messageConverters.getConverters());
    }

    @Component
    @ConditionalOnMissingBean
    static HttpMessageConverters messageConverters(List<HttpMessageConverter<?>> converters) {
      return new HttpMessageConverters(converters);
    }

  }

}
