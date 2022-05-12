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

package cn.taketoday.web.view.groovy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import jakarta.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Brian Clozel
 */
public class GroovyMarkupViewTests {

  private static final String RESOURCE_LOADER_PATH = "classpath*:cn/taketoday/web/view/groovy/";

  private WebApplicationContext webAppContext;

  private ServletContext servletContext;

  @BeforeEach
  public void setup() {
    this.webAppContext = mock(WebApplicationContext.class);
    this.servletContext = new MockServletContext();
    this.servletContext.setAttribute(ServletUtils.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
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
    assertThat(engine).isNotNull();
    assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
  }

  @Test
  public void detectTemplateEngine() throws Exception {
    GroovyMarkupView view = new GroovyMarkupView();
    view.setTemplateEngine(new TestTemplateEngine());
    view.setApplicationContext(this.webAppContext);

    DirectFieldAccessor accessor = new DirectFieldAccessor(view);
    TemplateEngine engine = (TemplateEngine) accessor.getPropertyValue("engine");
    assertThat(engine).isNotNull();
    assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
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
    MockHttpServletResponse response = renderViewWithModel("test.tpl", model, Locale.US);
    assertThat(response.getContentAsString()).contains("<h1>Hello Spring</h1>");
  }

  @Test
  public void renderI18nTemplate() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("name", "Spring");
    MockHttpServletResponse response = renderViewWithModel("i18n.tpl", model, Locale.FRANCE);
    assertThat(response.getContentAsString()).isEqualTo("<p>Bonjour Spring</p>");

    response = renderViewWithModel("i18n.tpl", model, Locale.GERMANY);
    assertThat(response.getContentAsString()).isEqualTo("<p>Include German</p><p>Hallo Spring</p>");

    response = renderViewWithModel("i18n.tpl", model, new Locale("es"));
    assertThat(response.getContentAsString()).isEqualTo("<p>Include Default</p><p>Hola Spring</p>");
  }

  @Test
  public void renderLayoutTemplate() throws Exception {
    Map<String, Object> model = new HashMap<>();
    MockHttpServletResponse response = renderViewWithModel("content.tpl", model, Locale.US);
    assertThat(response.getContentAsString()).isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
  }

  private MockHttpServletResponse renderViewWithModel(String viewUrl, Map<String,
          Object> model, Locale locale) throws Exception {

    GroovyMarkupView view = createViewWithUrl(viewUrl);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addPreferredLocale(locale);
    LocaleContextHolder.setLocale(locale);
    view.renderMergedTemplateModel(model, ServletUtils.getRequestContext(request, response));
    return response;
  }

  private GroovyMarkupView createViewWithUrl(String viewUrl) throws Exception {
    AnnotationConfigServletWebApplicationContext ctx = new AnnotationConfigServletWebApplicationContext();
    ctx.register(GroovyMarkupConfiguration.class);
    ctx.refresh();

    GroovyMarkupView view = new GroovyMarkupView();
    view.setUrl(viewUrl);
    view.setApplicationContext(ctx);
    view.afterPropertiesSet();
    return view;
  }

  public class TestTemplateEngine extends MarkupTemplateEngine {

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
