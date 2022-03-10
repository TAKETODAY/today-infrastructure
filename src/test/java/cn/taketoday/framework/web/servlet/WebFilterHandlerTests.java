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
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.SimpleBeanDefinitionRegistry;
import cn.taketoday.core.type.classreading.SimpleMetadataReaderFactory;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/8 22:48
 */
class WebFilterHandlerTests {

  private final WebFilterHandler handler = new WebFilterHandler();

  private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

  @SuppressWarnings("unchecked")
  @Test
  void defaultFilterConfiguration() throws IOException {
    AnnotatedBeanDefinition definition = createBeanDefinition(DefaultConfigurationFilter.class);
    this.handler.handle(definition, this.registry);
    BeanDefinition filterRegistrationBean = this.registry
            .getBeanDefinition(DefaultConfigurationFilter.class.getName());
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat(propertyValues.getPropertyValue("asyncSupported")).isEqualTo(false);
    assertThat((EnumSet<jakarta.servlet.DispatcherType>) propertyValues.getPropertyValue("dispatcherTypes"))
            .containsExactly(jakarta.servlet.DispatcherType.REQUEST);
    assertThat(((Map<String, String>) propertyValues.get("initParameters"))).isEmpty();
    assertThat((String[]) propertyValues.getPropertyValue("servletNames")).isEmpty();
    assertThat((String[]) propertyValues.getPropertyValue("urlPatterns")).isEmpty();
    assertThat(propertyValues.getPropertyValue("name")).isEqualTo(DefaultConfigurationFilter.class.getName());
    assertThat(propertyValues.getPropertyValue("filter")).isEqualTo(definition);
  }

  @Test
  void filterWithCustomName() throws IOException {
    AnnotatedBeanDefinition definition = createBeanDefinition(CustomNameFilter.class);
    this.handler.handle(definition, this.registry);
    BeanDefinition filterRegistrationBean = this.registry.getBeanDefinition("custom");
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat(propertyValues.get("name")).isEqualTo("custom");
  }

  @Test
  void asyncSupported() throws IOException {
    BeanDefinition filterRegistrationBean = handleBeanDefinitionForClass(AsyncSupportedFilter.class);
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat(propertyValues.get("asyncSupported")).isEqualTo(true);
  }

  @Test
  @SuppressWarnings("unchecked")
  void dispatcherTypes() throws IOException {
    BeanDefinition filterRegistrationBean = handleBeanDefinitionForClass(DispatcherTypesFilter.class);
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat((Set<jakarta.servlet.DispatcherType>) propertyValues.get("dispatcherTypes")).containsExactly(jakarta.servlet.DispatcherType.FORWARD,
            jakarta.servlet.DispatcherType.INCLUDE, jakarta.servlet.DispatcherType.REQUEST);
  }

  @SuppressWarnings("unchecked")
  @Test
  void initParameters() throws IOException {
    BeanDefinition filterRegistrationBean = handleBeanDefinitionForClass(InitParametersFilter.class);
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat((Map<String, String>) propertyValues.get("initParameters")).containsEntry("a", "alpha")
            .containsEntry("b", "bravo");
  }

  @Test
  void servletNames() throws IOException {
    BeanDefinition filterRegistrationBean = handleBeanDefinitionForClass(ServletNamesFilter.class);
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat((String[]) propertyValues.getPropertyValue("servletNames")).contains("alpha", "bravo");
  }

  @Test
  void urlPatterns() throws IOException {
    BeanDefinition filterRegistrationBean = handleBeanDefinitionForClass(UrlPatternsFilter.class);
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat((String[]) propertyValues.getPropertyValue("urlPatterns")).contains("alpha", "bravo");
  }

  @Test
  void urlPatternsFromValue() throws IOException {
    BeanDefinition filterRegistrationBean = handleBeanDefinitionForClass(UrlPatternsFromValueFilter.class);
    PropertyValues propertyValues = filterRegistrationBean.getPropertyValues();
    assertThat((String[]) propertyValues.getPropertyValue("urlPatterns")).contains("alpha", "bravo");
  }

  @Test
  void urlPatternsDeclaredTwice() {
    assertThatIllegalStateException()
            .isThrownBy(() -> handleBeanDefinitionForClass(UrlPatternsDeclaredTwiceFilter.class))
            .withMessageContaining("The urlPatterns and value attributes are mutually exclusive.");
  }

  private AnnotatedBeanDefinition createBeanDefinition(Class<?> filterClass) throws IOException {
    AnnotatedBeanDefinition definition = mock(AnnotatedBeanDefinition.class);
    given(definition.getBeanClassName()).willReturn(filterClass.getName());
    given(definition.getMetadata()).willReturn(
            new SimpleMetadataReaderFactory().getMetadataReader(filterClass.getName()).getAnnotationMetadata());
    return definition;
  }

  private BeanDefinition handleBeanDefinitionForClass(Class<?> filterClass) throws IOException {
    this.handler.handle(createBeanDefinition(filterClass), this.registry);
    return this.registry.getBeanDefinition(filterClass.getName());
  }

  @WebFilter
  class DefaultConfigurationFilter extends BaseFilter {

  }

  @WebFilter(asyncSupported = true)
  class AsyncSupportedFilter extends BaseFilter {

  }

  @WebFilter(dispatcherTypes = { jakarta.servlet.DispatcherType.REQUEST, jakarta.servlet.DispatcherType.FORWARD, DispatcherType.INCLUDE })
  class DispatcherTypesFilter extends BaseFilter {

  }

  @WebFilter(initParams = { @WebInitParam(name = "a", value = "alpha"), @WebInitParam(name = "b", value = "bravo") })
  class InitParametersFilter extends BaseFilter {

  }

  @WebFilter(servletNames = { "alpha", "bravo" })
  class ServletNamesFilter extends BaseFilter {

  }

  @WebFilter(urlPatterns = { "alpha", "bravo" })
  class UrlPatternsFilter extends BaseFilter {

  }

  @WebFilter({ "alpha", "bravo" })
  class UrlPatternsFromValueFilter extends BaseFilter {

  }

  @WebFilter(value = { "alpha", "bravo" }, urlPatterns = { "alpha", "bravo" })
  class UrlPatternsDeclaredTwiceFilter extends BaseFilter {

  }

  @WebFilter(filterName = "custom")
  class CustomNameFilter extends BaseFilter {

  }

  class BaseFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
    }

    @Override
    public void destroy() {

    }

  }

}
