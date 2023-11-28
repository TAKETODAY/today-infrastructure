/*
 * Copyright 2017 - 2023 the original author or authors.
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Abstract base for {@link AbstractFilterRegistrationBean} tests.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractFilterRegistrationBeanTests {

  @Mock
  ServletContext servletContext;

  @Mock
  FilterRegistration.Dynamic registration;

  @Test
  void startupWithDefaults() throws Exception {
    given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    bean.onStartup(this.servletContext);
    then(this.servletContext).should().addFilter(eq("mockFilter"), getExpectedFilter());
    then(this.registration).should().setAsyncSupported(true);
    then(this.registration).should().addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
  }

  @Test
  void startupWithSpecifiedValues() throws Exception {
    given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    bean.setName("test");
    bean.setAsyncSupported(false);
    bean.setInitParameters(Collections.singletonMap("a", "b"));
    bean.addInitParameter("c", "d");
    bean.setUrlPatterns(new LinkedHashSet<>(Arrays.asList("/a", "/b")));
    bean.addUrlPatterns("/c");
    bean.setServletNames(new LinkedHashSet<>(Arrays.asList("s1", "s2")));
    bean.addServletNames("s3");
    bean.setServletRegistrationBeans(Collections.singleton(mockServletRegistration("s4")));
    bean.addServletRegistrationBeans(mockServletRegistration("s5"));
    bean.setMatchAfter(true);
    bean.onStartup(this.servletContext);
    then(this.servletContext).should().addFilter(eq("test"), getExpectedFilter());
    then(this.registration).should().setAsyncSupported(false);
    Map<String, String> expectedInitParameters = new HashMap<>();
    expectedInitParameters.put("a", "b");
    expectedInitParameters.put("c", "d");
    then(this.registration).should().setInitParameters(expectedInitParameters);
    then(this.registration).should().addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/a", "/b",
            "/c");
    then(this.registration).should().addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), true, "s4", "s5",
            "s1", "s2", "s3");
  }

  @Test
  void specificName() throws Exception {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    bean.setName("specificName");
    bean.onStartup(this.servletContext);
    then(this.servletContext).should().addFilter(eq("specificName"), getExpectedFilter());
  }

  @Test
  void deducedName() throws Exception {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    bean.onStartup(this.servletContext);
    then(this.servletContext).should().addFilter(eq("mockFilter"), getExpectedFilter());
  }

  @Test
  void disable() throws Exception {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    bean.setEnabled(false);
    bean.onStartup(this.servletContext);
    then(this.servletContext).should(never()).addFilter(eq("mockFilter"), getExpectedFilter());
  }

  @Test
  void setServletRegistrationBeanMustNotBeNull() {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    assertThatIllegalArgumentException().isThrownBy(() -> bean.setServletRegistrationBeans(null))
            .withMessageContaining("ServletRegistrationBeans is required");
  }

  @Test
  void addServletRegistrationBeanMustNotBeNull() {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> bean.addServletRegistrationBeans((ServletRegistrationBean[]) null))
            .withMessageContaining("ServletRegistrationBeans is required");
  }

  @Test
  void setServletRegistrationBeanReplacesValue() throws Exception {
    given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean(mockServletRegistration("a"));
    bean.setServletRegistrationBeans(
            new LinkedHashSet<ServletRegistrationBean<?>>(Collections.singletonList(mockServletRegistration("b"))));
    bean.onStartup(this.servletContext);
    then(this.registration).should().addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST), false, "b");
  }

  @Test
  void modifyInitParameters() throws Exception {
    given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    bean.addInitParameter("a", "b");
    bean.getInitParameters().put("a", "c");
    bean.onStartup(this.servletContext);
    then(this.registration).should().setInitParameters(Collections.singletonMap("a", "c"));
  }

  @Test
  void setUrlPatternMustNotBeNull() {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    assertThatIllegalArgumentException().isThrownBy(() -> bean.setUrlPatterns(null))
            .withMessageContaining("UrlPatterns is required");
  }

  @Test
  void addUrlPatternMustNotBeNull() {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    assertThatIllegalArgumentException().isThrownBy(() -> bean.addUrlPatterns((String[]) null))
            .withMessageContaining("UrlPatterns is required");
  }

  @Test
  void setServletNameMustNotBeNull() {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    assertThatIllegalArgumentException().isThrownBy(() -> bean.setServletNames(null))
            .withMessageContaining("ServletNames is required");
  }

  @Test
  void addServletNameMustNotBeNull() {
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    assertThatIllegalArgumentException().isThrownBy(() -> bean.addServletNames((String[]) null))
            .withMessageContaining("ServletNames is required");
  }

  @Test
  void withSpecificDispatcherTypes() throws Exception {
    given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    bean.setDispatcherTypes(DispatcherType.INCLUDE, DispatcherType.FORWARD);
    bean.onStartup(this.servletContext);
    then(this.registration).should()
            .addMappingForUrlPatterns(EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD), false, "/*");
  }

  @Test
  void withSpecificDispatcherTypesEnumSet() throws Exception {
    given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
    AbstractFilterRegistrationBean<?> bean = createFilterRegistrationBean();
    EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD);
    bean.setDispatcherTypes(types);
    bean.onStartup(this.servletContext);
    then(this.registration).should().addMappingForUrlPatterns(types, false, "/*");
  }

  protected abstract Filter getExpectedFilter();

  protected abstract AbstractFilterRegistrationBean<?> createFilterRegistrationBean(
          ServletRegistrationBean<?>... servletRegistrationBeans);

  protected final ServletRegistrationBean<?> mockServletRegistration(String name) {
    ServletRegistrationBean<?> bean = new ServletRegistrationBean<>();
    bean.setName(name);
    return bean;
  }

}
