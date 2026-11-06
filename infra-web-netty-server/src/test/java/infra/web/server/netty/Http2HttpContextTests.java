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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import infra.context.ApplicationContext;
import infra.http.HttpHeaders;
import infra.http.ResponseTrailer;
import infra.web.DispatcherHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.stream.ChunkedInput;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Tests for native HTTP/2 header access and response handling.
 */
class Http2HttpContextTests {

  private final Http2StreamChannel channel = mock();

  private final HttpTrafficHandler httpTrafficHandler = mock();

  private final Http2Headers requestHeaders = new DefaultHttp2Headers()
          .method("GET").scheme("https").authority("example.org:8443").path("/test?q=1");

  @ParameterizedTest
  @ValueSource(strings = { "x-custom", "X-Custom", "X-CUSTOM" })
  void requestHeaderAccessIgnoresNameCase(String name) {
    requestHeaders.add("x-custom", "First").add("x-custom", "Second");
    Http2HttpContext context = createContext();

    assertThat(context.getHeader(name)).isEqualTo("First");
    assertThat(context.getHeaders(name)).containsExactly("First", "Second");
    assertThat(context.containsHeader(name)).isTrue();
    assertThat(context.containsHeaderValue(name, "FIRST")).isTrue();
    assertThat(context.containsHeaderValue(name, "missing")).isFalse();
    assertThat(context.getHeaderNames()).contains("x-custom").doesNotContain("X-Custom");
  }

  @Test
  void missingRequestHeadersReturnEmptyResults() {
    Http2HttpContext context = createContext();

    assertThat(context.getHeader("X-Missing")).isNull();
    assertThat(context.getHeaders("X-Missing")).isEmpty();
    assertThat(context.containsHeader("X-Missing")).isFalse();
    assertThat(context.containsHeaderValue("X-Missing", "value")).isFalse();
  }

  @Test
  void requestHeaderViewSupportsMixedCaseAccess() {
    requestHeaders.add("x-custom", "First").add("x-custom", "Second");
    HttpHeaders headers = createContext().createRequestHeaders();

    assertThat(headers.getFirst("X-Custom")).isEqualTo("First");
    assertThat(headers.get("X-CUSTOM")).containsExactly("First", "Second");
    assertThat(headers.contains("X-Custom")).isTrue();
    assertThat(headers.contains("X-CUSTOM", "Second")).isTrue();
  }

