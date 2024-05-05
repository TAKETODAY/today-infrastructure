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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.AfterEach;

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverters;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockServletConfig;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.support.GenericWebApplicationContext;

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
public abstract class AbstractServletHandlerMethodTests {

  @Nullable
  protected DispatcherServlet servlet;

  protected DispatcherServlet getServlet() {
    assertThat(servlet).as("DispatcherServlet not initialized").isNotNull();
    return servlet;
  }

  @AfterEach
  public void tearDown() {
    this.servlet = null;
  }

  protected WebApplicationContext initDispatcherServlet() {
    return initDispatcherServlet(null, null);
  }

  protected WebApplicationContext initDispatcherServlet(@Nullable Consumer<GenericWebApplicationContext> initializer) {
    return initDispatcherServlet(null, initializer);
  }

  /**
   * Initialize a DispatcherServlet instance registering zero or more controller classes.
   */
  protected WebApplicationContext initDispatcherServlet(@Nullable Class<?> controllerClass) {
    return initDispatcherServlet(controllerClass, null);
  }

  WebApplicationContext initDispatcherServlet(@Nullable Class<?> controllerClass,
          @Nullable Consumer<GenericWebApplicationContext> initializer) {

    final GenericWebApplicationContext wac = new GenericWebApplicationContext();

    servlet = new DispatcherServlet() {

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

    MockServletConfig config = new MockServletConfig();
    config.addInitParameter("cn.taketoday.mock.api.http.legacyDoHead", "true");
    wac.setServletConfig(config);
    wac.setServletContext(config.getServletContext());

    servlet.init(config);
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
