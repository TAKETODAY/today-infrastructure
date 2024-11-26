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

package infra.core.conversion.support;

import java.time.ZoneId;
import java.util.TimeZone;

import infra.core.conversion.Converter;

/**
 * Simple converter from Java 8's {@link ZoneId} to {@link TimeZone}.
 *
 * <p>Note that Framework's default ConversionService setup understands the 'from'/'to' convention
 * that the JSR-310 {@code java.time} package consistently uses. That convention is implemented
 * reflectively in {@link ObjectToObjectConverter}, not in specific JSR-310 converters.
 * It covers {@link TimeZone#toZoneId()} as well, and also
 * {@link java.util.Date#from(java.time.Instant)} and {@link java.util.Date#toInstant()}.
 *
 * @author Juergen Hoeller
 * @see TimeZone#getTimeZone(ZoneId)
 * @since 4.0
 */
final class ZoneIdToTimeZoneConverter implements Converter<ZoneId, TimeZone> {

  @Override
  public TimeZone convert(ZoneId source) {
    return TimeZone.getTimeZone(source);
  }

}
