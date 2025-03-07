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

package infra.scheduling.support;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link BitsCronField}.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class BitsCronFieldTests {

  @Test
  void parse() {
    assertThat(BitsCronField.parseSeconds("42")).has(clearRange(0, 41)).has(set(42)).has(clearRange(43, 59));
    assertThat(BitsCronField.parseSeconds("0-4,8-12")).has(setRange(0, 4)).has(clearRange(5, 7)).has(setRange(8, 12)).has(clearRange(13, 59));
    assertThat(BitsCronField.parseSeconds("57/2")).has(clearRange(0, 56)).has(set(57)).has(clear(58)).has(set(59));

    assertThat(BitsCronField.parseMinutes("30")).has(set(30)).has(clearRange(1, 29)).has(clearRange(31, 59));

    assertThat(BitsCronField.parseHours("23")).has(set(23)).has(clearRange(0, 23));
    assertThat(BitsCronField.parseHours("0-23/2")).has(set(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)).has(clear(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23));

    assertThat(BitsCronField.parseDaysOfMonth("1")).has(set(1)).has(clearRange(2, 31));

    assertThat(BitsCronField.parseMonth("1")).has(set(1)).has(clearRange(2, 12));

    assertThat(BitsCronField.parseDaysOfWeek("0")).has(set(7, 7)).has(clearRange(0, 6));

    assertThat(BitsCronField.parseDaysOfWeek("7-5")).has(clear(0)).has(setRange(1, 5)).has(clear(6)).has(set(7));
  }

  @Test
  void parseLists() {
    assertThat(BitsCronField.parseSeconds("15,30")).has(set(15, 30)).has(clearRange(1, 15)).has(clearRange(31, 59));
    assertThat(BitsCronField.parseMinutes("1,2,5,9")).has(set(1, 2, 5, 9)).has(clear(0)).has(clearRange(3, 4)).has(clearRange(6, 8)).has(clearRange(10, 59));
    assertThat(BitsCronField.parseHours("1,2,3")).has(set(1, 2, 3)).has(clearRange(4, 23));
    assertThat(BitsCronField.parseDaysOfMonth("1,2,3")).has(set(1, 2, 3)).has(clearRange(4, 31));
    assertThat(BitsCronField.parseMonth("1,2,3")).has(set(1, 2, 3)).has(clearRange(4, 12));
    assertThat(BitsCronField.parseDaysOfWeek("1,2,3")).has(set(1, 2, 3)).has(clearRange(4, 7));

    assertThat(BitsCronField.parseMinutes("5,10-30/2"))
            .has(clearRange(0, 5))
            .has(set(5))
            .has(clearRange(6, 10))
            .has(set(10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30))
            .has(clear(11, 13, 15, 17, 19, 21, 23, 25, 27, 29))
            .has(clearRange(31, 60));
  }

  @Test
  void invalidRange() {
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds(""));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds("0-12/0"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds("60"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseMinutes("60"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfMonth("0"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfMonth("32"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseMonth("0"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseMonth("13"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfWeek("8"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseSeconds("20-10"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfWeek("*SUN"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseDaysOfWeek("SUN*"));
    assertThatIllegalArgumentException().isThrownBy(() -> BitsCronField.parseHours("*ANYTHING_HERE"));
  }

  @Test
  void parseWildcards() {
    assertThat(BitsCronField.parseSeconds("*")).has(setRange(0, 60));
    assertThat(BitsCronField.parseMinutes("*")).has(setRange(0, 60));
    assertThat(BitsCronField.parseHours("*")).has(setRange(0, 23));
    assertThat(BitsCronField.parseDaysOfMonth("*")).has(clear(0)).has(setRange(1, 31));
    assertThat(BitsCronField.parseDaysOfMonth("?")).has(clear(0)).has(setRange(1, 31));
    assertThat(BitsCronField.parseMonth("*")).has(clear(0)).has(setRange(1, 12));
    assertThat(BitsCronField.parseDaysOfWeek("*")).has(clear(0)).has(setRange(1, 7));
    assertThat(BitsCronField.parseDaysOfWeek("?")).has(clear(0)).has(setRange(1, 7));
  }

  @Test
  void names() {
    assertThat(((BitsCronField) CronField.parseMonth("JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC")))
            .has(clear(0)).has(setRange(1, 12));
    assertThat(((BitsCronField) CronField.parseDaysOfWeek("SUN,MON,TUE,WED,THU,FRI,SAT")))
            .has(clear(0)).has(setRange(1, 7));
  }

  private static Condition<BitsCronField> set(int... indices) {
    return new Condition<BitsCronField>(String.format("set bits %s", Arrays.toString(indices))) {
      @Override
      public boolean matches(BitsCronField value) {
        for (int index : indices) {
          if (!value.getBit(index)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  private static Condition<BitsCronField> setRange(int min, int max) {
    return new Condition<BitsCronField>(String.format("set range %d-%d", min, max)) {
      @Override
      public boolean matches(BitsCronField value) {
        for (int i = min; i < max; i++) {
          if (!value.getBit(i)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  private static Condition<BitsCronField> clear(int... indices) {
    return new Condition<BitsCronField>(String.format("clear bits %s", Arrays.toString(indices))) {
      @Override
      public boolean matches(BitsCronField value) {
        for (int index : indices) {
          if (value.getBit(index)) {
            return false;
          }
        }
        return true;
      }
    };
  }

  private static Condition<BitsCronField> clearRange(int min, int max) {
    return new Condition<BitsCronField>(String.format("clear range %d-%d", min, max)) {
      @Override
      public boolean matches(BitsCronField value) {
        for (int i = min; i < max; i++) {
          if (value.getBit(i)) {
            return false;
          }
        }
        return true;
      }
    };
  }

}
