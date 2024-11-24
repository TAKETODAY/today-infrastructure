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

package infra.web.view.groovy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import infra.beans.DirectFieldAccessor;
import infra.context.ApplicationContextException;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.i18n.LocaleContextHolder;
import infra.web.mock.MockUtils;
import infra.web.mock.WebApplicationContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockContextImpl;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Brian Clozel
 */
public class GroovyMarkupViewTests {

  private static final String RESOURCE_LOADER_PATH = "classpath*:infra/web/view/groovy/";

  private WebApplicationContext webAppContext;

  private MockContextImpl mockContext;

  @BeforeEach
  public void setup() {
    this.webAppContext = Mockito.mock(WebApplicationContext.class);
    this.mockContext = new MockContextImpl();
    this.mockContext.setAttribute(MockUtils.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
  }

  @Test
  public void missingGroovyMarkupConfig() throws Exception {
    GroovyMarkupView view = new GroovyMarkupView();
    given(this.webAppContext.getBeansOfType(GroovyMarkupConfig.class, true, false))
            .willReturn(new HashMap<>());

    view.setUrl("sampleView");
    assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(() ->
                    view.setApplicationContext(this.webAppContext))
            .withMessageContaining("GroovyMarkupConfig");
  }

  @Test
  public void customTemplateEngine() throws Exception {
    GroovyMarkupView view = new GroovyMarkupView();
    view.setTemplateEngine(new TestTemplateEngine());
    view.setApplicationContext(this.webAppContext);

    DirectFieldAccessor accessor = new DirectFieldAccessor(view);
    TemplateEngine engine = (TemplateEngine) accessor.getPropertyValue("engine");
    Assertions.assertThat(engine).isNotNull();
    Assertions.assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
  }

  @Test
  public void detectTemplateEngine() throws Exception {
    GroovyMarkupView view = new GroovyMarkupView();
    view.setTemplateEngine(new TestTemplateEngine());
    view.setApplicationContext(this.webAppContext);

    DirectFieldAccessor accessor = new DirectFieldAccessor(view);
    TemplateEngine engine = (TemplateEngine) accessor.getPropertyValue("engine");
    Assertions.assertThat(engine).isNotNull();
    Assertions.assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
  }

  @Test
  public void checkResource() throws Exception {
    GroovyMarkupView view = createViewWithUrl("test.tpl");
    assertThat(view.checkResource(Locale.US)).isTrue();
  }

  @Test
  public void checkMissingResource() throws Exception {
    GroovyMarkupView view = createViewWithUrl("missing.tpl");
    assertThat(view.checkResource(Locale.US)).isFalse();
  }

  @Test
  public void checkI18nResource() throws Exception {
    GroovyMarkupView view = createViewWithUrl("i18n.tpl");
    assertThat(view.checkResource(Locale.FRENCH)).isTrue();
  }

  @Test
  public void checkI18nResourceMissingLocale() throws Exception {
    GroovyMarkupView view = createViewWithUrl("i18n.tpl");
    assertThat(view.checkResource(Locale.CHINESE)).isTrue();
  }

  @Test
  public void renderMarkupTemplate() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("name", "Spring");
    MockHttpResponseImpl response = renderViewWithModel("test.tpl", model, Locale.US);
    assertThat(response.getContentAsString()).contains("<h1>Hello Spring</h1>");
  }

  @Test
  public void renderI18nTemplate() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("name", "Spring");
    MockHttpResponseImpl response = renderViewWithModel("i18n.tpl", model, Locale.FRANCE);
    assertThat(response.getContentAsString()).isEqualTo("<p>Bonjour Spring</p>");

    response = renderViewWithModel("i18n.tpl", model, Locale.GERMANY);
    assertThat(response.getContentAsString()).isEqualTo("<p>Include German</p><p>Hallo Spring</p>");

    response = renderViewWithModel("i18n.tpl", model, new Locale("es"));
    assertThat(response.getContentAsString()).isEqualTo("<p>Include Default</p><p>Hola Spring</p>");
  }

  @Test
  public void renderLayoutTemplate() throws Exception {
    Map<String, Object> model = new HashMap<>();
    MockHttpResponseImpl response = renderViewWithModel("content.tpl", model, Locale.US);
    assertThat(response.getContentAsString()).isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
  }

  private MockHttpResponseImpl renderViewWithModel(String viewUrl, Map<String,
          Object> model, Locale locale) throws Exception {

    GroovyMarkupView view = createViewWithUrl(viewUrl);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    request.addPreferredLocale(locale);
    LocaleContextHolder.setLocale(locale);
    view.renderMergedTemplateModel(model, MockUtils.getRequestContext(request, response));
    return response;
  }

  private GroovyMarkupView createViewWithUrl(String viewUrl) throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(GroovyMarkupConfiguration.class);
    ctx.refresh();

    GroovyMarkupView view = new GroovyMarkupView();
    view.setUrl(viewUrl);
    view.setApplicationContext(ctx);
    view.afterPropertiesSet();
    return view;
  }

  public static class TestTemplateEngine extends MarkupTemplateEngine {

    public TestTemplateEngine() {
      super(new TemplateConfiguration());
    }

    @Override
    public Template createTemplate(Reader reader) {
      return null;
    }
  }

  @Configuration
  static class GroovyMarkupConfiguration {

    @Bean
    public GroovyMarkupConfig groovyMarkupConfigurer() {
      GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
      configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH);
      return configurer;
    }
  }

}
