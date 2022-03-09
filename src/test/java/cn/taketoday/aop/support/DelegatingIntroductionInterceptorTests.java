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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.IntroductionInterceptor;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.testfixture.interceptor.SerializableNopInterceptor;
import cn.taketoday.beans.testfixture.beans.INestedTestBean;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.NestedTestBean;
import cn.taketoday.beans.testfixture.beans.Person;
import cn.taketoday.beans.testfixture.beans.SerializablePerson;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.testfixture.TimeStamped;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 13.05.2003
 */
public class DelegatingIntroductionInterceptorTests {

  @Test
  public void testNullTarget() throws Exception {
    // Shouldn't accept null target
    assertThatIllegalArgumentException().isThrownBy(() ->
            new DelegatingIntroductionInterceptor(null));
  }

  @Test
  public void testIntroductionInterceptorWithDelegation() throws Exception {
    TestBean raw = new TestBean();
    assertThat(!(raw instanceof TimeStamped)).isTrue();
    ProxyFactory factory = new ProxyFactory(raw);

    TimeStamped ts = mock(TimeStamped.class);
    long timestamp = 111L;
    given(ts.getTimeStamp()).willReturn(timestamp);

    factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts)));

    TimeStamped tsp = (TimeStamped) factory.getProxy();
    assertThat(tsp.getTimeStamp() == timestamp).isTrue();
  }

  @Test
  public void testIntroductionInterceptorWithInterfaceHierarchy() throws Exception {
    TestBean raw = new TestBean();
    assertThat(!(raw instanceof SubTimeStamped)).isTrue();
    ProxyFactory factory = new ProxyFactory(raw);

    TimeStamped ts = mock(SubTimeStamped.class);
    long timestamp = 111L;
    given(ts.getTimeStamp()).willReturn(timestamp);

    factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts), SubTimeStamped.class));

    SubTimeStamped tsp = (SubTimeStamped) factory.getProxy();
    assertThat(tsp.getTimeStamp() == timestamp).isTrue();
  }

  @Test
  public void testIntroductionInterceptorWithSuperInterface() throws Exception {
    TestBean raw = new TestBean();
    assertThat(!(raw instanceof TimeStamped)).isTrue();
    ProxyFactory factory = new ProxyFactory(raw);

    TimeStamped ts = mock(SubTimeStamped.class);
    long timestamp = 111L;
    given(ts.getTimeStamp()).willReturn(timestamp);

    factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts), TimeStamped.class));

    TimeStamped tsp = (TimeStamped) factory.getProxy();
    assertThat(!(tsp instanceof SubTimeStamped)).isTrue();
    assertThat(tsp.getTimeStamp() == timestamp).isTrue();
  }

  @Test
  public void testAutomaticInterfaceRecognitionInDelegate() throws Exception {
    final long t = 1001L;
    class Tester implements TimeStamped, ITester {
      @Override
      public void foo() throws Exception {
      }

      @Override
      public long getTimeStamp() {
        return t;
      }
    }

    DelegatingIntroductionInterceptor ii = new DelegatingIntroductionInterceptor(new Tester());

    TestBean target = new TestBean();

    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvisor(0, new DefaultIntroductionAdvisor(ii));

    //assertTrue(Arrays.binarySearch(pf.getProxiedInterfaces(), TimeStamped.class) != -1);
    TimeStamped ts = (TimeStamped) pf.getProxy();

    assertThat(ts.getTimeStamp() == t).isTrue();
    ((ITester) ts).foo();

    ((ITestBean) ts).getAge();
  }

  @Test
  public void testAutomaticInterfaceRecognitionInSubclass() throws Exception {
    final long t = 1001L;
    @SuppressWarnings("serial")
    class TestII extends DelegatingIntroductionInterceptor implements TimeStamped, ITester {
      @Override
      public void foo() throws Exception {
      }

      @Override
      public long getTimeStamp() {
        return t;
      }
    }

    DelegatingIntroductionInterceptor ii = new TestII();

    TestBean target = new TestBean();

    ProxyFactory pf = new ProxyFactory(target);
    IntroductionAdvisor ia = new DefaultIntroductionAdvisor(ii);
    assertThat(ia.isPerInstance()).isTrue();
    pf.addAdvisor(0, ia);

    //assertTrue(Arrays.binarySearch(pf.getProxiedInterfaces(), TimeStamped.class) != -1);
    TimeStamped ts = (TimeStamped) pf.getProxy();

    assertThat(ts).isInstanceOf(TimeStamped.class);
    // Shouldn't proxy framework interfaces
    assertThat(!(ts instanceof MethodInterceptor)).isTrue();
    assertThat(!(ts instanceof IntroductionInterceptor)).isTrue();

    assertThat(ts.getTimeStamp() == t).isTrue();
    ((ITester) ts).foo();
    ((ITestBean) ts).getAge();

    // Test removal
    ii.suppressInterface(TimeStamped.class);
    // Note that we need to construct a new proxy factory,
    // or suppress the interface on the proxy factory
    pf = new ProxyFactory(target);
    pf.addAdvisor(0, new DefaultIntroductionAdvisor(ii));
    Object o = pf.getProxy();
    assertThat(!(o instanceof TimeStamped)).isTrue();
  }

  @SuppressWarnings("serial")
  @Test
  public void testIntroductionInterceptorDoesntReplaceToString() throws Exception {
    TestBean raw = new TestBean();
    assertThat(!(raw instanceof TimeStamped)).isTrue();
    ProxyFactory factory = new ProxyFactory(raw);

    TimeStamped ts = new SerializableTimeStamped(0);

    factory.addAdvisor(0, new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts) {
      @Override
      public String toString() {
        throw new UnsupportedOperationException("Shouldn't be invoked");
      }
    }));

    TimeStamped tsp = (TimeStamped) factory.getProxy();
    assertThat(tsp.getTimeStamp()).isEqualTo(0);

    assertThat(tsp.toString()).isEqualTo(raw.toString());
  }

  @Test
  public void testDelegateReturnsThisIsMassagedToReturnProxy() {
    NestedTestBean target = new NestedTestBean();
    String company = "Interface21";
    target.setCompany(company);
    TestBean delegate = new TestBean() {
      @Override
      public ITestBean getSpouse() {
        return this;
      }
    };
    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvice(new DelegatingIntroductionInterceptor(delegate));
    INestedTestBean proxy = (INestedTestBean) pf.getProxy();

    assertThat(proxy.getCompany()).isEqualTo(company);
    ITestBean introduction = (ITestBean) proxy;
    assertThat(introduction.getSpouse()).as("Introduced method returning delegate returns proxy").isSameAs(introduction);
    Assertions.assertThat(AopUtils.isAopProxy(introduction.getSpouse())).as("Introduced method returning delegate returns proxy").isTrue();
  }

  @Test
  public void testSerializableDelegatingIntroductionInterceptorSerializable() throws Exception {
    SerializablePerson serializableTarget = new SerializablePerson();
    String name = "Tony";
    serializableTarget.setName("Tony");

    ProxyFactory factory = new ProxyFactory(serializableTarget);
    factory.addInterface(Person.class);
    long time = 1000;
    TimeStamped ts = new SerializableTimeStamped(time);

    factory.addAdvisor(new DefaultIntroductionAdvisor(new DelegatingIntroductionInterceptor(ts)));
    factory.addAdvice(new SerializableNopInterceptor());

    Person p = (Person) factory.getProxy();

    assertThat(p.getName()).isEqualTo(name);
    assertThat(((TimeStamped) p).getTimeStamp()).isEqualTo(time);

    Person p1 = SerializationTestUtils.serializeAndDeserialize(p);
    assertThat(p1.getName()).isEqualTo(name);
    assertThat(((TimeStamped) p1).getTimeStamp()).isEqualTo(time);
  }

  // Test when target implements the interface: should get interceptor by preference.
  @Test
  public void testIntroductionMasksTargetImplementation() throws Exception {
    final long t = 1001L;
    @SuppressWarnings("serial")
    class TestII extends DelegatingIntroductionInterceptor implements TimeStamped {
      @Override
      public long getTimeStamp() {
        return t;
      }
    }

    DelegatingIntroductionInterceptor ii = new TestII();

    // != t
    TestBean target = new TargetClass(t + 1);

    ProxyFactory pf = new ProxyFactory(target);
    pf.addAdvisor(0, new DefaultIntroductionAdvisor(ii));

    TimeStamped ts = (TimeStamped) pf.getProxy();
    // From introduction interceptor, not target
    assertThat(ts.getTimeStamp() == t).isTrue();
  }

  @SuppressWarnings("serial")
  private static class SerializableTimeStamped implements TimeStamped, Serializable {

    private final long ts;

    public SerializableTimeStamped(long ts) {
      this.ts = ts;
    }

    @Override
    public long getTimeStamp() {
      return ts;
    }
  }

  public static class TargetClass extends TestBean implements TimeStamped {

    long t;

    public TargetClass(long t) {
      this.t = t;
    }

    @Override
    public long getTimeStamp() {
      return t;
    }
  }

  public interface ITester {

    void foo() throws Exception;
  }

  private static interface SubTimeStamped extends TimeStamped {
  }

}
