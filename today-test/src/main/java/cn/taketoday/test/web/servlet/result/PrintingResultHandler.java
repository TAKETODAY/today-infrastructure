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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.ResultHandler;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.Errors;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.mock.api.http.Cookie;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpSession;

/**
 * Result handler that prints {@link MvcResult} details to a given output
 * stream &mdash; for example: {@code System.out}, {@code System.err}, a
 * custom {@code java.io.PrintWriter}, etc.
 *
 * <p>An instance of this class is typically accessed via one of the
 * {@link MockMvcResultHandlers#print print} or {@link MockMvcResultHandlers#log log}
 * methods in {@link MockMvcResultHandlers}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class PrintingResultHandler implements ResultHandler {

  private static final String MISSING_CHARACTER_ENCODING = "<no character encoding set>";

  private final ResultValuePrinter printer;

  /**
   * Protected constructor.
   *
   * @param printer a {@link ResultValuePrinter} to do the actual writing
   */
  protected PrintingResultHandler(ResultValuePrinter printer) {
    this.printer = printer;
  }

  /**
   * Return the result value printer.
   *
   * @return the printer
   */
  protected ResultValuePrinter getPrinter() {
    return this.printer;
  }

  /**
   * Print {@link MvcResult} details.
   */
  @Override
  public final void handle(MvcResult result) throws Exception {
    this.printer.printHeading("MockHttpServletRequest");
    printRequest(result.getRequest());

    this.printer.printHeading("Handler");
    printHandler(result.getHandler(), result.getInterceptors());

    this.printer.printHeading("Async");
    printAsyncResult(result);

    this.printer.printHeading("Resolved Exception");
    printResolvedException(result.getResolvedException());

    this.printer.printHeading("ModelAndView");
    printModelAndView(result.getModelAndView());

    this.printer.printHeading("RedirectModel");
    printFlashMap(RequestContextUtils.getOutputRedirectModel(result.getRequestContext()));

    this.printer.printHeading("MockHttpServletResponse");
    printResponse(result.getResponse());
  }

  /**
   * Print the request.
   */
  protected void printRequest(HttpMockRequestImpl request) throws Exception {
    String body = (request.getCharacterEncoding() != null ?
            request.getContentAsString() : MISSING_CHARACTER_ENCODING);

    this.printer.printValue("HTTP Method", request.getMethod());
    this.printer.printValue("Request URI", request.getRequestURI());
    this.printer.printValue("Parameters", getParamsMultiValueMap(request));
    this.printer.printValue("Headers", getRequestHeaders(request));
    this.printer.printValue("Body", body);
    this.printer.printValue("Session Attrs", getSessionAttributes(request));
  }

  protected final HttpHeaders getRequestHeaders(HttpMockRequestImpl request) {
    HttpHeaders headers = HttpHeaders.forWritable();
    Enumeration<String> names = request.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      headers.put(name, Collections.list(request.getHeaders(name)));
    }
    return headers;
  }

  protected final MultiValueMap<String, String> getParamsMultiValueMap(HttpMockRequestImpl request) {
    Map<String, String[]> params = request.getParameterMap();
    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
    params.forEach((name, values) -> {
      if (params.get(name) != null) {
        for (String value : values) {
          multiValueMap.add(name, value);
        }
      }
    });
    return multiValueMap;
  }

  protected final Map<String, Object> getSessionAttributes(HttpMockRequestImpl request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      Enumeration<String> attrNames = session.getAttributeNames();
      if (attrNames != null) {
        return Collections.list(attrNames).stream().
                collect(Collectors.toMap(n -> n, session::getAttribute));
      }
    }
    return Collections.emptyMap();
  }

  protected void printAsyncResult(MvcResult result) throws Exception {
    HttpMockRequest request = result.getRequest();
    this.printer.printValue("Async started", request.isAsyncStarted());
    Object asyncResult = null;
    try {
      asyncResult = result.getAsyncResult(0);
    }
    catch (IllegalStateException ex) {
      // Not set
    }
    this.printer.printValue("Async result", asyncResult);
  }

  /**
   * Print the handler.
   */
  protected void printHandler(@Nullable Object handler, @Nullable HandlerInterceptor[] interceptors)
          throws Exception {

    if (handler == null) {
      this.printer.printValue("Type", null);
    }
    else {
      if (handler instanceof HandlerMethod handlerMethod) {
        this.printer.printValue("Type", handlerMethod.getBeanType().getName());
        this.printer.printValue("Method", handlerMethod);
      }
      else {
        this.printer.printValue("Type", handler.getClass().getName());
      }
    }
  }

  /**
   * Print exceptions resolved through a HandlerExceptionHandler.
   */
  protected void printResolvedException(@Nullable Throwable resolvedException) throws Exception {
    if (resolvedException == null) {
      this.printer.printValue("Type", null);
    }
    else {
      this.printer.printValue("Type", resolvedException.getClass().getName());
    }
  }

  /**
   * Print the ModelAndView.
   */
  protected void printModelAndView(@Nullable ModelAndView mav) throws Exception {
    this.printer.printValue("View name", (mav != null) ? mav.getViewName() : null);
    this.printer.printValue("View", (mav != null) ? mav.getView() : null);
    if (mav == null || mav.getModel().size() == 0) {
      this.printer.printValue("Model", null);
    }
    else {
      for (String name : mav.getModel().keySet()) {
        if (!name.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
          Object value = mav.getModel().get(name);
          this.printer.printValue("Attribute", name);
          this.printer.printValue("value", value);
          Errors errors = (Errors) mav.getModel().get(BindingResult.MODEL_KEY_PREFIX + name);
          if (errors != null) {
            this.printer.printValue("errors", errors.getAllErrors());
          }
        }
      }
    }
  }

  /**
   * Print "output" flash attributes.
   */
  protected void printFlashMap(RedirectModel flashMap) throws Exception {
    if (ObjectUtils.isEmpty(flashMap)) {
      this.printer.printValue("Attributes", null);
    }
    else {
      flashMap.forEach((name, value) -> {
        this.printer.printValue("Attribute", name);
        this.printer.printValue("value", value);
      });
    }
  }

  /**
   * Print the response.
   */
  protected void printResponse(MockHttpServletResponse response) throws Exception {
    this.printer.printValue("Status", response.getStatus());
    this.printer.printValue("Error message", response.getErrorMessage());
    this.printer.printValue("Headers", getResponseHeaders(response));
    this.printer.printValue("Content type", response.getContentType());
    String body = (MediaType.APPLICATION_JSON_VALUE.equals(response.getContentType()) ?
            response.getContentAsString(StandardCharsets.UTF_8) : response.getContentAsString());
    this.printer.printValue("Body", body);
    this.printer.printValue("Forwarded URL", response.getForwardedUrl());
    this.printer.printValue("Redirected URL", response.getRedirectedUrl());
    printCookies(response.getCookies());
  }

  /**
   * Print the supplied cookies in a human-readable form, assuming the
   * {@link Cookie} implementation does not provide its own {@code toString()}.
   *
   * @since 4.0
   */
  @SuppressWarnings("removal")
  private void printCookies(Cookie[] cookies) {
    String[] cookieStrings = new String[cookies.length];
    for (int i = 0; i < cookies.length; i++) {
      Cookie cookie = cookies[i];
      cookieStrings[i] = new ToStringBuilder(cookie)
              .append("name", cookie.getName())
              .append("value", cookie.getValue())
              .append("comment", cookie.getComment())
              .append("domain", cookie.getDomain())
              .append("maxAge", cookie.getMaxAge())
              .append("path", cookie.getPath())
              .append("secure", cookie.getSecure())
              .append("version", cookie.getVersion())
              .append("httpOnly", cookie.isHttpOnly())
              .toString();
    }
    this.printer.printValue("Cookies", cookieStrings);
  }

  protected final HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
    HttpHeaders headers = HttpHeaders.forWritable();
    for (String name : response.getHeaderNames()) {
      headers.put(name, response.getHeaders(name));
    }
    return headers;
  }

  /**
   * A contract for how to actually write result information.
   */
  protected interface ResultValuePrinter {

    void printHeading(String heading);

    void printValue(String label, @Nullable Object value);
  }

}
