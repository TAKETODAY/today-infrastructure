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

package infra.transaction.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.transaction.interceptor.NoRollbackRuleAttribute;
import infra.transaction.interceptor.RollbackRuleAttribute;
import infra.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:13
 */
class TxAdviceBeanDefinitionParserTests {

  @Test
  void getBeanClassReturnsTransactionInterceptor() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    assertThatNoException().isThrownBy(() -> {
      Class<?> beanClass = parser.getBeanClass(null);
      assertThat(beanClass).isEqualTo(TransactionInterceptor.class);
    });
  }

  @Test
  void addRollbackRuleAttributesToCreatesRollbackRules() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    assertThatNoException().isThrownBy(() -> {
      parser.addRollbackRuleAttributesTo(rollbackRules, "java.lang.Exception,java.lang.RuntimeException");
      assertThat(rollbackRules).hasSize(2);
      assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Exception");
      assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.RuntimeException");
    });
  }

  @Test
  void addNoRollbackRuleAttributesToCreatesNoRollbackRules() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    assertThatNoException().isThrownBy(() -> {
      parser.addNoRollbackRuleAttributesTo(rollbackRules, "java.lang.Error,java.lang.IllegalStateException");
      assertThat(rollbackRules).hasSize(2);
      assertThat(rollbackRules.get(0)).isInstanceOf(NoRollbackRuleAttribute.class);
      assertThat(rollbackRules.get(1)).isInstanceOf(NoRollbackRuleAttribute.class);
      assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Error");
      assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.IllegalStateException");
    });
  }

  @Test
  void addRollbackRuleAttributesToStripsWhitespace() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    parser.addRollbackRuleAttributesTo(rollbackRules, "java.lang.Exception , java.lang.RuntimeException ");
    assertThat(rollbackRules).hasSize(2);
    assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Exception");
    assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.RuntimeException");
  }

  @Test
  void addNoRollbackRuleAttributesToStripsWhitespace() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    parser.addNoRollbackRuleAttributesTo(rollbackRules, "java.lang.Error , java.lang.IllegalStateException ");
    assertThat(rollbackRules).hasSize(2);
    assertThat(rollbackRules.get(0)).isInstanceOf(NoRollbackRuleAttribute.class);
    assertThat(rollbackRules.get(1)).isInstanceOf(NoRollbackRuleAttribute.class);
    assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Error");
    assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.IllegalStateException");
  }

}