/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
