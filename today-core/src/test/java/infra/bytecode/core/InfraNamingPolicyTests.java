/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import infra.bytecode.reflect.MethodAccess;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/7 20:56
 */
class InfraNamingPolicyTests {

  private final Set<String> reservedClassNames = new HashSet<>();

  @Test
  void nullPrefix() {
    assertThat(getClassName(null)).isEqualTo("infra.bytecode.Object$$Infra$$0");
    assertThat(getClassName(null)).isEqualTo("infra.bytecode.Object$$Infra$$1");
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
  void prefixContainingInfraLabel() {
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