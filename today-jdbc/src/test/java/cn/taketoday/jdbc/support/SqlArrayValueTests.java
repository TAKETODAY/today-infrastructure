/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.support;

import org.junit.jupiter.api.Test;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/15 20:38
 */
class SqlArrayValueTests {

  private final Connection con = mock();

  private final PreparedStatement ps = mock();

  private final Array arr = mock();

  private final Object[] elements = new Object[] { 1, 2, 3 };

  private final SqlArrayValue sqlArrayValue = new SqlArrayValue("smallint", elements);

  @Test
  public void setValue() throws SQLException {
    given(this.ps.getConnection()).willReturn(this.con);
    given(this.con.createArrayOf("smallint", elements)).willReturn(this.arr);

    int paramIndex = 42;
    this.sqlArrayValue.setValue(this.ps, paramIndex);
    verify(ps).setArray(paramIndex, arr);

    this.sqlArrayValue.cleanup();
    verify(this.arr).free();
  }

}