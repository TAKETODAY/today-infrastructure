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

package infra.web.server.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import infra.annotation.config.web.WebProperties.Resources;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.Ordered;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.ui.template.TemplateAvailabilityProvider;
import infra.ui.template.TemplateAvailabilityProviders;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;
import infra.web.view.ModelAndView;

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