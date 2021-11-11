package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/1/6 16:52
 */
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int parameterIndex, UUID parameter) throws SQLException {
    ps.setString(parameterIndex, parameter.toString());
  }

  @Override
  public UUID getResult(ResultSet rs, String columnName) throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public UUID getResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public UUID getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  protected UUID fromString(String val) {
    return StringUtils.isEmpty(val) ? null : UUID.fromString(val);
  }
}
