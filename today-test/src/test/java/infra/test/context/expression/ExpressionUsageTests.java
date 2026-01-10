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

package infra.test.context.expression;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Clement
 * @author Dave Syer
 */
@JUnitConfig
class ExpressionUsageTests {

  @Autowired
  @Qualifier("derived")
  private Properties props;

  @Autowired
  @Qualifier("andy2")
  private Foo andy2;

  @Autowired
  @Qualifier("andy")
  private Foo andy;

  @Test
  void testSpr5906() throws Exception {
    // verify the property values have been evaluated as expressions
    assertThat(props.getProperty("user.name")).isEqualTo("Dave");
    assertThat(props.getProperty("username")).isEqualTo("Andy");

    // verify the property keys have been evaluated as expressions
    assertThat(props.getProperty("Dave")).isEqualTo("exists");
    assertThat(props.getProperty("Andy")).isEqualTo("exists also");
  }

  @Test
  void testSpr5847() throws Exception {
    assertThat(andy2.getName()).isEqualTo("Andy");
    assertThat(andy.getName()).isEqualTo("Andy");
  }

  public static class Foo {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}
