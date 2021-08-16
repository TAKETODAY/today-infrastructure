/**
 * Copyright 2009-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.jdbc.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Paul Krause
 */
public class BigIntegerTypeHandler extends BaseTypeHandler<BigInteger> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, BigInteger parameter) throws SQLException {
    ps.setBigDecimal(i, new BigDecimal(parameter));
  }

  @Override
  public BigInteger getResult(ResultSet rs, String columnName) throws SQLException {
    BigDecimal bigDecimal = rs.getBigDecimal(columnName);
    return bigDecimal == null ? null : bigDecimal.toBigInteger();
  }

  @Override
  public BigInteger getResult(ResultSet rs, int columnIndex) throws SQLException {
    BigDecimal bigDecimal = rs.getBigDecimal(columnIndex);
    return bigDecimal == null ? null : bigDecimal.toBigInteger();
  }

  @Override
  public BigInteger getResult(CallableStatement cs, int columnIndex) throws SQLException {
    BigDecimal bigDecimal = cs.getBigDecimal(columnIndex);
    return bigDecimal == null ? null : bigDecimal.toBigInteger();
  }
}
