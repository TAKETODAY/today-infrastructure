package cn.taketoday.jdbc.conversion;

import java.sql.Clob;
import java.sql.SQLException;

import cn.taketoday.context.conversion.Converter;
import cn.taketoday.context.exception.ConversionException;

/**
 * @author TODAY 2021/1/8 22:00
 */
public class ClobToStringConverter implements Converter<Clob, String> {

  @Override
  public String convert(final Clob source) {
    try {
      return source.getSubString(1, (int) source.length());
    }
    catch (SQLException e) {
      throw new ConversionException("error converting clob to String", e);
    }
    finally {
      try {
        source.free();
      }
      catch (SQLException ignore) { }
    }
  }

}
