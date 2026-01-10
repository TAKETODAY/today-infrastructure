/*
 * Copyright 2017 - 2026 the TODAY authors.
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