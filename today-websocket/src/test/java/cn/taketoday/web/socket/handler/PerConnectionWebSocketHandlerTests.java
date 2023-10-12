/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.socket.handler;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/12 11:40
 */
class PerConnectionWebSocketHandlerTests {

  @Test
  void onOpen() throws Exception {

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