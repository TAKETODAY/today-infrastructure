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
import org.aspectj.lang.JoinPoint.StaticPart;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.reflect.Factory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import infra.aop.MethodBeforeAdvice;
import infra.aop.framework.AopContext;
import infra.aop.framework.ProxyFactory;
import infra.aop.interceptor.ExposeInvocationInterceptor;
import infra.aop.support.AopUtils;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @author Ramnivas Laddad
 * @since 2.0
 */
public class MethodInvocationProceedingJoinPointTests {

  @Test
  public void testingBindingWithJoinPoint() {
    assertThatIllegalStateException().isThrownBy(AbstractAspectJAdvice::currentJoinPoint);
  }

  @Test
  public void testingBindingWithProceedingJoinPoint() {
    assertThatIllegalStateException().isThrownBy(AbstractAspectJAdvice::currentJoinPoint);
  }

  @Test
  public void testCanGetMethodSignatureFromJoinPoint() {
    final Object raw = new TestBean();
    // Will be set by advice during a method call
    final int newAge = 23;

    ProxyFactory pf = new ProxyFactory(raw);
    pf.setExposeProxy(true);
    pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
    AtomicInteger depth = new AtomicInteger();
    pf.addAdvice((MethodBeforeAdvice) (method) -> {
      JoinPoint jp = AbstractAspectJAdvice.currentJoinPoint();
      assertThat(jp.toString().contains(method.getMethod().getName())).as("Method named in toString").isTrue();
      // Ensure that these don't cause problems
      jp.toShortString();
      jp.toLongString();

      assertThat(AbstractAspectJAdvice.currentJoinPoint().getTarget()).isSameAs(method.getThis());
      assertThat(AopUtils.isAopProxy(AbstractAspectJAdvice.currentJoinPoint().getTarget())).isFalse();

      ITestBean thisProxy = (ITestBean) AbstractAspectJAdvice.currentJoinPoint().getThis();
      assertThat(AopUtils.isAopProxy(AbstractAspectJAdvice.currentJoinPoint().getThis())).isTrue();

      assertThat(thisProxy).isNotSameAs(method.getThis());

      // Check getting again doesn't cause a problem
      assertThat(AbstractAspectJAdvice.currentJoinPoint().getThis()).isSameAs(thisProxy);

      // Try reentrant call--will go through this advice.
      // Be sure to increment depth to avoid infinite recursion
      if (depth.getAndIncrement() == 0) {
        // Check that toString doesn't cause a problem
        thisProxy.toString();
        // Change age, so this will be returned by invocation
        thisProxy.setAge(newAge);
        assertThat(thisProxy.getAge()).isEqualTo(newAge);
      }

      assertThat(thisProxy).isSameAs(AopContext.currentProxy());
      assertThat(raw).isSameAs(method.getThis());

      assertThat(AbstractAspectJAdvice.currentJoinPoint().getSignature().getName()).isSameAs(method.getMethod().getName());
      assertThat(AbstractAspectJAdvice.currentJoinPoint().getSignature().getModifiers()).isEqualTo(method.getMethod().getModifiers());

      MethodSignature msig = (MethodSignature) AbstractAspectJAdvice.currentJoinPoint().getSignature();
      assertThat(AbstractAspectJAdvice.currentJoinPoint().getSignature()).as("Return same MethodSignature repeatedly").isSameAs(msig);
      assertThat(AbstractAspectJAdvice.currentJoinPoint()).as("Return same JoinPoint repeatedly").isSameAs(AbstractAspectJAdvice.currentJoinPoint());
      assertThat(msig.getDeclaringType()).isEqualTo(method.getMethod().getDeclaringClass());
      assertThat(Arrays.equals(method.getMethod().getParameterTypes(), msig.getParameterTypes())).isTrue();
      assertThat(msig.getReturnType()).isEqualTo(method.getMethod().getReturnType());
      assertThat(Arrays.equals(method.getMethod().getExceptionTypes(), msig.getExceptionTypes())).isTrue();
      msig.toLongString();
      msig.toShortString();
    });
    ITestBean itb = (ITestBean) pf.getProxy();
    // Any call will do
    assertThat(itb.getAge()).as("Advice reentrantly set age").isEqualTo(newAge);
  }

  @Test
  public void testCanGetSourceLocationFromJoinPoint() {
    final Object raw = new TestBean();
    ProxyFactory pf = new ProxyFactory(raw);
    pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
    pf.addAdvice((MethodBeforeAdvice) (method) -> {
      SourceLocation sloc = AbstractAspectJAdvice.currentJoinPoint().getSourceLocation();
      assertThat(AbstractAspectJAdvice.currentJoinPoint().getSourceLocation()).as("Same source location must be returned on subsequent requests").isEqualTo(sloc);
      assertThat(sloc.getWithinType()).isEqualTo(TestBean.class);
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(sloc::getLine);
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(sloc::getFileName);
    });
    ITestBean itb = (ITestBean) pf.getProxy();
    // Any call will do
    itb.getAge();
  }

  @Test
  public void testCanGetStaticPartFromJoinPoint() {
    final Object raw = new TestBean();
    ProxyFactory pf = new ProxyFactory(raw);
    pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
    pf.addAdvice((MethodBeforeAdvice) (method) -> {
      StaticPart staticPart = AbstractAspectJAdvice.currentJoinPoint().getStaticPart();
      assertThat(AbstractAspectJAdvice.currentJoinPoint().getStaticPart()).as("Same static part must be returned on subsequent requests").isEqualTo(staticPart);
      assertThat(staticPart.getKind()).isEqualTo(ProceedingJoinPoint.METHOD_EXECUTION);
      assertThat(staticPart.getSignature()).isSameAs(AbstractAspectJAdvice.currentJoinPoint().getSignature());
      assertThat(staticPart.getSourceLocation()).isEqualTo(AbstractAspectJAdvice.currentJoinPoint().getSourceLocation());
    });
    ITestBean itb = (ITestBean) pf.getProxy();
    // Any call will do
    itb.getAge();
  }

  @Test
  public void toShortAndLongStringFormedCorrectly() throws Exception {
    final Object raw = new TestBean();
    ProxyFactory pf = new ProxyFactory(raw);
    pf.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
    pf.addAdvice((MethodBeforeAdvice) (method) -> {
      // makeEncSJP, although meant for computing the enclosing join point,
      // it serves our purpose here
      StaticPart aspectJVersionJp = Factory.makeEncSJP(method.getMethod());
      JoinPoint jp = AbstractAspectJAdvice.currentJoinPoint();

      assertThat(jp.getSignature().toLongString()).isEqualTo(aspectJVersionJp.getSignature().toLongString());
      assertThat(jp.getSignature().toShortString()).isEqualTo(aspectJVersionJp.getSignature().toShortString());
      assertThat(jp.getSignature().toString()).isEqualTo(aspectJVersionJp.getSignature().toString());

      assertThat(jp.toLongString()).isEqualTo(aspectJVersionJp.toLongString());
      assertThat(jp.toShortString()).isEqualTo(aspectJVersionJp.toShortString());
      assertThat(jp.toString()).isEqualTo(aspectJVersionJp.toString());
    });
    ITestBean itb = (ITestBean) pf.getProxy();
    itb.getAge();
    itb.setName("foo");
    itb.getDoctor();
    itb.getStringArray();
    itb.getSpouse();
    itb.setSpouse(new TestBean());
    try {
      itb.unreliableFileOperation();
    }
    catch (IOException ex) {
      // we don't really care...
    }
  }

}
