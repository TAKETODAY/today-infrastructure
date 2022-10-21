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

package cn.taketoday.framework.web.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.web.context.async.AsyncWebRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 13:47
 */
public class NettyAsyncWebRequest implements AsyncWebRequest {
  private Long timeout;

  private final AtomicBoolean asyncCompleted = new AtomicBoolean();

  private final List<Runnable> timeoutHandlers = new ArrayList<>();

  private final List<Consumer<Throwable>> exceptionHandlers = new ArrayList<>();

  private final List<Runnable> completionHandlers = new ArrayList<>();

  private final NettyRequestContext request;
  private final ChannelHandlerContext channelContext;

  public NettyAsyncWebRequest(NettyRequestContext request) {
    this.request = request;
    this.channelContext = request.getChannelContext();
  }

  /**
   * In async processing, the timeout period begins after the
   * container processing thread has exited.
   */
  @Override
  public void setTimeout(Long timeout) {
    Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
    this.timeout = timeout;
  }

  @Override
  public void addTimeoutHandler(Runnable timeoutHandler) {
    this.timeoutHandlers.add(timeoutHandler);
  }

  @Override
  public void addErrorHandler(Consumer<Throwable> exceptionHandler) {
    this.exceptionHandlers.add(exceptionHandler);
  }

  @Override
  public void addCompletionHandler(Runnable runnable) {
    this.completionHandlers.add(runnable);
  }

  @Override
  public void startAsync() {
    EventExecutor executor = channelContext.executor();

    Promise<Object> objectPromise = executor.newPromise();

    objectPromise.addListener(future -> {

    });

    executor.execute(new Runnable() {
      @Override
      public void run() {

      }
    });

  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public void dispatch() {
    request.requestCompleted();
  }

  @Override
  public boolean isAsyncComplete() {
    return false;
  }
}
