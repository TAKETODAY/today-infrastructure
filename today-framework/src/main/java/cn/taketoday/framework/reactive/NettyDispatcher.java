/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.reactive;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.handler.DispatcherHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * default implementation is Synchronous Netty
 * {@link cn.taketoday.web.handler.DispatcherHandler}
 * like {@link cn.taketoday.web.servlet.DispatcherServlet}
 *
 * @author TODAY 2021/3/20 12:05
 * @see AsyncNettyDispatcherHandler
 * @see cn.taketoday.web.handler.DispatcherHandler
 * @see cn.taketoday.web.servlet.DispatcherServlet
 */
public class NettyDispatcher {
  protected final DispatcherHandler dispatcherHandler;

  public NettyDispatcher(DispatcherHandler dispatcherHandler) {
    Assert.notNull(dispatcherHandler, "DispatcherHandler must not be null");
    this.dispatcherHandler = dispatcherHandler;
  }

  /**
   * dispatch request in netty
   * <p>
   * default is using Synchronous
   * </p>
   *
   * @param ctx netty channel handler context
   * @param nettyContext netty request context
   */
  public void dispatch(final ChannelHandlerContext ctx, final NettyRequestContext nettyContext) throws Throwable {
    RequestContextHolder.set(nettyContext);
    try {
      dispatcherHandler.dispatch(nettyContext); // handling HTTP request
      nettyContext.sendIfNotCommitted();
    }
    finally {
      RequestContextHolder.remove();
    }
  }
}
