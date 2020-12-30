/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.jdbc.mapping.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.mapping.ColumnMapping;

/**
 * @author TODAY <br>
 *         2019-08-24 23:05
 */
public final class DelegatingResultResolver implements ResultResolver {

  private final Function supports;
  private final ResultResolver resolver;

  public static DelegatingResultResolver delegate(Function supports, ResultResolver resolver) {
    return new DelegatingResultResolver(supports, resolver);
  }

  public DelegatingResultResolver(Function supports, ResultResolver resolver) {
    this.supports = supports;
    this.resolver = resolver;
  }

  @Override
  public boolean supports(ColumnMapping property) {
    return supports.apply(property);
  }

  @Override
  public Object resolveResult(final ResultSet resultSet, final String index) throws SQLException {
    return resolver.resolveResult(resultSet, index);
  }

}
