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

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.testfixture.SerializationTestUtils;
import cn.taketoday.aop.testfixture.beans.ITestBean;
import cn.taketoday.aop.testfixture.beans.Person;
import cn.taketoday.aop.testfixture.beans.TestBean;
import cn.taketoday.aop.testfixture.interceptor.NopInterceptor;
import cn.taketoday.aop.testfixture.interceptor.SerializableNopInterceptor;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;
import cn.taketoday.core.io.Resource;

import static cn.taketoday.aop.testfixture.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class RegexpMethodPointcutAdvisorIntegrationTests {

  private static final Resource CONTEXT =
          qualifiedResource(RegexpMethodPointcutAdvisorIntegrationTests.class, "context.xml");

  @Test
  public void testSinglePattern() throws Throwable {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
    ITestBean advised = (ITestBean) bf.getBean("settersAdvised");
    // Interceptor behind regexp advisor
    NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
    assertThat(nop.getCount()).isEqualTo(0);

    int newAge = 12;
    // Not advised
    advised.exceptional(null);
    assertThat(nop.getCount()).isEqualTo(0);
    advised.setAge(newAge);
    assertThat(advised.getAge()).isEqualTo(newAge);
    // Only setter fired
    assertThat(nop.getCount()).isEqualTo(1);
  }

  @Test
  public void testMultiplePatterns() throws Throwable {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
    // This is a CGLIB proxy, so we can proxy it to the target class
    TestBean advised = (TestBean) bf.getBean("settersAndAbsquatulateAdvised");
    // Interceptor behind regexp advisor
    NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
    assertThat(nop.getCount()).isEqualTo(0);

    int newAge = 12;
    // Not advised
    advised.exceptional(null);
    assertThat(nop.getCount()).isEqualTo(0);

    // This is proxied
    advised.absquatulate();
    assertThat(nop.getCount()).isEqualTo(1);
    advised.setAge(newAge);
    assertThat(advised.getAge()).isEqualTo(newAge);
    // Only setter fired
    assertThat(nop.getCount()).isEqualTo(2);
  }

  @Test
  public void testSerialization() throws Throwable {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(CONTEXT);
    // This is a CGLIB proxy, so we can proxy it to the target class
    Person p = (Person) bf.getBean("serializableSettersAdvised");
    // Interceptor behind regexp advisor
    NopInterceptor nop = (NopInterceptor) bf.getBean("nopInterceptor");
    assertThat(nop.getCount()).isEqualTo(0);

    int newAge = 12;
    // Not advised
    assertThat(p.getAge()).isEqualTo(0);
    assertThat(nop.getCount()).isEqualTo(0);

    // This is proxied
    p.setAge(newAge);
    assertThat(nop.getCount()).isEqualTo(1);
    p.setAge(newAge);
    assertThat(p.getAge()).isEqualTo(newAge);
    // Only setter fired
    assertThat(nop.getCount()).isEqualTo(2);

    // Serialize and continue...
    p = SerializationTestUtils.serializeAndDeserialize(p);
    assertThat(p.getAge()).isEqualTo(newAge);
    // Remembers count, but we need to get a new reference to nop...
    nop = (SerializableNopInterceptor) ((Advised) p).getAdvisors()[0].getAdvice();
    assertThat(nop.getCount()).isEqualTo(2);
    assertThat(p.getName()).isEqualTo("serializableSettersAdvised");
    p.setAge(newAge + 1);
    assertThat(nop.getCount()).isEqualTo(3);
    assertThat(p.getAge()).isEqualTo((newAge + 1));
  }

}
