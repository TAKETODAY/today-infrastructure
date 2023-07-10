/*
 * Copyright 2012 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.app.loader.tools;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultTimeZoneOffset}
 *
 * @author Phillip Webb
 */
class DefaultTimeZoneOffsetTests {

	// gh-34424

	@Test
	void removeFromWithLongInDifferentTimeZonesReturnsSameValue() {
		long time = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
		TimeZone timeZone1 = TimeZone.getTimeZone("GMT");
		TimeZone timeZone2 = TimeZone.getTimeZone("GMT+8");
		TimeZone timeZone3 = TimeZone.getTimeZone("GMT-8");
		long result1 = new DefaultTimeZoneOffset(timeZone1).removeFrom(time);
		long result2 = new DefaultTimeZoneOffset(timeZone2).removeFrom(time);
		long result3 = new DefaultTimeZoneOffset(timeZone3).removeFrom(time);
		long dosTime1 = toDosTime(Calendar.getInstance(timeZone1), result1);
		long dosTime2 = toDosTime(Calendar.getInstance(timeZone2), result2);
		long dosTime3 = toDosTime(Calendar.getInstance(timeZone3), result3);
		assertThat(dosTime1).isEqualTo(dosTime2).isEqualTo(dosTime3);
	}

	@Test
	void removeFromWithFileTimeReturnsFileTime() {
		long time = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();
		long result = new DefaultTimeZoneOffset(TimeZone.getTimeZone("GMT+8")).removeFrom(time);
		assertThat(result).isNotEqualTo(time).isEqualTo(946656000000L);
	}

	/**
	 * Identical functionality to package-private
	 * org.apache.commons.compress.archivers.zip.ZipUtil.toDosTime(Calendar, long, byte[],
	 * int) method used by {@link ZipArchiveOutputStream} to convert times.
	 * @param calendar the source calendar
	 * @param time the time to convert
	 * @return the DOS time
	 */
	private long toDosTime(Calendar calendar, long time) {
		calendar.setTimeInMillis(time);
		final int year = calendar.get(Calendar.YEAR);
		final int month = calendar.get(Calendar.MONTH) + 1;
		return ((year - 1980) << 25) | (month << 21) | (calendar.get(Calendar.DAY_OF_MONTH) << 16)
				| (calendar.get(Calendar.HOUR_OF_DAY) << 11) | (calendar.get(Calendar.MINUTE) << 5)
				| (calendar.get(Calendar.SECOND) >> 1);
	}

}
