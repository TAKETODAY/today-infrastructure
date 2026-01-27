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
