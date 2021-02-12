package cn.taketoday.jdbc.type;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author TODAY 2021/2/12 13:51
 */
public class BytesInputStreamTypeHandler extends BaseTypeHandler<InputStream> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int parameterIndex, InputStream parameter) throws SQLException {
    ps.setBinaryStream(parameterIndex, parameter);
  }

  @Override
  public InputStream getResult(ResultSet rs, String columnName) throws SQLException {
    return new ByteArrayInputStream(rs.getBytes(columnName));
  }

  @Override
  public InputStream getResult(ResultSet rs, int columnIndex) throws SQLException {
    return new ByteArrayInputStream(rs.getBytes(columnIndex));
  }

  @Override
  public InputStream getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return new ByteArrayInputStream(cs.getBytes(columnIndex));
  }
}
