/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import infra.core.ParameterizedTypeReference;
import infra.http.CacheControl;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;

/**
 * Default {@link ServerResponse.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

  private final HttpStatusCode statusCode;

  private final HttpHeaders headers = HttpHeaders.forWritable();

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
    this.headers.setOrRemove(headerName, headerValues);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder headers(@Nullable HttpHeaders headers) {
    this.headers.setAll(headers);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cookie(HttpCookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie.getName(), cookie);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cookie(String name, String... values) {
    for (String value : values) {
      this.cookies.add(name, new HttpCookie(name, value));
    }
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cookies(@Nullable Collection<HttpCookie> cookies) {
    if (CollectionUtils.isNotEmpty(cookies)) {
      for (HttpCookie cookie : cookies) {
        this.cookies.add(cookie.getName(), cookie);
      }
    }
    return this;
  }

  @Override
  public ServerResponse.BodyBuilder cookies(@Nullable MultiValueMap<String, HttpCookie> cookies) {
    this.cookies.setAll(cookies);
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
  public ServerResponse.BodyBuilder eTag(@Nullable String etag) {
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
            .status(statusCode)
            .headers(headers)
            .cookies(cookies)
            .build();
  }

  @Override
  public <T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType) {
    return new DefaultEntityResponseBuilder<>(body, bodyType.getType())
            .status(statusCode)
            .headers(headers)
            .cookies(cookies)
            .build();
  }

  @Override
  public ServerResponse render(String name, Object... modelAttributes) {
    return new DefaultRenderingResponseBuilder(name)
            .status(statusCode)
            .headers(headers)
            .cookies(cookies)
            .modelAttributes(modelAttributes)
            .build();
  }

  @Override
  public ServerResponse render(String name, Map<String, ?> model) {
    return new DefaultRenderingResponseBuilder(name)
            .status(statusCode)
            .headers(headers)
            .cookies(cookies)
            .modelAttributes(model)
            .build();
  }

  @Override
  public ServerResponse render(ModelAndView modelAndView) {
    return new ModelAndViewRenderingResponseBuilder(modelAndView)
            .status(statusCode)
            .headers(headers)
            .cookies(cookies)
            .build();
  }

  @Override
  public ServerResponse stream(Consumer<ServerResponse.StreamBuilder> streamConsumer) {
    return StreamingServerResponse.create(this.statusCode, this.headers, this.cookies, streamConsumer, null);
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
