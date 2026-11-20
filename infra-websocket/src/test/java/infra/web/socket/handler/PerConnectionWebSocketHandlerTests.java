/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.socket.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.util.concurrent.Future;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/12 11:40
 */
class PerConnectionWebSocketHandlerTests {

  @Test
  void onOpen() throws Throwable {

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.refresh();

    EchoHandler.reset();
    PerConnectionWebSocketHandler handler = new PerConnectionWebSocketHandler(EchoHandler.class);
    handler.setBeanFactory(context.getBeanFactory());

    WebSocketSession session = new TestWebSocketSession();
    handler.onOpen(session);

    assertThat(EchoHandler.initCount).isEqualTo(1);
    assertThat(EchoHandler.destroyCount).isEqualTo(0);

    handler.onClose(session, CloseStatus.NORMAL);

    assertThat(EchoHandler.initCount).isEqualTo(1);
    assertThat(EchoHandler.destroyCount).isEqualTo(1);
  }

  @Test
  void createsSeparateHandlersWithoutAnAutowireCapableFactory() throws Exception {
    var provider = new BeanCreatingHandlerProvider<>(PlainHandler.class);
    provider.setBeanFactory(mock(BeanFactory.class));

    PlainHandler first = provider.getHandler();
    assertThat(provider.getHandler()).isNotSameAs(first);
    assertThat(provider.getHandlerType()).isEqualTo(PlainHandler.class);
    assertThat(provider.toString()).contains(PlainHandler.class.getName());
    provider.destroy(first);

    var handler = new PerConnectionWebSocketHandler(PlainHandler.class);
    var session = new TestWebSocketSession();
    handler.onOpen(session);
    assertThat(handler.toString()).contains(PlainHandler.class.getName());
    handler.onClose(session, CloseStatus.NORMAL);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void removesHandlerEvenIfDestroyThrows(boolean warn) throws Exception {
    var factory = mock(AutowireCapableBeanFactory.class);
    var delegate = mock(PlainHandler.class);
    var handler = new PerConnectionWebSocketHandler(PlainHandler.class);
    var session = new TestWebSocketSession();
    var message = WebSocketMessage.ping();
    var failure = new IllegalStateException("transport failure");
    Future<Void> complete = Future.ok();
    given(factory.createBean(PlainHandler.class)).willReturn(delegate);
    given(delegate.handleMessage(session, message)).willReturn(complete);
    given(delegate.onClose(session, CloseStatus.NORMAL)).willReturn(complete);
    willThrow(new IllegalStateException("destroy failed")).given(factory).destroyBean(delegate);
    handler.setBeanFactory(factory);
    handler.onOpen(session);
    assertThat(handler.handleMessage(session, message)).isSameAs(complete);
    handler.onError(session, failure);
    verify(delegate).onError(session, failure);

    Logger logger = (Logger) LoggerFactory.getLogger(PerConnectionWebSocketHandler.class);
    Level previous = logger.getLevel();
    logger.setLevel(warn ? Level.WARN : Level.OFF);
    try {
      assertThat(handler.onClose(session, CloseStatus.NORMAL)).isSameAs(complete);
      assertThatIllegalStateException().isThrownBy(() -> handler.onClose(session, CloseStatus.NORMAL))
              .withMessageContaining("WebSocketHandler not found");
      verify(factory).destroyBean(delegate);
    }
    finally {
      logger.setLevel(previous);
    }
  }

  static class PlainHandler extends WebSocketHandler {
  }

  public static class EchoHandler extends WebSocketHandler implements DisposableBean {

    private static int initCount;

    private static int destroyCount;

    public EchoHandler() {
      initCount++;
    }

    @Override
    public void destroy() throws Exception {
      destroyCount++;
    }

    public static void reset() {
      initCount = 0;
      destroyCount = 0;
    }
  }

}
