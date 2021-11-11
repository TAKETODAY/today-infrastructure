package cn.taketoday.jdbc.type;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author TODAY 2021/1/6 15:55
 */
@MappedTypes(DateTime.class)
public class JodaDateTimeTypeHandler extends BaseTypeHandler<DateTime> {
  private final DateTimeZone timeZone;

  public JodaDateTimeTypeHandler() {
    this(DateTimeZone.getDefault());
  }

  // it's possible to create instance for other timezone and re-register converter
  public JodaDateTimeTypeHandler(DateTimeZone timeZone) {
    this.timeZone = timeZone;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, DateTime parameter) throws SQLException {
    ps.setTimestamp(i, new Timestamp(parameter.getMillis()));
  }

  @Override
  public DateTime getResult(ResultSet rs, String columnName) throws SQLException {
    return getResultInternal(rs.getObject(columnName));
  }

  @Override
  public DateTime getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getResultInternal(rs.getObject(columnIndex));
  }

  @Override
  public DateTime getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getResultInternal(cs.getObject(columnIndex));
  }

  protected DateTime getResultInternal(final Object val) {
    if (val == null) {
      return null;
    }
    try {
      // Joda has it's own pluggable converters infrastructure
      // it will throw IllegalArgumentException if can't convert
      // look @ org.joda.time.convert.ConverterManager
      return new LocalDateTime(val).toDateTime(timeZone);
    }
    catch (IllegalArgumentException ex) {
      throw new TypeException("Error while converting type " + val.getClass().toString() + " to joda-time", ex);
    }
  }
}
