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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import infra.context.ApplicationContext;
import infra.http.ResponseTrailer;
import infra.web.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Protocol-level tests for HTTP/1.1 response trailers.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class ResponseTrailerTests {

  private final EmbeddedChannel channel = new EmbeddedChannel();

  @Test
  void http10RejectsRegistration() {
    HttpContext context = createContext(HttpVersion.HTTP_1_0, HttpMethod.GET);

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> context.setResponseTrailers(
                    ResponseTrailer.of("X-Checksum", () -> "v1")));
  }

  @Test
  void headRequestRejectsRegistration() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.HEAD);

    assertThatIllegalStateException()
            .isThrownBy(() -> context.setResponseTrailers(
                    ResponseTrailer.of("X-Checksum", () -> "v1")));
  }

  @Test
  void noBodyStatusRejectsRegistration() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    context.setStatus(204);

    assertThatIllegalStateException()
            .isThrownBy(() -> context.setResponseTrailers(
                    ResponseTrailer.of("X-Checksum", () -> "v1")));
  }

  @Test
  void trailersForceChunkedFramingAndDeclaration() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    context.setResponseTrailers(
            ResponseTrailer.of("Content-Digest", () -> "sha-256=:abc:"));

    context.flush();
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRANSFER_ENCODING)).isEqualTo("chunked");
    assertThat(response.headers().contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
    assertThat(response.headers().get(HttpHeaderNames.TRAILER)).isEqualTo("Content-Digest");
  }

  @Test
  void endResponseWritesTrailingHeaders() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    context.setResponseTrailers(
            ResponseTrailer.of("Content-Digest", () -> "sha-256=:abc:"));

    context.endResponse();

    LastHttpContent content = channel.readOutbound();
    assertThat(content.trailingHeaders().get("Content-Digest")).isEqualTo("sha-256=:abc:");
    assertThat(content.trailingHeaders().get("Content-Length")).isNull();
  }

  @Test
  void noTrailersKeepsEmptyTerminator() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);

    context.endResponse();

    LastHttpContent content = channel.readOutbound();
    assertThat(content.trailingHeaders().isEmpty()).isTrue();
  }

  @Test
  void supplierFailureClosesConnectionWithoutTerminator() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    context.setResponseTrailers(ResponseTrailer.of("X-Checksum", () -> {
      throw new IllegalStateException("boom");
    }));

    context.endResponse();

    assertThat(channel.isOpen()).isFalse();
    Object outbound = channel.readOutbound();
    assertThat(outbound).isNull();
  }

  @Test
  void resetClearsRegistration() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    AtomicInteger calls = new AtomicInteger();
    context.setResponseTrailers(ResponseTrailer.of("Content-Digest", () -> {
      calls.incrementAndGet();
      return "sha-256=:abc:";
    }));

    context.reset();
    context.endResponse();

    assertThat(calls).hasValue(0);
    LastHttpContent content = channel.readOutbound();
    assertThat(content.trailingHeaders().isEmpty()).isTrue();
  }

  @Test
  void sendErrorClearsRegistration() throws Exception {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    AtomicInteger calls = new AtomicInteger();
    context.setResponseTrailers(ResponseTrailer.of("Content-Digest", () -> {
      calls.incrementAndGet();
      return "sha-256=:abc:";
    }));

    context.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "boom");
    context.endResponse();

    assertThat(calls).hasValue(0);
    LastHttpContent content = channel.readOutbound();
    assertThat(content.trailingHeaders().isEmpty()).isTrue();
  }

  @Test
  void sendRedirectCommitsAndRejectsLaterRegistration() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);

    context.sendRedirect("/target");
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.status()).isEqualTo(HttpResponseStatus.FOUND);
    assertThat(response.headers().get(HttpHeaderNames.LOCATION)).isEqualTo("/target");
    assertThatIllegalStateException()
            .isThrownBy(() -> context.setResponseTrailers(
                    ResponseTrailer.of("Content-Digest", () -> "sha-256=:abc:")));
  }

  @Test
  void registrationInsideCommittingCallbackIsRejected() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    context.registerCallback(HttpContext.Lifecycle.COMMITTING, "register",
            () -> context.setResponseTrailers(
                    ResponseTrailer.of("Content-Digest", () -> "sha-256=:abc:")));

    assertThatIllegalStateException().isThrownBy(context::flush);
  }

  @ParameterizedTest
  @ValueSource(ints = { 204, 304 })
  void finalNoBodyStatusDiscardsTrailers(int status) {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    AtomicInteger calls = new AtomicInteger();
    context.setResponseTrailers(ResponseTrailer.of("Content-Digest", () -> {
      calls.incrementAndGet();
      return "sha-256=:abc:";
    }));
    context.registerCallback(HttpContext.Lifecycle.COMMITTING, "status", () -> context.setStatus(status));

    context.requestCompletedInternal(null);
    channel.flush();

    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.status().code()).isEqualTo(status);
    assertThat(response.headers().contains(HttpHeaderNames.TRAILER)).isFalse();
    assertThat(response.headers().contains(HttpHeaderNames.TRANSFER_ENCODING)).isFalse();
    LastHttpContent content = channel.readOutbound();
    assertThat(content.trailingHeaders().isEmpty()).isTrue();
    assertThat(calls).hasValue(0);
  }

  @Test
  void completionWaitsForEveryFileWriteBeforeEvaluatingTrailers() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    AtomicInteger calls = registerCountingTrailer(context);
    List<ChannelPromise> writes = delayFileWrites(context, 2);

    context.requestCompletedInternal(null);
    assertThat(calls).hasValue(0);
    writes.get(1).setSuccess();
    assertThat(calls).hasValue(0);
    writes.get(0).setSuccess();
    channel.runPendingTasks();

    assertThat(calls).hasValue(1);
    DefaultHttpResponse response = channel.readOutbound();
    assertThat(response.headers().get(HttpHeaderNames.TRAILER)).isEqualTo("Content-Digest");
    LastHttpContent content = channel.readOutbound();
    assertThat(content.trailingHeaders().get("Content-Digest")).isEqualTo("sha-256=:abc:");
    assertThat(channel.<Object>readOutbound()).isNull();
  }

  @Test
  void asynchronousFileWriteFailureClosesConnectionWithoutEvaluatingTrailers() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    AtomicInteger calls = registerCountingTrailer(context);
    List<ChannelPromise> writes = delayFileWrites(context, 2);

    context.requestCompletedInternal(null);
    channel.flush();
    assertThat(calls).hasValue(0);
    writes.get(0).setFailure(new IOException("file write failed"));
    writes.get(1).setSuccess();
    channel.runPendingTasks();

    assertThat(channel.isOpen()).isFalse();
    assertThat(calls).hasValue(0);
    assertThat(channel.<Object>readOutbound()).isInstanceOf(DefaultHttpResponse.class);
    assertThat(channel.<Object>readOutbound()).isNull();
  }

  @Test
  void fileWriteFailureBeforeCompletionDoesNotEvaluateTrailers() {
    HttpContext context = createContext(HttpVersion.HTTP_1_1, HttpMethod.GET);
    AtomicInteger calls = registerCountingTrailer(context);
    List<ChannelPromise> writes = delayFileWrites(context, 1);
    writes.get(0).setFailure(new IOException("file write failed"));

    context.requestCompletedInternal(null);
    channel.runPendingTasks();

    assertThat(channel.isOpen()).isFalse();
    assertThat(calls).hasValue(0);
    assertThat(channel.<Object>readOutbound()).isNull();
  }

  private AtomicInteger registerCountingTrailer(HttpContext context) {
    AtomicInteger calls = new AtomicInteger();
    context.setResponseTrailers(ResponseTrailer.of("Content-Digest", () -> {
      calls.incrementAndGet();
      return "sha-256=:abc:";
    }));
    return calls;
  }

  private List<ChannelPromise> delayFileWrites(HttpContext context, int count) {
    Object file = new Object();
    List<ChannelPromise> writes = new ArrayList<>();
    channel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
      @Override
      public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
        if (message == file) {
          writes.add(promise);
        }
        else {
          ctx.write(message, promise);
        }
      }
    });
    for (int i = 0; i < count; i++) {
      context.writeResponseFile(file);
    }
    assertThat(writes).hasSize(count);
    return writes;
  }

  private HttpContext createContext(HttpVersion version, HttpMethod method) {
    var request = new DefaultHttpRequest(version, method, "/test");
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock())
            .sendErrorHandler((request1, message) -> { })
            .build();
    return new HttpContext(channel, request, config, mock(ApplicationContext.class),
            mock(DispatcherHandler.class), mock(HttpTrafficHandler.class));
  }

}
