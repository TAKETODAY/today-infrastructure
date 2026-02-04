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

package infra.groovy.template.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import groovy.text.markup.BaseTemplate;
import groovy.text.markup.DelegatingIndentWriter;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import infra.app.web.context.StandardWebEnvironment;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.i18n.LocaleContextHolder;
import infra.core.io.ClassPathResource;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.BuildOutput;
import infra.test.classpath.resources.WithResource;
import infra.test.util.TestPropertyValues;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;
import infra.web.view.View;
import infra.web.view.groovy.GroovyMarkupConfig;
import infra.web.view.groovy.GroovyMarkupConfigurer;
import infra.web.view.groovy.GroovyMarkupViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GroovyTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 */
class GroovyTemplateAutoConfigurationTests {

  private final BuildOutput buildOutput = new BuildOutput(getClass());

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @BeforeEach
  void setup() {
    context.setEnvironment(new StandardWebEnvironment());
  }

  @AfterEach
  void close() {
    LocaleContextHolder.resetLocaleContext();
    this.context.close();
  }

  @Test
  void defaultConfiguration() {
    registerAndRefreshContext();
    assertThat(this.context.getBean(GroovyMarkupViewResolver.class)).isNotNull();
  }

  @Test
  void emptyTemplateLocation() {
    new File(this.buildOutput.getTestResourcesLocation(), "empty-templates/empty-directory").mkdirs();
    registerAndRefreshContext("groovy.template.resource-loader-path:classpath:/templates/empty-directory/");
  }

  @Test
  @WithResource(name = "templates/home.tpl", content = "yield 'home'")
  void defaultViewResolution() throws Exception {
    registerAndRefreshContext();
    MockHttpResponseImpl response = render("home");
    String result = response.getContentAsString();
    assertThat(result).contains("home");
    assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
  }

  @Test
  @WithResource(name = "templates/includes.tpl", content = """
          yield 'include'
          include template: 'included.tpl'
          """)
  @WithResource(name = "templates/included.tpl", content = "yield 'here'")
  void includesViewResolution() throws Exception {
    registerAndRefreshContext();
    MockHttpResponseImpl response = render("includes");
    String result = response.getContentAsString();
    assertThat(result).contains("here");
    assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
  }

  @Test
  @WithResource(name = "templates/includes.tpl", content = """
          yield 'include'
          include template: 'included.tpl'
          """)
  @WithResource(name = "templates/included_fr.tpl", content = "yield 'voila'")
  void localeViewResolution() throws Exception {
    registerAndRefreshContext();
    MockHttpResponseImpl response = render("includes", Locale.FRENCH);
    String result = response.getContentAsString();
    assertThat(result).contains("voila");
    assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-8");
  }

  @Test
  @WithResource(name = "templates/home.tpl", content = "yield 'home'")
  void customContentType() throws Exception {
    registerAndRefreshContext("groovy.template.contentType:application/json");
    MockHttpResponseImpl response = render("home");
    String result = response.getContentAsString();
    assertThat(result).contains("home");
    assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  @WithResource(name = "templates/prefix/prefixed.tpl", content = "yield \"prefixed\"")
  void customPrefix() throws Exception {
    registerAndRefreshContext("groovy.template.prefix:prefix/");
    MockHttpResponseImpl response = render("prefixed");
    String result = response.getContentAsString();
    assertThat(result).contains("prefixed");
  }

  @Test
  @WithResource(name = "templates/suffixed.groovytemplate", content = "yield \"suffixed\"")
  void customSuffix() throws Exception {
    registerAndRefreshContext("groovy.template.suffix:.groovytemplate");
    MockHttpResponseImpl response = render("suffixed");
    String result = response.getContentAsString();
    assertThat(result).contains("suffixed");
  }

  @Test
  void defaultResourceLoaderPath() throws Exception {
    registerAndRefreshContext();
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getResourceLoaderPath())
            .isEqualTo(GroovyTemplateProperties.DEFAULT_RESOURCE_LOADER_PATH);
  }

  @Test
  @WithResource(name = "custom-templates/custom.tpl", content = "yield \"custom\"")
  void customResourceLoaderPath() throws Exception {
    registerAndRefreshContext("groovy.template.resource-loader-path:classpath:/custom-templates/");
    MockHttpResponseImpl response = render("custom");
    String result = response.getContentAsString();
    assertThat(result).contains("custom");
  }

  @Test
  void disableCache() {
    registerAndRefreshContext("groovy.template.cache:false");
    assertThat(this.context.getBean(GroovyMarkupViewResolver.class).getCacheLimit()).isZero();
  }

