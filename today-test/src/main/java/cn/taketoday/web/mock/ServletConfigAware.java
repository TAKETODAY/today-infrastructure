/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.mock;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.mock.api.MockConfig;

/**
 * Interface to be implemented by any object that wishes to be notified of the
 * {@link MockConfig} (typically determined by the {@link WebApplicationContext})
 * that it runs in.
 *
 * <p>Note: Only satisfied if actually running within a Servlet-specific
 * WebApplicationContext. Otherwise, no ServletConfig will be set.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockContextAware
 * @since 4.0 2022/2/20 20:59
 */
public interface ServletConfigAware extends Aware {

  /**
   * Set the {@link MockConfig} that this object runs in.
   * <p>Invoked after population of normal bean properties but before an init
   * callback like InitializingBean's {@code afterPropertiesSet} or a
   * custom init-method. Invoked after ApplicationContextAware's
   * {@code setApplicationContext}.
   *
   * @param mockConfig the {@link MockConfig} to be used by this object
   * @see InitializingBean#afterPropertiesSet
   * @see ApplicationContextAware#setApplicationContext
   */
  void setServletConfig(MockConfig mockConfig);

}

