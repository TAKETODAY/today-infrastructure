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
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Arrays;

import cn.taketoday.aop.testfixture.SerializationTestUtils;
import cn.taketoday.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AspectProxyFactoryTests {

  @Test
  public void testWithNonAspect() {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
    assertThatIllegalArgumentException().isThrownBy(() ->
            proxyFactory.addAspect(TestBean.class));
  }

  @Test
  public void testWithSimpleAspect() throws Exception {
    TestBean bean = new TestBean();
    bean.setAge(2);
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(bean);
    proxyFactory.addAspect(MultiplyReturnValue.class);
    ITestBean proxy = proxyFactory.getProxy();
    assertThat(proxy.getAge()).as("Multiplication did not occur").isEqualTo((bean.getAge() * 2));
  }

  @Test
  public void testWithPerThisAspect() throws Exception {
    TestBean bean1 = new TestBean();
    TestBean bean2 = new TestBean();

    AspectJProxyFactory pf1 = new AspectJProxyFactory(bean1);
    pf1.addAspect(PerThisAspect.class);

    AspectJProxyFactory pf2 = new AspectJProxyFactory(bean2);
    pf2.addAspect(PerThisAspect.class);

    ITestBean proxy1 = pf1.getProxy();
    ITestBean proxy2 = pf2.getProxy();

    assertThat(proxy1.getAge()).isEqualTo(0);
    assertThat(proxy1.getAge()).isEqualTo(1);
    assertThat(proxy2.getAge()).isEqualTo(0);
    assertThat(proxy1.getAge()).isEqualTo(2);
  }

  @Test
  public void testWithInstanceWithNonAspect() throws Exception {
    AspectJProxyFactory pf = new AspectJProxyFactory();
    assertThatIllegalArgumentException().isThrownBy(() ->
            pf.addAspect(new TestBean()));
  }

  @Test
  public void testSerializable() throws Exception {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
    proxyFactory.addAspect(LoggingAspectOnVarargs.class);
    ITestBean proxy = proxyFactory.getProxy();
    assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
    ITestBean tb = SerializationTestUtils.serializeAndDeserialize(proxy);
    assertThat(tb.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
  }

  @Test
  public void testWithInstance() throws Exception {
    MultiplyReturnValue aspect = new MultiplyReturnValue();
    int multiple = 3;
    aspect.setMultiple(multiple);

    TestBean target = new TestBean();
    target.setAge(24);

    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
    proxyFactory.addAspect(aspect);

    ITestBean proxy = proxyFactory.getProxy();
    assertThat(proxy.getAge()).isEqualTo((target.getAge() * multiple));

    ITestBean serializedProxy = SerializationTestUtils.serializeAndDeserialize(proxy);
    assertThat(serializedProxy.getAge()).isEqualTo((target.getAge() * multiple));
  }

  @Test
  public void testWithNonSingletonAspectInstance() throws Exception {
    AspectJProxyFactory pf = new AspectJProxyFactory();
    assertThatIllegalArgumentException().isThrownBy(() -> pf.addAspect(new PerThisAspect()));
  }

  @Test  // SPR-13328
  public void testProxiedVarargsWithEnumArray() throws Exception {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
    proxyFactory.addAspect(LoggingAspectOnVarargs.class);
    ITestBean proxy = proxyFactory.getProxy();
    assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
  }

  @Test  // SPR-13328
  public void testUnproxiedVarargsWithEnumArray() throws Exception {
    AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestBean());
    proxyFactory.addAspect(LoggingAspectOnSetter.class);
    ITestBean proxy = proxyFactory.getProxy();
    assertThat(proxy.doWithVarargs(MyEnum.A, MyOtherEnum.C)).isTrue();
  }

  public interface ITestBean {

    int getAge();

    @SuppressWarnings("unchecked")
    <V extends MyInterface> boolean doWithVarargs(V... args);
  }

  @SuppressWarnings("serial")
  public static class TestBean implements ITestBean, Serializable {

    private int age;

    @Override
    public int getAge() {
      return age;
    }

    public void setAge(int age) {
      this.age = age;
    }

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

  @Aspect
  public static class LoggingAspectOnVarargs implements Serializable {

    @Around("execution(* doWithVarargs(*))")
    public Object doLog(ProceedingJoinPoint pjp) throws Throwable {
      LoggerFactory.getLogger(LoggingAspectOnVarargs.class).debug(Arrays.asList(pjp.getArgs()));
      return pjp.proceed();
    }
  }

  @Aspect
  public static class LoggingAspectOnSetter {

    @Around("execution(* setAge(*))")
    public Object doLog(ProceedingJoinPoint pjp) throws Throwable {
      LoggerFactory.getLogger(LoggingAspectOnSetter.class).debug(Arrays.asList(pjp.getArgs()));
      return pjp.proceed();
    }
  }
}

@Aspect
class MultiplyReturnValue implements Serializable {

  private int multiple = 2;

  public int invocations;

  public void setMultiple(int multiple) {
    this.multiple = multiple;
  }

  public int getMultiple() {
    return this.multiple;
  }

  @Around("execution(int *.getAge())")
  public Object doubleReturnValue(ProceedingJoinPoint pjp) throws Throwable {
    ++this.invocations;
    int result = (Integer) pjp.proceed();
    return result * this.multiple;
  }

}
