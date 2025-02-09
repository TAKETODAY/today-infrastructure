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

package infra.aop.aspectj;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/9 22:37
 */
class AbstractAspectJAdviceTests {

  @Test
  void setArgumentNamesFromStringArray_withoutJoinPointParameter() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithNoJoinPoint");
    assertThat(advice).satisfies(hasArgumentNames("arg1", "arg2"));
  }

  @Test
  void setArgumentNamesFromStringArray_withJoinPointAsFirstParameter() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsFirstParameter");
    assertThat(advice).satisfies(hasArgumentNames("THIS_JOIN_POINT", "arg1", "arg2"));
  }

  @Test
  void setArgumentNamesFromStringArray_withJoinPointAsLastParameter() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsLastParameter");
    assertThat(advice).satisfies(hasArgumentNames("arg1", "arg2", "THIS_JOIN_POINT"));
  }

  @Test
  void setArgumentNamesFromStringArray_withJoinPointAsMiddleParameter() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithJoinPointAsMiddleParameter");
    assertThat(advice).satisfies(hasArgumentNames("arg1", "THIS_JOIN_POINT", "arg2"));
  }

  @Test
  void setArgumentNamesFromStringArray_withProceedingJoinPoint() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithProceedingJoinPoint");
    assertThat(advice).satisfies(hasArgumentNames("THIS_JOIN_POINT", "arg1", "arg2"));
  }

  @Test
  void setArgumentNamesFromStringArray_withStaticPart() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithStaticPart");
    assertThat(advice).satisfies(hasArgumentNames("THIS_JOIN_POINT", "arg1", "arg2"));
  }

  private Consumer<AbstractAspectJAdvice> hasArgumentNames(String... argumentNames) {
    return advice -> assertThat(advice).extracting("argumentNames")
            .asInstanceOf(InstanceOfAssertFactories.array(String[].class))
            .containsExactly(argumentNames);
  }

  private AbstractAspectJAdvice getAspectJAdvice(final String methodName) {
    AbstractAspectJAdvice advice = new TestAspectJAdvice(getMethod(methodName),
            mock(AspectJExpressionPointcut.class), mock(AspectInstanceFactory.class));
    advice.setArgumentNamesFromStringArray("arg1", "arg2");
    return advice;
  }

  private Method getMethod(final String methodName) {
    return Arrays.stream(Sample.class.getDeclaredMethods())
            .filter(method -> method.getName().equals(methodName)).findFirst()
            .orElseThrow();
  }

  @SuppressWarnings("serial")
  public static class TestAspectJAdvice extends AbstractAspectJAdvice {

    public TestAspectJAdvice(Method aspectJAdviceMethod, AspectJExpressionPointcut pointcut,
            AspectInstanceFactory aspectInstanceFactory) {
      super(aspectJAdviceMethod, pointcut, aspectInstanceFactory);
    }

    @Override
    public boolean isBeforeAdvice() {
      return false;
    }

    @Override
    public boolean isAfterAdvice() {
      return false;
    }
  }

  @SuppressWarnings("unused")
  static class Sample {

    void methodWithNoJoinPoint(String arg1, String arg2) {
    }

    void methodWithJoinPointAsFirstParameter(JoinPoint joinPoint, String arg1, String arg2) {
    }

    void methodWithJoinPointAsLastParameter(String arg1, String arg2, JoinPoint joinPoint) {
    }

    void methodWithJoinPointAsMiddleParameter(String arg1, JoinPoint joinPoint, String arg2) {
    }

    void methodWithProceedingJoinPoint(ProceedingJoinPoint joinPoint, String arg1, String arg2) {
    }

    void methodWithStaticPart(JoinPoint.StaticPart staticPart, String arg1, String arg2) {
    }
  }

}