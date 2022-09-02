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

package cn.taketoday.ui.context.support;

import cn.taketoday.context.MessageSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.ui.context.Theme;

/**
 * Default {@link Theme} implementation, wrapping a name and an
 * underlying {@link MessageSource}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleTheme implements Theme {

  private final String name;

  private final MessageSource messageSource;

  /**
   * Create a SimpleTheme.
   *
   * @param name the name of the theme
   * @param messageSource the MessageSource that resolves theme messages
   */
  public SimpleTheme(String name, MessageSource messageSource) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(messageSource, "MessageSource must not be null");
    this.name = name;
    this.messageSource = messageSource;
  }

  @Override
  public final String getName() {
    return this.name;
  }

  @Override
  public final MessageSource getMessageSource() {
    return this.messageSource;
  }

}
