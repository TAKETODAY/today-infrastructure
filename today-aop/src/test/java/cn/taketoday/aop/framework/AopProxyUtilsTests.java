/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.DecoratingProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @author Sam Brannen
 */
public class AopProxyUtilsTests {

  @Test
  void completeProxiedInterfacesWorksWithNull() {
    AdvisedSupport as = new AdvisedSupport();
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces).containsExactly(StandardProxy.class, Advised.class);
  }

  @Test
  void completeProxiedInterfacesWorksWithNullOpaque() {
    AdvisedSupport as = new AdvisedSupport();
    as.setOpaque(true);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces).containsExactly(StandardProxy.class);
  }

  @Test
  void completeProxiedInterfacesAdvisedNotIncluded() {
    AdvisedSupport as = new AdvisedSupport();
    as.addInterface(ITestBean.class);
    as.addInterface(Comparable.class);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces).containsExactly(
            ITestBean.class, Comparable.class, StandardProxy.class, Advised.class);
  }

  @Test
  void completeProxiedInterfacesAdvisedIncluded() {
    AdvisedSupport as = new AdvisedSupport();
    as.addInterface(Advised.class);
    as.addInterface(ITestBean.class);
    as.addInterface(Comparable.class);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces).containsExactly(
            Advised.class, ITestBean.class, Comparable.class, StandardProxy.class);
  }

  @Test
  void completeProxiedInterfacesAdvisedNotIncludedOpaque() {
    AdvisedSupport as = new AdvisedSupport();
    as.setOpaque(true);
    as.addInterface(ITestBean.class);
    as.addInterface(Comparable.class);
    Class<?>[] completedInterfaces = AopProxyUtils.completeProxiedInterfaces(as);
    assertThat(completedInterfaces).containsExactly(ITestBean.class, Comparable.class, StandardProxy.class);
  }

  @Test
  void proxiedUserInterfacesWithSingleInterface() {
    ProxyFactory pf = new ProxyFactory();
    pf.setTarget(new TestBean());
    pf.addInterface(ITestBean.class);
    Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(pf.getProxy());
    assertThat(userInterfaces).containsExactly(ITestBean.class);
  }

  @Test
  void proxiedUserInterfacesWithMultipleInterfaces() {
    ProxyFactory pf = new ProxyFactory();
    pf.setTarget(new TestBean());
    pf.addInterface(ITestBean.class);
    pf.addInterface(Comparable.class);
    Class<?>[] userInterfaces = AopProxyUtils.proxiedUserInterfaces(pf.getProxy());
    assertThat(userInterfaces).containsExactly(ITestBean.class, Comparable.class);
  }

  @Test
  void proxiedUserInterfacesWithNoInterface() {
    Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[0],
            (proxy1, method, args) -> null);
    assertThatIllegalArgumentException().isThrownBy(() -> AopProxyUtils.proxiedUserInterfaces(proxy));
  }

  @Test
  void completeJdkProxyInterfacesFromNullInterface() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AopProxyUtils.completeJdkProxyInterfaces(ITestBean.class, null, Comparable.class))
            .withMessage("'userInterfaces' must not contain null values");
  }

  @Test
  void completeJdkProxyInterfacesFromClassThatIsNotAnInterface() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AopProxyUtils.completeJdkProxyInterfaces(TestBean.class))
            .withMessage(TestBean.class.getName() + " must be a non-sealed interface");
  }

  @Test
  void completeJdkProxyInterfacesFromSealedInterface() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AopProxyUtils.completeJdkProxyInterfaces(SealedInterface.class))
            .withMessage(SealedInterface.class.getName() + " must be a non-sealed interface");
  }

  @Test
  void completeJdkProxyInterfacesFromSingleClass() {
    Class<?>[] jdkProxyInterfaces = AopProxyUtils.completeJdkProxyInterfaces(ITestBean.class);
    assertThat(jdkProxyInterfaces).containsExactly(
            ITestBean.class, StandardProxy.class, Advised.class, DecoratingProxy.class);
  }

  @Test
  void completeJdkProxyInterfacesFromMultipleClasses() {
    Class<?>[] jdkProxyInterfaces = AopProxyUtils.completeJdkProxyInterfaces(ITestBean.class, Comparable.class);
    assertThat(jdkProxyInterfaces).containsExactly(
            ITestBean.class, Comparable.class, StandardProxy.class, Advised.class, DecoratingProxy.class);
  }

  sealed interface SealedInterface {
  }

  @SuppressWarnings("unused")
  static final class SealedClass implements SealedInterface {
  }

}
