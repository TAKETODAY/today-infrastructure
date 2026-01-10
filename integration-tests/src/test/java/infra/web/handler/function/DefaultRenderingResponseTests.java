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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import org.jspecify.annotations.Nullable;

import infra.http.ResponseCookie;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.mock.MockRequestContext;
import infra.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class DefaultRenderingResponseTests {

  static final ServerResponse.Context EMPTY_CONTEXT = Collections::emptyList;

  @Test
  public void create() throws Throwable {
    String name = "foo";
    RenderingResponse result = RenderingResponse.create(name).build();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    ModelAndView mav = getModelAndView(result, request, response);

    assertThat(mav.getViewName()).isEqualTo(name);
  }

  @Test
  public void status() throws Throwable {
    HttpStatus status = HttpStatus.I_AM_A_TEAPOT;
    RenderingResponse result = RenderingResponse.create("foo").status(status).build();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(response.getStatus()).isEqualTo(status.value());
  }

  @Nullable
  private static ModelAndView getModelAndView(
          RenderingResponse result, HttpMockRequestImpl request, MockHttpResponseImpl response) throws Throwable {
    MockRequestContext mockRequestContext = new MockRequestContext(null, request, response);
    Object write = result.writeTo(mockRequestContext, EMPTY_CONTEXT);
    mockRequestContext.flush();
    if (write instanceof ModelAndView andView) {
      return andView;
    }
    return null;
  }

  @Test
  public void headers() throws Throwable {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setOrRemove("foo", "bar");
    RenderingResponse result = RenderingResponse.create("foo")
            .headers(h -> h.addAll(headers))
            .build();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();

    assertThat(response.getHeader("foo")).isEqualTo("bar");
  }

  @Test
  public void modelAttribute() throws Throwable {
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttribute("foo", "bar").build();

    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();

    assertThat(mav.getModel().get("foo")).isEqualTo("bar");
  }

  @Test
  public void modelAttributeConventions() throws Throwable {
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttribute("bar").build();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(mav.getModel().get("string")).isEqualTo("bar");
  }

  @Test
  public void modelAttributes() throws Throwable {
    Map<String, String> model = Collections.singletonMap("foo", "bar");
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttributes(model).build();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(mav.getModel().get("foo")).isEqualTo("bar");
  }

  @Test
  public void modelAttributesConventions() throws Throwable {
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttributes("bar").build();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(mav.getModel().get("string")).isEqualTo("bar");
  }

  @Test
  public void cookies() throws Throwable {
    MultiValueMap<String, ResponseCookie> newCookies = new LinkedMultiValueMap<>();
    newCookies.add("name", ResponseCookie.forSimple("name", "value"));
    RenderingResponse result = RenderingResponse.create("foo").cookies(cookies -> cookies.addAll(newCookies)).build();
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(response.getCookies().length).isEqualTo(1);
    assertThat(response.getCookies()[0].getName()).isEqualTo("name");
    assertThat(response.getCookies()[0].getValue()).isEqualTo("value");
  }

  @Test
  public void notModifiedEtag() throws Throwable {
    String etag = "\"foo\"";
    RenderingResponse result = RenderingResponse.create("bar")
            .header(HttpHeaders.ETAG, etag)
            .build();

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "https://example.com");
    request.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNull();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

  @Test
  public void notModifiedLastModified() throws Throwable {
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

    RenderingResponse result = RenderingResponse.create("bar")
            .header(HttpHeaders.LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.format(oneMinuteBeforeNow))
            .build();

    HttpMockRequestImpl request = new HttpMockRequestImpl("GET", "https://example.com");
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateTimeFormatter.RFC_1123_DATE_TIME.format(now));
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNull();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

}
