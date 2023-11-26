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

package cn.taketoday.expression.spel.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.expression.ConstructorResolver;
import cn.taketoday.expression.MethodResolver;
import cn.taketoday.expression.PropertyAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/25 14:04
 */
class StandardEvaluationContextTests {

  private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

  @Test
  void applyDelegatesToSetDelegatesToTarget() {
    StandardEvaluationContext target = new StandardEvaluationContext(evaluationContext);
    assertThat(target).hasFieldOrProperty("reflectiveMethodResolver").isNotNull();
    assertThat(target.getBeanResolver()).isSameAs(this.evaluationContext.getBeanResolver());
    assertThat(target.getTypeLocator()).isSameAs(this.evaluationContext.getTypeLocator());
    assertThat(target.getTypeConverter()).isSameAs(this.evaluationContext.getTypeConverter());
    assertThat(target.getOperatorOverloader()).isSameAs(this.evaluationContext.getOperatorOverloader());
    assertThat(target.getPropertyAccessors()).satisfies(hasSameElements(
            this.evaluationContext.getPropertyAccessors()));
    assertThat(target.getConstructorResolvers()).satisfies(hasSameElements(
            this.evaluationContext.getConstructorResolvers()));
    assertThat(target.getMethodResolvers()).satisfies(hasSameElements(
            this.evaluationContext.getMethodResolvers()));
  }

  @Test
  void applyDelegatesToMakesACopyOfPropertyAccessors() {
    StandardEvaluationContext target = new StandardEvaluationContext(evaluationContext);
    PropertyAccessor propertyAccessor = mock(PropertyAccessor.class);
    this.evaluationContext.getPropertyAccessors().add(propertyAccessor);
    assertThat(target.getPropertyAccessors()).doesNotContain(propertyAccessor);
  }

  @Test
  void applyDelegatesToMakesACopyOfConstructorResolvers() {
    StandardEvaluationContext target = new StandardEvaluationContext(evaluationContext);
    ConstructorResolver methodResolver = mock(ConstructorResolver.class);
    this.evaluationContext.getConstructorResolvers().add(methodResolver);
    assertThat(target.getConstructorResolvers()).doesNotContain(methodResolver);
  }

  @Test
  void applyDelegatesToMakesACopyOfMethodResolvers() {
    StandardEvaluationContext target = new StandardEvaluationContext(evaluationContext);
    MethodResolver methodResolver = mock(MethodResolver.class);
    this.evaluationContext.getMethodResolvers().add(methodResolver);
    assertThat(target.getMethodResolvers()).doesNotContain(methodResolver);
  }

  private Consumer<List<?>> hasSameElements(List<?> candidates) {
    return actual -> {
      assertThat(actual.size()).isEqualTo(candidates.size());
      for (int i = 0; i < candidates.size(); i++) {
        assertThat(candidates.get(i)).isSameAs(actual.get(i));
      }
    };
  }

}