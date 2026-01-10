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
import java.util.Objects;

import infra.jdbc.ParameterBinder;
import infra.util.CollectionUtils;

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
