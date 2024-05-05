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

package cn.taketoday.web.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.web.mock.MockRequestContext;

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

  // SPR-9016

  @Test
  void dontApplyUriVariables() throws Exception {
    String url = "/test#{'one','abc'}";
    RedirectView redirectView = new RedirectView(url);
    redirectView.setExpandUriTemplateVariables(false);
    redirectView.renderMergedOutputModel(new HashMap<>(), context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo(url);
  }

}
