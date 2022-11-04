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

package cn.taketoday.web.handler.function;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.core.LinkedMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.view.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class DefaultRenderingResponseTests {

  static final ServerResponse.Context EMPTY_CONTEXT = Collections::emptyList;

  @Test
  public void create() throws Exception {
    String name = "foo";
    RenderingResponse result = RenderingResponse.create(name).build();

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView mav = getModelAndView(result, request, response);

    assertThat(mav.getViewName()).isEqualTo(name);
  }

  @Test
  public void status() throws Exception {
    HttpStatus status = HttpStatus.I_AM_A_TEAPOT;
    RenderingResponse result = RenderingResponse.create("foo").status(status).build();

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(response.getStatus()).isEqualTo(status.value());
  }

  @Nullable
  private static ModelAndView getModelAndView(
          RenderingResponse result, MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
    ServletRequestContext servletRequestContext = new ServletRequestContext(null, request, response);
    Object write = result.writeTo(servletRequestContext, EMPTY_CONTEXT);
    if (write instanceof ModelAndView andView) {
      return andView;
    }
    return null;
  }

  @Test
  public void headers() throws Exception {
    HttpHeaders headers = HttpHeaders.create();
    headers.set("foo", "bar");
    RenderingResponse result = RenderingResponse.create("foo")
            .headers(h -> h.addAll(headers))
            .build();

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();

    assertThat(response.getHeader("foo")).isEqualTo("bar");
  }

  @Test
  public void modelAttribute() throws Exception {
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttribute("foo", "bar").build();

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();

    assertThat(mav.getModel().get("foo")).isEqualTo("bar");
  }

  @Test
  public void modelAttributeConventions() throws Exception {
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttribute("bar").build();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(mav.getModel().get("string")).isEqualTo("bar");
  }

  @Test
  public void modelAttributes() throws Exception {
    Map<String, String> model = Collections.singletonMap("foo", "bar");
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttributes(model).build();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(mav.getModel().get("foo")).isEqualTo("bar");
  }

  @Test
  public void modelAttributesConventions() throws Exception {
    RenderingResponse result = RenderingResponse.create("foo")
            .modelAttributes("bar").build();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(mav.getModel().get("string")).isEqualTo("bar");
  }

  @Test
  public void cookies() throws Exception {
    MultiValueMap<String, HttpCookie> newCookies = new LinkedMultiValueMap<>();
    newCookies.add("name", new HttpCookie("name", "value"));
    RenderingResponse result =
            RenderingResponse.create("foo").cookies(cookies -> cookies.addAll(newCookies)).build();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNotNull();
    assertThat(response.getCookies().length).isEqualTo(1);
    assertThat(response.getCookies()[0].getName()).isEqualTo("name");
    assertThat(response.getCookies()[0].getValue()).isEqualTo("value");
  }

  @Test
  public void notModifiedEtag() throws Exception {
    String etag = "\"foo\"";
    RenderingResponse result = RenderingResponse.create("bar")
            .header(HttpHeaders.ETAG, etag)
            .build();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "https://example.com");
    request.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNull();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

  @Test
  public void notModifiedLastModified() throws Exception {
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

    RenderingResponse result = RenderingResponse.create("bar")
            .header(HttpHeaders.LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.format(oneMinuteBeforeNow))
            .build();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "https://example.com");
    request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateTimeFormatter.RFC_1123_DATE_TIME.format(now));
    MockHttpServletResponse response = new MockHttpServletResponse();

    ModelAndView mav = getModelAndView(result, request, response);
    assertThat(mav).isNull();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

}
