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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.accessibility.Accessible;
import javax.swing.JFrame;
import javax.swing.RootPaneContainer;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.interceptor.DebugInterceptor;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.DefaultIntroductionAdvisor;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.testfixture.TimeStamped;
import cn.taketoday.aop.testfixture.advice.CountingBeforeAdvice;
import cn.taketoday.aop.testfixture.beans.IOther;
import cn.taketoday.aop.testfixture.beans.ITestBean;
import cn.taketoday.aop.testfixture.beans.TestBean;
import cn.taketoday.aop.testfixture.interceptor.NopInterceptor;
import cn.taketoday.aop.testfixture.interceptor.TimestampIntroductionInterceptor;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Also tests AdvisedSupport and ProxyCreatorSupport superclasses.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 14.05.2003
 */
public class ProxyFactoryTests {

  @Test
  public void testIndexOfMethods() {
    TestBean target = new TestBean();
    ProxyFactory pf = new ProxyFactory(target);
    NopInterceptor nop = new NopInterceptor();
    Advisor advisor = new DefaultPointcutAdvisor(new CountingBeforeAdvice());
    Advised advised = (Advised) pf.getProxy();
    // Can use advised and ProxyFactory interchangeably
    advised.addAdvice(nop);
    pf.addAdvisor(advisor);
    assertThat(pf.indexOf(new NopInterceptor())).isEqualTo(-1);
    assertThat(pf.indexOf(nop)).isEqualTo(0);
    assertThat(pf.indexOf(advisor)).isEqualTo(1);
    assertThat(advised.indexOf(new DefaultPointcutAdvisor(null))).isEqualTo(-1);
  }

  @Test
  public void testRemoveAdvisorByReference() {
    TestBean target = new TestBean();
    ProxyFactory pf = new ProxyFactory(target);
    NopInterceptor nop = new NopInterceptor();
    CountingBeforeAdvice cba = new CountingBeforeAdvice();
    Advisor advisor = new DefaultPointcutAdvisor(cba);
    pf.addAdvice(nop);
    pf.addAdvisor(advisor);
    ITestBean proxied = (ITestBean) pf.getProxy();
    proxied.setAge(5);
    assertThat(cba.getCalls()).isEqualTo(1);
    assertThat(nop.getCount()).isEqualTo(1);
    assertThat(pf.removeAdvisor(advisor)).isTrue();
    assertThat(proxied.getAge()).isEqualTo(5);
    assertThat(cba.getCalls()).isEqualTo(1);
    assertThat(nop.getCount()).isEqualTo(2);
    assertThat(pf.removeAdvisor(new DefaultPointcutAdvisor(null))).isFalse();
  }

  @Test
  public void testRemoveAdvisorByIndex() {
    TestBean target = new TestBean();
    ProxyFactory pf = new ProxyFactory(target);
    NopInterceptor nop = new NopInterceptor();
    CountingBeforeAdvice cba = new CountingBeforeAdvice();
    Advisor advisor = new DefaultPointcutAdvisor(cba);
    pf.addAdvice(nop);
    pf.addAdvisor(advisor);
    NopInterceptor nop2 = new NopInterceptor();
    pf.addAdvice(nop2);
    ITestBean proxied = (ITestBean) pf.getProxy();
    proxied.setAge(5);
    assertThat(cba.getCalls()).isEqualTo(1);
    assertThat(nop.getCount()).isEqualTo(1);
    assertThat(nop2.getCount()).isEqualTo(1);
    // Removes counting before advisor
    pf.removeAdvisor(1);
    assertThat(proxied.getAge()).isEqualTo(5);
    assertThat(cba.getCalls()).isEqualTo(1);
    assertThat(nop.getCount()).isEqualTo(2);
    assertThat(nop2.getCount()).isEqualTo(2);
    // Removes Nop1
    pf.removeAdvisor(0);
    assertThat(proxied.getAge()).isEqualTo(5);
    assertThat(cba.getCalls()).isEqualTo(1);
    assertThat(nop.getCount()).isEqualTo(2);
    assertThat(nop2.getCount()).isEqualTo(3);

    // Check out of bounds
    try {
      pf.removeAdvisor(-1);
    }
    catch (AopConfigException ex) {
      // Ok
    }

    try {
      pf.removeAdvisor(2);
    }
    catch (AopConfigException ex) {
      // Ok
    }

    assertThat(proxied.getAge()).isEqualTo(5);
    assertThat(nop2.getCount()).isEqualTo(4);
  }

