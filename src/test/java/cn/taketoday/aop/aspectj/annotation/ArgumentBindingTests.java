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
  public void testBindingInPointcutUsedByAdvice() {
    TestBean tb = new TestBean();
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(tb);
    proxyFactory.addAspect(NamedPointcutWithArgs.class);

    ITestBean proxiedTestBean = proxyFactory.getProxy();
    assertThatIllegalArgumentException().isThrownBy(() ->
            proxiedTestBean.setName("Supercalifragalisticexpialidocious"));
  }

  @Test
  public void testAnnotationArgumentNameBinding() {
    TransactionalBean tb = new TransactionalBean();
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(tb);
    proxyFactory.addAspect(PointcutWithAnnotationArgument.class);

    ITransactionalBean proxiedTestBean = proxyFactory.getProxy();
    assertThatIllegalStateException().isThrownBy(
            proxiedTestBean::doInTransaction);
  }

  @Test
  public void testParameterNameDiscoverWithReferencePointcut() throws Exception {
    AspectJAdviceParameterNameDiscoverer discoverer =
            new AspectJAdviceParameterNameDiscoverer("somepc(formal) && set(* *)");
    discoverer.setRaiseExceptions(true);
    Method methodUsedForParameterTypeDiscovery =
            getClass().getMethod("methodWithOneParam", String.class);
    String[] pnames = discoverer.getParameterNames(methodUsedForParameterTypeDiscovery);
    assertThat(pnames.length).as("one parameter name").isEqualTo(1);
    assertThat(pnames[0]).isEqualTo("formal");
  }

  public void methodWithOneParam(String aParam) {
  }

  public interface ITransactionalBean {

    @Transactional
    void doInTransaction();
  }

  public static class TransactionalBean implements ITransactionalBean {

    @Override
    @Transactional
    public void doInTransaction() {
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
