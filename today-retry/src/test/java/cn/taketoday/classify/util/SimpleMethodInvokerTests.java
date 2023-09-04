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

package cn.taketoday.classify.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/9/4 10:25
 */
class SimpleMethodInvokerTests {

  @Test
  void construct() {
    assertThatThrownBy(() -> new SimpleMethodInvoker(null, "method"))
            .hasMessage("Object to invoke is required");

    SimpleMethodInvoker invoker = new SimpleMethodInvoker(this, "method");

    assertThatThrownBy(() -> new SimpleMethodInvoker(this, "privatemethod"))
            .hasMessage("No methods found for name: [privatemethod] in class: ["
                    + SimpleMethodInvokerTests.class + "] with arguments of type: [[]]");

    Object o = invoker.invokeMethod();
    assertThat(o).isEqualTo("ok");
  }

  public String method() {
    return "ok";
  }

  private String privatemethod() {
    return "ok";
  }

}