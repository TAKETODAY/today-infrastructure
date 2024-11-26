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

package infra.web.handler.function;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.RequestContext;
import infra.web.view.ModelAndView;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/28 22:24
 */
final class ModelAndViewRenderingResponseBuilder implements RenderingResponse.ViewBuilder {

  private final ModelAndView modelAndView;

  private HttpStatusCode status = HttpStatus.OK;
  private final HttpHeaders headers = HttpHeaders.forWritable();
  private final LinkedMultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

  public ModelAndViewRenderingResponseBuilder(ModelAndView modelAndView) {
    Assert.notNull(modelAndView, "ModelAndView is required");
    this.modelAndView = modelAndView;
  }

  @Override
  public RenderingResponse.ViewBuilder status(HttpStatusCode status) {
    Assert.notNull(status, "HttpStatusCode is required");
    this.status = status;
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder status(int status) {
    return status(HttpStatusCode.valueOf(status));
  }

  @Override
  public RenderingResponse.ViewBuilder cookie(HttpCookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie.getName(), cookie);
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookie(String name, String... values) {
    for (String value : values) {
      this.cookies.add(name, new HttpCookie(name, value));
    }
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookies(Consumer<MultiValueMap<String, HttpCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookies(@Nullable Collection<HttpCookie> cookies) {
    if (CollectionUtils.isNotEmpty(cookies)) {
      for (HttpCookie cookie : cookies) {
        this.cookies.add(cookie.getName(), cookie);
      }
    }
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookies(@Nullable MultiValueMap<String, HttpCookie> cookies) {
    this.cookies.setAll(cookies);
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder header(String headerName, String... headerValues) {
    this.headers.setOrRemove(headerName, headerValues);
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder headers(@Nullable HttpHeaders headers) {
    this.headers.setAll(headers);
    return this;
  }

  @Override
  public RenderingResponse build() {
    return new ModelAndViewRenderingResponse(status, headers, cookies, modelAndView);
  }

  static class ModelAndViewRenderingResponse extends AbstractServerResponse implements RenderingResponse {

    private final ModelAndView modelAndView;

    public ModelAndViewRenderingResponse(HttpStatusCode statusCode, HttpHeaders headers,
            MultiValueMap<String, HttpCookie> cookies, ModelAndView modelAndView) {

      super(statusCode, headers, cookies);
      this.modelAndView = modelAndView;
    }

    @Override
    public String name() {
      return modelAndView.getViewName();
    }

    @Override
    public Map<String, Object> model() {
      return modelAndView.getModel();
    }

    @Override
    protected Object writeToInternal(RequestContext request, Context context) {
      if (modelAndView.getStatus() == null) {
        modelAndView.setStatus(statusCode());
      }
      return modelAndView;
    }

  }
}
