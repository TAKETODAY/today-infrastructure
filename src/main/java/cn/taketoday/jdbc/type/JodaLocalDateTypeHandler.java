package cn.taketoday.jdbc.type;

import org.joda.time.LocalDate;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author TODAY
 * @date 2021/1/6 16:04
 */
@MappedTypes(LocalDate.class)
public class JodaLocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter) throws SQLException {
    ps.setDate(i, new Date(parameter.toDateTimeAtStartOfDay().getMillis()));
  }

  @Override
  public LocalDate getResult(ResultSet rs, String columnName) throws SQLException {
    return getResultInternal(rs.getObject(columnName));
  }

  @Override
  public LocalDate getResult(ResultSet rs, int columnIndex) throws SQLException {
    return getResultInternal(rs.getObject(columnIndex));
  }

  @Override
  public LocalDate getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return getResultInternal(cs.getObject(columnIndex));
  }

  protected LocalDate getResultInternal(final Object val) {
    if (val == null) {
      return null;
    }
    try {
      // Joda has it's own pluggable converters infrastructure
      // it will throw IllegalArgumentException if can't convert
      // look @ org.joda.time.convert.ConverterManager
      return new LocalDate(val);
    }
    catch (IllegalArgumentException ex) {
      throw new TypeException(
              "Don't know how to convert from type '" + val.getClass().getName() + "' to type '"
                      + LocalDate.class.getName() + "'", ex);
    }
  }
}
