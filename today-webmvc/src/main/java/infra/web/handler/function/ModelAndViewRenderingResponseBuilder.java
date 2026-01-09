/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/28 22:24
 */
final class ModelAndViewRenderingResponseBuilder implements RenderingResponse.ViewBuilder {

  private final ModelAndView modelAndView;

  private HttpStatusCode status = HttpStatus.OK;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final LinkedMultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

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
  public RenderingResponse.ViewBuilder cookie(ResponseCookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie.getName(), cookie);
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookie(String name, String... values) {
    for (String value : values) {
      this.cookies.add(name, ResponseCookie.from(name, value).build());
    }
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookies(@Nullable Collection<ResponseCookie> cookies) {
    if (CollectionUtils.isNotEmpty(cookies)) {
      for (ResponseCookie cookie : cookies) {
        this.cookies.add(cookie.getName(), cookie);
      }
    }
    return this;
  }

  @Override
  public RenderingResponse.ViewBuilder cookies(@Nullable MultiValueMap<String, ResponseCookie> cookies) {
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
            MultiValueMap<String, ResponseCookie> cookies, ModelAndView modelAndView) {

      super(statusCode, headers, cookies);
      this.modelAndView = modelAndView;
    }

    @Override
    @SuppressWarnings("NullAway")
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
