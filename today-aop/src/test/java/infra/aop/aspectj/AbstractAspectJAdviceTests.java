/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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