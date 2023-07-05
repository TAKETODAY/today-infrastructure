/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.infra.maven;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/10 21:21
 */
class EnvVariablesTests {

  @Test
  void asNull() {
    Map<String, String> args = new EnvVariables(null).asMap();
    assertThat(args).isEmpty();
  }

  @Test
  void asArray() {
    assertThat(new EnvVariables(getTestArgs()).asArray()).contains("key=My Value", "key1= tt ", "key2=   ",
            "key3=");
  }

  @Test
  void asMap() {
    assertThat(new EnvVariables(getTestArgs()).asMap()).containsExactly(entry("key", "My Value"),
            entry("key1", " tt "), entry("key2", "   "), entry("key3", ""));
  }

  private Map<String, String> getTestArgs() {
    Map<String, String> args = new LinkedHashMap<>();
    args.put("key", "My Value");
    args.put("key1", " tt ");
    args.put("key2", "   ");
    args.put("key3", null);
    return args;
  }

}