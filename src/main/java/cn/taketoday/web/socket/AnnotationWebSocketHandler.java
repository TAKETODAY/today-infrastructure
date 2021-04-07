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

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/4/5 12:29
 * @since 3.0
 */
public class AnnotationWebSocketHandler implements WebSocketHandler {

  @Override
  public void handshake(RequestContext context) {

  }

  @Override
  public void onOpen(WebSocketSession session) {

  }

  @Override
  public void handleMessage(WebSocketSession session, Message message) {

  }

  @Override
  public void onClose(WebSocketSession session) {

  }

  @Override
  public void onError(WebSocketSession session) {

  }

  @Override
  public boolean supportPartialMessage() {
    return false;
  }
}
