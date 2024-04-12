/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.TimeZone;

import cn.taketoday.format.annotation.DateTimeFormat.ISO;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Factory that creates a JSR-310 {@link DateTimeFormatter}.
 *
 * <p>Formatters will be created using the defined {@link #setPattern pattern},
 * {@link #setIso ISO}, and <code>xxxStyle</code> methods (considered in that order).
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #createDateTimeFormatter()
 * @see #createDateTimeFormatter(DateTimeFormatter)
 * @see #setPattern
 * @see #setIso
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see DateTimeFormatterFactoryBean
 * @since 4.0
 */
public class DateTimeFormatterFactory {

  @Nullable
  private String pattern;

  @Nullable
  private ISO iso;

  @Nullable
  private FormatStyle dateStyle;

  @Nullable
  private FormatStyle timeStyle;

  @Nullable
  private TimeZone timeZone;

  /**
   * Create a new {@code DateTimeFormatterFactory} instance.
   */
  public DateTimeFormatterFactory() {
  }

  /**
   * Create a new {@code DateTimeFormatterFactory} instance.
   *
   * @param pattern the pattern to use to format date values
   */
  public DateTimeFormatterFactory(String pattern) {
    this.pattern = pattern;
  }

  /**
   * Set the pattern to use to format date values.
   *
   * @param pattern the format pattern
   */
  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  /**
   * Set the ISO format used to format date values.
   *
   * @param iso the ISO format
   */
  public void setIso(ISO iso) {
    this.iso = iso;
  }

  /**
   * Set the style to use for date types.
   */
  public void setDateStyle(FormatStyle dateStyle) {
    this.dateStyle = dateStyle;
  }

  /**
   * Set the style to use for time types.
   */
  public void setTimeStyle(FormatStyle timeStyle) {
    this.timeStyle = timeStyle;
  }

  /**
   * Set the style to use for date and time types.
   */
  public void setDateTimeStyle(FormatStyle dateTimeStyle) {
    this.dateStyle = dateTimeStyle;
    this.timeStyle = dateTimeStyle;
  }

  /**
   * Set the two characters to use to format date values, in Joda-Time style.
   * <p>The first character is used for the date style; the second is for
   * the time style. Supported characters are:
   * <ul>
   * <li>'S' = Small</li>
   * <li>'M' = Medium</li>
   * <li>'L' = Long</li>
   * <li>'F' = Full</li>
   * <li>'-' = Omitted</li>
   * </ul>
   * <p>This method mimics the styles supported by Joda-Time. Note that
   * JSR-310 natively favors {@link FormatStyle} as used for
   * {@link #setDateStyle}, {@link #setTimeStyle} and {@link #setDateTimeStyle}.
   *
   * @param style two characters from the set {"S", "M", "L", "F", "-"}
   */
  public void setStylePattern(String style) {
    Assert.isTrue(style.length() == 2, "Style pattern must consist of two characters");
    this.dateStyle = convertStyleCharacter(style.charAt(0));
    this.timeStyle = convertStyleCharacter(style.charAt(1));
  }

  @Nullable
  private FormatStyle convertStyleCharacter(char c) {
    return switch (c) {
      case 'S' -> FormatStyle.SHORT;
      case 'M' -> FormatStyle.MEDIUM;
      case 'L' -> FormatStyle.LONG;
      case 'F' -> FormatStyle.FULL;
      case '-' -> null;
      default -> throw new IllegalArgumentException("Invalid style character '" + c + "'");
    };
  }

  /**
   * Set the {@code TimeZone} to normalize the date values into, if any.
   *
   * @param timeZone the time zone
   */
  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }

  /**
   * Create a new {@code DateTimeFormatter} using this factory.
   * <p>If no specific pattern or style has been defined,
   * {@link FormatStyle#MEDIUM medium date time format} will be used.
   *
   * @return a new date time formatter
   * @see #createDateTimeFormatter(DateTimeFormatter)
   */
  public DateTimeFormatter createDateTimeFormatter() {
    return createDateTimeFormatter(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
  }

  /**
   * Create a new {@code DateTimeFormatter} using this factory.
   * <p>If no specific pattern or style has been defined,
   * the supplied {@code fallbackFormatter} will be used.
   *
   * @param fallbackFormatter the fall-back formatter to use
   * when no specific factory properties have been set
   * @return a new date time formatter
   */
  public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
    DateTimeFormatter dateTimeFormatter = null;
    if (StringUtils.isNotEmpty(this.pattern)) {
      dateTimeFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(this.pattern);
    }
    else if (this.iso != null && this.iso != ISO.NONE) {
      dateTimeFormatter = switch (this.iso) {
        case DATE -> DateTimeFormatter.ISO_DATE;
        case TIME -> DateTimeFormatter.ISO_TIME;
        case DATE_TIME -> DateTimeFormatter.ISO_DATE_TIME;
        default -> throw new IllegalStateException("Unsupported ISO format: " + this.iso);
      };
    }
    else if (this.dateStyle != null && this.timeStyle != null) {
      dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(this.dateStyle, this.timeStyle);
    }
    else if (this.dateStyle != null) {
      dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(this.dateStyle);
    }
    else if (this.timeStyle != null) {
      dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(this.timeStyle);
    }

    if (dateTimeFormatter != null && this.timeZone != null) {
      dateTimeFormatter = dateTimeFormatter.withZone(this.timeZone.toZoneId());
    }
    return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
  }

}
