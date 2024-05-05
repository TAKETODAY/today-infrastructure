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

package cn.taketoday.web.server.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.annotation.config.web.WebProperties.Resources;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.ui.template.TemplateAvailabilityProvider;
import cn.taketoday.ui.template.TemplateAvailabilityProviders;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/28 21:59
 */
@ExtendWith(MockitoExtension.class)
class DefaultErrorViewResolverTests {

  private DefaultErrorViewResolver resolver;

  @Mock
  private TemplateAvailabilityProvider templateAvailabilityProvider;

  private final Resources resourcesProperties = new Resources();

  private final Map<String, Object> model = new HashMap<>();

  private final MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

  private final RequestContext request = new MockRequestContext(null,
          new HttpMockRequestImpl(), mockResponse);

  @BeforeEach
  void setup() {
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.refresh();
    TemplateAvailabilityProviders templateAvailabilityProviders = new TestTemplateAvailabilityProviders(
            this.templateAvailabilityProvider);
    this.resolver = new DefaultErrorViewResolver(applicationContext, this.resourcesProperties.staticLocations,
            templateAvailabilityProviders);
  }

  @Test
  void createWhenApplicationContextIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new DefaultErrorViewResolver(null, new Resources().staticLocations))
            .withMessageContaining("ApplicationContext is required");
  }

  @Test
  void createWhenResourcePropertiesIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DefaultErrorViewResolver(mock(ApplicationContext.class), null))
            .withMessageContaining("staticLocations is required");
  }

  @Test
  void resolveWhenNoMatchShouldReturnNull() {
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.NOT_FOUND, this.model);
    assertThat(resolved).isNull();
  }

  @Test
  void resolveWhenExactTemplateMatchShouldReturnTemplate() {
    given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/404"), any(Environment.class),
            any(ClassLoader.class), any(ResourceLoader.class)))
            .willReturn(true);
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.NOT_FOUND, this.model);
    assertThat(resolved).isNotNull();
    assertThat(resolved.getViewName()).isEqualTo("error/404");
    then(this.templateAvailabilityProvider).should()
            .isTemplateAvailable(eq("error/404"), any(Environment.class), any(ClassLoader.class),
                    any(ResourceLoader.class));
    then(this.templateAvailabilityProvider).shouldHaveNoMoreInteractions();
  }

  @Test
  void resolveWhenSeries5xxTemplateMatchShouldReturnTemplate() {
    given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/503"), any(Environment.class),
            any(ClassLoader.class), any(ResourceLoader.class)))
            .willReturn(false);
    given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/5xx"), any(Environment.class),
            any(ClassLoader.class), any(ResourceLoader.class)))
            .willReturn(true);
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.SERVICE_UNAVAILABLE,
            this.model);
    assertThat(resolved.getViewName()).isEqualTo("error/5xx");
  }

  @Test
  void resolveWhenSeries4xxTemplateMatchShouldReturnTemplate() {
    given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/404"), any(Environment.class),
            any(ClassLoader.class), any(ResourceLoader.class)))
            .willReturn(false);
    given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/4xx"), any(Environment.class),
            any(ClassLoader.class), any(ResourceLoader.class)))
            .willReturn(true);
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.NOT_FOUND, this.model);
    assertThat(resolved.getViewName()).isEqualTo("error/4xx");
  }

  @Test
  void resolveWhenExactResourceMatchShouldReturnResource() throws Exception {
    setResourceLocation("/exact");
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.NOT_FOUND, this.model);
    MockHttpResponseImpl response = render(resolved);
    assertThat(response.getContentAsString().trim()).isEqualTo("exact/404");
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
  }

  @Test
  void resolveWhenSeries4xxResourceMatchShouldReturnResource() throws Exception {
    setResourceLocation("/4xx");
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.NOT_FOUND, this.model);
    MockHttpResponseImpl response = render(resolved);
    assertThat(response.getContentAsString().trim()).isEqualTo("4xx/4xx");
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
  }

  @Test
  void resolveWhenSeries5xxResourceMatchShouldReturnResource() throws Exception {
    setResourceLocation("/5xx");
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.INTERNAL_SERVER_ERROR, this.model);
    MockHttpResponseImpl response = render(resolved);
    assertThat(response.getContentAsString().trim()).isEqualTo("5xx/5xx");
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
  }

  @Test
  void resolveWhenTemplateAndResourceMatchShouldFavorTemplate() {
    setResourceLocation("/exact");
    given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/404"), any(Environment.class),
            any(ClassLoader.class), any(ResourceLoader.class)))
            .willReturn(true);
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.NOT_FOUND, this.model);
    assertThat(resolved.getViewName()).isEqualTo("error/404");
  }

  @Test
  void resolveWhenExactResourceMatchAndSeriesTemplateMatchShouldFavorResource() throws Exception {
    setResourceLocation("/exact");
    given(this.templateAvailabilityProvider.isTemplateAvailable(eq("error/404"), any(Environment.class),
            any(ClassLoader.class), any(ResourceLoader.class)))
            .willReturn(false);
    ModelAndView resolved = this.resolver.resolveErrorView(this.request, HttpStatus.NOT_FOUND, this.model);
    then(this.templateAvailabilityProvider).shouldHaveNoMoreInteractions();
    MockHttpResponseImpl response = render(resolved);
    assertThat(response.getContentAsString().trim()).isEqualTo("exact/404");
    assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
  }

  @Test
  void orderShouldBeLowest() {
    assertThat(this.resolver.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  void setOrderShouldChangeOrder() {
    this.resolver.setOrder(123);
    assertThat(this.resolver.getOrder()).isEqualTo(123);
  }

  private void setResourceLocation(String path) {
    String packageName = getClass().getPackage().getName();
    this.resourcesProperties
            .setStaticLocations(new String[] { "classpath:" + packageName.replace('.', '/') + path + "/" });
    setup();
  }

  private MockHttpResponseImpl render(ModelAndView modelAndView) throws Exception {
    modelAndView.getView().render(this.model, this.request);
    return mockResponse;
  }

  static class TestTemplateAvailabilityProviders extends TemplateAvailabilityProviders {

    TestTemplateAvailabilityProviders(TemplateAvailabilityProvider provider) {
      super(Collections.singletonList(provider));
    }

  }

}