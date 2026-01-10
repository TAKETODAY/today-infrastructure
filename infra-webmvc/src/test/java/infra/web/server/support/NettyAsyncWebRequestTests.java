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