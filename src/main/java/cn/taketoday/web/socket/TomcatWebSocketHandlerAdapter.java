/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket;

import java.io.IOException;

import javax.websocket.MessageHandler;
import javax.websocket.Session;

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/4/5 14:20
 * @since 3.0
 */
public class TomcatWebSocketHandlerAdapter extends AbstractWebSocketHandlerAdapter {

  @Override
  protected WebSocketSession createSession(RequestContext context, WebSocketHandler handler) {
    return new DefaultWebSocketSession(context, handler);
  }

  @Override
  protected void upgrade(
          RequestContext context, WebSocketSession ses, WebSocketHandler handler) throws HandshakeFailedException {
    super.upgrade(context, ses, handler);

    DefaultWebSocketSession session = (DefaultWebSocketSession) ses;
    final Session nativeSession = session.obtainNativeSession();

//    final WebSocketContainer container = nativeSession.getContainer();
//    final WsServerContainer webSocketContainer = (WsServerContainer) ContainerProvider.getWebSocketContainer();

    if (handler.supportPartialMessage()) {
      nativeSession.addMessageHandler(new MessageHandler.Partial<String>() {

        @Override
        public void onMessage(String partialMessage, boolean last) {
          try {
            session.sendText(partialMessage, last);
          }
          catch (IOException e) {

          }
        }
      });
    }
    else {

    }

  }
}
