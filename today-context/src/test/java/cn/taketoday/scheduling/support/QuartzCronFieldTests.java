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

package cn.taketoday.scheduling.support;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link QuartzCronField}.
 *
 * @author Arjen Poutsma
 */
class QuartzCronFieldTests {

  @Test
  void lastDayOfMonth() {
    QuartzCronField field = QuartzCronField.parseDaysOfMonth("L");

    LocalDate last = LocalDate.of(2020, 6, 16);
    LocalDate expected = LocalDate.of(2020, 6, 30);
    assertThat(field.nextOrSame(last)).isEqualTo(expected);
  }

  @Test
  void lastDayOfMonthOffset() {
    QuartzCronField field = QuartzCronField.parseDaysOfMonth("L-3");

    LocalDate last = LocalDate.of(2020, 6, 16);
    LocalDate expected = LocalDate.of(2020, 6, 27);
    assertThat(field.nextOrSame(last)).isEqualTo(expected);
  }

  @Test
  void lastWeekdayOfMonth() {
    QuartzCronField field = QuartzCronField.parseDaysOfMonth("LW");

    LocalDate last = LocalDate.of(2020, 6, 16);
    LocalDate expected = LocalDate.of(2020, 6, 30);
    LocalDate actual = field.nextOrSame(last);
    assertThat(actual).isNotNull();
    assertThat(actual.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void lastDayOfWeekOffset() {
    // last Thursday (4) of the month
    QuartzCronField field = QuartzCronField.parseDaysOfWeek("4L");

    LocalDate last = LocalDate.of(2020, 6, 16);
    LocalDate expected = LocalDate.of(2020, 6, 25);
    assertThat(field.nextOrSame(last)).isEqualTo(expected);
  }

  @Test
  void dayOfWeek_0() {
    // third Sunday (0) of the month
    QuartzCronField field = QuartzCronField.parseDaysOfWeek("0#3");

    LocalDate last = LocalDate.of(2024, 1, 1);
    LocalDate expected = LocalDate.of(2024, 1, 21);
    assertThat(field.nextOrSame(last)).isEqualTo(expected);
  }

  @Test
  void dayOfWeek_1() {
    // third Monday (1) of the month
    QuartzCronField field = QuartzCronField.parseDaysOfWeek("1#3");

    LocalDate last = LocalDate.of(2024, 1, 1);
    LocalDate expected = LocalDate.of(2024, 1, 15);
    assertThat(field.nextOrSame(last)).isEqualTo(expected);
  }

  @Test
  void dayOfWeek_2() {
    // third Tuesday (2) of the month
    QuartzCronField field = QuartzCronField.parseDaysOfWeek("2#3");

    LocalDate last = LocalDate.of(2024, 1, 1);
    LocalDate expected = LocalDate.of(2024, 1, 16);
    assertThat(field.nextOrSame(last)).isEqualTo(expected);
  }

  @Test
  void dayOfWeek_7() {
    // third Sunday (7 as alternative to 0) of the month
    QuartzCronField field = QuartzCronField.parseDaysOfWeek("7#3");

    LocalDate last = LocalDate.of(2024, 1, 1);
    LocalDate expected = LocalDate.of(2024, 1, 21);
    assertThat(field.nextOrSame(last)).isEqualTo(expected);
  }

  @Test
  void invalidValues() {
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth(""));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("1"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("1L"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("LL"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("4L"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("0L"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("W"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("W1"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("WW"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfMonth("32W"));

    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek(""));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("1"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("L"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("L1"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("LL"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("-4L"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("8L"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("#"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("1#"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("#1"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("1#L"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("L#1"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("8#1"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("2#1,2#3,2#5"));
    assertThatIllegalArgumentException().isThrownBy(() -> QuartzCronField.parseDaysOfWeek("FRI#-1"));
  }

}
