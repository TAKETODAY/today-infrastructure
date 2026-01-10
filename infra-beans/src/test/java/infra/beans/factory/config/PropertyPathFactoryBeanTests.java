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

package infra.beans.factory.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.Resource;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 15:27
 */
class PropertyPathFactoryBeanTests {

  private static final Resource CONTEXT = qualifiedResource(PropertyPathFactoryBeanTests.class, "context.xml");

  @Test
  public void testPropertyPathFactoryBeanWithSingletonResult() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getBean("propertyPath1")).isEqualTo(12);
    assertThat(xbf.getBean("propertyPath2")).isEqualTo(11);
    assertThat(xbf.getBean("tb.age")).isEqualTo(10);
    assertThat(xbf.getType("otb.spouse")).isEqualTo(ITestBean.class);
    Object result1 = xbf.getBean("otb.spouse");
    Object result2 = xbf.getBean("otb.spouse");
    boolean condition = result1 instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(result1 == result2).isTrue();
    assertThat(((TestBean) result1).getAge()).isEqualTo(99);
  }

  @Test
  public void testPropertyPathFactoryBeanWithPrototypeResult() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getType("tb.spouse")).isNull();
    assertThat(xbf.getType("propertyPath3")).isEqualTo(TestBean.class);
    Object result1 = xbf.getBean("tb.spouse");
    Object result2 = xbf.getBean("propertyPath3");
    Object result3 = xbf.getBean("propertyPath3");
    boolean condition2 = result1 instanceof TestBean;
    assertThat(condition2).isTrue();
    boolean condition1 = result2 instanceof TestBean;
    assertThat(condition1).isTrue();
    boolean condition = result3 instanceof TestBean;
    assertThat(condition).isTrue();
    assertThat(((TestBean) result1).getAge()).isEqualTo(11);
    assertThat(((TestBean) result2).getAge()).isEqualTo(11);
    assertThat(((TestBean) result3).getAge()).isEqualTo(11);
    assertThat(result1 != result2).isTrue();
    assertThat(result1 != result3).isTrue();
    assertThat(result2 != result3).isTrue();
  }

  @Test
  public void testPropertyPathFactoryBeanWithNullResult() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getType("tb.spouse.spouse")).isNull();
    assertThat(xbf.getBean("tb.spouse.spouse")).isNull();
  }

  @Test
  public void testPropertyPathFactoryBeanAsInnerBean() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    TestBean spouse = (TestBean) xbf.getBean("otb.spouse");
    TestBean tbWithInner = (TestBean) xbf.getBean("tbWithInner");
    assertThat(tbWithInner.getSpouse()).isSameAs(spouse);
    boolean condition = !tbWithInner.getFriends().isEmpty();
    assertThat(condition).isTrue();
    assertThat(tbWithInner.getFriends().iterator().next()).isSameAs(spouse);
  }

  @Test
  public void testPropertyPathFactoryBeanAsNullReference() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getBean("tbWithNullReference", TestBean.class).getSpouse()).isNull();
  }

  @Test
  public void testPropertyPathFactoryBeanAsInnerNull() {
    StandardBeanFactory xbf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
    assertThat(xbf.getBean("tbWithInnerNull", TestBean.class).getSpouse()).isNull();
  }

}
