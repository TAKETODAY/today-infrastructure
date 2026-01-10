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

import infra.beans.factory.DisposableBean;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;

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
