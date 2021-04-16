/*
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

package cn.taketoday.framework.server.light;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.DefaultMultiValueMap;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpStatus;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.resolver.ParameterReadFailedException;

/**
 * @author TODAY 2021/4/13 11:35
 */
public class LightRequestContext extends RequestContext {
  private final HttpRequest request;
  private final HttpResponse response;

  private ResponseOutputBuffer responseBody;

  private final LightHttpConfig config;
  private List<RequestPart> requestParts;

  public LightRequestContext(HttpRequest request, HttpResponse response, LightHttpConfig config) {
    this.request = request;
    this.response = response;
    this.config = config;
  }

  @Override
  protected String doGetRequestPath() {
    return request.getRequestURI();
  }

  @Override
  public String getRequestURL() {
    return request.getBaseURL().toString() + getRequestPath();
  }

  @Override
  protected String doGetQueryString() {
    return request.getURI().getQuery();
  }

  @Override
  protected HttpCookie[] doGetCookies() {
    final HttpHeaders headers = request.getHeaders();
    final List<String> allCookie = headers.get(Constant.COOKIE);
    if (CollectionUtils.isEmpty(allCookie)) {
      return EMPTY_COOKIES;
    }
    // TODO doGetCookies
    return new HttpCookie[0];
  }

  @Override
  public void addCookie(HttpCookie cookie) {
    final String header = cookie.toString();
    responseHeaders().add(Constant.SET_COOKIE, header);
  }

  @Override
  protected Map<String, String[]> doGetParameters() {
    try {
      final MultiValueMap<String, String> parameters = request.getParameters();
      // form-data
      for (final RequestPart requestPart : getRequestParts()) {
        if (!(requestPart instanceof MultipartFile)) {
          parameters.add(requestPart.getName(), requestPart.getStringValue());
        }
      }
      return parameters.toArrayMap(String[]::new);
    }
    catch (IOException e) {
      throw new ParameterReadFailedException("doGetParameters read failed", e);
    }
  }

  @Override
  protected String doGetMethod() {
    return request.getMethod();
  }

  @Override
  public String remoteAddress() {
    final InetAddress inetAddress = request.getSocket().getInetAddress();
    if (inetAddress == null) {
      return null;
    }
    return inetAddress.getHostAddress();
  }

  @Override
  public long getContentLength() {
    try {
      return request.getBody().available();
    }
    catch (IOException e) {
      return 0;
    }
  }

  @Override
  protected InputStream doGetInputStream() {
    return request.getBody();
  }

  /**
   * map list MultipartFile
   *
   * @throws cn.taketoday.web.resolver.NotMultipartRequestException
   *         if this request is not of type multipart/form-data
   * @throws cn.taketoday.web.resolver.MultipartFileParsingException
   *         multipart parse failed
   */
  @Override
  protected MultiValueMap<String, MultipartFile> parseMultipartFiles() {
    final DefaultMultiValueMap<String, MultipartFile> ret = new DefaultMultiValueMap<>();
    for (final RequestPart requestPart : getRequestParts()) {
      if (requestPart instanceof MultipartFile) {
        ret.add(requestPart.getName(), (MultipartFile) requestPart);
      }
    }
    return ret;
  }

  private List<RequestPart> getRequestParts() {
    if (requestParts == null) {
      final MultipartIterator multipartIterator = new MultipartIterator(request, config);
      final ArrayList<RequestPart> parts = new ArrayList<>();
      while (multipartIterator.hasNext()) {
        parts.add(multipartIterator.next());
      }
      requestParts = parts;
    }
    return requestParts;
  }

  @Override
  public String getContentType() {
    return request.getHeaders().getFirst(Constant.CONTENT_TYPE);
  }

  @Override
  protected HttpHeaders createRequestHeaders() {
    return request.getHeaders();
  }

  @Override
  public boolean committed() {
    return response.committed();
  }

  private void assertNotCommitted() {
    if (committed()) {
      throw new IllegalStateException("The response has been committed");
    }
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    response.redirect(location, false);
  }

  @Override
  public void setStatus(HttpStatus status) {
    response.setStatus(status);
  }

  @Override
  public void setStatus(int sc) {
    response.setStatus(HttpStatus.valueOf(sc));
  }

  @Override
  public void setStatus(int status, String message) {
    response.setStatus(HttpStatus.valueOf(status));
  }

  @Override
  public int getStatus() {
    return response.getStatus().value();
  }

  @Override
  public void sendError(int sc) throws IOException {
    response.sendError(HttpStatus.valueOf(sc));
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    response.sendError(HttpStatus.valueOf(sc), msg);
  }

  @Override
  protected OutputStream doGetOutputStream() {
    if (responseBody == null) {
      responseBody = new ResponseOutputBuffer(config.getResponseBodyInitialSize());
    }
    return responseBody;
  }

  @Override
  public <T> T nativeRequest() {
    return (T) request;
  }

  @Override
  public <T> T nativeRequest(Class<T> requestClass) {
    return null;
  }

  @Override
  public <T> T nativeResponse() {
    return null;
  }

  @Override
  public <T> T nativeResponse(Class<T> responseClass) {
    return null;
  }

  //
  @Override
  protected HttpHeaders createResponseHeaders() {
    return response.getHeaders();
  }

  @Override
  public void reset() {
    assertNotCommitted();
    super.reset();

    if (responseBody != null) {
      responseBody.reset();
    }
    response.reset();
  }

  // response

  /**
   * Send HTTP message to the client
   */
  public void sendIfNotCommitted() throws IOException {
    if (!committed()) {
      send();
    }
  }

  public void send() throws IOException {
    assertNotCommitted();
    response.write(responseBody);
  }

  @Override
  public String toString() {
    return "Light HTTP: " + super.toString();
  }
}
