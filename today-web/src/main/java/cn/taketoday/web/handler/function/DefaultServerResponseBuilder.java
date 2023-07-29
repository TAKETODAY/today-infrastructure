/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;

/**
 * Default {@link ServerResponse.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

  private final HttpStatusCode statusCode;

  private final HttpHeaders headers = HttpHeaders.create();

  private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

  public DefaultServerResponseBuilder(ServerResponse other) {
    Assert.notNull(other, "ServerResponse is required");
    this.statusCode = other.statusCode();
    this.headers.addAll(other.headers());
    this.cookies.addAll(other.cookies());
  }

  public DefaultServerResponseBuilder(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatusCode is required");
    this.statusCode = status;
  }

  @Override
  public ServerResponse.BodyBuilder header(String headerName, String... headerValues) {
    for (String headerValue : headerValues) {
      this.headers.add(headerName, headerValue);
    }
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cookie(HttpCookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie.getName(), cookie);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder allow(HttpMethod... allowedMethods) {
    this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder allow(Set<HttpMethod> allowedMethods) {
    this.headers.setAllow(allowedMethods);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder contentLength(long contentLength) {
    this.headers.setContentLength(contentLength);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder contentType(MediaType contentType) {
    this.headers.setContentType(contentType);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder eTag(String etag) {
    if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
      etag = "\"" + etag;
    }
    if (!etag.endsWith("\"")) {
      etag = etag + "\"";
    }
    this.headers.setETag(etag);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder lastModified(ZonedDateTime lastModified) {
    this.headers.setLastModified(lastModified);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder lastModified(Instant lastModified) {
    this.headers.setLastModified(lastModified);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder location(URI location) {
    this.headers.setLocation(location);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cacheControl(CacheControl cacheControl) {
    this.headers.setCacheControl(cacheControl);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder varyBy(String... requestHeaders) {
    this.headers.setVary(Arrays.asList(requestHeaders));
    return this;
  }

  @Override
  public ServerResponse build() {
    return build((request) -> null);
  }

  @Override
  public ServerResponse build(WriteFunction writeFunction) {
    return new WriteFunctionResponse(this.statusCode, this.headers, this.cookies, writeFunction);
  }

  @Override
  public ServerResponse body(Object body) {
    return new DefaultEntityResponseBuilder<>(body, null)
            .status(this.statusCode)
            .headers(headers -> headers.putAll(this.headers))
            .cookies(cookies -> cookies.addAll(this.cookies))
            .build();
  }

  @Override
  public <T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType) {
    return new DefaultEntityResponseBuilder<>(body, bodyType.getType())
            .status(this.statusCode)
            .headers(headers -> headers.putAll(this.headers))
            .cookies(cookies -> cookies.addAll(this.cookies))
            .build();
  }

  @Override
  public ServerResponse render(String name, Object... modelAttributes) {
    return new DefaultRenderingResponseBuilder(name)
            .status(this.statusCode)
            .headers(headers -> headers.putAll(this.headers))
            .cookies(cookies -> cookies.addAll(this.cookies))
            .modelAttributes(modelAttributes)
            .build();
  }

  @Override
  public ServerResponse render(String name, Map<String, ?> model) {
    return new DefaultRenderingResponseBuilder(name)
            .status(this.statusCode)
            .headers(headers -> headers.putAll(this.headers))
            .cookies(cookies -> cookies.addAll(this.cookies))
            .modelAttributes(model)
            .build();
  }

  @Override
  public ServerResponse render(ModelAndView modelAndView) {
    return new ModelAndViewRenderingResponseBuilder(modelAndView)
            .status(this.statusCode)
            .headers(headers -> headers.putAll(this.headers))
            .cookies(cookies -> cookies.addAll(this.cookies))
            .build();
  }

  private static class WriteFunctionResponse extends AbstractServerResponse {

    private final WriteFunction writeFunction;

    public WriteFunctionResponse(HttpStatusCode statusCode, HttpHeaders headers,
            MultiValueMap<String, HttpCookie> cookies, WriteFunction writeFunction) {

      super(statusCode, headers, cookies);
      Assert.notNull(writeFunction, "WriteFunction is required");
      this.writeFunction = writeFunction;
    }

    @Nullable
    @Override
    protected Object writeToInternal(RequestContext request, Context context) throws Throwable {
      return this.writeFunction.write(request);
    }
  }

}
