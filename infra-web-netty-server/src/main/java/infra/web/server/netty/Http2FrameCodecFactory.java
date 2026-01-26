package infra.web.server.netty;

import io.netty.handler.codec.http2.Http2FrameCodec;

/**
 * Factory interface for creating {@link Http2FrameCodec} instances.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/26 22:40
 */
public interface Http2FrameCodecFactory {

  /**
   * Creates a new instance of {@link Http2FrameCodec}.
   *
   * @return a new {@link Http2FrameCodec} instance
   */
  Http2FrameCodec create();
}
