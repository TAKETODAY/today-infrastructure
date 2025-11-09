/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.interceptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 22:35
 */
class NoRollbackRuleAttributeTests {

  @Test
  void constructorWithClass() {
    NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(RuntimeException.class);
    assertThat(rule).isNotNull();
    assertThat(rule.getExceptionName()).isEqualTo(RuntimeException.class.getName());
  }

  @Test
  void constructorWithString() {
    String exceptionName = "java.lang.RuntimeException";
    NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(exceptionName);
    assertThat(rule).isNotNull();
    assertThat(rule.getExceptionName()).isEqualTo(exceptionName);
  }

  @Test
  void toStringReturnsNoPlusSuperToString() {
    NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(RuntimeException.class);
    assertThat(rule.toString()).startsWith("No");
    assertThat(rule.toString()).contains(RuntimeException.class.getName());
  }

}