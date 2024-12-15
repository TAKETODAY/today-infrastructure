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

package infra.web.view.script;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockUtils;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for pure JavaScript templates running on Nashorn engine.
 *
 * @author Sebastien Deleuze
 */
@DisabledForJreRange(min = JRE.JAVA_15) // Nashorn JavaScript engine removed in Java 15
public class NashornScriptTemplateTests {

  private WebApplicationContext webAppContext;

  private MockContext mockContext;

  @BeforeEach
  public void setup() {
    this.webAppContext = Mockito.mock(WebApplicationContext.class);
    this.mockContext = new MockContextImpl();
    this.mockContext.setAttribute(MockUtils.WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.webAppContext);
  }

  @Test
  public void renderTemplate() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("title", "Layout example");
    model.put("body", "This is the body");
    String url = "infra/web/mock/view/script/nashorn/template.html";
    MockHttpResponseImpl response = render(url, model, ScriptTemplatingConfiguration.class);
    assertThat(response.getContentAsString()).isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
  }

  @Test
  public void renderTemplateWithUrl() throws Exception {
    String url = "infra/web/mock/view/script/nashorn/template.html";
    MockHttpResponseImpl response = render(url, null, ScriptTemplatingWithUrlConfiguration.class);
    assertThat(response.getContentAsString()).isEqualTo(("<html><head><title>Check url parameter</title></head><body><p>" + url + "</p></body></html>"));
  }

  private MockHttpResponseImpl render(String viewUrl, Map<String, Object> model,
          Class<?> configuration) throws Exception {

    ScriptTemplateView view = createViewWithUrl(viewUrl, configuration);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    view.renderMergedOutputModel(model, new MockRequestContext(webAppContext, request, response));
    return response;
  }

  private ScriptTemplateView createViewWithUrl(String viewUrl, Class<?> configuration) throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(configuration);
    ctx.refresh();

    ScriptTemplateView view = new ScriptTemplateView();
    view.setApplicationContext(ctx);
    view.setUrl(viewUrl);
    view.afterPropertiesSet();
    return view;
  }

  @Configuration
  static class ScriptTemplatingConfiguration {

    @Bean
    public ScriptTemplateConfigurer nashornConfigurer() {
      ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
      configurer.setEngineName("nashorn");
      configurer.setScripts("infra/web/mock/view/script/nashorn/render.js");
      configurer.setRenderFunction("render");
      return configurer;
    }
  }

  @Configuration
  static class ScriptTemplatingWithUrlConfiguration {

    @Bean
    public ScriptTemplateConfigurer nashornConfigurer() {
      ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
      configurer.setEngineName("nashorn");
      configurer.setScripts("infra/web/mock/view/script/nashorn/render.js");
      configurer.setRenderFunction("renderWithUrl");
      return configurer;
    }
  }

}
