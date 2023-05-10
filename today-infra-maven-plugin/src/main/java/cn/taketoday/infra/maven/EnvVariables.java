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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for working with Env variables.
 *
 * @author Dmytro Nosan
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class EnvVariables {

  private final Map<String, String> variables;

  EnvVariables(Map<String, String> variables) {
    this.variables = parseEnvVariables(variables);
  }

  private static Map<String, String> parseEnvVariables(Map<String, String> args) {
    if (args == null || args.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<String, String> e : args.entrySet()) {
      if (e.getKey() != null) {
        result.put(e.getKey(), getValue(e.getValue()));
      }
    }
    return result;
  }

  private static String getValue(String value) {
    return (value != null) ? value : "";
  }

  Map<String, String> asMap() {
    return Collections.unmodifiableMap(this.variables);
  }

  String[] asArray() {
    List<String> args = new ArrayList<>(this.variables.size());
    for (Map.Entry<String, String> arg : this.variables.entrySet()) {
      args.add(arg.getKey() + "=" + arg.getValue());
    }
    return args.toArray(new String[0]);
  }

}
