/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server.netty;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import infra.context.ApplicationContext;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.DispatcherHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for form parameter parsing in {@link NettyHttpContext}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class NettyHttpContextParametersTests {

  private static final String FORM_URLENCODED = MediaType.APPLICATION_FORM_URLENCODED_VALUE;

  private final EmbeddedChannel channel = new EmbeddedChannel();

  private final ApplicationContext applicationContext = mock();

  private final DispatcherHandler dispatcherHandler = mock();

  private final HttpTrafficHandler trafficHandler = mock();

  @AfterEach
  void tearDown() {
    channel.finishAndReleaseAll();
  }

  @Test
  void chunkedFormBodyIsParsed() {
    HttpRequest request = formRequest("/echo?q=query", FORM_URLENCODED);
    request.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
    HttpContext context = createContext(request, config());

    sendBody(context, "name=today", StandardCharsets.US_ASCII);

    assertThat(context.getParameters())
            .containsEntry("q", List.of("query"))
            .containsEntry("name", List.of("today"));
  }

  @Test
  void nonFormContentTypeDoesNotConsumeBody() throws Exception {
    HttpRequest request = formRequest("/echo", "application/x-www-form-urlencodedx");
    HttpContext context = createContext(request, config());

    sendBody(context, "name=today", StandardCharsets.US_ASCII);

    assertThat(context.getParameters()).isEmpty();
    assertThat(context.getInputStream().readAllBytes()).isEqualTo("name=today".getBytes(StandardCharsets.US_ASCII));
  }

  @Test
  void getRequestDoesNotParseFormBody() throws Exception {
    HttpRequest request = formRequest(HttpMethod.GET, "/echo", FORM_URLENCODED, "10");
    HttpContext context = createContext(request, config());

    sendBody(context, "name=today", StandardCharsets.US_ASCII);

    assertThat(context.getParameters()).isEmpty();
    assertThat(context.getInputStream().readAllBytes()).isEqualTo("name=today".getBytes(StandardCharsets.US_ASCII));
  }

  @Test
  void contentLengthFormBodyIsParsed() {
    String body = "name=today&city=%E6%9D%AD%E5%B7%9E&note=a+b";
    HttpRequest request = formRequest(HttpMethod.POST, "/echo?q=query", FORM_URLENCODED, String.valueOf(body.length()));
    HttpContext context = createContext(request, config());

    sendBody(context, body, StandardCharsets.US_ASCII);

    assertThat(context.getParameters())
            .containsEntry("q", List.of("query"))
            .containsEntry("name", List.of("today"))
            .containsEntry("city", List.of("杭州"))
            .containsEntry("note", List.of("a b"));
  }

  @Test
  void formContentTypeParametersAreAccepted() {
    String body = "name=today";
    HttpRequest request = formRequest(HttpMethod.POST, "/echo",
            FORM_URLENCODED + "; charset=UTF-8", String.valueOf(body.length()));
    HttpContext context = createContext(request, config());

    sendBody(context, body, StandardCharsets.US_ASCII);

    assertThat(context.getParameters()).containsEntry("name", List.of("today"));
  }

  @Test
  void queryParametersComeBeforeBodyParameters() {
    String body = "tag=body&id=2";
    HttpRequest request = formRequest(HttpMethod.POST, "/echo?tag=query&id=1", FORM_URLENCODED, String.valueOf(body.length()));
    HttpContext context = createContext(request, config());

    sendBody(context, body, StandardCharsets.US_ASCII);

    assertThat(context.getParameters())
            .containsEntry("tag", List.of("query", "body"))
            .containsEntry("id", List.of("1", "2"));
  }

  @Test
  void nonFormContentTypeWithContentLengthDoesNotConsumeBody() throws Exception {
    String body = "name=today";
    HttpRequest request = formRequest(HttpMethod.POST, "/echo", MediaType.APPLICATION_JSON_VALUE, String.valueOf(body.length()));
    HttpContext context = createContext(request, config());

    sendBody(context, body, StandardCharsets.US_ASCII);

    assertThat(context.getParameters()).isEmpty();
    assertThat(context.getInputStream().readAllBytes()).isEqualTo(body.getBytes(StandardCharsets.US_ASCII));
  }

  @Test
  void zeroContentLengthFormBodyProducesNoParameters() {
    HttpRequest request = formRequest(HttpMethod.POST, "/echo", FORM_URLENCODED, "0");
    HttpContext context = createContext(request, config());

    sendBody(context, "", StandardCharsets.US_ASCII);

    assertThat(context.getParameters()).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void cachedFormBodyIsMergedWithQueryParameters() {
    HttpRequest request = formRequest(HttpMethod.POST, "/echo?q=query", FORM_URLENCODED, "0");
    HttpContext context = createContext(request, config());

    MultiValueMap<String, String> formBody = MultiValueMap.forSmartListAdaptation(new LinkedHashMap<>());
    formBody.add("name", "today");
    context.setAttribute(NettyHttpContext.FORM_URLENCODED_ATTRIBUTE, formBody);

    assertThat(context.getParameters())
            .containsEntry("q", List.of("query"))
            .containsEntry("name", List.of("today"));
  }

  private HttpContext createContext(HttpRequest request, NettyRequestConfig config) {
    return new HttpContext(channel, request, config, applicationContext, dispatcherHandler, trafficHandler);
  }

  private static HttpRequest formRequest(String uri, String contentType) {
    return formRequest(HttpMethod.POST, uri, contentType, null);
  }

  private static HttpRequest formRequest(HttpMethod method, String uri, String contentType, String contentLength) {
    HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, uri);
    if (contentType != null) {
      request.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }
    if (contentLength != null) {
      request.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
    }
    return request;
  }

  private static NettyRequestConfig config() {
    return NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler((request, message) -> { })
            .build();
  }

  private static void sendBody(HttpContext context, String body, Charset charset) {
    ByteBuf content = Unpooled.copiedBuffer(body, charset);
    context.onDataReceived(content, true);
  }

}
