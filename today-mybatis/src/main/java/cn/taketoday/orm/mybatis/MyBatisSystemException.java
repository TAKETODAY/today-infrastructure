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

package cn.taketoday.orm.mybatis;

import java.io.Serial;

import cn.taketoday.dao.UncategorizedDataAccessException;

/**
 * MyBatis specific subclass of {@code UncategorizedDataAccessException}, for MyBatis system errors that do not match
 * any concrete {@code cn.taketoday.dao} exceptions.
 *
 * In MyBatis 3 {@code org.apache.ibatis.exceptions.PersistenceException} is a {@code RuntimeException}, but using this
 * wrapper class to bring everything under a single hierarchy will be easier for client code to handle.
 *
 * @author Hunter Presnall
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("squid:MaximumInheritanceDepth") // It is the intended design
public class MyBatisSystemException extends UncategorizedDataAccessException {

  @Serial
  private static final long serialVersionUID = 1L;

  public MyBatisSystemException(Throwable cause) {
    super(null, cause);
  }

}
