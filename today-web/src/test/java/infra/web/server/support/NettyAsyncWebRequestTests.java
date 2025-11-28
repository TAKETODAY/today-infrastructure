/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.server.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/11 13:14
 */
class NettyAsyncWebRequestTests {

  @Test
  void constructorShouldInitializeFields() {
    NettyRequestContext request = mock(NettyRequestContext.class);
    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);

    assertThat(asyncWebRequest.isAsyncStarted()).isFalse();
  }

  @Test
  void startAsyncShouldSetAsyncStartedFlag() {
    NettyRequestContext request = mock(NettyRequestContext.class);
    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);

    asyncWebRequest.startAsync();

    assertThat(asyncWebRequest.isAsyncStarted()).isTrue();
  }

//  @Test
//  void startAsyncWithTimeoutShouldScheduleTimeoutTask() {
//    Channel channel = mock(Channel.class);
//    NettyRequestContext request = new NettyRequestContext(mock(), channel, mock(), NettyRequestConfig.forBuilder(false)
//            .httpDataFactory(new DefaultHttpDataFactory())
//            .sendErrorHandler((request1, message) -> System.out.println(message))
//            .build(), mock());
//
//    EventLoop eventLoop = mock(EventLoop.class);
//    ScheduledFuture scheduledFuture = mock(ScheduledFuture.class);
//
//    when(channel.eventLoop()).thenReturn(eventLoop);
//    when(eventLoop.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(scheduledFuture);
//
//    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);
//    asyncWebRequest.setTimeout(5000L);
//    asyncWebRequest.startAsync();
//
//    verify(eventLoop).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
//  }

//  @Test
//  void dispatchShouldSetAsyncStartedToFalseAndMarkCompleted() {
//    Channel channel = mock(Channel.class);
//    NettyRequestContext request = new NettyRequestContext(mock(), channel, mock(), NettyRequestConfig.forBuilder(false)
//            .httpDataFactory(new DefaultHttpDataFactory())
//            .sendErrorHandler((request1, message) -> System.out.println(message))
//            .build(), mock());
//
//    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);
//    asyncWebRequest.startAsync();
//
//    Object result = new Object();
//    asyncWebRequest.dispatch(result);
//
//    assertThat(asyncWebRequest.isAsyncStarted()).isFalse();
//    assertThat(asyncWebRequest.isAsyncComplete()).isTrue();
//  }

//  @Test
//  void dispatchShouldCancelTimeoutFutureWhenExists() {
//    Channel channel = mock(Channel.class);
//    DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
//    NettyRequestContext request = new NettyRequestContext(mock(), channel, httpRequest, NettyRequestConfig.forBuilder(false)
//            .httpDataFactory(new DefaultHttpDataFactory())
//            .sendErrorHandler((request1, message) -> System.out.println(message))
//            .build(), new DispatcherHandler());
//
//    EventLoop eventLoop = mock(EventLoop.class);
//    ScheduledFuture scheduledFuture = mock(ScheduledFuture.class);
//
//    when(channel.eventLoop()).thenReturn(eventLoop);
//    when(eventLoop.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(scheduledFuture);
//
//    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);
//    asyncWebRequest.setTimeout(5000L);
//    asyncWebRequest.startAsync();
//
//    Object result = new Object();
//    asyncWebRequest.dispatch(result);
//
//    verify(scheduledFuture).cancel(true);
//  }

//  @Test
//  void dispatchShouldCallRequestDispatchConcurrentResult() throws Throwable {
//    Channel channel = mock(Channel.class);
//    DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
//    NettyRequestContext request = new NettyRequestContext(mock(), channel, httpRequest, NettyRequestConfig.forBuilder(false)
//            .httpDataFactory(new DefaultHttpDataFactory())
//            .sendErrorHandler((request1, message) -> System.out.println(message))
//            .build(), new DispatcherHandler());
//
//    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);
//    asyncWebRequest.startAsync();
//
//    Object result = new Object();
//    asyncWebRequest.dispatch(result);
//
//    verify(channel).writeAndFlush(any());
//  }

  @Test
  void isAsyncStartedShouldReturnFalseByDefault() {
    NettyRequestContext request = mock(NettyRequestContext.class);
    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);

    assertThat(asyncWebRequest.isAsyncStarted()).isFalse();
  }

//  @Test
//  void checkTimeoutShouldDispatchTimeoutHandlersWhenNotCompleted() {
//    NettyRequestContext request = mock(NettyRequestContext.class);
//    NettyAsyncWebRequest asyncWebRequest = new NettyAsyncWebRequest(request);
//
//    Runnable timeoutHandler = mock(Runnable.class);
//    asyncWebRequest.addTimeoutHandler(timeoutHandler);
//
//    asyncWebRequest.checkTimeout();
//
//    verify(timeoutHandler).run();
//  }

}