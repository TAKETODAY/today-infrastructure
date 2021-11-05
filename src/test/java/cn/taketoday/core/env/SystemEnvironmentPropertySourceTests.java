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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SystemEnvironmentPropertySource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class SystemEnvironmentPropertySourceTests {

  private Map<String, Object> envMap;

  private PropertySource<?> ps;

  @BeforeEach
  void setUp() {
    envMap = new HashMap<>();
    ps = new SystemEnvironmentPropertySource("sysEnv", envMap);
  }

  @Test
  void none() {
    assertThat(ps.containsProperty("a.key")).isEqualTo(false);
    assertThat(ps.getProperty("a.key")).isNull();
  }

  @Test
  void normalWithoutPeriod() {
    envMap.put("akey", "avalue");

    assertThat(ps.containsProperty("akey")).isEqualTo(true);
    assertThat(ps.getProperty("akey")).isEqualTo("avalue");
  }

  @Test
  void normalWithPeriod() {
    envMap.put("a.key", "a.value");

    assertThat(ps.containsProperty("a.key")).isEqualTo(true);
    assertThat(ps.getProperty("a.key")).isEqualTo("a.value");
  }

  @Test
  void withUnderscore() {
    envMap.put("a_key", "a_value");

    assertThat(ps.containsProperty("a_key")).isEqualTo(true);
    assertThat(ps.containsProperty("a.key")).isEqualTo(true);

    assertThat(ps.getProperty("a_key")).isEqualTo("a_value");
    assertThat(ps.getProperty("a.key")).isEqualTo("a_value");
  }

  @Test
  void withBothPeriodAndUnderscore() {
    envMap.put("a_key", "a_value");
    envMap.put("a.key", "a.value");

    assertThat(ps.getProperty("a_key")).isEqualTo("a_value");
    assertThat(ps.getProperty("a.key")).isEqualTo("a.value");
  }

  @Test
  void withUppercase() {
    envMap.put("A_KEY", "a_value");
    envMap.put("A_LONG_KEY", "a_long_value");
    envMap.put("A_DOT.KEY", "a_dot_value");
    envMap.put("A_HYPHEN-KEY", "a_hyphen_value");

    assertThat(ps.containsProperty("A_KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("a_key")).isEqualTo(true);
    assertThat(ps.containsProperty("a.key")).isEqualTo(true);
    assertThat(ps.containsProperty("a-key")).isEqualTo(true);
    assertThat(ps.containsProperty("A_LONG_KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A.LONG.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A-LONG-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A.LONG-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A-LONG.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A_long_KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A.long.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A-long-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A.long-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A-long.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A_DOT.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A-DOT.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A_dot.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A-dot.KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A_HYPHEN-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A.HYPHEN-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A_hyphen-KEY")).isEqualTo(true);
    assertThat(ps.containsProperty("A.hyphen-KEY")).isEqualTo(true);

    assertThat(ps.getProperty("A_KEY")).isEqualTo("a_value");
    assertThat(ps.getProperty("A.KEY")).isEqualTo("a_value");
    assertThat(ps.getProperty("A-KEY")).isEqualTo("a_value");
    assertThat(ps.getProperty("a_key")).isEqualTo("a_value");
    assertThat(ps.getProperty("a.key")).isEqualTo("a_value");
    assertThat(ps.getProperty("a-key")).isEqualTo("a_value");
    assertThat(ps.getProperty("A_LONG_KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A.LONG.KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A-LONG-KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A.LONG-KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A-LONG.KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A_long_KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A.long.KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A-long-KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A.long-KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A-long.KEY")).isEqualTo("a_long_value");
    assertThat(ps.getProperty("A_DOT.KEY")).isEqualTo("a_dot_value");
    assertThat(ps.getProperty("A-DOT.KEY")).isEqualTo("a_dot_value");
    assertThat(ps.getProperty("A_dot.KEY")).isEqualTo("a_dot_value");
    assertThat(ps.getProperty("A-dot.KEY")).isEqualTo("a_dot_value");
    assertThat(ps.getProperty("A_HYPHEN-KEY")).isEqualTo("a_hyphen_value");
    assertThat(ps.getProperty("A.HYPHEN-KEY")).isEqualTo("a_hyphen_value");
    assertThat(ps.getProperty("A_hyphen-KEY")).isEqualTo("a_hyphen_value");
    assertThat(ps.getProperty("A.hyphen-KEY")).isEqualTo("a_hyphen_value");
  }

}
