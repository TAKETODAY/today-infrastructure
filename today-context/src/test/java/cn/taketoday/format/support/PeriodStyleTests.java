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

package cn.taketoday.format.support;

import org.junit.jupiter.api.Test;

import java.time.Period;
import java.time.temporal.ChronoUnit;

import cn.taketoday.format.annotation.PeriodStyle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PeriodStyle}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 */
class PeriodStyleTests {

  @Test
  void detectAndParseWhenValueIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.detectAndParse(null))
            .withMessageContaining("Value must not be null");
  }

  @Test
  void detectAndParseWhenIso8601ShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("p15m")).isEqualTo(Period.parse("p15m"));
    assertThat(PeriodStyle.detectAndParse("P15M")).isEqualTo(Period.parse("P15M"));
    assertThat(PeriodStyle.detectAndParse("-P15M")).isEqualTo(Period.parse("P-15M"));
    assertThat(PeriodStyle.detectAndParse("+P15M")).isEqualTo(Period.parse("P15M"));
    assertThat(PeriodStyle.detectAndParse("P2D")).isEqualTo(Period.parse("P2D"));
    assertThat(PeriodStyle.detectAndParse("-P20Y")).isEqualTo(Period.parse("P-20Y"));

  }

  @Test
  void detectAndParseWhenSimpleDaysShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("10d")).hasDays(10);
    assertThat(PeriodStyle.detectAndParse("10D")).hasDays(10);
    assertThat(PeriodStyle.detectAndParse("+10d")).hasDays(10);
    assertThat(PeriodStyle.detectAndParse("-10D")).hasDays(-10);
  }

  @Test
  void detectAndParseWhenSimpleWeeksShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("10w")).isEqualTo(Period.ofWeeks(10));
    assertThat(PeriodStyle.detectAndParse("10W")).isEqualTo(Period.ofWeeks(10));
    assertThat(PeriodStyle.detectAndParse("+10w")).isEqualTo(Period.ofWeeks(10));
    assertThat(PeriodStyle.detectAndParse("-10W")).isEqualTo(Period.ofWeeks(-10));
  }

  @Test
  void detectAndParseWhenSimpleMonthsShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("10m")).hasMonths(10);
    assertThat(PeriodStyle.detectAndParse("10M")).hasMonths(10);
    assertThat(PeriodStyle.detectAndParse("+10m")).hasMonths(10);
    assertThat(PeriodStyle.detectAndParse("-10M")).hasMonths(-10);
  }

  @Test
  void detectAndParseWhenSimpleYearsShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("10y")).hasYears(10);
    assertThat(PeriodStyle.detectAndParse("10Y")).hasYears(10);
    assertThat(PeriodStyle.detectAndParse("+10y")).hasYears(10);
    assertThat(PeriodStyle.detectAndParse("-10Y")).hasYears(-10);
  }

  @Test
  void detectAndParseWhenSimpleWithoutSuffixShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("10")).hasDays(10);
    assertThat(PeriodStyle.detectAndParse("+10")).hasDays(10);
    assertThat(PeriodStyle.detectAndParse("-10")).hasDays(-10);
  }

  @Test
  void detectAndParseWhenSimpleWithoutSuffixButWithChronoUnitShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("10", ChronoUnit.MONTHS)).hasMonths(10);
    assertThat(PeriodStyle.detectAndParse("+10", ChronoUnit.MONTHS)).hasMonths(10);
    assertThat(PeriodStyle.detectAndParse("-10", ChronoUnit.MONTHS)).hasMonths(-10);
  }

  @Test
  void detectAndParseWhenComplexShouldReturnPeriod() {
    assertThat(PeriodStyle.detectAndParse("1y2m")).isEqualTo(Period.of(1, 2, 0));
    assertThat(PeriodStyle.detectAndParse("1y2m3d")).isEqualTo(Period.of(1, 2, 3));
    assertThat(PeriodStyle.detectAndParse("2m3d")).isEqualTo(Period.of(0, 2, 3));
    assertThat(PeriodStyle.detectAndParse("1y3d")).isEqualTo(Period.of(1, 0, 3));
    assertThat(PeriodStyle.detectAndParse("-1y3d")).isEqualTo(Period.of(-1, 0, 3));
    assertThat(PeriodStyle.detectAndParse("-1y-3d")).isEqualTo(Period.of(-1, 0, -3));
  }

  @Test
  void detectAndParseWhenBadFormatShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.detectAndParse("10foo"))
            .withMessageContaining("'10foo' is not a valid period");
  }

  @Test
  void detectWhenSimpleShouldReturnSimple() {
    assertThat(PeriodStyle.detect("10")).isEqualTo(PeriodStyle.SIMPLE);
    assertThat(PeriodStyle.detect("+10")).isEqualTo(PeriodStyle.SIMPLE);
    assertThat(PeriodStyle.detect("-10")).isEqualTo(PeriodStyle.SIMPLE);
    assertThat(PeriodStyle.detect("10m")).isEqualTo(PeriodStyle.SIMPLE);
    assertThat(PeriodStyle.detect("10y")).isEqualTo(PeriodStyle.SIMPLE);
    assertThat(PeriodStyle.detect("10d")).isEqualTo(PeriodStyle.SIMPLE);
    assertThat(PeriodStyle.detect("10D")).isEqualTo(PeriodStyle.SIMPLE);
  }

  @Test
  void detectWhenIso8601ShouldReturnIso8601() {
    assertThat(PeriodStyle.detect("p20")).isEqualTo(PeriodStyle.ISO8601);
    assertThat(PeriodStyle.detect("P20")).isEqualTo(PeriodStyle.ISO8601);
    assertThat(PeriodStyle.detect("-P15M")).isEqualTo(PeriodStyle.ISO8601);
    assertThat(PeriodStyle.detect("+P15M")).isEqualTo(PeriodStyle.ISO8601);
    assertThat(PeriodStyle.detect("P10Y")).isEqualTo(PeriodStyle.ISO8601);
    assertThat(PeriodStyle.detect("P2D")).isEqualTo(PeriodStyle.ISO8601);
    assertThat(PeriodStyle.detect("-P6")).isEqualTo(PeriodStyle.ISO8601);
    assertThat(PeriodStyle.detect("-P-6M")).isEqualTo(PeriodStyle.ISO8601);
  }

  @Test
  void detectWhenUnknownShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.detect("bad"))
            .withMessageContaining("'bad' is not a valid period");
  }

  @Test
  void parseIso8601ShouldParse() {
    assertThat(PeriodStyle.ISO8601.parse("p20d")).isEqualTo(Period.parse("p20d"));
    assertThat(PeriodStyle.ISO8601.parse("P20D")).isEqualTo(Period.parse("P20D"));
    assertThat(PeriodStyle.ISO8601.parse("P15M")).isEqualTo(Period.parse("P15M"));
    assertThat(PeriodStyle.ISO8601.parse("+P15M")).isEqualTo(Period.parse("P15M"));
    assertThat(PeriodStyle.ISO8601.parse("P10Y")).isEqualTo(Period.parse("P10Y"));
    assertThat(PeriodStyle.ISO8601.parse("P2D")).isEqualTo(Period.parse("P2D"));
    assertThat(PeriodStyle.ISO8601.parse("-P6D")).isEqualTo(Period.parse("-P6D"));
    assertThat(PeriodStyle.ISO8601.parse("-P-6Y+3M")).isEqualTo(Period.parse("-P-6Y+3M"));
  }

  @Test
  void parseIso8601WithUnitShouldIgnoreUnit() {
    assertThat(PeriodStyle.ISO8601.parse("p20d", ChronoUnit.SECONDS)).isEqualTo(Period.parse("p20d"));
    assertThat(PeriodStyle.ISO8601.parse("P20D", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P20D"));
    assertThat(PeriodStyle.ISO8601.parse("P15M", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P15M"));
    assertThat(PeriodStyle.ISO8601.parse("+P15M", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P15M"));
    assertThat(PeriodStyle.ISO8601.parse("P10Y", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P10Y"));
    assertThat(PeriodStyle.ISO8601.parse("P2D", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P2D"));
    assertThat(PeriodStyle.ISO8601.parse("-P6D", ChronoUnit.SECONDS)).isEqualTo(Period.parse("-P6D"));
    assertThat(PeriodStyle.ISO8601.parse("-P-6Y+3M", ChronoUnit.SECONDS)).isEqualTo(Period.parse("-P-6Y+3M"));
  }

  @Test
  void parseIso8601WhenSimpleShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.ISO8601.parse("10d"))
            .withMessageContaining("'10d' is not a valid ISO-8601 period");
  }

  @Test
  void parseSimpleShouldParse() {
    assertThat(PeriodStyle.SIMPLE.parse("10m")).hasMonths(10);
  }

  @Test
  void parseSimpleWithUnitShouldUseUnitAsFallback() {
    assertThat(PeriodStyle.SIMPLE.parse("10m", ChronoUnit.DAYS)).hasMonths(10);
    assertThat(PeriodStyle.SIMPLE.parse("10", ChronoUnit.MONTHS)).hasMonths(10);
  }

  @Test
  void parseSimpleWhenUnknownUnitShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.SIMPLE.parse("10x")).satisfies(
            (ex) -> assertThat(ex.getCause().getMessage()).isEqualTo("Does not match simple period pattern"));
  }

  @Test
  void parseSimpleWhenIso8601ShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.SIMPLE.parse("PT10H"))
            .withMessageContaining("'PT10H' is not a valid simple period");
  }

  @Test
  void printIso8601ShouldPrint() {
    Period period = Period.parse("-P-6M+3D");
    assertThat(PeriodStyle.ISO8601.print(period)).isEqualTo("P6M-3D");
  }

  @Test
  void printIso8601ShouldIgnoreUnit() {
    Period period = Period.parse("-P3Y");
    assertThat(PeriodStyle.ISO8601.print(period, ChronoUnit.DAYS)).isEqualTo("P-3Y");
  }

  @Test
  void printSimpleWhenZeroWithoutUnitShouldPrintInDays() {
    Period period = Period.ofMonths(0);
    assertThat(PeriodStyle.SIMPLE.print(period)).isEqualTo("0d");
  }

  @Test
  void printSimpleWhenZeroWithUnitShouldPrintInUnit() {
    Period period = Period.ofYears(0);
    assertThat(PeriodStyle.SIMPLE.print(period, ChronoUnit.YEARS)).isEqualTo("0y");
  }

  @Test
  void printSimpleWhenNonZeroShouldIgnoreUnit() {
    Period period = Period.of(1, 2, 3);
    assertThat(PeriodStyle.SIMPLE.print(period, ChronoUnit.YEARS)).isEqualTo("1y2m3d");
  }

}
