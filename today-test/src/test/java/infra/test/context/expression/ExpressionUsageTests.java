/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

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
