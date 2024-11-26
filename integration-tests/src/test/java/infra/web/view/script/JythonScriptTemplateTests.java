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
 * Unit tests for String templates running on Jython.
 *
 * @author Sebastien Deleuze
 */
public class JythonScriptTemplateTests {

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
    String url = "infra/web/view/script/jython/template.html";
    MockHttpResponseImpl response = render(url, model);
    assertThat(response.getContentAsString()).isEqualTo("<html><head><title>Layout example</title></head><body><p>This is the body</p></body></html>");
  }

  private MockHttpResponseImpl render(String viewUrl, Map<String, Object> model) throws Exception {
    ScriptTemplateView view = createViewWithUrl(viewUrl);
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    view.renderMergedOutputModel(model, new MockRequestContext(webAppContext, request, response));
    return response;
  }

  private ScriptTemplateView createViewWithUrl(String viewUrl) throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ScriptTemplatingConfiguration.class);
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
    public ScriptTemplateConfigurer jythonConfigurer() {
      ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
      configurer.setScripts("infra/web/view/script/jython/render.py");
      configurer.setEngineName("jython");
      configurer.setRenderFunction("render");
      return configurer;
    }
  }

}
