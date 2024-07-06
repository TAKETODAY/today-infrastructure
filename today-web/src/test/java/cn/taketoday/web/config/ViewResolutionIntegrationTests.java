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

package cn.taketoday.web.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.mock.web.MockMockConfig;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.ui.ModelMap;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.mock.MockDispatcher;
import cn.taketoday.web.mock.support.AnnotationConfigWebApplicationContext;
import cn.taketoday.web.view.freemarker.FreeMarkerConfigurer;
import cn.taketoday.web.view.freemarker.FreeMarkerViewResolver;
import cn.taketoday.web.view.groovy.GroovyMarkupConfigurer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Integration tests for view resolution with {@code @EnableWebMvc}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class ViewResolutionIntegrationTests {

  @Nested
  class FreeMarkerTests {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final String EXPECTED_BODY = """
            <html>
            <body>
            <h1>Hello, Java Café</h1>
            </body>
            </html>
            """;

    @Test
    void freemarkerWithInvalidConfig() {
      assertThatRuntimeException()
              .isThrownBy(() -> runTest(InvalidFreeMarkerWebConfig.class))
              .withMessageContaining("In addition to a FreeMarker view resolver ");
    }

    @Test
    void freemarkerWithDefaultEncoding() throws Exception {
      runTestAndAssertResults(DEFAULT_ENCODING, FreeMarkerDefaultEncodingConfig.class);
    }

    @Test
    void freemarkerWithExistingViewResolverWithDefaultEncoding() throws Exception {
      runTestAndAssertResults(DEFAULT_ENCODING, ExistingViewResolverConfig.class);
    }

    @Test
    void freemarkerWithExplicitDefaultEncoding() throws Exception {
      runTestAndAssertResults("UTF-8", ExplicitDefaultEncodingConfig.class);
    }

    private static void runTestAndAssertResults(String encoding, Class<?> configClass) throws Exception {
      MockHttpResponseImpl response = runTest(configClass);
      assertThat(response.isCharset()).as("character encoding set in response").isTrue();
      assertThat(response.getContentAsString()).isEqualTo(EXPECTED_BODY);
      assertThat(response.getCharacterEncoding()).isEqualTo(encoding);
      assertThat(response.getContentType()).isEqualTo("text/html;charset=" + encoding);
    }

    @Configuration
    static class InvalidFreeMarkerWebConfig extends WebMvcConfigurationSupport {

      @Override
      public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.freeMarker();
      }
    }

    @Configuration
    static class FreeMarkerDefaultEncodingConfig extends AbstractWebConfig {

      @Override
      public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.freeMarker();
      }

      @Bean
      public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("classpath:templates/");
        return configurer;
      }
    }

    @Configuration
    static class ExistingViewResolverConfig extends AbstractWebConfig {

      @Bean
      public FreeMarkerViewResolver freeMarkerViewResolver() {
        return new FreeMarkerViewResolver("", ".ftl");
      }

      @Bean
      public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("classpath:templates/");
        return configurer;
      }
    }

    @Configuration
    static class ExplicitDefaultEncodingConfig extends AbstractWebConfig {

      @Override
      public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.freeMarker();
      }

      @Bean
      public FreeMarkerConfigurer freeMarkerConfigurer() {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setTemplateLoaderPath("classpath:templates/");
        configurer.setDefaultCharset(UTF_8);
        return configurer;
      }
    }

  }

  @Nested
  class GroovyMarkupTests {

    @Test
    void groovyMarkupInvalidConfig() {
      assertThatRuntimeException()
              .isThrownBy(() -> runTest(InvalidGroovyMarkupWebConfig.class))
              .withMessageContaining("In addition to a Groovy markup view resolver ");
    }

    @Test
    void groovyMarkup() throws Exception {
      MockHttpResponseImpl response = runTest(GroovyMarkupWebConfig.class);
      assertThat(response.getContentAsString()).isEqualTo("<html><body>Hello, Java Café</body></html>");
    }

    @Configuration
    static class InvalidGroovyMarkupWebConfig extends WebMvcConfigurationSupport {

      @Override
      public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.groovy();
      }
    }

    @Configuration
    static class GroovyMarkupWebConfig extends AbstractWebConfig {

      @Override
      public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.groovy();
      }

      @Bean
      public GroovyMarkupConfigurer groovyMarkupConfigurer() {
        GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
        configurer.setResourceLoaderPath("classpath:templates/");
        return configurer;
      }
    }
  }

  private static MockHttpResponseImpl runTest(Class<?> configClass) throws Exception {
    String basePath = "cn/taketoday/web/config";
    MockContext mockContext = new MockContextImpl(basePath);
    MockConfig mockConfig = new MockMockConfig(mockContext);
    MockRequest request = new HttpMockRequestImpl("GET", "/");
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(configClass);
    context.setMockContext(mockContext);
    context.refresh();
    MockDispatcher dispatcher = new MockDispatcher(context);
    dispatcher.init(mockConfig);
    dispatcher.service(request, response);
    return response;
  }

  @Controller
  static class SampleController {

    @GetMapping
    String index(ModelMap model) {
      model.put("hello", "Hello");
      return "index";
    }
  }

  @EnableWebMvc
  abstract static class AbstractWebConfig implements WebMvcConfigurer {

    @Bean
    public SampleController sampleController() {
      return new SampleController();
    }
  }

}
