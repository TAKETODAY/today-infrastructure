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

package cn.taketoday.test.web.mock.client;

import java.io.StringWriter;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.http.codec.multipart.DefaultPartHttpMessageReader;
import cn.taketoday.http.codec.multipart.FilePart;
import cn.taketoday.http.codec.multipart.Part;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.http.Cookie;
import cn.taketoday.mock.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.mock.http.client.reactive.MockClientHttpResponse;
import cn.taketoday.mock.http.server.reactive.MockServerHttpRequest;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.mock.web.MockPart;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.test.web.mock.MvcResult;
import cn.taketoday.test.web.mock.RequestBuilder;
import cn.taketoday.test.web.mock.request.MockHttpRequestBuilder;
import cn.taketoday.test.web.mock.request.MockMultipartHttpRequestBuilder;
import cn.taketoday.test.web.mock.request.MockMvcRequestBuilders;
import cn.taketoday.test.web.mock.request.RequestPostProcessor;
import cn.taketoday.test.web.mock.result.MockMvcResultHandlers;
import cn.taketoday.test.web.reactive.server.MockServerClientHttpResponse;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.ModelAndView;
import reactor.core.publisher.Mono;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.asyncDispatch;

/**
 * Connector that handles requests by invoking a {@link MockMvc} rather than
 * making actual requests over HTTP.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MockMvcHttpConnector implements ClientHttpConnector {

  private static final DefaultPartHttpMessageReader MULTIPART_READER = new DefaultPartHttpMessageReader();

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final MockMvc mockMvc;

  private final List<RequestPostProcessor> requestPostProcessors;

  public MockMvcHttpConnector(MockMvc mockMvc) {
    this(mockMvc, Collections.emptyList());
  }

  private MockMvcHttpConnector(MockMvc mockMvc, List<RequestPostProcessor> requestPostProcessors) {
    this.mockMvc = mockMvc;
    this.requestPostProcessors = new ArrayList<>(requestPostProcessors);
  }

  @Override
  public Mono<ClientHttpResponse> connect(
          HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    RequestBuilder requestBuilder = adaptRequest(method, uri, requestCallback);
    try {
      MvcResult mvcResult = this.mockMvc.perform(requestBuilder).andReturn();
      if (mvcResult.getRequest().isAsyncStarted()) {
        mvcResult.getAsyncResult();
        mvcResult = this.mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
      }
      return Mono.just(adaptResponse(mvcResult));
    }
    catch (Exception ex) {
      return Mono.error(ex);
    }
  }

  private RequestBuilder adaptRequest(
          HttpMethod httpMethod, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    MockClientHttpRequest httpRequest = new MockClientHttpRequest(httpMethod, uri);

    AtomicReference<byte[]> contentRef = new AtomicReference<>();
    httpRequest.setWriteHandler(dataBuffers ->
            DataBufferUtils.join(dataBuffers)
                    .doOnNext(buffer -> {
                      byte[] bytes = new byte[buffer.readableByteCount()];
                      buffer.read(bytes);
                      DataBufferUtils.release(buffer);
                      contentRef.set(bytes);
                    })
                    .then());

    // Initialize the client request
    requestCallback.apply(httpRequest).block(TIMEOUT);

    MockHttpRequestBuilder requestBuilder =
            initRequestBuilder(httpMethod, uri, httpRequest, contentRef.get());

    requestBuilder.headers(httpRequest.getHeaders());
    for (List<HttpCookie> cookies : httpRequest.getCookies().values()) {
      for (HttpCookie cookie : cookies) {
        requestBuilder.cookie(new Cookie(cookie.getName(), cookie.getValue()));
      }
    }

    this.requestPostProcessors.forEach(requestBuilder::with);

    return requestBuilder;
  }

  private MockHttpRequestBuilder initRequestBuilder(
          HttpMethod httpMethod, URI uri, MockClientHttpRequest httpRequest, @Nullable byte[] bytes) {

    String contentType = httpRequest.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
    if (!StringUtils.startsWithIgnoreCase(contentType, "multipart/")) {
      MockHttpRequestBuilder requestBuilder = MockMvcRequestBuilders.request(httpMethod, uri);
      if (ObjectUtils.isNotEmpty(bytes)) {
        requestBuilder.content(bytes);
      }
      return requestBuilder;
    }

    // Parse the multipart request in order to adapt to Web Part's
    MockMultipartHttpRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(httpMethod, uri);

    Assert.notNull(bytes, "No multipart content");
    ReactiveHttpInputMessage inputMessage = MockServerHttpRequest.post(uri.toString())
            .headers(httpRequest.getHeaders())
            .body(Mono.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)));

    MULTIPART_READER.read(ResolvableType.forClass(Part.class), inputMessage, Collections.emptyMap())
            .flatMap(part ->
                    DataBufferUtils.join(part.content())
                            .doOnNext(buffer -> {
                              byte[] partBytes = new byte[buffer.readableByteCount()];
                              buffer.read(partBytes);
                              DataBufferUtils.release(buffer);

                              // Adapt to cn.taketoday.mock.api.http.Part...
                              MockPart mockPart = (part instanceof FilePart filePart ?
                                      new MockPart(part.name(), filePart.filename(), partBytes) :
                                      new MockPart(part.name(), partBytes));
                              mockPart.getHeaders().putAll(part.headers());
                              requestBuilder.part(mockPart);
                            }))
            .blockLast(TIMEOUT);

    return requestBuilder;
  }

  private MockClientHttpResponse adaptResponse(MvcResult mvcResult) {
    MockClientHttpResponse clientResponse = new MockMvcServerClientHttpResponse(mvcResult);
    MockHttpResponseImpl servletResponse = mvcResult.getResponse();
    for (String header : servletResponse.getHeaderNames()) {
      for (String value : servletResponse.getHeaders(header)) {
        clientResponse.getHeaders().add(header, value);
      }
    }
    if (servletResponse.getForwardedUrl() != null) {
      clientResponse.getHeaders().add("Forwarded-Url", servletResponse.getForwardedUrl());
    }
    for (Cookie cookie : servletResponse.getCookies()) {
      ResponseCookie httpCookie =
              ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
                      .maxAge(Duration.ofSeconds(cookie.getMaxAge()))
                      .domain(cookie.getDomain())
                      .path(cookie.getPath())
                      .secure(cookie.getSecure())
                      .httpOnly(cookie.isHttpOnly())
                      .sameSite(cookie.getAttribute("samesite"))
                      .build();
      clientResponse.getCookies().add(httpCookie.getName(), httpCookie);
    }
    byte[] bytes = servletResponse.getContentAsByteArray();
    DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    clientResponse.setBody(Mono.just(dataBuffer));
    return clientResponse;
  }

  /**
   * Create a new instance that applies the given {@link RequestPostProcessor}s
   * to performed requests.
   */
  public MockMvcHttpConnector with(List<RequestPostProcessor> postProcessors) {
    return new MockMvcHttpConnector(this.mockMvc, postProcessors);
  }

  private static class MockMvcServerClientHttpResponse
          extends MockClientHttpResponse implements MockServerClientHttpResponse {

    private final MvcResult mvcResult;

    public MockMvcServerClientHttpResponse(MvcResult result) {
      super(result.getResponse().getStatus());
      this.mvcResult = new PrintingMvcResult(result);
    }

    @Override
    public Object getServerResult() {
      return this.mvcResult;
    }
  }

  private static class PrintingMvcResult implements MvcResult {

    private final MvcResult mvcResult;

    public PrintingMvcResult(MvcResult mvcResult) {
      this.mvcResult = mvcResult;
    }

    @Override
    public HttpMockRequestImpl getRequest() {
      return this.mvcResult.getRequest();
    }

    @Override
    public MockHttpResponseImpl getResponse() {
      return this.mvcResult.getResponse();
    }

    @Override
    public RequestContext getRequestContext() {
      return mvcResult.getRequestContext();
    }

    @Nullable
    @Override
    public Object getHandler() {
      return this.mvcResult.getHandler();
    }

    @Nullable
    @Override
    public HandlerInterceptor[] getInterceptors() {
      return this.mvcResult.getInterceptors();
    }

    @Nullable
    @Override
    public ModelAndView getModelAndView() {
      return this.mvcResult.getModelAndView();
    }

    @Nullable
    @Override
    public Throwable getResolvedException() {
      return this.mvcResult.getResolvedException();
    }

    @Override
    public RedirectModel getFlashMap() {
      return this.mvcResult.getFlashMap();
    }

    @Override
    public Object getAsyncResult() {
      return this.mvcResult.getAsyncResult();
    }

    @Override
    public Object getAsyncResult(long timeToWait) {
      return this.mvcResult.getAsyncResult(timeToWait);
    }

    @Override
    public String toString() {
      StringWriter writer = new StringWriter();
      try {
        MockMvcResultHandlers.print(writer).handle(this);
      }
      catch (Exception ex) {
        writer.append("Unable to format ")
                .append(String.valueOf(this))
                .append(": ")
                .append(ex.getMessage());
      }
      return writer.toString();
    }
  }

}
