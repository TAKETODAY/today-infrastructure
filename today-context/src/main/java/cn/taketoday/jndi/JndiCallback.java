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

package cn.taketoday.jndi;

import javax.naming.Context;
import javax.naming.NamingException;

import cn.taketoday.lang.Nullable;

/**
 * Callback interface to be implemented by classes that need to perform an
 * operation (such as a lookup) in a JNDI context. This callback approach
 * is valuable in simplifying error handling, which is performed by the
 * JndiTemplate class. This is a similar to JdbcTemplate's approach.
 *
 * <p>Note that there is hardly any need to implement this callback
 * interface, as JndiTemplate provides all usual JNDI operations via
 * convenience methods.
 *
 * @param <T> the resulting object type
 * @author Rod Johnson
 * @see JndiTemplate
 * @see cn.taketoday.jdbc.core.JdbcTemplate
 */
@FunctionalInterface
public interface JndiCallback<T> {

  /**
   * Do something with the given JNDI context.
   * <p>Implementations don't need to worry about error handling
   * or cleanup, as the JndiTemplate class will handle this.
   *
   * @param ctx the current JNDI context
   * @return a result object, or {@code null}
   * @throws NamingException if thrown by JNDI methods
   */
  @Nullable
  T doInContext(Context ctx) throws NamingException;

}

