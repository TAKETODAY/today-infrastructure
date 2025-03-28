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

import org.junit.jupiter.api.Test;

import infra.aop.Pointcut;
import infra.aop.aspectj.AspectJExpressionPointcut;
import infra.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactoryTests.ExceptionThrowingAspect;
import infra.aop.framework.AopConfigException;
import infra.aop.testfixture.PerTargetAspect;
import infra.aop.testfixture.aspectj.CommonExpressions;
import infra.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AspectJPointcutAdvisorTests {

  private final AspectJAdvisorFactory af = new ReflectiveAspectJAdvisorFactory();

  @Test
  void testSingleton() throws SecurityException, NoSuchMethodException {
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(CommonExpressions.MATCH_ALL_METHODS);

    InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(
            ajexp, TestBean.class.getMethod("getAge"), af,
            new SingletonMetadataAwareAspectInstanceFactory(new ExceptionThrowingAspect(null), "someBean"),
            1, "someBean");

    assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isSameAs(Pointcut.TRUE);
    assertThat(ajpa.isPerInstance()).isFalse();
  }

  @Test
  void testPerTarget() throws SecurityException, NoSuchMethodException {
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(CommonExpressions.MATCH_ALL_METHODS);

    InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(
            ajexp, TestBean.class.getMethod("getAge"), af,
            new SingletonMetadataAwareAspectInstanceFactory(new PerTargetAspect(), "someBean"),
            1, "someBean");

    assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isNotSameAs(Pointcut.TRUE);
    boolean condition = ajpa.getAspectMetadata().getPerClausePointcut() instanceof AspectJExpressionPointcut;
    assertThat(condition).isTrue();
    assertThat(ajpa.isPerInstance()).isTrue();

    assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getClassFilter().matches(TestBean.class)).isTrue();
    assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
            TestBean.class.getMethod("getAge"), TestBean.class)).isFalse();

    assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
            TestBean.class.getMethod("getSpouse"), TestBean.class)).isTrue();
  }

  @Test
  void testPerCflowTarget() {
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
            testIllegalInstantiationModel(ReflectiveAspectJAdvisorFactoryTests.PerCflowAspect.class));
  }

  @Test
  void testPerCflowBelowTarget() {
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
            testIllegalInstantiationModel(ReflectiveAspectJAdvisorFactoryTests.PerCflowBelowAspect.class));
  }

  private void testIllegalInstantiationModel(Class<?> c) throws AopConfigException {
    new AspectMetadata(c, "someBean");
  }

}
