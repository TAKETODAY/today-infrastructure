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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import infra.context.ApplicationContext;
import infra.http.HttpHeaders;
import infra.web.Trailer;
import infra.web.DispatcherHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedNioFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Resource ownership tests for response files awaiting transfer to the pipeline.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class PendingResponseFileTests {

  @TempDir
  Path directory;

  private final EmbeddedChannel channel = new EmbeddedChannel();

  @AfterEach
  void releaseChannel() {
    channel.finishAndReleaseAll();
  }

  @Test
  void channelInactiveReleasesBufferedBody() throws Exception {
    HttpContext context = createContext();
    context.getOutputStream().write(1);
    ByteBuf body = (ByteBuf) pendingBody(context);

    context.onChannelInactive();
    context.onChannelInactive();

    assertThat(body.refCnt()).isZero();
    assertThat(pendingBody(context)).isNull();
  }

  @Test
  void channelInactiveClosesPendingInput() throws Exception {
    HttpContext context = createContext();
    ChunkedInput<?> input = mock();
    setPendingBody(context, input);

    context.onChannelInactive();
    context.onChannelInactive();

    then(input).should().close();
    assertThat(pendingBody(context)).isNull();
  }

  @Test
  void sendFileRejectsBufferedBody() throws Exception {
    HttpContext context = createContext();
    context.getOutputStream().write(1);
    ByteBuf body = (ByteBuf) pendingBody(context);

    assertThatThrownBy(() -> context.asHttpOutputMessage().sendFile(file(), 0, 4))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("response body");

    assertThat(body.refCnt()).isOne();
    context.onChannelInactive();
    assertThat(body.refCnt()).isZero();
  }

  @Test
  void writingBodyAfterSendFileFails() throws Exception {
    HttpContext context = createContext();
    context.asHttpOutputMessage().sendFile(file(), 0, 4);

    assertThatThrownBy(() -> context.getOutputStream().write(1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("response file");

    context.reset();
  }

  @Test
  void synchronousWriteFailureClosesPreparedChunkedInput() throws Exception {
    Channel failingChannel = mock();
    HttpContext context = createContext(failingChannel);
    ChunkedInput<?> input = mock();
    IllegalStateException failure = new IllegalStateException("write failed");
    doThrow(failure).when(failingChannel).writeAndFlush(input);

    assertThatThrownBy(() -> context.writeResponseFile(input)).isSameAs(failure);

    then(input).should().close();
  }

  @Test
  void resetClosesPendingChunkedInputOnce() throws Exception {
    HttpContext context = createContext();
    ChunkedInput<?> input = mock();
    setPendingBody(context, input);

    context.reset();
    context.reset();

    then(input).should().close();
    assertThat(pendingBody(context)).isNull();
  }

  @Test
  void exceptionalCompletionClosesPendingChunkedInput() throws Exception {
    HttpContext context = createContext();
    ChunkedInput<?> input = mock();
    setPendingBody(context, input);

    context.requestCompletedInternal(new IOException("response failed"));

    then(input).should().close();
    assertThat(pendingBody(context)).isNull();
  }

  @Test
  void resetReleasesPendingFileRegion() throws Exception {
    HttpContext context = createContext();
    DefaultFileRegion region = new DefaultFileRegion(file().toFile(), 0, 4);
    setPendingBody(context, region);

    context.reset();
    context.reset();

    assertThat(region.refCnt()).isZero();
    assertThat(pendingBody(context)).isNull();
  }

  @Test
  void closeFailureStillDiscardsPendingInput() throws Exception {
    HttpContext context = createContext();
    ChunkedInput<?> input = mock();
    doThrow(new IOException("close failed")).when(input).close();
    setPendingBody(context, input);

    context.reset();
    context.reset();

    then(input).should().close();
    assertThat(pendingBody(context)).isNull();
  }

  @Test
  void sendFileSetsContentLength() throws Exception {
    HttpContext context = createContext();
    context.asHttpOutputMessage().sendFile(file(), 0, 4);

    assertThat(context.responseHeaders().getContentLength()).isEqualTo(4);
    assertThat(pendingBody(context)).isNotNull();
    context.reset();
  }

  @Test
  void sendFileRejectsNegativeCount() throws Exception {
    HttpContext context = createContext();
    assertThatThrownBy(() -> context.asHttpOutputMessage().sendFile(file(), 0, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Count");

    assertThat(pendingBody(context)).isNull();
    assertThat(context.responseHeaders().contains(HttpHeaders.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void sendFileRejectsNegativePosition() throws Exception {
    HttpContext context = createContext();
    assertThatThrownBy(() -> context.asHttpOutputMessage().sendFile(file(), -1, 4))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Position");

    assertThat(pendingBody(context)).isNull();
    assertThat(context.responseHeaders().contains(HttpHeaders.CONTENT_LENGTH)).isFalse();
  }

  @Test
  void sendFileRejectsCountBeyondFileLength() throws Exception {
    HttpContext context = createContext();
    assertThatThrownBy(() -> context.asHttpOutputMessage().sendFile(file(), 0, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Count");
  }

  @Test
  void sendFileRejectsPositionBeyondFileLength() throws Exception {
    HttpContext context = createContext();
    assertThatThrownBy(() -> context.asHttpOutputMessage().sendFile(file(), 5, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Position");
  }

  @Test
  void sendFileRejectsNullFile() {
    HttpContext context = createContext();
    assertThatThrownBy(() -> context.asHttpOutputMessage().sendFile((File) null, 0, 1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void replacingFileReleasesPreviousFileRegion() throws Exception {
    HttpContext context = createContext();
    Path file = file();
    context.asHttpOutputMessage().sendFile(file, 0, 4);
    DefaultFileRegion first = (DefaultFileRegion) pendingBody(context);

    context.asHttpOutputMessage().sendFile(file, 0, 4);

    assertThat(first.refCnt()).isZero();
    assertThat(pendingBody(context)).isNotSameAs(first);
    context.reset();
  }

  @Test
  void replacingChunkedFileClosesPreviousFile() throws Exception {
    HttpContext context = spy(createContext());
    context.setTrailerFields(Trailer.field("Content-Digest", () -> "digest"));
    List<ChunkedNioFile> inputs = new ArrayList<>();
    doAnswer(invocation -> {
      ChunkedNioFile input = invocation.getArgument(0);
      inputs.add(input);
      return input;
    }).when(context).modifyChunkedFile(any());
    Path file = file();
    context.asHttpOutputMessage().sendFile(file, 0, 4);

    context.asHttpOutputMessage().sendFile(file, 0, 4);

    assertThat(inputs.get(0).isEndOfInput()).isTrue();
    assertThat(inputs.get(1).isEndOfInput()).isFalse();
    context.reset();
    assertThat(inputs.get(1).isEndOfInput()).isTrue();
  }

  @Test
  void lateTrailerRegistrationConvertsFileRegionToChunkedInput() throws Exception {
    HttpContext context = createContext();
    context.asHttpOutputMessage().sendFile(file(), 0, 4);
    DefaultFileRegion region = (DefaultFileRegion) pendingBody(context);
    context.setTrailerFields(Trailer.field("Content-Digest", () -> "digest"));

    Object prepared = context.prepareResponseFile(region);
    setPendingBody(context, prepared);

    assertThat(prepared).isInstanceOf(ChunkedNioFile.class);
    assertThat(region.refCnt()).isZero();
    context.reset();
    assertThat(((ChunkedNioFile) prepared).isEndOfInput()).isTrue();
  }

  @Test
  void lateTrailerAdaptationFailureReleasesRegionAndNewFile() throws Exception {
    HttpContext context = spy(createContext());
    context.asHttpOutputMessage().sendFile(file(), 0, 4);
    DefaultFileRegion region = (DefaultFileRegion) pendingBody(context);
    context.setTrailerFields(Trailer.field("Content-Digest", () -> "digest"));
    AtomicReference<ChunkedNioFile> opened = new AtomicReference<>();
    IllegalStateException failure = new IllegalStateException("adaptation failed");
    doAnswer(invocation -> {
      opened.set(invocation.getArgument(0));
      throw failure;
    }).when(context).modifyChunkedFile(any());
    setPendingBody(context, null);

    assertThatThrownBy(() -> context.prepareResponseFile(region)).isSameAs(failure);

    assertThat(region.refCnt()).isZero();
    assertThat(opened.get().isEndOfInput()).isTrue();
  }

  @Test
  void lateTrailerOpenFailureReleasesRegion() throws Exception {
    HttpContext context = createContext();
    Path file = file();
    context.asHttpOutputMessage().sendFile(file, 0, 4);
    DefaultFileRegion region = (DefaultFileRegion) pendingBody(context);
    context.setTrailerFields(Trailer.field("Content-Digest", () -> "digest"));
    Files.delete(file);
    setPendingBody(context, null);

    assertThatThrownBy(() -> context.prepareResponseFile(region))
            .isInstanceOf(UncheckedIOException.class);

    assertThat(region.refCnt()).isZero();
  }

  @Test
  void fileWithoutTrailersKeepsZeroCopyRegion() throws Exception {
    HttpContext context = createContext();
    context.asHttpOutputMessage().sendFile(file(), 0, 4);
    Object region = pendingBody(context);

    assertThat(context.prepareResponseFile(region)).isSameAs(region);
    context.reset();
  }

  @Test
  void adaptationFailureClosesNewFile() throws Exception {
    HttpContext context = spy(createContext());
    context.setTrailerFields(Trailer.field("Content-Digest", () -> "digest"));
    AtomicReference<ChunkedNioFile> opened = new AtomicReference<>();
    IllegalStateException failure = new IllegalStateException("adaptation failed");
    doAnswer(invocation -> {
      opened.set(invocation.getArgument(0));
      throw failure;
    }).when(context).modifyChunkedFile(any());
    Path file = file();

    assertThatThrownBy(() -> context.asHttpOutputMessage().sendFile(file, 0, 4))
            .isSameAs(failure);

    assertThat(opened.get().isEndOfInput()).isTrue();
    assertThat(pendingBody(context)).isNull();
  }

  private Path file() throws IOException {
    return Files.writeString(directory.resolve("response.txt"), "body");
  }

  private HttpContext createContext() {
    return createContext(channel);
  }

  private HttpContext createContext(Channel responseChannel) {
    NettyRequestConfig config = NettyRequestConfig.forBuilder(false)
            .multipartParser(mock()).sendErrorHandler(mock()).build();
    return new HttpContext(responseChannel, new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"),
            config, mock(ApplicationContext.class), mock(DispatcherHandler.class), mock(HttpTrafficHandler.class));
  }

  private static @Nullable Object pendingBody(NettyHttpContext context) throws Exception {
    Field field = NettyHttpContext.class.getDeclaredField("pendingBody");
    field.setAccessible(true);
    return field.get(context);
  }

  private static void setPendingBody(NettyHttpContext context, @Nullable Object body) throws Exception {
    Field field = NettyHttpContext.class.getDeclaredField("pendingBody");
    field.setAccessible(true);
    field.set(context, body);
  }

}
