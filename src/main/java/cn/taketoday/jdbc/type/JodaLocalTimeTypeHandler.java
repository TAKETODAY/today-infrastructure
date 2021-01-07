package cn.taketoday.jdbc.type;

import org.joda.time.LocalTime;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author TODAY
 * @date 2021/1/6 16:21
 */
public class JodaLocalTimeTypeHandler extends BaseTypeHandler<LocalTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalTime parameter) throws SQLException {
    ps.setTimestamp(i, new Timestamp(parameter.toDateTimeToday().getMillis()));
  }

  @Override
  public LocalTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getResultInternal(rs.getObject(columnName));
  }

  @Override
  public LocalTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getResultInternal(rs.getObject(columnIndex));
  }

  @Override
  public LocalTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getResultInternal(cs.getObject(columnIndex));
  }

  protected LocalTime getResultInternal(final Object val) {
    if (val == null) {
      return null;
    }
    try {
      // Joda has it's own pluggable converters infrastructure
      // it will throw IllegalArgumentException if can't convert
      // look @ org.joda.time.convert.ConverterManager
      return new LocalTime(val);
    }
    catch (IllegalArgumentException ex) {
      throw new TypeException("Don't know how to convert from type '" + val.getClass().getName() + "' to type '"
                                      + LocalTime.class.getName() + "'", ex);
    }
  }

}
