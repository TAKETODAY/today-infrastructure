package cn.taketoday.jdbc.conversion;

import java.sql.Time;
import java.time.OffsetTime;

import cn.taketoday.context.conversion.Converter;

/**
 * @author TODAY 2021/2/12 14:12
 */
public class OffsetTimeToSQLTimeConverter implements Converter<OffsetTime, Time> {

  @Override
  public Time convert(OffsetTime source) {
    return Time.valueOf(source.toLocalTime());
  }

}
