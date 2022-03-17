/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.socket.tomcat;

import org.apache.coyote.http11.upgrade.UpgradeInfo;
import org.apache.tomcat.util.net.SocketWrapperBase;
import org.apache.tomcat.websocket.Transformation;
import org.apache.tomcat.websocket.WsSession;
import org.apache.tomcat.websocket.server.WsFrameServer;

/**
 * @author TODAY 2021/5/5 22:12
 * @since 3.0.1
 */
final class TomcatFrameServer extends WsFrameServer {

  public TomcatFrameServer(SocketWrapperBase<?> socketWrapper, UpgradeInfo upgradeInfo, WsSession wsSession,
                           Transformation transformation, ClassLoader applicationClassLoader) {
    super(socketWrapper, upgradeInfo, wsSession, transformation, applicationClassLoader);
  }

  @Override
  protected Transformation getTransformation() {
    return super.getTransformation();
  }

  @Override
  protected boolean isOpen() {
    return super.isOpen();
  }
}
