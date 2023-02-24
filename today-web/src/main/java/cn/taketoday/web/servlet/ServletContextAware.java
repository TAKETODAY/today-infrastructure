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
package cn.taketoday.web.servlet;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.context.ApplicationContextAware;
import jakarta.servlet.ServletContext;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link ServletContext} (typically determined by the {@link WebApplicationContext})
 * that it runs in.
 *
 * @author Harry Yang
 * @since 2018-08-03 15:45
 */
public interface ServletContextAware extends Aware {

  /**
   * Set the {@link ServletContext} that this object runs in.
   * <p>Invoked after population of normal bean properties but before an init
   * callback like InitializingBean's {@code afterPropertiesSet} or a
   * custom init-method. Invoked after ApplicationContextAware's
   * {@code setApplicationContext}.
   *
   * @param servletContext the ServletContext object to be used by this object
   * @see cn.taketoday.beans.factory.InitializingBean#afterPropertiesSet
   * @see ApplicationContextAware#setApplicationContext
   */
  void setServletContext(ServletContext servletContext);
}
