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

package infra.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.junit.jupiter.api.Test;

import infra.aop.Pointcut;
import infra.aop.aspectj.AspectJExpressionPointcut;
import infra.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactoryTests.ExceptionThrowingAspect;
import infra.aop.testfixture.PerTargetAspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
class AspectMetadataTests {

  @Test
  void notAnAspect() {
    assertThatIllegalArgumentException().isThrownBy(() -> new AspectMetadata(String.class, "someBean"));
  }

  @Test
  void singletonAspect() {
    AspectMetadata am = new AspectMetadata(ExceptionThrowingAspect.class, "someBean");
    assertThat(am.isPerThisOrPerTarget()).isFalse();
    assertThat(am.getPerClausePointcut()).isSameAs(Pointcut.TRUE);
    assertThat(am.getAjType().getPerClause().getKind()).isEqualTo(PerClauseKind.SINGLETON);
  }

  @Test
  void perTargetAspect() {
    AspectMetadata am = new AspectMetadata(PerTargetAspect.class, "someBean");
    assertThat(am.isPerThisOrPerTarget()).isTrue();
    assertThat(am.getPerClausePointcut()).isNotSameAs(Pointcut.TRUE);
    assertThat(am.getAjType().getPerClause().getKind()).isEqualTo(PerClauseKind.PERTARGET);
    assertThat(am.getPerClausePointcut()).isInstanceOf(AspectJExpressionPointcut.class);
    assertThat(((AspectJExpressionPointcut) am.getPerClausePointcut()).getExpression())
            .isEqualTo("execution(* *.getSpouse())");
  }

  @Test
  void perThisAspect() {
    AspectMetadata am = new AspectMetadata(PerThisAspect.class, "someBean");
    assertThat(am.isPerThisOrPerTarget()).isTrue();
    assertThat(am.getPerClausePointcut()).isNotSameAs(Pointcut.TRUE);
    assertThat(am.getAjType().getPerClause().getKind()).isEqualTo(PerClauseKind.PERTHIS);
    assertThat(am.getPerClausePointcut()).isInstanceOf(AspectJExpressionPointcut.class);
    assertThat(((AspectJExpressionPointcut) am.getPerClausePointcut()).getExpression())
            .isEqualTo("execution(* *.getSpouse())");
  }

}
