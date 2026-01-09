/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.parsing;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import infra.jdbc.ParameterBinder;

/**
 * @author TODAY 2021/6/8 23:53
 * @since 4.0
 */
final class ListParameterIndexApplier extends ParameterIndexHolder {
  private final List<Integer> indices;

  ListParameterIndexApplier(List<Integer> index) {
    this.indices = index;
  }

  @Override
  public void bind(
          final ParameterBinder binder, final PreparedStatement statement) throws SQLException {
    for (final int index : indices) {
      binder.bind(statement, index);
    }
  }

  public void addIndex(int index) {
    indices.add(index);
  }

  //---------------------------------------------------------------------
  // Implementation of Object
  //---------------------------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof final ListParameterIndexApplier applier))
      return false;
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

  //---------------------------------------------------------------------
  // Implementation of Iterable interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<Integer> iterator() {
    return indices.iterator();
  }

}
