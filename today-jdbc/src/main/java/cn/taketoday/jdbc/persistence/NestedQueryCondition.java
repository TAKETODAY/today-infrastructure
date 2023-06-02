/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/11 21:49
 */
public class NestedQueryCondition extends QueryCondition {

  private final QueryCondition chain;

  public NestedQueryCondition(QueryCondition chain) {
    this.chain = chain;
  }

  @Override
  protected boolean matches() {
    return chain != null;
  }

  @Override
  protected int setParameter(PreparedStatement ps, int idx) throws SQLException {
    int nextIdx = chain.setParameter(ps, idx);
    if (nextNode != null) {
      nextIdx = nextNode.setParameter(ps, nextIdx);
    }
    return nextIdx;
  }

  @Override
  protected int setParameterInternal(PreparedStatement ps, int idx) throws SQLException {
    int nextIdx = chain.setParameterInternal(ps, idx);
    if (nextNode != null) {
      nextIdx = nextNode.setParameterInternal(ps, nextIdx);
    }
    return nextIdx;
  }

  @Override
  protected void renderInternal(StringBuilder sql) {
    sql.append(' ');
    sql.append('(');

    chain.render(sql);

    sql.append(' ');
    sql.append(')');
  }

}