  @Test
  @WithResource(name = "templates/message.tpl", content = "yield \"Message: ${greeting}\"")
  void renderTemplate() throws Exception {
    registerAndRefreshContext();
    GroovyMarkupConfig config = this.context.getBean(GroovyMarkupConfig.class);
    MarkupTemplateEngine engine = config.getTemplateEngine();
    Writer writer = new StringWriter();
    engine.createTemplate(new ClassPathResource("templates/message.tpl").getFile())
            .make(new HashMap<>(Collections.singletonMap("greeting", "Hello World")))
            .writeTo(writer);
    assertThat(writer.toString()).contains("Hello World");
  }

  @Test
  void enableAutoEscape() {
    registerAndRefreshContext("groovy.template.auto-escape:true");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isAutoEscape()).isTrue();
  }

  @Test
  void enableAutoIndent() {
    registerAndRefreshContext("groovy.template.auto-indent:true");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isAutoIndent()).isTrue();
  }

  @Test
  void defaultAutoIndentString() {
    registerAndRefreshContext();
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getAutoIndentString())
            .isEqualTo(DelegatingIndentWriter.SPACES);
  }

  @Test
  void customAutoIndentString() {
    registerAndRefreshContext("groovy.template.auto-indent-string:\\t");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getAutoIndentString()).isEqualTo("\\t");
  }

  @Test
  void enableAutoNewLine() {
    registerAndRefreshContext("groovy.template.auto-new-line:true");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isAutoNewLine()).isTrue();
  }

  @Test
  void defaultBaseTemplateClass() {
    registerAndRefreshContext();
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getBaseTemplateClass())
            .isEqualTo(BaseTemplate.class);
  }

  @Test
  void customBaseTemplateClass() {
    registerAndRefreshContext("groovy.template.base-template-class:" + CustomBaseTemplate.class.getName());
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getBaseTemplateClass())
            .isEqualTo(CustomBaseTemplate.class);
  }

  @Test
  void defaultDeclarationEncoding() {
    registerAndRefreshContext();
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getDeclarationEncoding()).isNull();
  }

  @Test
  void customDeclarationEncoding() {
    registerAndRefreshContext("groovy.template.declaration-encoding:UTF-8");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getDeclarationEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void enableExpandEmptyElements() {
    registerAndRefreshContext("groovy.template.expand-empty-elements:true");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isExpandEmptyElements()).isTrue();
  }

  @Test
  void defaultLocale() {
    registerAndRefreshContext();
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getLocale()).isEqualTo(Locale.getDefault());
  }

  @Test
  void customLocale() {
    registerAndRefreshContext("groovy.template.locale:en_US");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getLocale()).isEqualTo(Locale.US);
  }

  @Test
  void defaultNewLineString() {
    registerAndRefreshContext();
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getNewLineString())
            .isEqualTo(System.lineSeparator());
  }

  @Test
  void customNewLineString() {
    registerAndRefreshContext("groovy.template.new-line-string:\\r\\n");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).getNewLineString()).isEqualTo("\\r\\n");
  }

  @Test
  void enableUseDoubleQuotes() {
    registerAndRefreshContext("groovy.template.use-double-quotes:true");
    assertThat(this.context.getBean(GroovyMarkupConfigurer.class).isUseDoubleQuotes()).isTrue();
  }

  private void registerAndRefreshContext(String... env) {
    TestPropertyValues.of(env).applyTo(this.context);
    this.context.register(GroovyTemplateAutoConfiguration.class);
    this.context.refresh();
  }

  private MockHttpResponseImpl render(String viewName) throws Exception {
    return render(viewName, Locale.UK);
  }

  private MockHttpResponseImpl render(String viewName, Locale locale) throws Exception {
    LocaleContextHolder.setLocale(locale);
    GroovyMarkupViewResolver resolver = this.context.getBean(GroovyMarkupViewResolver.class);
    View view = resolver.resolveViewName(viewName, locale);
    assertThat(view).isNotNull();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.setAttribute(MockUtils.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    view.render(null, new MockRequestContext(request, response));
    return response;
  }

  static class CustomBaseTemplate extends BaseTemplate {

    CustomBaseTemplate(MarkupTemplateEngine templateEngine, Map model, Map<String, String> modelTypes,
            TemplateConfiguration configuration) {
      super(templateEngine, model, modelTypes, configuration);
    }

    @Override
    public @Nullable Object run() {
      return null;
    }

  }

}
