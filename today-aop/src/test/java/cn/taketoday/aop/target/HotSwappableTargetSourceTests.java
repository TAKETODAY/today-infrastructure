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

package cn.taketoday.aop.target;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.testfixture.SerializationTestUtils;
import cn.taketoday.aop.testfixture.beans.Person;
import cn.taketoday.aop.testfixture.beans.SerializablePerson;
import cn.taketoday.aop.testfixture.beans.SideEffectBean;
import cn.taketoday.aop.testfixture.interceptor.SerializableNopInterceptor;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;

import static cn.taketoday.aop.testfixture.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class HotSwappableTargetSourceTests {

  /** Initial count value set in bean factory XML */
  private static final int INITIAL_COUNT = 10;

  private StandardBeanFactory beanFactory;

  @BeforeEach
  public void setup() {
    this.beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
            qualifiedResource(HotSwappableTargetSourceTests.class, "context.xml"));
  }

  /**
   * We must simulate container shutdown, which should clear threads.
   */
  @AfterEach
  public void close() {
    // Will call pool.close()
    this.beanFactory.destroySingletons();
  }

  /**
   * Check it works like a normal invoker
   */
  @Test
  public void testBasicFunctionality() {
    SideEffectBean proxied = (SideEffectBean) beanFactory.getBean("swappable");
    assertThat(proxied.getCount()).isEqualTo(INITIAL_COUNT);
    proxied.doWork();
    assertThat(proxied.getCount()).isEqualTo((INITIAL_COUNT + 1));

    proxied = (SideEffectBean) beanFactory.getBean("swappable");
    proxied.doWork();
    assertThat(proxied.getCount()).isEqualTo((INITIAL_COUNT + 2));
  }

  @Test
  public void testValidSwaps() {
    SideEffectBean target1 = (SideEffectBean) beanFactory.getBean("target1");
    SideEffectBean target2 = (SideEffectBean) beanFactory.getBean("target2");

    SideEffectBean proxied = (SideEffectBean) beanFactory.getBean("swappable");
    assertThat(proxied.getCount()).isEqualTo(target1.getCount());
    proxied.doWork();
    assertThat(proxied.getCount()).isEqualTo((INITIAL_COUNT + 1));

    HotSwappableTargetSource swapper = (HotSwappableTargetSource) beanFactory.getBean("swapper");
    Object old = swapper.swap(target2);
    assertThat(old).as("Correct old target was returned").isEqualTo(target1);

    // TODO should be able to make this assertion: need to fix target handling
    // in AdvisedSupport
    //assertEquals(target2, ((Advised) proxied).getTarget());

    assertThat(proxied.getCount()).isEqualTo(20);
    proxied.doWork();
    assertThat(target2.getCount()).isEqualTo(21);

    // Swap it back
    swapper.swap(target1);
    assertThat(proxied.getCount()).isEqualTo(target1.getCount());
  }

  @Test
  public void testRejectsSwapToNull() {
    HotSwappableTargetSource swapper = (HotSwappableTargetSource) beanFactory.getBean("swapper");
    assertThatIllegalArgumentException().as("Shouldn't be able to swap to invalid value").isThrownBy(() ->
                    swapper.swap(null))
            .withMessageContaining("null");
    // It shouldn't be corrupted, it should still work
    testBasicFunctionality();
  }

  @Test
  public void testSerialization() throws Exception {
    SerializablePerson sp1 = new SerializablePerson();
    sp1.setName("Tony");
    SerializablePerson sp2 = new SerializablePerson();
    sp1.setName("Gordon");

    HotSwappableTargetSource hts = new HotSwappableTargetSource(sp1);
    ProxyFactory pf = new ProxyFactory();
    pf.addInterface(Person.class);
    pf.setTargetSource(hts);
    pf.addAdvisor(new DefaultPointcutAdvisor(new SerializableNopInterceptor()));
    Person p = (Person) pf.getProxy();

    assertThat(p.getName()).isEqualTo(sp1.getName());
    hts.swap(sp2);
    assertThat(p.getName()).isEqualTo(sp2.getName());

    p = SerializationTestUtils.serializeAndDeserialize(p);
    // We need to get a reference to the client-side targetsource
    hts = (HotSwappableTargetSource) ((Advised) p).getTargetSource();
    assertThat(p.getName()).isEqualTo(sp2.getName());
    hts.swap(sp1);
    assertThat(p.getName()).isEqualTo(sp1.getName());

  }

}
