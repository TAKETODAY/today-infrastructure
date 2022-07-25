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

package cn.taketoday.core.env;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link SimpleCommandLineArgsParser}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 */
class SimpleCommandLineArgsParserTests {

  @Test
  void withNoOptions() {
    assertThat(SimpleCommandLineArgsParser.parse().getOptionValues("foo")).isNull();
  }

  @Test
  void withSingleOptionAndNoValue() {
    CommandLineArgs args = SimpleCommandLineArgsParser.parse("--o1");
    assertThat(args.containsOption("o1")).isTrue();
    assertThat(args.getOptionValues("o1")).isEqualTo(Collections.EMPTY_LIST);
  }

  @Test
  void withSingleOptionAndValue() {
    CommandLineArgs args = SimpleCommandLineArgsParser.parse("--o1=v1");
    assertThat(args.containsOption("o1")).isTrue();
    assertThat(args.getOptionValues("o1")).containsExactly("v1");
  }

  @Test
  void withMixOfOptionsHavingValueAndOptionsHavingNoValue() {
    CommandLineArgs args = SimpleCommandLineArgsParser.parse("--o1=v1", "--o2");
    assertThat(args.containsOption("o1")).isTrue();
    assertThat(args.containsOption("o2")).isTrue();
    assertThat(args.containsOption("o3")).isFalse();
    assertThat(args.getOptionValues("o1")).containsExactly("v1");
    assertThat(args.getOptionValues("o2")).isEqualTo(Collections.EMPTY_LIST);
    assertThat(args.getOptionValues("o3")).isNull();
  }

  @Test
  void withEmptyOptionText() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> SimpleCommandLineArgsParser.parse("--"));
  }

  @Test
  void withEmptyOptionName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> SimpleCommandLineArgsParser.parse("--=v1"));
  }

  @Test
  void withEmptyOptionValue() {
    CommandLineArgs args = SimpleCommandLineArgsParser.parse("--o1=");
    assertThat(args.containsOption("o1")).isTrue();
    assertThat(args.getOptionValues("o1")).containsExactly("");
  }

  @Test
  void withEmptyOptionNameAndEmptyOptionValue() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> SimpleCommandLineArgsParser.parse("--="));
  }

  @Test
  void withNonOptionArguments() {
    CommandLineArgs args = SimpleCommandLineArgsParser.parse("--o1=v1", "noa1", "--o2=v2", "noa2");
    assertThat(args.getOptionValues("o1")).containsExactly("v1");
    assertThat(args.getOptionValues("o2")).containsExactly("v2");

    List<String> nonOptions = args.getNonOptionArgs();
    assertThat(nonOptions).containsExactly("noa1", "noa2");
  }

  @Test
  void assertOptionNamesIsUnmodifiable() {
    CommandLineArgs args = SimpleCommandLineArgsParser.parse();
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> args.getOptionNames().add("bogus"));
  }

  @Test
  void assertNonOptionArgsIsUnmodifiable() {
    CommandLineArgs args = SimpleCommandLineArgsParser.parse();
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> args.getNonOptionArgs().add("foo"));
  }

}
