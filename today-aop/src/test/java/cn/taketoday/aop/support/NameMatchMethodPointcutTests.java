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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.testfixture.SerializationTestUtils;
import cn.taketoday.aop.testfixture.beans.Person;
import cn.taketoday.aop.testfixture.beans.SerializablePerson;
import cn.taketoday.aop.testfixture.interceptor.NopInterceptor;
import cn.taketoday.aop.testfixture.interceptor.SerializableNopInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class NameMatchMethodPointcutTests {

  protected NameMatchMethodPointcut pc;

  protected Person proxied;

  protected SerializableNopInterceptor nop;

  /**
   * Create an empty pointcut, populating instance variables.
   */
  @BeforeEach
  public void setup() {
    ProxyFactory pf = new ProxyFactory(new SerializablePerson());
    nop = new SerializableNopInterceptor();
    pc = new NameMatchMethodPointcut();
    pf.addAdvisor(new DefaultPointcutAdvisor(pc, nop));
    proxied = (Person) pf.getProxy();
  }

  @Test
  public void testMatchingOnly() {
    // Can't do exact matching through isMatch
    assertThat(pc.isMatch("echo", "ech*")).isTrue();
    assertThat(pc.isMatch("setName", "setN*")).isTrue();
    assertThat(pc.isMatch("setName", "set*")).isTrue();
    assertThat(pc.isMatch("getName", "set*")).isFalse();
    assertThat(pc.isMatch("setName", "set")).isFalse();
    assertThat(pc.isMatch("testing", "*ing")).isTrue();
  }

  @Test
  public void testEmpty() throws Throwable {
    assertThat(nop.getCount()).isEqualTo(0);
    proxied.getName();
    proxied.setName("");
    proxied.echo(null);
    assertThat(nop.getCount()).isEqualTo(0);
  }

  @Test
  public void testMatchOneMethod() throws Throwable {
    pc.addMethodName("echo");
    pc.addMethodName("set*");
    assertThat(nop.getCount()).isEqualTo(0);
    proxied.getName();
    proxied.getName();
    assertThat(nop.getCount()).isEqualTo(0);
    proxied.echo(null);
    assertThat(nop.getCount()).isEqualTo(1);

    proxied.setName("");
    assertThat(nop.getCount()).isEqualTo(2);
    proxied.setAge(25);
    assertThat(proxied.getAge()).isEqualTo(25);
    assertThat(nop.getCount()).isEqualTo(3);
  }

  @Test
  public void testSets() throws Throwable {
    pc.setMappedNames("set*", "echo");
    assertThat(nop.getCount()).isEqualTo(0);
    proxied.getName();
    proxied.setName("");
    assertThat(nop.getCount()).isEqualTo(1);
    proxied.echo(null);
    assertThat(nop.getCount()).isEqualTo(2);
  }

  @Test
  public void testSerializable() throws Throwable {
    testSets();
    // Count is now 2
    Person p2 = SerializationTestUtils.serializeAndDeserialize(proxied);
    NopInterceptor nop2 = (NopInterceptor) ((Advised) p2).getAdvisors()[0].getAdvice();
    p2.getName();
    assertThat(nop2.getCount()).isEqualTo(2);
    p2.echo(null);
    assertThat(nop2.getCount()).isEqualTo(3);
  }

  @Test
  public void testEqualsAndHashCode() {
    NameMatchMethodPointcut pc1 = new NameMatchMethodPointcut();
    NameMatchMethodPointcut pc2 = new NameMatchMethodPointcut();

    String foo = "foo";

    assertThat(pc2).isEqualTo(pc1);
    assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());

    pc1.setMappedName(foo);
    assertThat(pc1.equals(pc2)).isFalse();
    assertThat(pc1.hashCode() != pc2.hashCode()).isTrue();

    pc2.setMappedName(foo);
    assertThat(pc2).isEqualTo(pc1);
    assertThat(pc2.hashCode()).isEqualTo(pc1.hashCode());
  }

}
