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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.converter.ResourceHttpMessageConverter;
import infra.http.converter.ResourceRegionHttpMessageConverter;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.util.StringUtils;
import infra.web.handler.function.ResourceHandlerFunction.HeadMethodResource;
import infra.web.mock.MockRequestContext;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ResourceHandlerFunctionTests {

  private final Resource resource = new ClassPathResource("response.txt", getClass());

  private final ResourceHandlerFunction handlerFunction = new ResourceHandlerFunction(this.resource, (r, h) -> { });

  private ServerResponse.Context context;

  private ResourceHttpMessageConverter messageConverter;

  @BeforeEach
  void createContext() {
    this.messageConverter = new ResourceHttpMessageConverter();
    ResourceRegionHttpMessageConverter regionConverter = new ResourceRegionHttpMessageConverter();
    this.context = () -> Arrays.asList(messageConverter, regionConverter);
  }

  @Test
  void get() throws Throwable {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);
    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity()).isEqualTo(this.resource);

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getStatus()).isEqualTo(200);
    byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());
    byte[] actualBytes = mockResponse.getContentAsByteArray();
    assertThat(actualBytes).isEqualTo(expectedBytes);
    assertThat(mockResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(mockResponse.getContentLength()).isEqualTo(this.resource.contentLength());
  }

  @Test
  void getRange() throws Throwable {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.addHeader("Range", "bytes=0-5");
    MockResponse mockResponse = new MockResponse();

    MockRequestContext requestContext = new MockRequestContext(
            null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);
    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity()).isEqualTo(this.resource);

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getStatus()).isEqualTo(206);
    byte[] expectedBytes = new byte[6];
    try (InputStream is = this.resource.getInputStream()) {
      is.read(expectedBytes);
    }
    byte[] actualBytes = mockResponse.getContentAsByteArray();
    assertThat(actualBytes).isEqualTo(expectedBytes);
    assertThat(mockResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(mockResponse.getContentLength()).isEqualTo(6);
    assertThat(mockResponse.getHeader(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
  }

  @Test
  void getInvalidRange() throws Throwable {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.addHeader("Range", "bytes=0-10, 0-10, 0-10, 0-10, 0-10, 0-10");

    MockResponse mockResponse = new MockResponse();
    MockRequestContext requestContext = new MockRequestContext(
            null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);
    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity()).isEqualTo(this.resource);

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getStatus()).isEqualTo(416);
    byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());
    byte[] actualBytes = mockResponse.getContentAsByteArray();
    assertThat(actualBytes).isEqualTo(expectedBytes);
    assertThat(mockResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(mockResponse.getContentLength()).isEqualTo(this.resource.contentLength());
    assertThat(mockResponse.getHeader(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
  }

  @Test
  void head() throws Throwable {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("HEAD", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);

    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity().getName()).isEqualTo(this.resource.getName());

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getStatus()).isEqualTo(200);
    byte[] actualBytes = mockResponse.getContentAsByteArray();
    assertThat(actualBytes.length).isEqualTo(0);
    assertThat(mockResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(mockResponse.getContentLength()).isEqualTo(this.resource.contentLength());
  }

  @Test
  void options() throws Throwable {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("OPTIONS", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.headers().getAllow()).isEqualTo(Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isNull();
    requestContext.flush();

    assertThat(mockResponse.getStatus()).isEqualTo(200);
    String allowHeader = mockResponse.getHeader("Allow");
    String[] methods = StringUtils.tokenizeToStringArray(allowHeader, ",");
    assertThat(methods).containsExactlyInAnyOrder("GET", "HEAD", "OPTIONS");
    byte[] actualBytes = mockResponse.getContentAsByteArray();
    assertThat(actualBytes.length).isEqualTo(0);
  }

  @Test
  void postMethodNotAllowed() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("POST", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(new ResourceHttpMessageConverter()));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.headers().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
  }

  @Test
  void putMethodNotAllowed() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("PUT", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(new ResourceHttpMessageConverter()));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.headers().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
  }

  @Test
  void deleteMethodNotAllowed() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("DELETE", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(new ResourceHttpMessageConverter()));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.headers().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
  }

  @Test
  void patchMethodNotAllowed() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("PATCH", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(new ResourceHttpMessageConverter()));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.headers().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
  }

  @Test
  void traceMethodNotAllowed() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("TRACE", "/", true);

    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(new ResourceHttpMessageConverter()));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.headers().getAllow()).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
  }

  @Test
  void headMethodResourceGetInputStreamReturnsEmptyStream() throws IOException {
    Resource originalResource = new ClassPathResource("response.txt", getClass());
    HeadMethodResource headResource = new HeadMethodResource(originalResource);

    try (InputStream inputStream = headResource.getInputStream()) {
      assertThat(inputStream).isNotNull();
      assertThat(inputStream.read()).isEqualTo(-1); // EOF
    }
  }

  @Test
  void headMethodResourceDelegatesAllMethods() throws IOException {
    Resource originalResource = new ClassPathResource("response.txt", getClass());
    HeadMethodResource headResource = new HeadMethodResource(originalResource);

    assertThat(headResource.exists()).isEqualTo(originalResource.exists());
    assertThat(headResource.getURL()).isEqualTo(originalResource.getURL());
    assertThat(headResource.getURI()).isEqualTo(originalResource.getURI());
    assertThat(headResource.getFile()).isEqualTo(originalResource.getFile());
    assertThat(headResource.contentLength()).isEqualTo(originalResource.contentLength());
    assertThat(headResource.lastModified()).isEqualTo(originalResource.lastModified());
    assertThat(headResource.getName()).isEqualTo(originalResource.getName());
    assertThat(headResource.toString()).isEqualTo(originalResource.toString());
  }

  @Test
  void resourceHandlerFunctionWithHeadersConsumer() throws Throwable {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    MockResponse mockResponse = new MockResponse();
    var requestContext = new MockRequestContext(null, mockRequest, mockResponse);
    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(new ResourceHttpMessageConverter()));

    BiConsumer<Resource, HttpHeaders> headersConsumer = (resource, headers) -> headers.set("X-Test-Header", "test-value");
    ResourceHandlerFunction handlerFunctionWithHeaders = new ResourceHandlerFunction(this.resource, headersConsumer);

    ServerResponse response = handlerFunctionWithHeaders.handle(request);
    Object mav = response.writeTo(requestContext, this.context);

    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);
    assertThat(mockResponse.getHeader("X-Test-Header")).isEqualTo("test-value");
  }

}
