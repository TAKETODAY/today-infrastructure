/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.jdbc.sql;

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
  protected void setParameter(PreparedStatement ps) throws SQLException {
    chain.setParameter(ps);

    if (nextNode != null) {
      nextNode.setParameter(ps);
    }
  }

  @Override
  protected void setNextNodePosition(QueryCondition next) {
    int basePosition = chain.getLastPosition();

    if (next instanceof NestedQueryCondition nested) {
      // 批量更新 position
      nested.position = basePosition + 1;
      nested.chain.updatePosition(basePosition);
    }
    else {
      next.updatePosition(basePosition);
//      next.position = lastPosition + 1;
//      chain.position = lastPosition;
    }
  }

  @Override
  protected void setParameterInternal(PreparedStatement ps) throws SQLException {
    chain.setParameterInternal(ps);
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
