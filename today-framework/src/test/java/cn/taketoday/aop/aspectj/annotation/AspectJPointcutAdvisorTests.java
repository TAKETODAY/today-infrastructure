/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.aop.aspectj.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.aspectj.AspectJExpressionPointcut;
import cn.taketoday.aop.aspectj.AspectJExpressionPointcutTests;
import cn.taketoday.aop.aspectj.annotation.ReflectiveAspectJAdvisorFactoryTests.ExceptionThrowingAspect;
import cn.taketoday.aop.framework.AopConfigException;
import cn.taketoday.beans.testfixture.beans.TestBean;
import test.aop.PerTargetAspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AspectJPointcutAdvisorTests {

  private final AspectJAdvisorFactory af = new ReflectiveAspectJAdvisorFactory();

  @Test
  public void testSingleton() throws SecurityException, NoSuchMethodException {
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(AspectJExpressionPointcutTests.MATCH_ALL_METHODS);

    InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(
            ajexp, TestBean.class.getMethod("getAge"), af,
            new SingletonMetadataAwareAspectInstanceFactory(new ExceptionThrowingAspect(null), "someBean"),
            1, "someBean");

    assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isSameAs(Pointcut.TRUE);
    assertThat(ajpa.isPerInstance()).isFalse();
  }

  @Test
  public void testPerTarget() throws SecurityException, NoSuchMethodException {
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(AspectJExpressionPointcutTests.MATCH_ALL_METHODS);

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
  public void testPerCflowTarget() {
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
            testIllegalInstantiationModel(ReflectiveAspectJAdvisorFactoryTests.PerCflowAspect.class));
  }

  @Test
  public void testPerCflowBelowTarget() {
    assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
            testIllegalInstantiationModel(ReflectiveAspectJAdvisorFactoryTests.PerCflowBelowAspect.class));
  }

  private void testIllegalInstantiationModel(Class<?> c) throws AopConfigException {
    new AspectMetadata(c, "someBean");
  }

}
