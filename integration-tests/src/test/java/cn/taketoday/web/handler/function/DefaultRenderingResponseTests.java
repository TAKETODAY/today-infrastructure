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

package cn.taketoday.web.handler.function;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.view.ModelAndView;

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
    MultiValueMap<String, HttpCookie> newCookies = new LinkedMultiValueMap<>();
    newCookies.add("name", new HttpCookie("name", "value"));
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
