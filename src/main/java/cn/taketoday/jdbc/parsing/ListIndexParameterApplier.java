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
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

import cn.taketoday.jdbc.ParameterSetter;

/**
 * @author TODAY 2021/6/8 23:53
 */
final class ListIndexParameterApplier extends ParameterApplier {
  private final List<Integer> indices;

  ListIndexParameterApplier(List<Integer> index) {
    this.indices = index;
  }

  @Override
  public void apply(ParameterSetter parameterSetter, PreparedStatement statement) throws SQLException {
    for (final Integer integer : indices) {
      parameterSetter.setParameter(statement, integer);
    }
  }

  @Override
  public void forEach(IntConsumer action) {
    for (final Integer index : indices) {
      action.accept(index);
    }
  }

  public void addIndex(int index) {
    indices.add(index);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ListIndexParameterApplier)) return false;
    final ListIndexParameterApplier applier = (ListIndexParameterApplier) o;
    return Objects.equals(indices, applier.indices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indices);
  }

  @Override
  public String toString() {
    return "indices=" + indices;
  }
}
