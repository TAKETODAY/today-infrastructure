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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpDecoderConfig;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.util.AsciiString;

/**
 * Netty channel initializer for HTTP/HTTP2 support
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-07-02 21:34
 */
sealed class HttpChannelInitializer extends ChannelInitializer<Channel> implements ChannelHandler permits SecuredHttpChannelInitializer {

  protected static final String HttpCodec = "HttpServerCodec";
  protected static final String HttpTrafficHandler = "HttpTrafficHandler";
  protected static final String H2CUpgradeHandler = "H2CUpgradeHandler";
  protected static final String H2ToHttp11Codec = "H2ToHttp11Codec";
  protected static final String H2MultiplexHandler = "H2MultiplexHandler";

  private static final Http2StreamFrameToHttpObjectCodec HTTP2_STREAM_FRAME_TO_HTTP_OBJECT =
          new Http2StreamFrameToHttpObjectCodec(true) {

            @Override
            public boolean isSharable() {
              return true;
            }
          };

  private final ChannelHandler httpTrafficHandler;

  private final @Nullable ChannelConfigurer channelConfigurer;

  /**
   * A configuration object for specifying the behavior
   * of {@link HttpObjectDecoder} and its subclasses.
   */
  private final HttpDecoderConfig httpDecoderConfig;

  protected final boolean http2Enabled;

  protected HttpChannelInitializer(ChannelHandler httpTrafficHandler, boolean http2Enabled,
          @Nullable ChannelConfigurer channelConfigurer, HttpDecoderConfig httpDecoderConfig) {
    this.httpTrafficHandler = httpTrafficHandler;
    this.http2Enabled = http2Enabled;
    this.channelConfigurer = channelConfigurer;
    this.httpDecoderConfig = httpDecoderConfig;
  }

  @Override
  protected final void initChannel(final Channel ch) {
    ChannelConfigurer configurer = channelConfigurer;
    if (configurer != null) {
      configurer.initChannel(ch);
    }
    if (http2Enabled) {
      configureHttp11OrH2Channel(ch);
    }
    else {
      configureHttp11Channel(ch);
    }
    if (configurer != null) {
      configurer.postInitChannel(ch);
    }
  }

  protected void configureHttp11Channel(Channel ch) {
    ch.pipeline()
            .addLast(HttpCodec, new HttpServerCodec(httpDecoderConfig))
            .addLast(HttpTrafficHandler, httpTrafficHandler);
  }

  protected void configureHttp11OrH2Channel(Channel channel) {
    HttpServerCodec httpServerCodec = new HttpServerCodec(httpDecoderConfig);
    Http11OrH2CleartextCodec upgrader = new Http11OrH2CleartextCodec(createHttp2FrameCodec());
    ChannelHandler http2ServerHandler = new H2CleartextCodec(upgrader, true);

    CleartextHttp2ServerUpgradeHandler h2cUpgradeHandler = new CleartextHttp2ServerUpgradeHandler(
            httpServerCodec, new HttpServerUpgradeHandler(httpServerCodec, upgrader), http2ServerHandler);

    channel.pipeline()
            .addLast(H2CUpgradeHandler, h2cUpgradeHandler)
            .addLast(HttpTrafficHandler, httpTrafficHandler);
  }

  protected final Http2FrameCodec createHttp2FrameCodec() {
    Http2FrameCodecBuilder codecBuilder = Http2FrameCodecBuilder.forServer();
    return codecBuilder.build();
  }

  protected final void addH2StreamHandlers(Channel ch) {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(H2ToHttp11Codec, HTTP2_STREAM_FRAME_TO_HTTP_OBJECT)
            .addLast(HttpTrafficHandler, httpTrafficHandler);
  }

  @Override
  public final boolean isSharable() {
    return true;
  }

  final class Http11OrH2CleartextCodec extends ChannelInitializer<Channel> implements HttpServerUpgradeHandler.UpgradeCodecFactory {

    final Http2FrameCodec http2FrameCodec;

    Http11OrH2CleartextCodec(Http2FrameCodec http2FrameCodec) {
      this.http2FrameCodec = http2FrameCodec;
    }

    @Override
    protected void initChannel(Channel ch) {
      addH2StreamHandlers(ch);
    }

    @Override
    public HttpServerUpgradeHandler.@Nullable UpgradeCodec newUpgradeCodec(CharSequence protocol) {
      if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
        return new Http2ServerUpgradeCodec(http2FrameCodec, new H2CleartextCodec(this, false));
      }
      return null;
    }
  }

  private static final class H2CleartextCodec extends ChannelHandlerAdapter {

    private final Http11OrH2CleartextCodec upgrader;

    private final boolean addHttp2FrameCodec;

    H2CleartextCodec(Http11OrH2CleartextCodec upgrader, boolean addHttp2FrameCodec) {
      this.upgrader = upgrader;
      this.addHttp2FrameCodec = addHttp2FrameCodec;
    }

    @Override
    public boolean isSharable() {
      return false;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
      ChannelPipeline pipeline = ctx.pipeline();

      if (addHttp2FrameCodec) {
        pipeline.addAfter(ctx.name(), HttpCodec, upgrader.http2FrameCodec);
      }

      // Add this handler at the end of the pipeline as it does not forward all channelRead events
      pipeline.addLast(H2MultiplexHandler, new Http2MultiplexHandler(upgrader))
              .remove(this)
              .remove(HttpTrafficHandler);
    }
  }

}
