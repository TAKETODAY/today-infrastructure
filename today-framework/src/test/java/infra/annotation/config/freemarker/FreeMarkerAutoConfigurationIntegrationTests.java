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

package infra.annotation.config.freemarker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Locale;

import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.annotation.config.web.RandomPortWebServerConfig;
import infra.annotation.config.web.WebMvcAutoConfiguration;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.util.TestPropertyValues;
import infra.mock.api.http.HttpMockRequest;
import infra.web.server.context.AnnotationConfigWebServerApplicationContext;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;
import infra.web.view.AbstractTemplateViewResolver;
import infra.web.view.View;
import infra.web.view.freemarker.FreeMarkerConfig;
import infra.web.view.freemarker.FreeMarkerConfigurer;
import infra.web.view.freemarker.FreeMarkerViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FreeMarkerAutoConfiguration} Mock support.
 *
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
class FreeMarkerAutoConfigurationIntegrationTests {

  private AnnotationConfigWebServerApplicationContext context;

  @AfterEach
  void close() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void defaultConfiguration() {
    load();
    assertThat(this.context.getBean(FreeMarkerViewResolver.class)).isNotNull();
    assertThat(this.context.getBean(FreeMarkerConfigurer.class)).isNotNull();
    assertThat(this.context.getBean(FreeMarkerConfig.class)).isNotNull();
    assertThat(this.context.getBean(freemarker.template.Configuration.class)).isNotNull();
  }

  @Test
  void defaultViewResolution() throws Exception {
    load();
    MockHttpResponseImpl response = render("home");
    String result = response.getContentAsString();
    assertThat(result).contains("home");
    assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
  }

  @Test
  void customContentType() throws Exception {
    load("freemarker.contentType=application/json");
    MockHttpResponseImpl response = render("home");
    String result = response.getContentAsString();
    assertThat(result).contains("home");
    assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  void customPrefix() throws Exception {
    load("freemarker.prefix:prefix/");
    MockHttpResponseImpl response = render("prefixed");
    String result = response.getContentAsString();
    assertThat(result).contains("prefixed");
  }

  @Test
  void customSuffix() throws Exception {
    load("freemarker.suffix:.freemarker");
    MockHttpResponseImpl response = render("suffixed");
    String result = response.getContentAsString();
    assertThat(result).contains("suffixed");
  }

  @Test
  void customTemplateLoaderPath() throws Exception {
    load("freemarker.templateLoaderPath:classpath:/custom-templates/");
    MockHttpResponseImpl response = render("custom");
    String result = response.getContentAsString();
    assertThat(result).contains("custom");
  }

  @Test
  void disableCache() {
    load("freemarker.cache:false");
    assertThat(this.context.getBean(FreeMarkerViewResolver.class).getCacheLimit()).isZero();
  }

  @Test
  void allowSessionOverride() {
    load("freemarker.allow-session-override:true");
    AbstractTemplateViewResolver viewResolver = this.context.getBean(FreeMarkerViewResolver.class);
    assertThat(viewResolver).hasFieldOrPropertyWithValue("allowSessionOverride", true);
  }

  @SuppressWarnings("deprecation")
  @Test
  void customFreeMarkerSettings() {
    load("freemarker.settings.boolean_format:yup,nope");
    assertThat(this.context.getBean(FreeMarkerConfigurer.class).getConfiguration().getSetting("boolean_format"))
            .isEqualTo("yup,nope");
  }

  @Test
  void renderTemplate() throws Exception {
    load();
    FreeMarkerConfigurer freemarker = this.context.getBean(FreeMarkerConfigurer.class);
    StringWriter writer = new StringWriter();
    freemarker.getConfiguration().getTemplate("message.ftl").process(new DataModel(), writer);
    assertThat(writer.toString()).contains("Hello World");
  }

  private void load(String... env) {
    load(BaseConfiguration.class, env);
  }

  private void load(Class<?> config, String... env) {
    this.context = new AnnotationConfigWebServerApplicationContext();
    TestPropertyValues.of(env).applyTo(this.context);
    this.context.register(config);
    this.context.refresh();
  }

  private MockHttpResponseImpl render(String viewName) throws Exception {
    FreeMarkerViewResolver resolver = this.context.getBean(FreeMarkerViewResolver.class);
    View view = resolver.resolveViewName(viewName, Locale.UK);
    assertThat(view).isNotNull();
    HttpMockRequest request = new HttpMockRequestImpl();
    request.setAttribute(MockUtils.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    MockRequestContext requestContext = new MockRequestContext(null, request, response);
    view.render(null, requestContext);
    return response;
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({ FreeMarkerAutoConfiguration.class, RandomPortWebServerConfig.class,
          WebMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
  static class BaseConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @Import(BaseConfiguration.class)
  static class FilterRegistrationResourceConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @Import(BaseConfiguration.class)
  static class FilterRegistrationOtherConfiguration {

  }

  public static class DataModel {

    public String getGreeting() {
      return "Hello World";
    }

  }

}
