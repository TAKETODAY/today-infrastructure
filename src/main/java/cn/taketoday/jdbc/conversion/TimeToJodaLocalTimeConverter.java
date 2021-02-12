package cn.taketoday.jdbc.conversion;

import org.joda.time.LocalTime;

import java.sql.Time;

import cn.taketoday.context.conversion.Converter;

/**
 * @author TODAY 2021/2/11 12:01
 */
public class TimeToJodaLocalTimeConverter implements Converter<Time, LocalTime> {

  @Override
  public LocalTime convert(Time source) {
    return new LocalTime(source);
  }

}
