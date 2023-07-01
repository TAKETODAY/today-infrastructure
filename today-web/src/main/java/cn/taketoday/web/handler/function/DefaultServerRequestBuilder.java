/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.util.UriBuilder;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.pattern.PathMatchInfo;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

/**
 * Default {@link ServerRequest.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultServerRequestBuilder implements ServerRequest.Builder {

  private final RequestContext requestContext;

  private final List<HttpMessageConverter<?>> messageConverters;

  private HttpMethod method;

  private URI uri;

  private final HttpHeaders headers = HttpHeaders.create();

  private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

  private final Map<String, Object> attributes = new LinkedHashMap<>();

  private final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

  @Nullable
  private InetSocketAddress remoteAddress;

  private byte[] body = new byte[0];

  public DefaultServerRequestBuilder(ServerRequest other) {
    Assert.notNull(other, "ServerRequest is required");
    this.requestContext = other.requestContext();
    this.messageConverters = new ArrayList<>(other.messageConverters());
    this.method = other.method();
    this.uri = other.uri();
    params(params -> params.addAll(other.params()));
    cookies(cookies -> cookies.addAll(other.cookies()));
    headers(headers -> headers.addAll(other.headers().asHttpHeaders()));
    attributes(attributes -> attributes.putAll(other.attributes()));
    this.remoteAddress = other.remoteAddress().orElse(null);
  }

  @Override
  public ServerRequest.Builder method(HttpMethod method) {
    Assert.notNull(method, "HttpMethod is required");
    this.method = method;
    return this;
  }

  @Override
  public ServerRequest.Builder uri(URI uri) {
    Assert.notNull(uri, "URI is required");
    this.uri = uri;
    return this;
  }

  @Override
  public ServerRequest.Builder header(String headerName, String... headerValues) {
    for (String headerValue : headerValues) {
      this.headers.add(headerName, headerValue);
    }
    return this;
  }

  @Override
  public ServerRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public ServerRequest.Builder cookie(String name, String... values) {
    for (String value : values) {
      this.cookies.add(name, new HttpCookie(name, value));
    }
    return this;
  }

  @Override
  public ServerRequest.Builder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public ServerRequest.Builder body(byte[] body) {
    this.body = body;
    return this;
  }

  @Override
  public ServerRequest.Builder body(String body) {
    return body(body.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public ServerRequest.Builder attribute(String name, Object value) {
    Assert.notNull(name, "'name' is required");
    this.attributes.put(name, value);
    return this;
  }

  @Override
  public ServerRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
    attributesConsumer.accept(this.attributes);
    return this;
  }

  @Override
  public ServerRequest.Builder param(String name, String... values) {
    for (String value : values) {
      this.params.add(name, value);
    }
    return this;
  }

  @Override
  public ServerRequest.Builder params(Consumer<MultiValueMap<String, String>> paramsConsumer) {
    paramsConsumer.accept(this.params);
    return this;
  }

  @Override
  public ServerRequest.Builder remoteAddress(InetSocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @Override
  public ServerRequest build() {
    return new BuiltServerRequest(this.requestContext, this.method, this.uri, this.headers, this.cookies,
            this.attributes, this.params, this.remoteAddress, this.body, this.messageConverters);
  }

  private static class BuiltServerRequest implements ServerRequest {

    private final HttpMethod method;

    private final URI uri;

    private final HttpHeaders headers;

    private final RequestContext requestContext;

    private final MultiValueMap<String, HttpCookie> cookies;

    private final Map<String, Object> attributes;

    private final byte[] body;

    private final List<HttpMessageConverter<?>> messageConverters;

    private final MultiValueMap<String, String> params;

    @Nullable
    private final InetSocketAddress remoteAddress;

    public BuiltServerRequest(RequestContext requestContext,
            HttpMethod method, URI uri, HttpHeaders headers,
            MultiValueMap<String, HttpCookie> cookies,
            Map<String, Object> attributes, MultiValueMap<String, String> params,
            @Nullable InetSocketAddress remoteAddress, byte[] body, List<HttpMessageConverter<?>> messageConverters) {

      this.uri = uri;
      this.body = body;
      this.method = method;
      this.remoteAddress = remoteAddress;
      this.requestContext = requestContext;
      this.headers = HttpHeaders.copyOf(headers);
      this.messageConverters = messageConverters;
      this.params = new LinkedMultiValueMap<>(params);
      this.cookies = new LinkedMultiValueMap<>(cookies);
      this.attributes = new LinkedHashMap<>(attributes);
    }

    @Override
    public HttpMethod method() {
      return this.method;
    }

    @Override
    public String methodName() {
      return this.method.name();
    }

    @Override
    public MultiValueMap<String, Multipart> multipartData() throws IOException {
      return requestContext.getMultipartRequest().multipartData();
    }

    @Override
    public URI uri() {
      return this.uri;
    }

    @Override
    public UriBuilder uriBuilder() {
      return UriComponentsBuilder.fromUri(this.uri);
    }

    @Override
    public Headers headers() {
      return new DefaultServerRequest.DefaultRequestHeaders(this.headers);
    }

    @Override
    public MultiValueMap<String, HttpCookie> cookies() {
      return this.cookies;
    }

    @Override
    public Optional<InetSocketAddress> remoteAddress() {
      return Optional.ofNullable(this.remoteAddress);
    }

    @Override
    public List<HttpMessageConverter<?>> messageConverters() {
      return this.messageConverters;
    }

    @Override
    public <T> T body(Class<T> bodyType) throws IOException {
      return bodyInternal(bodyType, bodyType);
    }

    @Override
    public <T> T body(ParameterizedTypeReference<T> bodyType) throws IOException {
      Type type = bodyType.getType();
      return bodyInternal(type, DefaultServerRequest.bodyClass(type));
    }

    @SuppressWarnings("unchecked")
    private <T> T bodyInternal(Type bodyType, Class<?> bodyClass) throws IOException {
      HttpInputMessage inputMessage = new BuiltInputMessage();
      MediaType contentType = headers().contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

      for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
        if (messageConverter instanceof GenericHttpMessageConverter<?> converter) {
          if (converter.canRead(bodyType, bodyClass, contentType)) {
            return (T) converter.read(bodyType, bodyClass, inputMessage);
          }
        }
        if (messageConverter.canRead(bodyClass, contentType)) {
          var theConverter = (HttpMessageConverter<T>) messageConverter;
          Class<? extends T> clazz = (Class<? extends T>) bodyClass;
          return theConverter.read(clazz, inputMessage);
        }
      }
      throw new HttpMediaTypeNotSupportedException(contentType, Collections.emptyList(), method());
    }

    @Override
    public Map<String, Object> attributes() {
      return this.attributes;
    }

    @Override
    public MultiValueMap<String, String> params() {
      return this.params;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<String, String> pathVariables() {
      HandlerMatchingMetadata matchingMetadata = requestContext.getMatchingMetadata();
      if (matchingMetadata != null) {
        PathMatchInfo pathMatchInfo = matchingMetadata.getPathMatchInfo();
        if (pathMatchInfo != null) {
          return pathMatchInfo.getUriVariables();
        }
      }

      if (attributes.get(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE)
              instanceof Map pathVariables) {
        return pathVariables;
      }
      return Collections.emptyMap();
    }

    @Override
    public RequestContext requestContext() {
      return this.requestContext;
    }

    private class BuiltInputMessage implements HttpInputMessage {

      @Override
      public InputStream getBody() throws IOException {
        return new BodyInputStream(body);
      }

      @Override
      public HttpHeaders getHeaders() {
        return headers;
      }
    }
  }

  private static class BodyInputStream extends ServletInputStream {

    private final InputStream delegate;

    public BodyInputStream(byte[] body) {
      this.delegate = new ByteArrayInputStream(body);
    }

    @Override
    public boolean isFinished() {
      return false;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int read() throws IOException {
      return this.delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return this.delegate.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
      return this.delegate.read(b);
    }

    @Override
    public long skip(long n) throws IOException {
      return this.delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
      return this.delegate.available();
    }

    @Override
    public void close() throws IOException {
      this.delegate.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
      this.delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
      this.delegate.reset();
    }

    @Override
    public boolean markSupported() {
      return this.delegate.markSupported();
    }
  }

}
