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

package cn.taketoday.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import cn.taketoday.aop.interceptor.ExposeInvocationInterceptor;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.testfixture.beans.IOther;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 13.03.2003
 */
@SuppressWarnings("serial")
public class JdkDynamicProxyTests extends AbstractAopProxyTests implements Serializable {

  @Override
  protected Object createProxy(ProxyCreatorSupport as) {
    assertThat(as.isProxyTargetClass()).as("Not forcible CGLIB").isFalse();
    Object proxy = as.createAopProxy().getProxy();
    assertThat(AopUtils.isJdkDynamicProxy(proxy)).as("Should be a JDK proxy: " + proxy.getClass()).isTrue();
    return proxy;
  }

  @Override
  protected AopProxy createAopProxy(AdvisedSupport as) {
    return new JdkDynamicAopProxy(as);
  }

  @Test
  public void testNullConfig() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new JdkDynamicAopProxy(null));
  }

  @Test
  public void testProxyIsJustInterface() {
    TestBean raw = new TestBean();
    raw.setAge(32);
    AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
    pc.setTarget(raw);
    JdkDynamicAopProxy aop = new JdkDynamicAopProxy(pc);

    Object proxy = aop.getProxy();
    boolean condition = proxy instanceof ITestBean;
    assertThat(condition).isTrue();
    boolean condition1 = proxy instanceof TestBean;
    assertThat(condition1).isFalse();
  }

  @Test
  public void testInterceptorIsInvokedWithNoTarget() {
    // Test return value
    final int age = 25;
    MethodInterceptor mi = (invocation -> age);

    AdvisedSupport pc = new AdvisedSupport(ITestBean.class);
    pc.addAdvice(mi);
    AopProxy aop = createAopProxy(pc);

    ITestBean tb = (ITestBean) aop.getProxy();
    assertThat(tb.getAge()).as("correct return value").isEqualTo(age);
  }

  @Test
  public void testTargetCanGetInvocationWithPrivateClass() {
    final ExposedInvocationTestBean expectedTarget = new ExposedInvocationTestBean() {
      @Override
      protected void assertions(MethodInvocation invocation) {
        assertThat(invocation.getThis()).isEqualTo(this);
        assertThat(invocation.getMethod().getDeclaringClass()).as("Invocation should be on ITestBean: " + invocation.getMethod()).isEqualTo(ITestBean.class);
      }
    };

    AdvisedSupport pc = new AdvisedSupport(ITestBean.class, IOther.class);
    pc.addAdvice(ExposeInvocationInterceptor.INSTANCE);
    TrapTargetInterceptor tii = new TrapTargetInterceptor() {
      @Override
      public Object invoke(MethodInvocation invocation) throws Throwable {
        // Assert that target matches BEFORE invocation returns
        assertThat(invocation.getThis()).as("Target is correct").isEqualTo(expectedTarget);
        return super.invoke(invocation);
      }
    };
    pc.addAdvice(tii);
    pc.setTarget(expectedTarget);
    AopProxy aop = createAopProxy(pc);

    ITestBean tb = (ITestBean) aop.getProxy();
    tb.getName();
  }

  @Test
  public void testProxyNotWrappedIfIncompatible() {
    FooBar bean = new FooBar();
    ProxyCreatorSupport as = new ProxyCreatorSupport();
    as.setInterfaces(Foo.class);
    as.setTarget(bean);

    Foo proxy = (Foo) createProxy(as);
    assertThat(proxy.getBarThis()).as("Target should be returned when return types are incompatible").isSameAs(bean);
    assertThat(proxy.getFooThis()).as("Proxy should be returned when return types are compatible").isSameAs(proxy);
  }

  @Test
  public void testEqualsAndHashCodeDefined() {
    AdvisedSupport as = new AdvisedSupport(Named.class);
    as.setTarget(new Person());
    JdkDynamicAopProxy aopProxy = new JdkDynamicAopProxy(as);
    Named proxy = (Named) aopProxy.getProxy();
    Named named = new Person();
    assertThat(proxy).isEqualTo(named);
    assertThat(named.hashCode()).isEqualTo(proxy.hashCode());
  }

  @Test  // SPR-13328
  @SuppressWarnings("unchecked")
  public void testVarargsWithEnumArray() {
    ProxyFactory proxyFactory = new ProxyFactory(new VarargTestBean());
    VarargTestInterface proxy = (VarargTestInterface) proxyFactory.getProxy();
    assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
  }

  public interface Foo {

    Bar getBarThis();

    Foo getFooThis();
  }

  public interface Bar {
  }

  public static class FooBar implements Foo, Bar {

    @Override
    public Bar getBarThis() {
      return this;
    }

    @Override
    public Foo getFooThis() {
      return this;
    }
  }

  public interface Named {

    String getName();

    @Override
    boolean equals(Object other);

    @Override
    int hashCode();
  }

  public static class Person implements Named {

    private final String name = "Rob Harrop";

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Person person = (Person) o;
      if (!name.equals(person.name)) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  public interface VarargTestInterface {

    @SuppressWarnings("unchecked")
    <V extends MyInterface> boolean doWithVarargs(V... args);
  }

  public static class VarargTestBean implements VarargTestInterface {

    @SuppressWarnings("unchecked")
    @Override
    public <V extends MyInterface> boolean doWithVarargs(V... args) {
      return true;
    }
  }

  public interface MyInterface {
  }

  public enum MyEnum implements MyInterface {

    A, B;
  }

  public enum MyOtherEnum implements MyInterface {

    C, D;
  }

}
