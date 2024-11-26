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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.converter.ResourceHttpMessageConverter;
import infra.http.converter.ResourceRegionHttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.StringUtils;
import infra.web.mock.MockRequestContext;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class ResourceHandlerFunctionTests {

  private final Resource resource = new ClassPathResource("response.txt", getClass());

  private final ResourceHandlerFunction handlerFunction = new ResourceHandlerFunction(this.resource, (r, h) -> { });

  private ServerResponse.Context context;

  private ResourceHttpMessageConverter messageConverter;

  @BeforeEach
  public void createContext() {
    this.messageConverter = new ResourceHttpMessageConverter();
    ResourceRegionHttpMessageConverter regionConverter = new ResourceRegionHttpMessageConverter();
    this.context = () -> Arrays.asList(messageConverter, regionConverter);
  }

  @Test
  public void get() throws Throwable {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    var requestContext = new MockRequestContext(null, servletRequest, servletResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);
    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity()).isEqualTo(this.resource);

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(servletResponse.getStatus()).isEqualTo(200);
    byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());
    byte[] actualBytes = servletResponse.getContentAsByteArray();
    assertThat(actualBytes).isEqualTo(expectedBytes);
    assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(servletResponse.getContentLength()).isEqualTo(this.resource.contentLength());
  }

  @Test
  public void getRange() throws Throwable {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addHeader("Range", "bytes=0-5");
    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();

    MockRequestContext requestContext = new MockRequestContext(
            null, servletRequest, servletResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);
    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity()).isEqualTo(this.resource);

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(servletResponse.getStatus()).isEqualTo(206);
    byte[] expectedBytes = new byte[6];
    try (InputStream is = this.resource.getInputStream()) {
      is.read(expectedBytes);
    }
    byte[] actualBytes = servletResponse.getContentAsByteArray();
    assertThat(actualBytes).isEqualTo(expectedBytes);
    assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(servletResponse.getContentLength()).isEqualTo(6);
    assertThat(servletResponse.getHeader(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
  }

  @Test
  public void getInvalidRange() throws Throwable {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addHeader("Range", "bytes=0-10, 0-10, 0-10, 0-10, 0-10, 0-10");

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    MockRequestContext requestContext = new MockRequestContext(
            null, servletRequest, servletResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);
    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity()).isEqualTo(this.resource);

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(servletResponse.getStatus()).isEqualTo(416);
    byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());
    byte[] actualBytes = servletResponse.getContentAsByteArray();
    assertThat(actualBytes).isEqualTo(expectedBytes);
    assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(servletResponse.getContentLength()).isEqualTo(this.resource.contentLength());
    assertThat(servletResponse.getHeader(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
  }

  @Test
  public void head() throws Throwable {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("HEAD", "/", true);

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    var requestContext = new MockRequestContext(null, servletRequest, servletResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response).isInstanceOf(EntityResponse.class);

    @SuppressWarnings("unchecked")
    EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
    assertThat(entityResponse.entity().getName()).isEqualTo(this.resource.getName());

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(servletResponse.getStatus()).isEqualTo(200);
    byte[] actualBytes = servletResponse.getContentAsByteArray();
    assertThat(actualBytes.length).isEqualTo(0);
    assertThat(servletResponse.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(servletResponse.getContentLength()).isEqualTo(this.resource.contentLength());
  }

  @Test
  public void options() throws Throwable {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("OPTIONS", "/", true);

    MockHttpResponseImpl servletResponse = new MockHttpResponseImpl();
    var requestContext = new MockRequestContext(null, servletRequest, servletResponse);

    ServerRequest request = new DefaultServerRequest(requestContext, Collections.singletonList(messageConverter));

    ServerResponse response = this.handlerFunction.handle(request);
    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.headers().getAllow()).isEqualTo(Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));

    Object mav = response.writeTo(requestContext, this.context);
    assertThat(mav).isNull();
    requestContext.flush();

    assertThat(servletResponse.getStatus()).isEqualTo(200);
    String allowHeader = servletResponse.getHeader("Allow");
    String[] methods = StringUtils.tokenizeToStringArray(allowHeader, ",");
    assertThat(methods).containsExactlyInAnyOrder("GET", "HEAD", "OPTIONS");
    byte[] actualBytes = servletResponse.getContentAsByteArray();
    assertThat(actualBytes.length).isEqualTo(0);
  }

}
