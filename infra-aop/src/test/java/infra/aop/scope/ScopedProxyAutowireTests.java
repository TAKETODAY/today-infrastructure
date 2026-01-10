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

package infra.aop.scope;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ScopedProxyAutowireTests {

  @Test
  public void testScopedProxyInheritsAutowireCandidateFalse() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            qualifiedResource(ScopedProxyAutowireTests.class, "scopedAutowireFalse.xml"));

    var beanNamesForType = bf.getBeanNamesForType(TestBean.class, false, false);
    assertThat(beanNamesForType).contains("scoped");
    var beanNamesForType1 = bf.getBeanNamesForType(TestBean.class, true, false);
    assertThat(beanNamesForType1).contains("scoped");
    assertThat(bf.containsSingleton("scoped")).isFalse();
    TestBean autowired = (TestBean) bf.getBean("autowired");
    TestBean unscoped = (TestBean) bf.getBean("unscoped");
    assertThat(autowired.getChild()).isSameAs(unscoped);
  }

  @Test
  public void testScopedProxyReplacesAutowireCandidateTrue() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            qualifiedResource(ScopedProxyAutowireTests.class, "scopedAutowireTrue.xml"));

    assertThat(bf.getBeanNamesForType(TestBean.class, true, false)).contains("scoped");
    assertThat(bf.getBeanNamesForType(TestBean.class, false, false)).contains("scoped");
    assertThat(bf.containsSingleton("scoped")).isFalse();
    TestBean autowired = (TestBean) bf.getBean("autowired");
    TestBean scoped = (TestBean) bf.getBean("scoped");
    assertThat(autowired.getChild()).isSameAs(scoped);
  }

  static class TestBean {

    private TestBean child;

    public void setChild(TestBean child) {
      this.child = child;
    }

    public TestBean getChild() {
      return this.child;
    }
  }

}
