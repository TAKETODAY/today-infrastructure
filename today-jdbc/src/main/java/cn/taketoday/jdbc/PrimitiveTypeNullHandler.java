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

package cn.taketoday.jdbc;

import java.sql.ResultSet;

import cn.taketoday.beans.BeanProperty;

/**
 * use this handler when {@link ObjectPropertySetter}
 * handle {@link ObjectPropertySetter#setTo(Object, ResultSet, int)}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/7/30 09:24
 */
public interface PrimitiveTypeNullHandler {

  /**
   * handle null when {@link ObjectPropertySetter} {@code property} is {@code null}
   * and {@code property} is primitive-type
   */
  void handleNull(BeanProperty property, Object obj);
}
