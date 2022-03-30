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

package cn.taketoday.framework.test.util;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;

/**
 * Application context related test utilities.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public abstract class ApplicationContextTestUtils {

  /**
   * Closes this {@link ApplicationContext} and its parent hierarchy if any.
   *
   * @param context the context to close (can be {@code null})
   */
  public static void closeAll(ApplicationContext context) {
    if (context != null) {
      if (context instanceof ConfigurableApplicationContext) {
        context.close();
      }
      closeAll(context.getParent());
    }
  }

}
