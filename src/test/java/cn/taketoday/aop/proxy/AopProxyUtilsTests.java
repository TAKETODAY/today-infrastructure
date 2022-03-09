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

package cn.taketoday.aop.proxy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.AdvisedSupport;
import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.framework.StandardProxy;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY
 */
public class AopProxyUtilsTests {

  @Test
  public void testCompleteProxiedInterfacesWorksWithNull() {
    AdvisedSupport as = new AdvisedSupport();
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces.length).isEqualTo(2);
    List<?> ifaces = Arrays.asList(completedInterfaces);
    assertThat(ifaces.contains(Advised.class)).isTrue();
    assertThat(ifaces.contains(StandardProxy.class)).isTrue();
  }

  @Test
  public void testCompleteProxiedInterfacesWorksWithNullOpaque() {
    AdvisedSupport as = new AdvisedSupport();
    as.setOpaque(true);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces.length).isEqualTo(1);
  }

  @Test
  public void testCompleteProxiedInterfacesAdvisedNotIncluded() {
    AdvisedSupport as = new AdvisedSupport();
    as.addInterface(ITestBean.class);
    as.addInterface(Comparable.class);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces.length).isEqualTo(4);

    // Can't assume ordering for others, so use a list
    List<?> l = Arrays.asList(completedInterfaces);
    assertThat(l.contains(Advised.class)).isTrue();
    assertThat(l.contains(ITestBean.class)).isTrue();
    assertThat(l.contains(Comparable.class)).isTrue();
  }

  @Test
  public void testCompleteProxiedInterfacesAdvisedIncluded() {
    AdvisedSupport as = new AdvisedSupport();
    as.addInterface(ITestBean.class);
    as.addInterface(Comparable.class);
    as.addInterface(Advised.class);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces.length).isEqualTo(4);

    // Can't assume ordering for others, so use a list
    List<?> l = Arrays.asList(completedInterfaces);
    assertThat(l.contains(Advised.class)).isTrue();
    assertThat(l.contains(ITestBean.class)).isTrue();
    assertThat(l.contains(Comparable.class)).isTrue();
  }

  @Test
  public void testCompleteProxiedInterfacesAdvisedNotIncludedOpaque() {
    AdvisedSupport as = new AdvisedSupport();
    as.setOpaque(true);
    as.addInterface(ITestBean.class);
    as.addInterface(Comparable.class);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces.length).isEqualTo(3);

    // Can't assume ordering for others, so use a list
    List<?> l = Arrays.asList(completedInterfaces);
    assertThat(l.contains(Advised.class)).isFalse();
    assertThat(l.contains(ITestBean.class)).isTrue();
    assertThat(l.contains(Comparable.class)).isTrue();
  }

  @Test
  public void testProxiedUserInterfacesWithSingleInterface() {
    ProxyFactory pf = new ProxyFactory();
    pf.setTarget(new TestBean());
    pf.addInterface(ITestBean.class);
    Object proxy = pf.getProxy();
    Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(proxy);
    assertThat(userInterfaces.length).isEqualTo(1);
    assertThat(userInterfaces[0]).isEqualTo(ITestBean.class);
  }

  @Test
  public void testProxiedUserInterfacesWithMultipleInterfaces() {
    ProxyFactory pf = new ProxyFactory();
    pf.setTarget(new TestBean());
    pf.addInterface(ITestBean.class);
    pf.addInterface(Comparable.class);
    Object proxy = pf.getProxy();
    Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(proxy);
    assertThat(userInterfaces.length).isEqualTo(2);
    assertThat(userInterfaces[0]).isEqualTo(ITestBean.class);
    assertThat(userInterfaces[1]).isEqualTo(Comparable.class);
  }

  @Test
  public void testProxiedUserInterfacesWithNoInterface() {
    Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[0], (proxy1, method, args) -> null);
    assertThatIllegalArgumentException().isThrownBy(() -> AopProxyUtils.proxiedUserInterfaces(proxy));
  }

  @Test
  void isLambda() {
    assertIsLambda(AopProxyUtilsTests.staticLambdaExpression);
    assertIsLambda(AopProxyUtilsTests::staticStringFactory);

    assertIsLambda(this.instanceLambdaExpression);
    assertIsLambda(this::instanceStringFactory);
  }

  @Test
  void isNotLambda() {
    assertIsNotLambda(new EnigmaSupplier());

    assertIsNotLambda(new Supplier<String>() {
      @Override
      public String get() {
        return "anonymous inner class";
      }
    });

    assertIsNotLambda(new Fake$$LambdaSupplier());
  }

  private static void assertIsLambda(Supplier<String> supplier) {
    assertThat(AopProxyUtils.isLambda(supplier.getClass())).isTrue();
  }

  private static void assertIsNotLambda(Supplier<String> supplier) {
    assertThat(AopProxyUtils.isLambda(supplier.getClass())).isFalse();
  }

  private static final Supplier<String> staticLambdaExpression = () -> "static lambda expression";

  private final Supplier<String> instanceLambdaExpression = () -> "instance lambda expressions";

  private static String staticStringFactory() {
    return "static string factory";
  }

  private String instanceStringFactory() {
    return "instance string factory";
  }

  private static class EnigmaSupplier implements Supplier<String> {
    @Override
    public String get() {
      return "enigma";
    }
  }

  private static class Fake$$LambdaSupplier implements Supplier<String> {
    @Override
    public String get() {
      return "fake lambda";
    }
  }

}
