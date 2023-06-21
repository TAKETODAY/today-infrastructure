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

package cn.taketoday.aop.scope;

import org.junit.jupiter.api.Test;

import java.util.Set;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;

import static cn.taketoday.core.testfixture.io.ResourceTestUtils.qualifiedResource;
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

    Set<String> beanNamesForType = bf.getBeanNamesForType(TestBean.class, false, false);
    assertThat(beanNamesForType.contains("scoped")).isTrue();
    Set<String> beanNamesForType1 = bf.getBeanNamesForType(TestBean.class, true, false);
    assertThat(beanNamesForType1.contains("scoped")).isTrue();
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

    assertThat(bf.getBeanNamesForType(TestBean.class, true, false).contains("scoped")).isTrue();
    assertThat(bf.getBeanNamesForType(TestBean.class, false, false).contains("scoped")).isTrue();
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