  @Test
  void authorityIsExposedAsHostAndPseudoHeadersAreHidden() {
    requestHeaders.set("host", "ignored.example.org");
    Http2HttpContext context = createContext();

    assertThat(context.getHeader("Host")).isEqualTo("example.org:8443");
    assertThat(context.requestUri()).isEqualTo("/test?q=1");
    assertThat(context.version().text()).isEqualTo("HTTP/2.0");
    assertThat(context.getHeaderNames()).containsExactly("host");
    for (String name : List.of(":method", ":scheme", ":authority", ":path")) {
      assertThat(context.getHeader(name)).isNull();
      assertThat(context.containsHeader(name)).isFalse();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "connection", "keep-alive", "proxy-connection", "transfer-encoding", "upgrade" })
  void connectionSpecificRequestHeadersAreRemoved(String name) {
    requestHeaders.set(name, "forbidden");
    Http2HttpContext context = createContext();

    assertThat(context.containsHeader(name)).isFalse();
    assertThat(context.createRequestHeaders().contains(name)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = { "x-custom", "X-Custom", "X-CUSTOM" })
  void responseHeaderWritesNormalizeNames(String name) {
    Http2HttpContext context = createContext();

    context.setHeader(name, "First");
    context.addHeader(name, "Second");

    assertThat(context.nettyResponseHeaders.getAll("x-custom")).containsExactly("First", "Second");
    assertThat(context.containsResponseHeader("X-CUSTOM")).isTrue();
    assertThat(context.responseHeaders().get("X-Custom")).containsExactly("First", "Second");
    assertThat(context.nettyResponseHeaders.names()).containsExactly("x-custom");
  }

  @Test
  void settingResponseHeaderWithDifferentCaseReplacesAllValues() {
    Http2HttpContext context = createContext();
    context.addHeader("X-Custom", "First");
    context.addHeader("x-custom", "Second");

    context.setHeader("X-CUSTOM", "Replacement");

    assertThat(context.nettyResponseHeaders.getAll("x-custom")).containsExactly("Replacement");
  }

  @Test
  void settingNullResponseHeaderRemovesExistingValues() {
    Http2HttpContext context = createContext();
    context.setHeader("X-Custom", "value");

    context.setHeader("X-CUSTOM", null);

    assertThat(context.containsResponseHeader("x-custom")).isFalse();
  }

  @Test
  void bulkResponseHeaderWritesNormalizeNamesAndPreserveValues() {
    Http2HttpContext context = createContext();
    context.setHeader("X-Custom", "old");
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("X-Custom", "First");
    headers.add("X-Custom", "Second");
    headers.set("X-Other", "other");

    context.setHeaders(headers);
    context.addHeaders(headers);

    assertThat(context.nettyResponseHeaders.getAll("x-custom"))
            .containsExactly("First", "Second", "First", "Second");
    assertThat(context.nettyResponseHeaders.getAll("x-other")).containsExactly("other", "other");
    assertThat(context.nettyResponseHeaders.names()).containsExactlyInAnyOrder("x-custom", "x-other");
  }

  @Test
  void responseHeaderRemovalIgnoresCaseBeforeViewIsCreated() {
    Http2HttpContext context = createContext();
    context.setHeader("X-Custom", "value");

    assertThat(context.removeHeader("X-CUSTOM")).isTrue();
    assertThat(context.removeHeader("X-Custom")).isFalse();
    assertThat(context.nettyResponseHeaders.contains("x-custom")).isFalse();
  }

  @Test
  void responseHeaderViewAndContextShareNormalizedHeaders() {
    Http2HttpContext context = createContext();
    HttpHeaders headers = context.responseHeaders();
    headers.set("X-Custom", "First");
    context.addHeader("X-CUSTOM", "Second");

    assertThat(headers.get("x-CUSTOM")).containsExactly("First", "Second");
    assertThat(context.containsResponseHeader("X-Custom")).isTrue();
    assertThat(context.removeHeader("X-CUSTOM")).isTrue();
    assertThat(headers.contains("X-Custom")).isFalse();

    context.setHeader("X-Other", "value");
    assertThat(headers.remove("X-OTHER")).containsExactly("value");
    assertThat(context.containsResponseHeader("X-Other")).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = { "connection", "keep-alive", "proxy-connection", "transfer-encoding", "upgrade" })
  void responseHeadRemovesConnectionSpecificHeaders(String name) {
    Http2HttpContext context = createContext();
    context.setHeader(name, "forbidden");
    context.setHeader("X-Custom", "value");

    context.writeResponseHead(HttpResponseStatus.CREATED, context.nettyResponseHeaders);

    ArgumentCaptor<Http2HeadersFrame> frame = ArgumentCaptor.forClass(Http2HeadersFrame.class);
    then(channel).should().write(frame.capture());
    assertThat(frame.getValue().headers().status().toString()).isEqualTo("201");
    assertThat(frame.getValue().headers().contains(name)).isFalse();
    assertThat(frame.getValue().headers().get("x-custom").toString()).isEqualTo("value");
    assertThat(frame.getValue().isEndStream()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = { "100-continue", "100-Continue", "100-CONTINUE" })
  void continueExpectationIgnoresValueCaseAndCanBeRemoved(String value) {
    requestHeaders.set("expect", value);
    Http2HttpContext context = createContext();

    assertThat(context.is100ContinueExpected()).isTrue();
    context.removeExpectHeader();
    assertThat(context.is100ContinueExpected()).isFalse();
    assertThat(context.getHeader("Expect")).isNull();
  }

  @Test
  void unsupportedExpectationDoesNotTriggerContinue() {
    requestHeaders.set("expect", "something-else");

    assertThat(createContext().is100ContinueExpected()).isFalse();
  }

  @Test
  void underlyingHttp2HeadersRequireNormalizedNames() {
    Http2Headers headers = new DefaultHttp2Headers(true, true, 8);
    headers.set("content-type", "text/plain");

    assertThat(headers.get("Content-Type")).isNull();
    assertThat(headers.contains("Content-Type", "TEXT/PLAIN", true)).isFalse();
    assertThatExceptionOfType(io.netty.handler.codec.http2.Http2Exception.class)
            .isThrownBy(() -> headers.set("Content-Type", "text/plain"));
  }

  @Test
  void fileChunksDoNotEndStream() throws Exception {
    Http2FrameStream stream = mock();
    when(channel.stream()).thenReturn(stream);
    ChunkedInput<ByteBuf> input = mock();
    when(input.readChunk(any(ByteBufAllocator.class))).thenReturn(Unpooled.buffer().writeByte(1));

    Http2HttpContext context = createContext();

    @SuppressWarnings("unchecked")
    ChunkedInput<Http2DataFrame> file = (ChunkedInput<Http2DataFrame>) context.modifyChunkedFile(input);
    Http2DataFrame frame = file.readChunk(mock(ByteBufAllocator.class));

    assertThat(frame).isNotNull();
    assertThat(frame.isEndStream()).isFalse();
    assertThat(frame.stream()).isSameAs(stream);
  }

  @Test
  void regularResponseWritesEndStreamFrame() {
    Http2HttpContext context = createContext();

    context.endResponse();

    then(channel).should().writeAndFlush(any(DefaultHttp2DataFrame.class));
  }

  @Test
  void trailersAreSentAsTrailingHeadersFrameWithLowercaseNames() {
    Http2HttpContext context = createContext();
    context.setResponseTrailers(
            ResponseTrailer.of("Content-Digest", () -> "sha-256=:abc:"));

    context.endResponse();

    ArgumentCaptor<Http2HeadersFrame> captor = ArgumentCaptor.forClass(Http2HeadersFrame.class);
    then(channel).should().writeAndFlush(captor.capture());
    Http2HeadersFrame frame = captor.getValue();
    assertThat(frame.isEndStream()).isTrue();
    assertThat(frame.headers().get("content-digest")).isEqualTo("sha-256=:abc:");
    assertThat(frame.headers().names()).containsExactly("content-digest");
    assertThat(frame.headers().contains(Http2Headers.PseudoHeaderName.STATUS.value())).isFalse();
  }

  @Test
  void trailersSkipContentLengthAndTransferEncoding() {
    Http2HttpContext context = createContext();
    context.setResponseTrailers(
            ResponseTrailer.of("Content-Digest", () -> "sha-256=:abc:"));

    context.flush();

    ArgumentCaptor<Http2HeadersFrame> captor = ArgumentCaptor.forClass(Http2HeadersFrame.class);
    then(channel).should().write(captor.capture());
    Http2Headers headers = captor.getValue().headers();
    assertThat(headers.contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
    assertThat(headers.contains(HttpHeaderNames.TRANSFER_ENCODING)).isFalse();
    assertThat(headers.get("trailer")).isEqualTo("Content-Digest");
  }

  @Test
  void trailerFailureClosesStream() {
    Http2HttpContext context = createContext();
    context.setResponseTrailers(ResponseTrailer.of("X-Checksum", () -> {
      throw new IllegalStateException("boom");
    }));

    context.endResponse();

    then(channel).should().close();
    then(channel).should(never()).writeAndFlush(any());
  }

  @Test
  void fileWriteFailureClosesChannel() throws Exception {
    ChannelFuture future = mock();
    when(channel.writeAndFlush(any())).thenReturn(future);
    when(future.addListener(any(ChannelFutureListener.class))).thenReturn(future);

    Http2HttpContext context = createContext();
    context.writeResponseFile(mock(ByteBuf.class));

    ArgumentCaptor<ChannelFutureListener> listener = ArgumentCaptor.forClass(ChannelFutureListener.class);
    then(future).should().addListener(listener.capture());

    when(future.isSuccess()).thenReturn(false);
    when(future.channel()).thenReturn(channel);
    listener.getValue().operationComplete(future);

    then(channel).should().close();
  }

  @Test
  void continueExpectationDoesNotBypassMaxContentLength() throws Exception {
    Http2Headers headers = new DefaultHttp2Headers()
            .method("POST").scheme("https").path("/upload")
            .set(HttpHeaderNames.EXPECT, HttpHeaders.CONTINUE);
    DispatcherHandler dispatcher = mock();
    when(dispatcher.requestContinueExpected(any())).thenReturn(true);
    Http2HttpContext context = createContext(dispatcher, config(10), headers);

    context.run();

    ByteBuf data = Unpooled.buffer(11).writeZero(11);
    context.onDataReceived(data, false);

    assertThatThrownBy(() -> context.getInputStream().read())
            .isInstanceOf(IOException.class);
  }

  @Test
  void oversizedContentLengthRejectsContinue() {
    Http2Headers headers = new DefaultHttp2Headers()
            .method("POST").scheme("https").path("/upload")
            .set(HttpHeaderNames.CONTENT_LENGTH, "11")
            .set(HttpHeaderNames.EXPECT, HttpHeaders.CONTINUE);
    DispatcherHandler dispatcher = mock();
    when(dispatcher.requestContinueExpected(any())).thenReturn(true);
    Http2HttpContext context = createContext(dispatcher, config(10), headers);

    assertThat(context.tryWriteResponseContinue()).isFalse();
  }

  private Http2HttpContext createContext() {
    ApplicationContext applicationContext = mock();
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock()).sendErrorHandler(mock()).build();
    return new Http2HttpContext(channel, requestHeaders, config, applicationContext,
            new DispatcherHandler(applicationContext), httpTrafficHandler);
  }

  private Http2HttpContext createContext(DispatcherHandler dispatcher, NettyRequestConfig config, Http2Headers headers) {
    return new Http2HttpContext(channel, headers, config, mock(ApplicationContext.class), dispatcher, httpTrafficHandler);
  }

  private static NettyRequestConfig config(long maxContentLength) {
    return NettyRequestConfig.forBuilder(false)
            .maxContentLength(maxContentLength)
            .multipartParser(mock()).sendErrorHandler(mock()).build();
  }

}