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