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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SimpleCommandLinePropertySource}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 */
class SimpleCommandLinePropertySourceTests {

  @Test
  void withDefaultName() {
    PropertySource<?> ps = new SimpleCommandLinePropertySource();
    assertThat(ps.getName()).isEqualTo(CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME);
  }

  @Test
  void withCustomName() {
    PropertySource<?> ps = new SimpleCommandLinePropertySource("ps1", new String[0]);
    assertThat(ps.getName()).isEqualTo("ps1");
  }

  @Test
  void withNoArgs() {
    PropertySource<?> ps = new SimpleCommandLinePropertySource();
    assertThat(ps.containsProperty("foo")).isFalse();
    assertThat(ps.getProperty("foo")).isNull();
  }

  @Test
  void withOptionArgsOnly() {
    CommandLinePropertySource<?> ps = new SimpleCommandLinePropertySource("--o1=v1", "--o2");
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isTrue();
    assertThat(ps.containsProperty("o3")).isFalse();
    assertThat(ps.getProperty("o1")).isEqualTo("v1");
    assertThat(ps.getProperty("o2")).isEqualTo("");
    assertThat(ps.getProperty("o3")).isNull();
  }

  @Test
    // gh-24464
  void withOptionalArg_andArgIsEmpty() {
    EnumerablePropertySource<?> ps = new SimpleCommandLinePropertySource("--foo=");

    assertThat(ps.containsProperty("foo")).isTrue();
    assertThat(ps.getProperty("foo")).isEqualTo("");
  }

  @Test
  void withDefaultNonOptionArgsNameAndNoNonOptionArgsPresent() {
    EnumerablePropertySource<?> ps = new SimpleCommandLinePropertySource("--o1=v1", "--o2");

    assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isTrue();

    assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
    assertThat(ps.getProperty("nonOptionArgs")).isNull();
    assertThat(ps.getPropertyNames()).hasSize(2);
  }

  @Test
  void withDefaultNonOptionArgsNameAndNonOptionArgsPresent() {
    CommandLinePropertySource<?> ps = new SimpleCommandLinePropertySource("--o1=v1", "noa1", "--o2", "noa2");

    assertThat(ps.containsProperty("nonOptionArgs")).isTrue();
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isTrue();

    String nonOptionArgs = ps.getProperty("nonOptionArgs");
    assertThat(nonOptionArgs).isEqualTo("noa1,noa2");
  }

  @Test
  void withCustomNonOptionArgsNameAndNoNonOptionArgsPresent() {
    CommandLinePropertySource<?> ps = new SimpleCommandLinePropertySource("--o1=v1", "noa1", "--o2", "noa2");
    ps.setNonOptionArgsPropertyName("NOA");

    assertThat(ps.containsProperty("nonOptionArgs")).isFalse();
    assertThat(ps.containsProperty("NOA")).isTrue();
    assertThat(ps.containsProperty("o1")).isTrue();
    assertThat(ps.containsProperty("o2")).isTrue();
    String nonOptionArgs = ps.getProperty("NOA");
    assertThat(nonOptionArgs).isEqualTo("noa1,noa2");
  }

  @Test
  void covertNonOptionArgsToStringArrayAndList() {
    CommandLinePropertySource<?> ps = new SimpleCommandLinePropertySource("--o1=v1", "noa1", "--o2", "noa2");
    StandardEnvironment env = new StandardEnvironment();
    env.getPropertySources().addFirst(ps);

    String nonOptionArgs = env.getProperty("nonOptionArgs");
    assertThat(nonOptionArgs).isEqualTo("noa1,noa2");

    String[] nonOptionArgsArray = env.getProperty("nonOptionArgs", String[].class);
    assertThat(nonOptionArgsArray[0]).isEqualTo("noa1");
    assertThat(nonOptionArgsArray[1]).isEqualTo("noa2");

    @SuppressWarnings("unchecked")
    List<String> nonOptionArgsList = env.getProperty("nonOptionArgs", List.class);
    assertThat(nonOptionArgsList.get(0)).isEqualTo("noa1");
    assertThat(nonOptionArgsList.get(1)).isEqualTo("noa2");
  }

}
