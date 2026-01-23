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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * Configure netty channel when {@link ChannelInitializer#initChannel(Channel)}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/6/5 17:27
 */
public interface ChannelConfigurer {

  /**
   * Configure netty channel when {@link ChannelInitializer#initChannel(Channel)}
   */
  default void initChannel(Channel ch) {
  }

  /**
   * post init
   *
   * @param ch channel
   */
  default void postInitChannel(Channel ch) {
  }

}
