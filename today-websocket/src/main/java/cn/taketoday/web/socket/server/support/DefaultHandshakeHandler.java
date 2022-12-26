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

package cn.taketoday.web.socket.server.support;

import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.socket.server.RequestUpgradeStrategy;
import jakarta.servlet.ServletContext;

/**
 * A default {@link cn.taketoday.web.socket.server.HandshakeHandler} implementation,
 * extending {@link AbstractHandshakeHandler} with Servlet-specific initialization support.
 * See {@link AbstractHandshakeHandler}'s javadoc for details on supported servers etc.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultHandshakeHandler extends AbstractHandshakeHandler implements ServletContextAware {

  public DefaultHandshakeHandler() { }

  public DefaultHandshakeHandler(RequestUpgradeStrategy requestUpgradeStrategy) {
    super(requestUpgradeStrategy);
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    RequestUpgradeStrategy strategy = getRequestUpgradeStrategy();
    if (strategy instanceof ServletContextAware) {
      ((ServletContextAware) strategy).setServletContext(servletContext);
    }
  }

}
