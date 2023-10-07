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

package cn.taketoday.bytecode.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.bytecode.reflect.MethodAccess;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/7 20:56
 */
class InfraNamingPolicyTests {

  private final Set<String> reservedClassNames = new HashSet<>();

  @Test
  void nullPrefix() {
    assertThat(getClassName(null)).isEqualTo("cn.taketoday.bytecode.Object$$Infra$$0");
    assertThat(getClassName(null)).isEqualTo("cn.taketoday.bytecode.Object$$Infra$$1");
  }

  @Test
  void javaPrefix() {
    assertThat(getClassName("java.util.ArrayList")).isEqualTo("_java.util.ArrayList$$Infra$$0");
    assertThat(getClassName("java.util.ArrayList")).isEqualTo("_java.util.ArrayList$$Infra$$1");
  }

  @Test
  void javaxPrefix() {
    assertThat(getClassName("javax.sql.RowSet")).isEqualTo("_javax.sql.RowSet$$Infra$$0");
    assertThat(getClassName("javax.sql.RowSet")).isEqualTo("_javax.sql.RowSet$$Infra$$1");
  }

  @Test
  void examplePrefix() {
    assertThat(getClassName("example.MyComponent")).isEqualTo("example.MyComponent$$Infra$$0");
    assertThat(getClassName("example.MyComponent")).isEqualTo("example.MyComponent$$Infra$$1");
  }

  @Test
  void prefixContainingSpringLabel() {
    String generated1 = "example.MyComponent$$Infra$$0";
    String generated2 = "example.MyComponent$$Infra$$1";

    assertThat(getClassName(generated1)).isEqualTo(generated1);
    assertThat(getClassName(generated1)).isEqualTo(generated2);
  }

  @Test
  void methodAccess() {
    String prefix = "example.MyComponent";
    String source = MethodAccess.class.getName();
    assertThat(getClassName(prefix, "a.b.c", null)).isEqualTo("example.MyComponent$$Infra$$0");
    assertThat(getClassName(prefix, source, null)).isEqualTo("example.MyComponent$$Infra$$MethodAccess$$0");
    assertThat(getClassName(prefix, source, null)).isEqualTo("example.MyComponent$$Infra$$MethodAccess$$1");
  }

  private String getClassName(String prefix) {
    return getClassName(prefix, null, null);
  }

  private String getClassName(String prefix, String source, Object key) {
    String className = NamingPolicy.forInfrastructure().getClassName(prefix, source, key, reservedClassNames::contains);
    reservedClassNames.add(className);
    return className;
  }

}