  @Test
  public void testReplaceAdvisor() {
    TestBean target = new TestBean();
    ProxyFactory pf = new ProxyFactory(target);
    NopInterceptor nop = new NopInterceptor();
    CountingBeforeAdvice cba1 = new CountingBeforeAdvice();
    CountingBeforeAdvice cba2 = new CountingBeforeAdvice();
    Advisor advisor1 = new DefaultPointcutAdvisor(cba1);
    Advisor advisor2 = new DefaultPointcutAdvisor(cba2);
    pf.addAdvisor(advisor1);
    pf.addAdvice(nop);
    ITestBean proxied = (ITestBean) pf.getProxy();
    // Use the type cast feature
    // Replace etc methods on advised should be same as on ProxyFactory
    Advised advised = (Advised) proxied;
    proxied.setAge(5);
    assertThat(cba1.getCalls()).isEqualTo(1);
    assertThat(cba2.getCalls()).isEqualTo(0);
    assertThat(nop.getCount()).isEqualTo(1);
    assertThat(advised.replaceAdvisor(new DefaultPointcutAdvisor(new NopInterceptor()), advisor2)).isFalse();
    assertThat(advised.replaceAdvisor(advisor1, advisor2)).isTrue();
    assertThat(pf.getAdvisors()[0]).isEqualTo(advisor2);
    assertThat(proxied.getAge()).isEqualTo(5);
    assertThat(cba1.getCalls()).isEqualTo(1);
    assertThat(nop.getCount()).isEqualTo(2);
    assertThat(cba2.getCalls()).isEqualTo(1);
    assertThat(pf.replaceAdvisor(new DefaultPointcutAdvisor(null), advisor1)).isFalse();
  }

  @Test
  public void testAddRepeatedInterface() {
    TimeStamped tst = () -> {
      throw new UnsupportedOperationException("getTimeStamp");
    };
    ProxyFactory pf = new ProxyFactory(tst);
    // We've already implicitly added this interface.
    // This call should be ignored without error
    pf.addInterface(TimeStamped.class);
    // All cool
    assertThat(pf.getProxy()).isInstanceOf(TimeStamped.class);
  }

