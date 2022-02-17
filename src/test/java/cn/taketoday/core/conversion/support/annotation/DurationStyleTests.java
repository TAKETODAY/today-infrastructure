/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.conversion.support.annotation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DurationStyle}.
 *
 * @author Phillip Webb
 */
class DurationStyleTests {

	@Test
	void detectAndParseWhenValueIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationStyle.detectAndParse(null))
				.withMessageContaining("Value must not be null");
	}

	@Test
	void detectAndParseWhenIso8601ShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("PT20.345S")).isEqualTo(Duration.parse("PT20.345S"));
		assertThat(DurationStyle.detectAndParse("PT15M")).isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.detectAndParse("+PT15M")).isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.detectAndParse("PT10H")).isEqualTo(Duration.parse("PT10H"));
		assertThat(DurationStyle.detectAndParse("P2D")).isEqualTo(Duration.parse("P2D"));
		assertThat(DurationStyle.detectAndParse("P2DT3H4M")).isEqualTo(Duration.parse("P2DT3H4M"));
		assertThat(DurationStyle.detectAndParse("-PT6H3M")).isEqualTo(Duration.parse("-PT6H3M"));
		assertThat(DurationStyle.detectAndParse("-PT-6H+3M")).isEqualTo(Duration.parse("-PT-6H+3M"));
	}

	@Test
	void detectAndParseWhenSimpleNanosShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10ns")).hasNanos(10);
		assertThat(DurationStyle.detectAndParse("10NS")).hasNanos(10);
		assertThat(DurationStyle.detectAndParse("+10ns")).hasNanos(10);
		assertThat(DurationStyle.detectAndParse("-10ns")).hasNanos(-10);
	}

	@Test
	void detectAndParseWhenSimpleMicrosShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10us")).hasNanos(10000);
		assertThat(DurationStyle.detectAndParse("10US")).hasNanos(10000);
		assertThat(DurationStyle.detectAndParse("+10us")).hasNanos(10000);
		assertThat(DurationStyle.detectAndParse("-10us")).hasNanos(-10000);
	}

	@Test
	void detectAndParseWhenSimpleMillisShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10ms")).hasMillis(10);
		assertThat(DurationStyle.detectAndParse("10MS")).hasMillis(10);
		assertThat(DurationStyle.detectAndParse("+10ms")).hasMillis(10);
		assertThat(DurationStyle.detectAndParse("-10ms")).hasMillis(-10);
	}

	@Test
	void detectAndParseWhenSimpleSecondsShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10s")).hasSeconds(10);
		assertThat(DurationStyle.detectAndParse("10S")).hasSeconds(10);
		assertThat(DurationStyle.detectAndParse("+10s")).hasSeconds(10);
		assertThat(DurationStyle.detectAndParse("-10s")).hasSeconds(-10);
	}

	@Test
	void detectAndParseWhenSimpleMinutesShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10m")).hasMinutes(10);
		assertThat(DurationStyle.detectAndParse("10M")).hasMinutes(10);
		assertThat(DurationStyle.detectAndParse("+10m")).hasMinutes(10);
		assertThat(DurationStyle.detectAndParse("-10m")).hasMinutes(-10);
	}

	@Test
	void detectAndParseWhenSimpleHoursShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10h")).hasHours(10);
		assertThat(DurationStyle.detectAndParse("10H")).hasHours(10);
		assertThat(DurationStyle.detectAndParse("+10h")).hasHours(10);
		assertThat(DurationStyle.detectAndParse("-10h")).hasHours(-10);
	}

	@Test
	void detectAndParseWhenSimpleDaysShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10d")).hasDays(10);
		assertThat(DurationStyle.detectAndParse("10D")).hasDays(10);
		assertThat(DurationStyle.detectAndParse("+10d")).hasDays(10);
		assertThat(DurationStyle.detectAndParse("-10d")).hasDays(-10);
	}

	@Test
	void detectAndParseWhenSimpleWithoutSuffixShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10")).hasMillis(10);
		assertThat(DurationStyle.detectAndParse("+10")).hasMillis(10);
		assertThat(DurationStyle.detectAndParse("-10")).hasMillis(-10);
	}

	@Test
	void detectAndParseWhenSimpleWithoutSuffixButWithChronoUnitShouldReturnDuration() {
		assertThat(DurationStyle.detectAndParse("10", ChronoUnit.SECONDS)).hasSeconds(10);
		assertThat(DurationStyle.detectAndParse("+10", ChronoUnit.SECONDS)).hasSeconds(10);
		assertThat(DurationStyle.detectAndParse("-10", ChronoUnit.SECONDS)).hasSeconds(-10);
	}

	@Test
	void detectAndParseWhenBadFormatShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationStyle.detectAndParse("10foo"))
				.withMessageContaining("'10foo' is not a valid duration");
	}

	@Test
	void detectWhenSimpleShouldReturnSimple() {
		assertThat(DurationStyle.detect("10")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("+10")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("-10")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("10ns")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("10ms")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("10s")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("10m")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("10h")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("10d")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("-10ms")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("-10ms")).isEqualTo(DurationStyle.SIMPLE);
		assertThat(DurationStyle.detect("10D")).isEqualTo(DurationStyle.SIMPLE);
	}

	@Test
	void detectWhenIso8601ShouldReturnIso8601() {
		assertThat(DurationStyle.detect("PT20.345S")).isEqualTo(DurationStyle.ISO8601);
		assertThat(DurationStyle.detect("PT15M")).isEqualTo(DurationStyle.ISO8601);
		assertThat(DurationStyle.detect("+PT15M")).isEqualTo(DurationStyle.ISO8601);
		assertThat(DurationStyle.detect("PT10H")).isEqualTo(DurationStyle.ISO8601);
		assertThat(DurationStyle.detect("P2D")).isEqualTo(DurationStyle.ISO8601);
		assertThat(DurationStyle.detect("P2DT3H4M")).isEqualTo(DurationStyle.ISO8601);
		assertThat(DurationStyle.detect("-PT6H3M")).isEqualTo(DurationStyle.ISO8601);
		assertThat(DurationStyle.detect("-PT-6H+3M")).isEqualTo(DurationStyle.ISO8601);
	}

	@Test
	void detectWhenUnknownShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationStyle.detect("bad"))
				.withMessageContaining("'bad' is not a valid duration");
	}

	@Test
	void parseIso8601ShouldParse() {
		assertThat(DurationStyle.ISO8601.parse("PT20.345S")).isEqualTo(Duration.parse("PT20.345S"));
		assertThat(DurationStyle.ISO8601.parse("PT15M")).isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.ISO8601.parse("+PT15M")).isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.ISO8601.parse("PT10H")).isEqualTo(Duration.parse("PT10H"));
		assertThat(DurationStyle.ISO8601.parse("P2D")).isEqualTo(Duration.parse("P2D"));
		assertThat(DurationStyle.ISO8601.parse("P2DT3H4M")).isEqualTo(Duration.parse("P2DT3H4M"));
		assertThat(DurationStyle.ISO8601.parse("-PT6H3M")).isEqualTo(Duration.parse("-PT6H3M"));
		assertThat(DurationStyle.ISO8601.parse("-PT-6H+3M")).isEqualTo(Duration.parse("-PT-6H+3M"));
	}

	@Test
	void parseIso8601WithUnitShouldIgnoreUnit() {
		assertThat(DurationStyle.ISO8601.parse("PT20.345S", ChronoUnit.SECONDS)).isEqualTo(Duration.parse("PT20.345S"));
		assertThat(DurationStyle.ISO8601.parse("PT15M", ChronoUnit.SECONDS)).isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.ISO8601.parse("+PT15M", ChronoUnit.SECONDS)).isEqualTo(Duration.parse("PT15M"));
		assertThat(DurationStyle.ISO8601.parse("PT10H", ChronoUnit.SECONDS)).isEqualTo(Duration.parse("PT10H"));
		assertThat(DurationStyle.ISO8601.parse("P2D")).isEqualTo(Duration.parse("P2D"));
		assertThat(DurationStyle.ISO8601.parse("P2DT3H4M", ChronoUnit.SECONDS)).isEqualTo(Duration.parse("P2DT3H4M"));
		assertThat(DurationStyle.ISO8601.parse("-PT6H3M", ChronoUnit.SECONDS)).isEqualTo(Duration.parse("-PT6H3M"));
		assertThat(DurationStyle.ISO8601.parse("-PT-6H+3M", ChronoUnit.SECONDS)).isEqualTo(Duration.parse("-PT-6H+3M"));
	}

	@Test
	void parseIso8601WhenSimpleShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationStyle.ISO8601.parse("10d"))
				.withMessageContaining("'10d' is not a valid ISO-8601 duration");
	}

	@Test
	void parseSimpleShouldParse() {
		assertThat(DurationStyle.SIMPLE.parse("10m")).hasMinutes(10);
	}

	@Test
	void parseSimpleWithUnitShouldUseUnitAsFallback() {
		assertThat(DurationStyle.SIMPLE.parse("10m", ChronoUnit.SECONDS)).hasMinutes(10);
		assertThat(DurationStyle.SIMPLE.parse("10", ChronoUnit.MINUTES)).hasMinutes(10);
	}

	@Test
	void parseSimpleWhenUnknownUnitShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationStyle.SIMPLE.parse("10mb"))
				.satisfies((ex) -> assertThat(ex.getCause().getMessage()).isEqualTo("Unknown unit 'mb'"));
	}

	@Test
	void parseSimpleWhenIso8601ShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationStyle.SIMPLE.parse("PT10H"))
				.withMessageContaining("'PT10H' is not a valid simple duration");
	}

	@Test
	void printIso8601ShouldPrint() {
		Duration duration = Duration.parse("-PT-6H+3M");
		assertThat(DurationStyle.ISO8601.print(duration)).isEqualTo("PT5H57M");
	}

	@Test
	void printIso8601ShouldIgnoreUnit() {
		Duration duration = Duration.parse("-PT-6H+3M");
		assertThat(DurationStyle.ISO8601.print(duration, ChronoUnit.DAYS)).isEqualTo("PT5H57M");
	}

	@Test
	void printSimpleWithoutUnitShouldPrintInMs() {
		Duration duration = Duration.ofSeconds(1);
		assertThat(DurationStyle.SIMPLE.print(duration)).isEqualTo("1000ms");
	}

	@Test
	void printSimpleWithSecondsUnitShouldPrintInUnit() {
		Duration duration = Duration.ofMillis(1000);
		assertThat(DurationStyle.SIMPLE.print(duration, ChronoUnit.SECONDS)).isEqualTo("1s");
	}

	@Test
	void printSimpleWithMicrosUnitShouldPrintInUnit() {
		Duration duration = Duration.ofNanos(2000);
		assertThat(DurationStyle.SIMPLE.print(duration, ChronoUnit.MICROS)).isEqualTo("2us");
	}

}
