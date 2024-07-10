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

package cn.taketoday.web.server.support;

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
  void initChannel(Channel ch);
  
}
