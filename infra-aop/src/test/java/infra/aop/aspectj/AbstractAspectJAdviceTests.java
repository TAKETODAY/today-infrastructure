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

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import infra.aop.AopInvocationException;
import infra.aop.interceptor.ExposeInvocationInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/9 22:37
 */
class AbstractAspectJAdviceTests {

  @Test
  void safePointcutExcludesOnlyItsOwnAdviceMethod() {
    var pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression("execution(* *(..))");
    var instances = new SingletonAspectInstanceFactory(new Sample());
    Method method = getMethod("noArguments");
    var advice = new AspectJMethodBeforeAdvice(method, pointcut, instances);
    advice.setAspectName("sample");
    var advisor = new AspectJPointcutAdvisor(advice);
    var equivalent = new AspectJPointcutAdvisor(advice);
    var matcher = advisor.getPointcut().getMethodMatcher();

    assertThat(advice.getAspectInstanceFactory()).isSameAs(instances);
    assertThat(advice.toString()).contains("noArguments", "aspect name 'sample'");
    assertThat(advisor.equals(advisor)).isTrue();
    assertThat(advisor).isEqualTo(equivalent).hasSameHashCodeAs(equivalent).isNotEqualTo(advice);
    assertThat(AspectJAopUtils.isBeforeAdvice(advisor)).isTrue();
    assertThat(matcher).isEqualTo(equivalent.getPointcut().getMethodMatcher())
            .hasSameHashCodeAs(equivalent.getPointcut().getMethodMatcher());
    assertThat(matcher.matches(method, Sample.class)).isFalse();
    assertThat(matcher.matches(getMethod("methodWithNoJoinPoint"), Sample.class)).isTrue();
  }

  @Test
  void reportsArgumentTypeMismatchWhenInvokingAdviceReflectively() {
    var pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression("execution(* *(..))");
    var advice = new TestAspectJAdvice(getMethod("methodWithNoJoinPoint"), pointcut,
            new SingletonAspectInstanceFactory(new Sample()));

    assertThatExceptionOfType(AopInvocationException.class)
            .isThrownBy(() -> advice.invokeAdviceMethodWithGivenArgs(new Object[] { 42, "second" }))
            .withMessageContaining("Mismatch on arguments to advice method", "methodWithNoJoinPoint")
            .withCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void plainInvocationCannotProvideAnAspectJJoinPoint() throws Throwable {
    var pointcut = new AspectJExpressionPointcut();
    var instances = new SingletonAspectInstanceFactory(new Sample());
    var advice = new AspectJAroundAdvice(getMethod("noArguments"), pointcut, instances);
    MethodInvocation invocation = mock();
    given(invocation.proceed()).willAnswer(call -> {
      assertThatIllegalStateException().isThrownBy(AbstractAspectJAdvice::currentJoinPoint)
              .withMessageContaining("not a Framework ProxyMethodInvocation");
      assertThatIllegalStateException().isThrownBy(advice::getJoinPointMatch)
              .withMessageContaining("not a ProxyMethodInvocation");
      return "done";
    });

    assertThat(advice.isBeforeAdvice()).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> advice.invoke(invocation))
            .withMessageContaining("not a Framework ProxyMethodInvocation");
    assertThat(ExposeInvocationInterceptor.INSTANCE.invoke(invocation)).isEqualTo("done");
  }

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

  @Test
  void onlyAroundAdviceAcceptsProceedingJoinPoint() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithProceedingJoinPoint");

    assertThatIllegalArgumentException().isThrownBy(advice::calculateArgumentBindings)
            .withMessageContaining("only supported for around advice");
  }

  @Test
  void rejectsExplicitArgumentNamesWithWrongArity() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithNoJoinPoint");
    advice.setArgumentNamesFromStringArray("arg1");

    assertThatIllegalStateException().isThrownBy(advice::calculateArgumentBindings)
            .withMessageContaining("Expecting to find 2 arguments", "actually found 1");
  }

  @Test
  void requiresAnActualPointcutMatchToBindPointcutArguments() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithNoJoinPoint");
    JoinPoint joinPoint = mock();

    assertThatIllegalStateException().isThrownBy(() -> advice.argBinding(joinPoint, null, null, null))
            .withMessageContaining("Required to bind 2 arguments, but only bound 0");
  }

  @Test
  void adviceWithoutReturnOrExceptionBindingRejectsThoseNames() {
    AbstractAspectJAdvice advice = getAspectJAdvice("methodWithNoJoinPoint");

    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> advice.setReturningName("result"))
            .withMessageContaining("Only afterReturning advice");
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> advice.setThrowingName("failure"))
            .withMessageContaining("Only afterThrowing advice");
  }

  @Test
  void afterReturningAdviceRequiresAResolvableTypeOrBoundName() {
    var advice = new AspectJAfterReturningAdvice(getMethod("methodWithNoJoinPoint"),
            new AspectJExpressionPointcut(), mock(AspectInstanceFactory.class));
    advice.setArgumentNamesFromStringArray("arg1", "arg2");

    assertThatIllegalArgumentException().isThrownBy(() -> advice.setReturningName("example.MissingType"))
            .withMessageContaining("Returning name 'example.MissingType'");
    advice.setReturningName("result");
    assertThatIllegalStateException().isThrownBy(advice::calculateArgumentBindings)
            .withMessageContaining("Returning argument name 'result' was not bound");
  }

  @Test
  void afterThrowingAdviceRequiresAResolvableTypeOrBoundName() {
    var advice = new AspectJAfterThrowingAdvice(getMethod("methodWithNoJoinPoint"),
            new AspectJExpressionPointcut(), mock(AspectInstanceFactory.class));
    advice.setArgumentNamesFromStringArray("arg1", "arg2");

    assertThatIllegalArgumentException().isThrownBy(() -> advice.setThrowingName("example.MissingType"))
            .withMessageContaining("Throwing name 'example.MissingType'");
    advice.setThrowingName("failure");
    assertThatIllegalStateException().isThrownBy(advice::calculateArgumentBindings)
            .withMessageContaining("Throwing argument name 'failure' was not bound");
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

    void noArguments() {
    }

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