  @Test
  public void testGetsAllInterfaces() {
    // Extend to get new interface
    class TestBeanSubclass extends TestBean implements Comparable<Object> {
      @Override
      public int compareTo(Object arg0) {
        throw new UnsupportedOperationException("compareTo");
      }
    }
    TestBeanSubclass raw = new TestBeanSubclass();
    ProxyFactory factory = new ProxyFactory(raw);
    //System.out.println("Proxied interfaces are " + StringUtils.arrayToDelimitedString(factory.getProxiedInterfaces(), ","));
    assertThat(factory.getProxiedInterfaces().length).as("Found correct number of interfaces").isEqualTo(5);
    ITestBean tb = (ITestBean) factory.getProxy();
    assertThat(tb).as("Picked up secondary interface").isInstanceOf(IOther.class);
    raw.setAge(25);
    assertThat(tb.getAge() == raw.getAge()).isTrue();

    long t = 555555L;
    TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor(t);

    Class<?>[] oldProxiedInterfaces = factory.getProxiedInterfaces();

    factory.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));

    Class<?>[] newProxiedInterfaces = factory.getProxiedInterfaces();
    assertThat(newProxiedInterfaces.length).as("Advisor proxies one more interface after introduction").isEqualTo(oldProxiedInterfaces.length + 1);

    TimeStamped ts = (TimeStamped) factory.getProxy();
    assertThat(ts.getTimeStamp() == t).isTrue();
    // Shouldn't fail;
    ((IOther) ts).absquatulate();
  }

  @Test
  public void testInterceptorInclusionMethods() {
    class MyInterceptor implements MethodInterceptor {
      @Override
      public Object invoke(MethodInvocation invocation) throws Throwable {
        throw new UnsupportedOperationException();
      }
    }

    NopInterceptor di = new NopInterceptor();
    NopInterceptor diUnused = new NopInterceptor();
    ProxyFactory factory = new ProxyFactory(new TestBean());
    factory.addAdvice(0, di);
    assertThat(factory.getProxy()).isInstanceOf(ITestBean.class);
    assertThat(factory.adviceIncluded(di)).isTrue();
    assertThat(!factory.adviceIncluded(diUnused)).isTrue();
    assertThat(factory.countAdvicesOfType(NopInterceptor.class) == 1).isTrue();
    assertThat(factory.countAdvicesOfType(MyInterceptor.class) == 0).isTrue();

    factory.addAdvice(0, diUnused);
    assertThat(factory.adviceIncluded(diUnused)).isTrue();
    assertThat(factory.countAdvicesOfType(NopInterceptor.class) == 2).isTrue();
  }

  @Test
  public void testSealedInterfaceExclusion() {
    // String implements ConstantDesc on JDK 12+, sealed as of JDK 17
    ProxyFactory factory = new ProxyFactory(new String());
    NopInterceptor di = new NopInterceptor();
    factory.addAdvice(0, di);
    Object proxy = factory.getProxy();
    assertThat(proxy).isInstanceOf(CharSequence.class);
  }

  /**
   * Should see effect immediately on behavior.
   */
  @Test
  public void testCanAddAndRemoveAspectInterfacesOnSingleton() {
    ProxyFactory config = new ProxyFactory(new TestBean());

    assertThat(config.getProxy() instanceof TimeStamped).as("Shouldn't implement TimeStamped before manipulation").isFalse();

    long time = 666L;
    TimestampIntroductionInterceptor ti = new TimestampIntroductionInterceptor();
    ti.setTime(time);

    // Add to front of interceptor chain
    int oldCount = config.getAdvisors().length;
    config.addAdvisor(0, new DefaultIntroductionAdvisor(ti, TimeStamped.class));

    assertThat(config.getAdvisors().length == oldCount + 1).isTrue();

    TimeStamped ts = (TimeStamped) config.getProxy();
    assertThat(ts.getTimeStamp() == time).isTrue();

    // Can remove
    config.removeAdvice(ti);

    assertThat(config.getAdvisors().length == oldCount).isTrue();

    assertThatExceptionOfType(RuntimeException.class)
            .as("Existing object won't implement this interface any more")
            .isThrownBy(ts::getTimeStamp); // Existing reference will fail

    assertThat(config.getProxy() instanceof TimeStamped).as("Should no longer implement TimeStamped").isFalse();

    // Now check non-effect of removing interceptor that isn't there
    config.removeAdvice(new DebugInterceptor());

    assertThat(config.getAdvisors().length == oldCount).isTrue();

    ITestBean it = (ITestBean) ts;
    DebugInterceptor debugInterceptor = new DebugInterceptor();
    config.addAdvice(0, debugInterceptor);
    it.getSpouse();
    assertThat(debugInterceptor.getCount()).isEqualTo(1);
    config.removeAdvice(debugInterceptor);
    it.getSpouse();
    // not invoked again
    assertThat(debugInterceptor.getCount() == 1).isTrue();
  }

  @Test
  public void testProxyTargetClassWithInterfaceAsTarget() {
    ProxyFactory pf = new ProxyFactory();
    pf.setTargetClass(ITestBean.class);
    Object proxy = pf.getProxy();
    assertThat(AopUtils.isJdkDynamicProxy(proxy)).as("Proxy is a JDK proxy").isTrue();
    assertThat(proxy instanceof ITestBean).isTrue();
    assertThat(AopProxyUtils.ultimateTargetClass(proxy)).isEqualTo(ITestBean.class);

    ProxyFactory pf2 = new ProxyFactory(proxy);
    Object proxy2 = pf2.getProxy();
    assertThat(AopUtils.isJdkDynamicProxy(proxy2)).as("Proxy is a JDK proxy").isTrue();
    assertThat(proxy2 instanceof ITestBean).isTrue();
    assertThat(AopProxyUtils.ultimateTargetClass(proxy2)).isEqualTo(ITestBean.class);
  }

  @Test
  public void testProxyTargetClassWithConcreteClassAsTarget() {
    ProxyFactory pf = new ProxyFactory();
    pf.setTargetClass(TestBean.class);
    Object proxy = pf.getProxy();
    assertThat(AopUtils.isCglibProxy(proxy)).as("Proxy is a CGLIB proxy").isTrue();
    assertThat(proxy instanceof TestBean).isTrue();
    assertThat(AopProxyUtils.ultimateTargetClass(proxy)).isEqualTo(TestBean.class);

    ProxyFactory pf2 = new ProxyFactory(proxy);
    pf2.setProxyTargetClass(true);
    Object proxy2 = pf2.getProxy();
    assertThat(AopUtils.isCglibProxy(proxy2)).as("Proxy is a CGLIB proxy").isTrue();
    assertThat(proxy2 instanceof TestBean).isTrue();
    assertThat(AopProxyUtils.ultimateTargetClass(proxy2)).isEqualTo(TestBean.class);
  }

  @Test
  @Disabled("Not implemented yet, see https://jira.springframework.org/browse/SPR-5708")
  public void testExclusionOfNonPublicInterfaces() {
    JFrame frame = new JFrame();
    ProxyFactory proxyFactory = new ProxyFactory(frame);
    Object proxy = proxyFactory.getProxy();
    assertThat(proxy instanceof RootPaneContainer).isTrue();
    assertThat(proxy instanceof Accessible).isTrue();
  }

  @Test
  public void testInterfaceProxiesCanBeOrderedThroughAnnotations() {
    Object proxy1 = new ProxyFactory(new A()).getProxy();
    Object proxy2 = new ProxyFactory(new B()).getProxy();
    List<Object> list = new ArrayList<>(2);
    list.add(proxy1);
    list.add(proxy2);
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0)).isSameAs(proxy2);
    assertThat(list.get(1)).isSameAs(proxy1);
  }

  @Test
  public void testTargetClassProxiesCanBeOrderedThroughAnnotations() {
    ProxyFactory pf1 = new ProxyFactory(new A());
    pf1.setProxyTargetClass(true);
    ProxyFactory pf2 = new ProxyFactory(new B());
    pf2.setProxyTargetClass(true);
    Object proxy1 = pf1.getProxy();
    Object proxy2 = pf2.getProxy();
    List<Object> list = new ArrayList<>(2);
    list.add(proxy1);
    list.add(proxy2);
    AnnotationAwareOrderComparator.sort(list);
    assertThat(list.get(0)).isSameAs(proxy2);
    assertThat(list.get(1)).isSameAs(proxy1);
  }

  @Test
  public void testInterceptorWithoutJoinpoint() {
    final TestBean target = new TestBean("tb");
    ITestBean proxy = ProxyFactory.getProxy(ITestBean.class, (MethodInterceptor) invocation -> {
      assertThat(invocation.getThis()).isNull();
      return invocation.getMethod().invoke(target, invocation.getArguments());
    });
    assertThat(proxy.getName()).isEqualTo("tb");
  }

  @Test
  public void testCharSequenceProxy() {
    CharSequence target = "test";
    ProxyFactory pf = new ProxyFactory(target);
    ClassLoader cl = target.getClass().getClassLoader();
    assertThat(((CharSequence) pf.getProxy(cl)).toString()).isEqualTo(target);
  }

  @Test
  public void testDateProxy() {
    Date target = new Date();
    ProxyFactory pf = new ProxyFactory(target);
    pf.setProxyTargetClass(true);
    ClassLoader cl = target.getClass().getClassLoader();
    assertThat(((Date) pf.getProxy(cl)).getTime()).isEqualTo(target.getTime());
  }

  @Test
  public void testJdbcSavepointProxy() throws SQLException {
    Savepoint target = new Savepoint() {
      @Override
      public int getSavepointId() throws SQLException {
        return 1;
      }

      @Override
      public String getSavepointName() throws SQLException {
        return "sp";
      }
    };
    ProxyFactory pf = new ProxyFactory(target);
    ClassLoader cl = Savepoint.class.getClassLoader();
    assertThat(((Savepoint) pf.getProxy(cl)).getSavepointName()).isEqualTo("sp");
  }

  @Order(2)
  public static class A implements Runnable {

    @Override
    public void run() {
    }
  }

  @Order(1)
  public static class B implements Runnable {

    @Override
    public void run() {
    }
  }

}
