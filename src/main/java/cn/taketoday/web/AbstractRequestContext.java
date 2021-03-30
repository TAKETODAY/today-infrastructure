/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.context.EmptyObject;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.http.DefaultHttpHeaders;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.resolver.MultipartFileParsingException;
import cn.taketoday.web.ui.ModelAndView;

import static cn.taketoday.context.Constant.DEFAULT_CHARSET;

/**
 * Abstract {@link RequestContext}
 *
 * @author TODAY 2020-03-29 22:20
 */
public abstract class AbstractRequestContext implements RequestContext {

  private String contextPath;
  private Object requestBody;
  private HttpCookie[] cookies;
  private String[] pathVariables;
  private ModelAndView modelAndView;

  private PrintWriter writer;
  private BufferedReader reader;
  private InputStream inputStream;
  private OutputStream outputStream;

  private Map<String, List<MultipartFile>> multipartFiles;

  /** @since 3.0 */
  protected HttpHeaders requestHeaders;
  /** @since 3.0 */
  protected HttpHeaders responseHeaders;

  @Override
  public String contextPath() {
    final String contextPath = this.contextPath;
    if (contextPath == null) {
      return this.contextPath = getContextPathInternal();
    }
    return contextPath;
  }

  public final void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  protected String getContextPathInternal() {
    return Constant.BLANK;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    final BufferedReader reader = this.reader;
    if (reader == null) {
      return this.reader = getReaderInternal();
    }
    return reader;
  }

  protected BufferedReader getReaderInternal() throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream(), DEFAULT_CHARSET));
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    final PrintWriter writer = this.writer;
    if (writer == null) {
      return this.writer = getWriterInternal();
    }
    return writer;
  }

  protected PrintWriter getWriterInternal() throws IOException {
    return new PrintWriter(getOutputStream(), true);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    final InputStream inputStream = this.inputStream;
    if (inputStream == null) {
      return this.inputStream = getInputStreamInternal();
    }
    return inputStream;
  }

  protected abstract InputStream getInputStreamInternal() throws IOException;

  @Override
  public OutputStream getOutputStream() throws IOException {
    final OutputStream outputStream = this.outputStream;
    if (outputStream == null) {
      return this.outputStream = getOutputStreamInternal();
    }
    return outputStream;
  }

  protected abstract OutputStream getOutputStreamInternal() throws IOException;

  @Override
  public ModelAndView modelAndView() {
    final ModelAndView ret = this.modelAndView;
    return ret == null ? this.modelAndView = new ModelAndView(this) : ret;
  }

  @Override
  public boolean hasModelAndView() {
    return modelAndView != null;
  }

  @Override
  public Object requestBody() {
    return requestBody;
  }

  @Override
  public void requestBody(Object body) {
    this.requestBody = body != null ? body : EmptyObject.INSTANCE;
  }

  @Override
  public String[] pathVariables() {
    return pathVariables;
  }

  @Override
  public String[] pathVariables(String[] variables) {
    return this.pathVariables = variables;
  }

  // -----------------------------------

  @Override
  public HttpCookie[] cookies() {
    final HttpCookie[] cookies = this.cookies;
    if (cookies == null) {
      return this.cookies = getCookiesInternal();
    }
    return cookies;
  }

  @Override
  public HttpCookie cookie(final String name) {
    for (final HttpCookie cookie : cookies()) {
      if (Objects.equals(name, cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * @return an array of all the Cookies included with this request,or
   * {@link #EMPTY_COOKIES} if the request has no cookies
   */
  protected abstract HttpCookie[] getCookiesInternal();

  // -----------------------------------------------------

  @Override
  public Map<String, List<MultipartFile>> multipartFiles() {
    final Map<String, List<MultipartFile>> multipartFiles = this.multipartFiles;
    if (multipartFiles == null) {
      return this.multipartFiles = parseMultipartFiles();
    }
    return multipartFiles;
  }

  /**
   * map list MultipartFile
   *
   * @throws MultipartFileParsingException
   *         if this request is not of type multipart/form-data
   */
  protected abstract Map<String, List<MultipartFile>> parseMultipartFiles();

  // parameter @since 3.0

  /**
   * @param name
   *         a <code>String</code> specifying the name of the parameter
   *
   * @since 3.0
   */
  @Override
  public String parameter(String name) {
    final String[] parameters = parameters(name);
    if (ObjectUtils.isNotEmpty(parameters)) {
      return parameters[0];
    }
    return null;
  }

  /**
   * @param name
   *         a <code>String</code> containing the name of the parameter whose
   *         value is requested
   *
   * @since 3.0
   */
  @Override
  public String[] parameters(String name) {
    final Map<String, String[]> parameters = parameters();
    if (CollectionUtils.isEmpty(parameters)) {
      return null;
    }
    return parameters.get(name);
  }

  /**
   * @since 3.0
   */
  @Override
  public Enumeration<String> parameterNames() {
    final Map<String, String[]> parameters = parameters();
    if (CollectionUtils.isEmpty(parameters)) {
      return null;
    }
    return Collections.enumeration(parameters.keySet());
  }

  /**
   * @since 3.0
   */
  @Override
  public abstract Map<String, String[]> parameters();

  // HTTP headers

  @Override
  public HttpHeaders responseHeaders() {
    HttpHeaders ret = this.responseHeaders;
    if (ret == null) {
      this.responseHeaders = ret = createResponseHeaders();
    }
    return ret;
  }

  protected HttpHeaders createResponseHeaders() {
    return new DefaultHttpHeaders();
  }

  @Override
  public HttpHeaders requestHeaders() {
    HttpHeaders ret = this.requestHeaders;
    if (ret == null) {
      this.requestHeaders = ret = createRequestHeaders();
    }
    return ret;
  }

  protected abstract HttpHeaders createRequestHeaders();

  @Override
  public void contentType(String contentType) {
    requestHeaders().set(Constant.CONTENT_TYPE, contentType);
  }

  /**
   * If {@link #responseHeaders} is not null
   */
  public void applyHeaders() {
    final HttpHeaders responseHeaders = this.responseHeaders;
    if (!CollectionUtils.isEmpty(responseHeaders)) {
      doApplyHeaders(responseHeaders);
    }
  }

  protected void doApplyHeaders(HttpHeaders responseHeaders) { }

  protected void resetResponseHeader() {
    if (responseHeaders != null) {
      responseHeaders.clear();
    }
  }

  @Override
  public String toString() {
    return method() + " " + requestURL();
  }
}
