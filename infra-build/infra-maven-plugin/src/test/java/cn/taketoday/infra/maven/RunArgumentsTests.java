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

package cn.taketoday.infra.maven;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RunArguments}.
 *
 * @author Stephane Nicoll
 */
class RunArgumentsTests {

  @Test
  void parseNull() {
    String[] args = parseArgs(null);
    assertThat(args).isNotNull();
    assertThat(args).isEmpty();
  }

  @Test
  void parseNullArray() {
    String[] args = new RunArguments((String[]) null).asArray();
    assertThat(args).isNotNull();
    assertThat(args).isEmpty();
  }

  @Test
  void parseArrayContainingNullValue() {
    String[] args = new RunArguments(new String[] { "foo", null, "bar" }).asArray();
    assertThat(args).isNotNull();
    assertThat(args).containsOnly("foo", "bar");
  }

  @Test
  void parseArrayContainingEmptyValue() {
    String[] args = new RunArguments(new String[] { "foo", "", "bar" }).asArray();
    assertThat(args).isNotNull();
    assertThat(args).containsOnly("foo", "", "bar");
  }

  @Test
  void parseEmpty() {
    String[] args = parseArgs("   ");
    assertThat(args).isNotNull();
    assertThat(args).isEmpty();
  }

  @Test
  void parseDebugFlags() {
    String[] args = parseArgs("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
    assertThat(args).hasSize(2);
    assertThat(args[0]).isEqualTo("-Xdebug");
    assertThat(args[1]).isEqualTo("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
  }

  @Test
  void parseWithExtraSpaces() {
    String[] args = parseArgs("     -Dfoo=bar        -Dfoo2=bar2  ");
    assertThat(args).hasSize(2);
    assertThat(args[0]).isEqualTo("-Dfoo=bar");
    assertThat(args[1]).isEqualTo("-Dfoo2=bar2");
  }

  @Test
  void parseWithNewLinesAndTabs() {
    String[] args = parseArgs("     -Dfoo=bar \n\t\t -Dfoo2=bar2  ");
    assertThat(args).hasSize(2);
    assertThat(args[0]).isEqualTo("-Dfoo=bar");
    assertThat(args[1]).isEqualTo("-Dfoo2=bar2");
  }

  @Test
  void quoteHandledProperly() {
    String[] args = parseArgs("-Dvalue=\"My Value\"    ");
    assertThat(args).hasSize(1);
    assertThat(args[0]).isEqualTo("-Dvalue=My Value");
  }

  private String[] parseArgs(String args) {
    return new RunArguments(args).asArray();
  }

}
