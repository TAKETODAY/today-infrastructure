/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.SimpleBeanDefinitionRegistry;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/8 22:49
 */
class WebServletHandlerTests {

  private final WebServletHandler handler = new WebServletHandler();

  private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

  @SuppressWarnings("unchecked")
  @Test
  void defaultServletConfiguration() throws IOException {
    AnnotatedBeanDefinition servletDefinition = createBeanDefinition(DefaultConfigurationServlet.class);
    this.handler.handle(servletDefinition, this.registry);
    BeanDefinition servletRegistrationBean = this.registry
            .getBeanDefinition(DefaultConfigurationServlet.class.getName());
    PropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
    assertThat(propertyValues.getPropertyValue("asyncSupported")).isEqualTo(false);
    assertThat(((Map<String, String>) propertyValues.getPropertyValue("initParameters"))).isEmpty();
    assertThat((Integer) propertyValues.getPropertyValue("loadOnStartup")).isEqualTo(-1);
    assertThat(propertyValues.getPropertyValue("name")).isEqualTo(DefaultConfigurationServlet.class.getName());
    assertThat((String[]) propertyValues.getPropertyValue("urlMappings")).isEmpty();
    assertThat(propertyValues.getPropertyValue("servlet")).isEqualTo(servletDefinition);
  }

  @Test
  void servletWithCustomName() throws IOException {
    AnnotatedBeanDefinition definition = createBeanDefinition(CustomNameServlet.class);
    this.handler.handle(definition, this.registry);
    BeanDefinition servletRegistrationBean = this.registry.getBeanDefinition("custom");
    PropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
    assertThat(propertyValues.getPropertyValue("name")).isEqualTo("custom");
  }

  @Test
  void asyncSupported() throws IOException {
    BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(AsyncSupportedServlet.class);
    PropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
    assertThat(propertyValues.getPropertyValue("asyncSupported")).isEqualTo(true);
  }

  @SuppressWarnings("unchecked")
  @Test
  void initParameters() throws IOException {
    BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(InitParametersServlet.class);
    PropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
    assertThat((Map<String, String>) propertyValues.getPropertyValue("initParameters")).containsEntry("a", "alpha")
            .containsEntry("b", "bravo");
  }

  @Test
  void urlMappings() throws IOException {
    BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(UrlPatternsServlet.class);
    PropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
    assertThat((String[]) propertyValues.getPropertyValue("urlMappings")).contains("alpha", "bravo");
  }

  @Test
  void urlMappingsFromValue() throws IOException {
    BeanDefinition servletRegistrationBean = handleBeanDefinitionForClass(UrlPatternsFromValueServlet.class);
    PropertyValues propertyValues = servletRegistrationBean.getPropertyValues();
    assertThat((String[]) propertyValues.getPropertyValue("urlMappings")).contains("alpha", "bravo");
  }

  @Test
  void urlPatternsDeclaredTwice() {
    assertThatIllegalStateException()
            .isThrownBy(() -> handleBeanDefinitionForClass(UrlPatternsDeclaredTwiceServlet.class))
            .withMessageContaining("The urlPatterns and value attributes are mutually exclusive.");
  }

  private AnnotatedBeanDefinition createBeanDefinition(Class<?> servletClass) throws IOException {
    AnnotatedBeanDefinition definition = mock(AnnotatedBeanDefinition.class);
    given(definition.getBeanClassName()).willReturn(servletClass.getName());
    given(definition.getMetadata()).willReturn(
            new SimpleMetadataReaderFactory().getMetadataReader(servletClass.getName()).getAnnotationMetadata());
    return definition;
  }

  private BeanDefinition handleBeanDefinitionForClass(Class<?> filterClass) throws IOException {
    this.handler.handle(createBeanDefinition(filterClass), this.registry);
    return this.registry.getBeanDefinition(filterClass.getName());
  }

  @WebServlet
  class DefaultConfigurationServlet extends HttpServlet {

  }

  @WebServlet(asyncSupported = true)
  class AsyncSupportedServlet extends HttpServlet {

  }

  @WebServlet(initParams = { @WebInitParam(name = "a", value = "alpha"), @WebInitParam(name = "b", value = "bravo") })
  class InitParametersServlet extends HttpServlet {

  }

  @WebServlet(urlPatterns = { "alpha", "bravo" })
  class UrlPatternsServlet extends HttpServlet {

  }

  @WebServlet({ "alpha", "bravo" })
  class UrlPatternsFromValueServlet extends HttpServlet {

  }

  @WebServlet(value = { "alpha", "bravo" }, urlPatterns = { "alpha", "bravo" })
  class UrlPatternsDeclaredTwiceServlet extends HttpServlet {

  }

  @WebServlet(name = "custom")
  class CustomNameServlet extends HttpServlet {

  }

}
