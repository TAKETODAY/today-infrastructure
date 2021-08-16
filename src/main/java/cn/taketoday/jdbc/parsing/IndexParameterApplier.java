/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
import java.util.Objects;
import java.util.function.IntConsumer;

import cn.taketoday.jdbc.ParameterSetter;

/**
 * @author TODAY 2021/6/8 23:52
 */
final class IndexParameterApplier extends ParameterApplier {
  final int index;

  IndexParameterApplier(int index) {
    this.index = index;
  }

  @Override
  public void apply(ParameterSetter parameterSetter, PreparedStatement statement) throws SQLException {
    parameterSetter.setParameter(statement, index);
  }

  @Override
  public void forEach(IntConsumer action) {
    action.accept(index);
  }

  public int getIndex() {
    return index;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IndexParameterApplier)) return false;
    final IndexParameterApplier that = (IndexParameterApplier) o;
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
}
