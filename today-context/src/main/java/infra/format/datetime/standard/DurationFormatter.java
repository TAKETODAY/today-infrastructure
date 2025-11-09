/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.format.datetime.standard;

import org.jspecify.annotations.Nullable;

import java.text.ParseException;
import java.time.Duration;
import java.util.Locale;

import infra.format.Formatter;
import infra.format.annotation.DurationFormat;
import infra.format.annotation.DurationFormat.Unit;

/**
 * {@link Formatter} implementation for a JSR-310 {@link Duration},
 * following JSR-310's parsing rules for a Duration by default and
 * supporting additional {@code DurationFormat.Style} styles.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DurationFormatterUtils
 * @see DurationFormat.Style
 * @since 4.0
 */
class DurationFormatter implements Formatter<Duration> {

  private final DurationFormat.Style style;

  @Nullable
  private final Unit defaultUnit;

  /**
   * Create a {@code DurationFormatter} following JSR-310's parsing rules for a Duration
   * (the {@link DurationFormat.Style#ISO8601 ISO-8601} style).
   */
  DurationFormatter() {
    this(DurationFormat.Style.ISO8601);
  }

  /**
   * Create a {@code DurationFormatter} in a specific {@link DurationFormat.Style}.
   * <p>When a unit is needed but cannot be determined (e.g. printing a Duration in the
   * {@code SIMPLE} style), {@code DurationFormat.Unit#MILLIS} is used.
   */
  public DurationFormatter(DurationFormat.Style style) {
    this(style, null);
  }

  /**
   * Create a {@code DurationFormatter} in a specific {@link DurationFormat.Style} with an
   * optional {@code DurationFormat.Unit}.
   * <p>If a {@code defaultUnit} is specified, it may be used in parsing cases when no
   * unit is present in the string (provided the style allows for such a case). It will
   * also be used as the representation's resolution when printing in the
   * {@link DurationFormat.Style#SIMPLE} style. Otherwise, the style defines its default
   * unit.
   *
   * @param style the {@code DurationStyle} to use
   * @param defaultUnit the {@code DurationFormat.Unit} to fall back to when parsing and printing
   */
  public DurationFormatter(DurationFormat.Style style, @Nullable Unit defaultUnit) {
    this.style = style;
    this.defaultUnit = defaultUnit;
  }

  @Override
  public Duration parse(String text, Locale locale) throws ParseException {
    if (this.defaultUnit == null) {
      //delegate to the style
      return DurationFormatterUtils.parse(text, this.style);
    }
    return DurationFormatterUtils.parse(text, this.style, this.defaultUnit);
  }

  @Override
  public String print(Duration object, Locale locale) {
    if (this.defaultUnit == null) {
      //delegate the ultimate of the default unit to the style
      return DurationFormatterUtils.print(object, this.style);
    }
    return DurationFormatterUtils.print(object, this.style, this.defaultUnit);
  }

}
