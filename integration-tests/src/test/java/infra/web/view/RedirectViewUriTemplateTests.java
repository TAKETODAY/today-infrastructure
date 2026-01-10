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

package infra.web.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RedirectViewUriTemplateTests {

  private HttpMockRequestImpl request;

  private MockHttpResponseImpl response;

  RequestContext context;

  @BeforeEach
  public void setUp() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    this.request = new HttpMockRequestImpl();
    this.response = new MockHttpResponseImpl();
    context.refresh();

    this.context = new MockRequestContext(context, request, response);
  }

  @Test
  void uriTemplate() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");

    String baseUrl = "https://url.somewhere.com";
    RedirectView redirectView = new RedirectView(baseUrl + "/{foo}");
    redirectView.renderMergedOutputModel(model, context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo((baseUrl + "/bar"));
  }

  @Test
  void uriTemplateEncode() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar/bar baz");

    String baseUrl = "https://url.somewhere.com";
    RedirectView redirectView = new RedirectView(baseUrl + "/context path/{foo}");
    redirectView.renderMergedOutputModel(model, context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo((baseUrl + "/context path/bar%2Fbar%20baz"));
  }

  @Test
  void uriTemplateAndArrayQueryParam() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");
    model.put("fooArr", new String[] { "baz", "bazz" });

    RedirectView redirectView = new RedirectView("/foo/{foo}");
    redirectView.renderMergedOutputModel(model, context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo("/foo/bar?fooArr=baz&fooArr=bazz");
  }

  @Test
  void uriTemplateWithObjectConversion() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("foo", 611L);

    RedirectView redirectView = new RedirectView("/foo/{foo}");
    redirectView.renderMergedOutputModel(model, context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo("/foo/611");
  }

  @Test
  void uriTemplateReuseCurrentRequestVars() throws Exception {
    Map<String, Object> model = new HashMap<>();
    model.put("key1", "value1");
    model.put("name", "value2");
    model.put("key3", "value3");

    Map<String, String> currentRequestUriTemplateVars = new HashMap<>();
    currentRequestUriTemplateVars.put("var1", "v1");
    currentRequestUriTemplateVars.put("name", "v2");
    currentRequestUriTemplateVars.put("var3", "v3");

    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(context) {
      @Override
      public Map<String, String> getUriVariables() {
        return currentRequestUriTemplateVars;
      }
    };
    context.setMatchingMetadata(metadata);

    String url = "https://url.somewhere.com";
    RedirectView redirectView = new RedirectView(url + "/{key1}/{var1}/{name}");
    redirectView.renderMergedOutputModel(model, context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo((url + "/value1/v1/value2?key3=value3"));
  }

  @Test
  void uriTemplateNullValue() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new RedirectView("/{foo}").renderMergedOutputModel(new HashMap<>(), context));
  }

  @Test
  void emptyRedirectString() throws Exception {
    Map<String, Object> model = new HashMap<>();

    RedirectView redirectView = new RedirectView("");
    redirectView.renderMergedOutputModel(model, context);

    assertThat(this.response.getRedirectedUrl()).isEmpty();
  }

  //

  @Test
  void dontApplyUriVariables() throws Exception {
    String url = "/test#{'one','abc'}";
    RedirectView redirectView = new RedirectView(url);
    redirectView.setExpandUriTemplateVariables(false);
    redirectView.renderMergedOutputModel(new HashMap<>(), context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo(url);
  }

}
