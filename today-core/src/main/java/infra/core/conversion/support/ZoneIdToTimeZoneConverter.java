/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
