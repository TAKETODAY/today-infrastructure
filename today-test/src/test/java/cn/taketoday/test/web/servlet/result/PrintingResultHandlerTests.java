/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.result;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.test.web.servlet.StubMvcResult;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.view.RedirectModel;
import jakarta.servlet.http.Cookie;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link PrintingResultHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see cn.taketoday.test.web.servlet.samples.standalone.resulthandlers.PrintingResultHandlerSmokeTests
 */
class PrintingResultHandlerTests {

  private final TestPrintingResultHandler handler = new TestPrintingResultHandler();

  private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/") {
    @Override
    public boolean isAsyncStarted() {
      return false;
    }
  };

  private final MockHttpServletResponse response = new MockHttpServletResponse();

  private final StubMvcResult mvcResult = new StubMvcResult(
          this.request, null, null, null, null, null, this.response);

  @Test
  void printRequest() throws Exception {
    this.request.addParameter("param", "paramValue");
    this.request.addHeader("header", "headerValue");
    this.request.setCharacterEncoding("UTF-16");
    String palindrome = "ablE was I ere I saw Elba";
    byte[] bytes = palindrome.getBytes(UTF_16);
    this.request.setContent(bytes);
    this.request.getSession().setAttribute("foo", "bar");

    this.handler.handle(this.mvcResult);

    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("header", "headerValue");

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("param", "paramValue");

    assertValue("MockHttpServletRequest", "HTTP Method", this.request.getMethod());
    assertValue("MockHttpServletRequest", "Request URI", this.request.getRequestURI());
    assertValue("MockHttpServletRequest", "Parameters", params);
    assertValue("MockHttpServletRequest", "Headers", headers);
    assertValue("MockHttpServletRequest", "Body", palindrome);
    assertValue("MockHttpServletRequest", "Session Attrs", Collections.singletonMap("foo", "bar"));
  }

  @Test
  void printRequestWithoutSession() throws Exception {
    this.request.addParameter("param", "paramValue");
    this.request.addHeader("header", "headerValue");
    this.request.setCharacterEncoding("UTF-16");
    String palindrome = "ablE was I ere I saw Elba";
    byte[] bytes = palindrome.getBytes(UTF_16);
    this.request.setContent(bytes);

    this.handler.handle(this.mvcResult);

    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("header", "headerValue");

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("param", "paramValue");

    assertValue("MockHttpServletRequest", "HTTP Method", this.request.getMethod());
    assertValue("MockHttpServletRequest", "Request URI", this.request.getRequestURI());
    assertValue("MockHttpServletRequest", "Parameters", params);
    assertValue("MockHttpServletRequest", "Headers", headers);
    assertValue("MockHttpServletRequest", "Body", palindrome);
  }

  @Test
  void printRequestWithEmptySessionMock() throws Exception {
    this.request.addParameter("param", "paramValue");
    this.request.addHeader("header", "headerValue");
    this.request.setCharacterEncoding("UTF-16");
    String palindrome = "ablE was I ere I saw Elba";
    byte[] bytes = palindrome.getBytes(UTF_16);
    this.request.setContent(bytes);
    this.request.setSession(mock());

    this.handler.handle(this.mvcResult);

    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("header", "headerValue");

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("param", "paramValue");

    assertValue("MockHttpServletRequest", "HTTP Method", this.request.getMethod());
    assertValue("MockHttpServletRequest", "Request URI", this.request.getRequestURI());
    assertValue("MockHttpServletRequest", "Parameters", params);
    assertValue("MockHttpServletRequest", "Headers", headers);
    assertValue("MockHttpServletRequest", "Body", palindrome);
  }

  @Test
  @SuppressWarnings("removal")
  void printResponse() throws Exception {
    Cookie enigmaCookie = new Cookie("enigma", "42");
    enigmaCookie.setHttpOnly(true);
    enigmaCookie.setMaxAge(1234);
    enigmaCookie.setDomain(".example.com");
    enigmaCookie.setPath("/crumbs");
    enigmaCookie.setSecure(true);

    this.response.setStatus(400);
    this.response.addHeader("header", "headerValue");
    this.response.setContentType("text/plain");
    this.response.getWriter().print("content");
    this.response.setForwardedUrl("redirectFoo");
    this.response.sendRedirect("/redirectFoo");
    this.response.addCookie(new Cookie("cookie", "cookieValue"));
    this.response.addCookie(enigmaCookie);

    this.handler.handle(this.mvcResult);

    // Manually validate cookie values since maxAge changes...
    List<String> cookieValues = this.response.getHeaders("Set-Cookie");
    assertThat(cookieValues).hasSize(2);
    assertThat(cookieValues.get(0)).isEqualTo("cookie=cookieValue");
    assertThat(cookieValues.get(1).startsWith(
            "enigma=42; Path=/crumbs; Domain=.example.com; Max-Age=1234; Expires=")).as("Actual: " + cookieValues.get(1)).isTrue();

    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("header", "headerValue");
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.setLocation(URI.create("/redirectFoo"));
    headers.put("Set-Cookie", cookieValues);

    String heading = "MockHttpServletResponse";
    assertValue(heading, "Status", this.response.getStatus());
    assertValue(heading, "Error message", response.getErrorMessage());
    assertValue(heading, "Headers", headers);
    assertValue(heading, "Content type", this.response.getContentType());
    assertValue(heading, "Body", this.response.getContentAsString());
    assertValue(heading, "Forwarded URL", this.response.getForwardedUrl());
    assertValue(heading, "Redirected URL", this.response.getRedirectedUrl());

    Map<String, Map<String, Object>> printedValues = this.handler.getPrinter().printedValues;
    String[] cookies = (String[]) printedValues.get(heading).get("Cookies");
    assertThat(cookies).hasSize(2);
    String cookie1 = cookies[0];
    String cookie2 = cookies[1];
    assertThat(cookie1.startsWith("[" + Cookie.class.getSimpleName())).isTrue();
    assertThat(cookie1.contains("name = 'cookie', value = 'cookieValue'")).isTrue();
    assertThat(cookie1.endsWith("]")).isTrue();
    assertThat(cookie2.startsWith("[" + Cookie.class.getSimpleName())).isTrue();
    assertThat(cookie2.contains("name = 'enigma', value = '42', " +
            "comment = [null], domain = '.example.com', maxAge = 1234, " +
            "path = '/crumbs', secure = true, version = 0, httpOnly = true")).isTrue();
    assertThat(cookie2.endsWith("]")).isTrue();
  }

  @Test
  void printRequestWithCharacterEncoding() throws Exception {
    this.request.setCharacterEncoding("UTF-8");
    this.request.setContent("text".getBytes(UTF_8));

    this.handler.handle(this.mvcResult);

    assertValue("MockHttpServletRequest", "Body", "text");
  }

  @Test
  void printRequestWithoutCharacterEncoding() throws Exception {
    this.handler.handle(this.mvcResult);

    assertValue("MockHttpServletRequest", "Body", "<no character encoding set>");
  }

  @Test
  void printResponseWithCharacterEncoding() throws Exception {
    this.response.setCharacterEncoding("UTF-8");
    this.response.getWriter().print("text");

    this.handler.handle(this.mvcResult);
    assertValue("MockHttpServletResponse", "Body", "text");
  }

  @Test
  void printResponseWithDefaultCharacterEncoding() throws Exception {
    this.response.getWriter().print("text");

    this.handler.handle(this.mvcResult);

    assertValue("MockHttpServletResponse", "Body", "text");
  }

  @Test
  void printHandlerNull() throws Exception {
    StubMvcResult mvcResult = new StubMvcResult(this.request, null, null, null, null, null, this.response);
    this.handler.handle(mvcResult);

    assertValue("Handler", "Type", null);
  }

  @Test
  void printHandler() throws Exception {
    this.mvcResult.setHandler(new Object());
    this.handler.handle(this.mvcResult);

    assertValue("Handler", "Type", Object.class.getName());
  }

  @Test
  void printHandlerMethod() throws Exception {
    HandlerMethod handlerMethod = new HandlerMethod(this, "handle");
    this.mvcResult.setHandler(handlerMethod);
    this.handler.handle(mvcResult);

    assertValue("Handler", "Type", this.getClass().getName());
    assertValue("Handler", "Method", handlerMethod);
  }

  @Test
  void resolvedExceptionNull() throws Exception {
    this.handler.handle(this.mvcResult);

    assertValue("Resolved Exception", "Type", null);
  }

  @Test
  void resolvedException() throws Exception {
    this.mvcResult.setResolvedException(new Exception());
    this.handler.handle(this.mvcResult);

    assertValue("Resolved Exception", "Type", Exception.class.getName());
  }

  @Test
  void modelAndViewNull() throws Exception {
    this.handler.handle(this.mvcResult);

    assertValue("ModelAndView", "View name", null);
    assertValue("ModelAndView", "View", null);
    assertValue("ModelAndView", "Model", null);
  }

  @Test
  void modelAndView() throws Exception {
    BindException bindException = new BindException(new Object(), "target");
    bindException.reject("errorCode");

    ModelAndView mav = new ModelAndView("viewName");
    mav.addObject("attrName", "attrValue");
    mav.addObject(BindingResult.MODEL_KEY_PREFIX + "attrName", bindException);

    this.mvcResult.setMav(mav);
    this.handler.handle(this.mvcResult);

    assertValue("ModelAndView", "View name", "viewName");
    assertValue("ModelAndView", "View", null);
    assertValue("ModelAndView", "Attribute", "attrName");
    assertValue("ModelAndView", "value", "attrValue");
    assertValue("ModelAndView", "errors", bindException.getAllErrors());
  }

  @Test
  void flashMapNull() throws Exception {
    this.handler.handle(mvcResult);

    assertValue("RedirectModel", "Type", null);
  }

  @Test
  void flashMap() throws Exception {
    RedirectModel flashMap = new RedirectModel();
    flashMap.put("attrName", "attrValue");
    this.request.setAttribute(RedirectModel.OUTPUT_ATTRIBUTE, flashMap);

    this.handler.handle(this.mvcResult);

    assertValue("RedirectModel", "Attribute", "attrName");
    assertValue("RedirectModel", "value", "attrValue");
  }

  private void assertValue(String heading, String label, Object value) {
    Map<String, Map<String, Object>> printedValues = this.handler.getPrinter().printedValues;
    assertThat(printedValues.containsKey(heading)).as("Heading '" + heading + "' not printed").isTrue();
    assertThat(printedValues.get(heading).get(label)).as("For label '" + label + "' under heading '" + heading + "' =>").isEqualTo(value);
  }

  private static class TestPrintingResultHandler extends PrintingResultHandler {

    TestPrintingResultHandler() {
      super(new TestResultValuePrinter());
    }

    @Override
    public TestResultValuePrinter getPrinter() {
      return (TestResultValuePrinter) super.getPrinter();
    }

    private static class TestResultValuePrinter implements ResultValuePrinter {

      private String printedHeading;

      private final Map<String, Map<String, Object>> printedValues = new HashMap<>();

      @Override
      public void printHeading(String heading) {
        this.printedHeading = heading;
        this.printedValues.put(heading, new HashMap<>());
      }

      @Override
      public void printValue(String label, Object value) {
        Assert.notNull(this.printedHeading,
                "Heading not printed before label " + label + " with value " + value);
        this.printedValues.get(this.printedHeading).put(label, value);
      }
    }
  }

  public void handle() {
  }

}
