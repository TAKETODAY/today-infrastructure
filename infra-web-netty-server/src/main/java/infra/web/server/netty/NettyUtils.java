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

import infra.lang.TodayStrategies;
import io.netty.channel.Channel;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/24 18:59
 */
abstract class NettyUtils {

  /**
   * Specifies whether the channel ID will be prepended to the log message when possible.
   * By default, it will be prepended.
   */
  static final boolean LOG_CHANNEL_INFO = TodayStrategies.getFlag("netty.logChannelInfo", true);

  static final char CHANNEL_ID_PREFIX = '[';
  static final char CHANNEL_ID_SUFFIX_2 = ' ';
  static final String ORIGINAL_CHANNEL_ID_PREFIX = "[id: 0x";
  static final int ORIGINAL_CHANNEL_ID_PREFIX_LENGTH = ORIGINAL_CHANNEL_ID_PREFIX.length();

  /**
   * Append channel ID to a log message for correlated traces.
   *
   * @param channel current channel associated with the msg
   * @param msg the log msg
   * @return a formatted msg
   */
  public static String format(Channel channel, String msg) {
    if (LOG_CHANNEL_INFO) {
      StringBuilder result;
      String channelStr = channel.toString();
      if (channelStr.charAt(0) == CHANNEL_ID_PREFIX) {
        channelStr = channelStr.substring(ORIGINAL_CHANNEL_ID_PREFIX_LENGTH);
        result = new StringBuilder(1 + channelStr.length() + 1 + msg.length())
                .append(CHANNEL_ID_PREFIX)
                .append(channelStr);
      }
      else {
        int ind = channelStr.indexOf(ORIGINAL_CHANNEL_ID_PREFIX);
        result = new StringBuilder(1 + (channelStr.length() - ORIGINAL_CHANNEL_ID_PREFIX_LENGTH) + 1 + msg.length())
                .append(channelStr, 0, ind)
                .append(CHANNEL_ID_PREFIX)
                .append(channelStr.substring(ind + ORIGINAL_CHANNEL_ID_PREFIX_LENGTH));
      }
      return result.append(CHANNEL_ID_SUFFIX_2)
              .append(msg)
              .toString();
    }
    else {
      return msg;
    }
  }

}
