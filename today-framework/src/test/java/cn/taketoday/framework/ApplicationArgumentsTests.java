/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/2 21:28
 */
class ApplicationArgumentsTests {

  private static final String[] ARGS = new String[] { "--foo=bar", "--foo=baz", "--debug", "spring", "boot" };

  @Test
  void argumentsMustNotBeNull() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ApplicationArguments((String[]) null))
            .withMessageContaining("Args is required");
  }

  @Test
  void getArgs() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.getSourceArgs()).isEqualTo(ARGS);
  }

  @Test
  void optionNames() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    Set<String> expected = new HashSet<>(Arrays.asList("foo", "debug"));
    assertThat(arguments.getOptionNames()).isEqualTo(expected);
  }

  @Test
  void containsOption() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.containsOption("foo")).isTrue();
    assertThat(arguments.containsOption("debug")).isTrue();
    assertThat(arguments.containsOption("spring")).isFalse();
  }

  @Test
  void getOptionValues() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.getOptionValues("foo")).isEqualTo(Arrays.asList("bar", "baz"));
    assertThat(arguments.getOptionValues("debug")).isEmpty();
    assertThat(arguments.getOptionValues("spring")).isNull();
  }

  @Test
  void getNonOptionArgs() {
    ApplicationArguments arguments = new ApplicationArguments(ARGS);
    assertThat(arguments.getNonOptionArgs()).containsExactly("spring", "boot");
  }

  @Test
  void getNoNonOptionArgs() {
    ApplicationArguments arguments = new ApplicationArguments("--debug");
    assertThat(arguments.getNonOptionArgs()).isEmpty();
  }

}