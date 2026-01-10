/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.support;

import org.junit.jupiter.api.Test;

import infra.aop.framework.Advised;
import infra.aop.testfixture.interceptor.NopInterceptor;
import infra.aop.testfixture.interceptor.SerializableNopInterceptor;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.Person;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.Resource;
import infra.core.testfixture.io.SerializationTestUtils;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
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
