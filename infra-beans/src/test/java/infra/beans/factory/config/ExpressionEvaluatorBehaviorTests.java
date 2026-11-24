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

package infra.beans.factory.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ExpressionEvaluatorBehaviorTests {

  @Test
  void evaluatesEmbeddedValuesWithAndWithoutResolver() {
    StandardBeanFactory factory = new StandardBeanFactory();
    ExpressionEvaluator evaluator = new ExpressionEvaluator(factory);

    assertThat(evaluator.evaluate("plain")).isEqualTo("plain");
    assertThat(evaluator.evaluate((String) null)).isNull();
    assertThat(evaluator.evaluate("42", Integer.class)).isEqualTo(42);

    factory.addEmbeddedValueResolver(value -> "resolved:" + value);
    factory.setBeanExpressionResolver((expression, context) -> expression.toUpperCase());
    assertThat(evaluator.evaluate("value")).isEqualTo("RESOLVED:VALUE");
    assertThat(evaluator.evaluate("value", String.class)).isEqualTo("RESOLVED:VALUE");
  }

  @Test
  void createsAndCachesFactoryEvaluator() {
    StandardBeanFactory factory = new StandardBeanFactory();

    ExpressionEvaluator first = ExpressionEvaluator.from(factory);
    ExpressionEvaluator second = ExpressionEvaluator.from(factory);

    assertThat(first).isSameAs(second);
    assertThat(factory.getBean(ExpressionEvaluator.BEAN_NAME)).isSameAs(first);
  }

  @Test
  void expressionContextResolvesBeansAndScopeObjects() {
    StandardBeanFactory factory = new StandardBeanFactory();
    Object bean = new Object();
    factory.registerSingleton("bean", bean);
    Scope scope = new CustomScopeConfigurerBehaviorTests.RecordingScope();
    BeanExpressionContext context = new BeanExpressionContext(factory, scope);

    assertThat(context.containsObject("bean")).isTrue();
    assertThat(context.getObject("bean")).isSameAs(bean);
    assertThat(context.containsObject("unknown")).isFalse();
    assertThat(context.getObject("unknown")).isNull();
    assertThat(context).isEqualTo(new BeanExpressionContext(factory, scope));
    assertThat(context).isNotEqualTo(new BeanExpressionContext(new StandardBeanFactory(), scope));
    assertThat(context.hashCode()).isEqualTo(factory.hashCode());
  }

}
