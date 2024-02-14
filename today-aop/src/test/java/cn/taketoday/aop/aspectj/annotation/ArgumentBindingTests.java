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

package cn.taketoday.aop.aspectj.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import cn.taketoday.aop.aspectj.AspectJAdviceParameterNameDiscoverer;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ArgumentBindingTests {

  @Test
  void annotationArgumentNameBinding() {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TransactionalBean());
    proxyFactory.addAspect(PointcutWithAnnotationArgument.class);
    ITransactionalBean proxiedTestBean = proxyFactory.getProxy();

    assertThatIllegalStateException()
            .isThrownBy(proxiedTestBean::doInTransaction)
            .withMessage("Invoked with @Transactional");
  }

  @Test
  void bindingInPointcutUsedByAdvice() {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
    proxyFactory.addAspect(NamedPointcutWithArgs.class);
    ITestBean proxiedTestBean = proxyFactory.getProxy();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> proxiedTestBean.setName("enigma"))
            .withMessage("enigma");
  }

  @Test
  void bindingWithDynamicAdvice() {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
    proxyFactory.addAspect(DynamicPointcutWithArgs.class);
    ITestBean proxiedTestBean = proxyFactory.getProxy();

    proxiedTestBean.applyName(1);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> proxiedTestBean.applyName("enigma"))
            .withMessage("enigma");
  }

  @Test
  void parameterNameDiscoverWithReferencePointcut() throws Exception {
    AspectJAdviceParameterNameDiscoverer discoverer =
            new AspectJAdviceParameterNameDiscoverer("somepc(formal) && set(* *)");
    discoverer.setRaiseExceptions(true);
    Method method = getClass().getDeclaredMethod("methodWithOneParam", String.class);
    assertThat(discoverer.getParameterNames(method)).containsExactly("formal");
  }

  @SuppressWarnings("unused")
  private void methodWithOneParam(String aParam) {
  }

  interface ITransactionalBean {

    @Transactional
    void doInTransaction();
  }

  static class TransactionalBean implements ITransactionalBean {

    @Override
    @Transactional
    public void doInTransaction() {
    }
  }

  /**
   * Mimics Spring's @Transactional annotation without actually introducing the dependency.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @interface Transactional {
  }

  @Aspect
  static class PointcutWithAnnotationArgument {

    @Around("execution(* cn.taketoday..*.*(..)) && @annotation(transactional)")
    public Object around(ProceedingJoinPoint pjp, Transactional transactional) {
      throw new IllegalStateException("Invoked with @Transactional");
    }
  }

  @Aspect
  static class NamedPointcutWithArgs {

    @Pointcut("execution(* *(..)) && args(s,..)")
    public void pointcutWithArgs(String s) { }

    @Around("pointcutWithArgs(aString)")
    public Object doAround(ProceedingJoinPoint pjp, String aString) {
      throw new IllegalArgumentException(aString);
    }
  }

  @Aspect("pertarget(execution(* *(..)))")
  static class DynamicPointcutWithArgs {

    @Around("execution(* *(..)) && args(java.lang.String)")
    public Object doAround(ProceedingJoinPoint pjp) {
      throw new IllegalArgumentException(String.valueOf(pjp.getArgs()[0]));
    }
  }

}

/**
 * Represents Framework's Transactional annotation without actually introducing the dependency
 */
@Retention(RetentionPolicy.RUNTIME)
@interface Transactional {
}

@Aspect
class PointcutWithAnnotationArgument {

  @Around(value = "execution(* cn.taketoday..*.*(..)) && @annotation(transaction)")
  public Object around(ProceedingJoinPoint pjp, Transactional transaction) throws Throwable {
    System.out.println("Invoked with transaction " + transaction);
    throw new IllegalStateException();
  }

}

@Aspect
class NamedPointcutWithArgs {

  @Pointcut("execution(* *(..)) && args(s,..)")
  public void pointcutWithArgs(String s) { }

  @Around("pointcutWithArgs(aString)")
  public Object doAround(ProceedingJoinPoint pjp, String aString) throws Throwable {
    System.out.println("got '" + aString + "' at '" + pjp + "'");
    throw new IllegalArgumentException(aString);
  }

}
