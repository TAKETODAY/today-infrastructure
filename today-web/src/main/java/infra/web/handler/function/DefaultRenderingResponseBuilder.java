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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import infra.core.Conventions;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;

/**
 * Default {@link RenderingResponse.Builder} implementation.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultRenderingResponseBuilder implements RenderingResponse.Builder {

  private final String name;

  private HttpStatusCode status = HttpStatus.OK;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final LinkedHashMap<String, Object> model = new LinkedHashMap<>();

  private final LinkedMultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

  public DefaultRenderingResponseBuilder(RenderingResponse other) {
    Assert.notNull(other, "RenderingResponse is required");
    this.name = other.name();
    this.status = other.statusCode();
    this.headers.putAll(other.headers());
    this.model.putAll(other.model());
  }

  public DefaultRenderingResponseBuilder(String name) {
    Assert.notNull(name, "Name is required");
    this.name = name;
  }

  @Override
  public RenderingResponse.Builder status(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatusCode is required");
    this.status = status;
    return this;
  }

  @Override
  public RenderingResponse.Builder status(int status) {
    return status(HttpStatusCode.valueOf(status));
  }

  @Override
  public RenderingResponse.Builder cookie(ResponseCookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie.getName(), cookie);
    return this;
  }

  @Override
  public RenderingResponse.Builder cookie(String name, String... values) {
    for (String value : values) {
      this.cookies.add(name, ResponseCookie.from(name, value).build());
    }
    return this;
  }

  @Override
  public RenderingResponse.Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public RenderingResponse.Builder cookies(@Nullable Collection<ResponseCookie> cookies) {
    if (CollectionUtils.isNotEmpty(cookies)) {
      for (ResponseCookie cookie : cookies) {
        this.cookies.add(cookie.getName(), cookie);
      }
    }
    return this;
  }

  @Override
  public RenderingResponse.Builder cookies(@Nullable MultiValueMap<String, ResponseCookie> cookies) {
    this.cookies.setAll(cookies);
    return this;
  }

  @Override
  public RenderingResponse.Builder modelAttribute(Object attribute) {
    Assert.notNull(attribute, "Attribute is required");
    if (attribute instanceof Collection && ((Collection<?>) attribute).isEmpty()) {
      return this;
    }
    return modelAttribute(Conventions.getVariableName(attribute), attribute);
  }

  @Override
  public RenderingResponse.Builder modelAttribute(String name, @Nullable Object value) {
    Assert.notNull(name, "Name is required");
    this.model.put(name, value);
    return this;
  }

  @Override
  public RenderingResponse.Builder modelAttributes(Object... attributes) {
    modelAttributes(Arrays.asList(attributes));
    return this;
  }

  @Override
  public RenderingResponse.Builder modelAttributes(Collection<?> attributes) {
    attributes.forEach(this::modelAttribute);
    return this;
  }

  @Override
  public RenderingResponse.Builder modelAttributes(Map<String, ?> attributes) {
    this.model.putAll(attributes);
    return this;
  }

  @Override
  public RenderingResponse.Builder header(String headerName, String... headerValues) {
    headers.setOrRemove(headerName, headerValues);
    return this;
  }

  @Override
  public RenderingResponse.Builder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public RenderingResponse.Builder headers(@Nullable HttpHeaders headers) {
    this.headers.setAll(headers);
    return this;
  }

  @Override
  public RenderingResponse build() {
    return new DefaultRenderingResponse(this.status, this.headers, this.cookies, this.name, this.model);
  }

  private static final class DefaultRenderingResponse extends AbstractServerResponse implements RenderingResponse {

    private final String name;
    private final Map<String, Object> model;

    public DefaultRenderingResponse(HttpStatusCode statusCode, HttpHeaders headers,
            MultiValueMap<String, ResponseCookie> cookies, String name, Map<String, Object> model) {

      super(statusCode, headers, cookies);
      this.name = name;
      this.model = Collections.unmodifiableMap(new LinkedHashMap<>(model));
    }

    @Override
    public String name() {
      return this.name;
    }

    @Override
    public Map<String, Object> model() {
      return this.model;
    }

    @Override
    protected Object writeToInternal(RequestContext request, Context context) {

      ModelAndView mav = new ModelAndView(this.name, statusCode());
      mav.addAllObjects(this.model);
      return mav;
    }

  }

}
