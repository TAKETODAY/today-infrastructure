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

package cn.taketoday.jdbc.parsing;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Objects;

import cn.taketoday.jdbc.ParameterBinder;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2021/6/8 23:52
 * @since 4.0
 */
final class DefaultParameterIndexHolder extends ParameterIndexHolder {
  final int index;

  DefaultParameterIndexHolder(int index) {
    this.index = index;
  }

  @Override
  public void bind(ParameterBinder binder, PreparedStatement statement) throws SQLException {
    binder.bind(statement, index);
  }

  public int getIndex() {
    return index;
  }

  //---------------------------------------------------------------------
  // Implementation of Object
  //---------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final DefaultParameterIndexHolder that))
      return false;
    return index == that.index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(index);
  }

  @Override
  public String toString() {
    return "index=" + index;
  }

  //---------------------------------------------------------------------
  // Implementation of Iterable interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<Integer> iterator() {
    return CollectionUtils.singletonIterator(index);
  }

}
