/*
 * Copyright 2002-present the original author or authors.
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
