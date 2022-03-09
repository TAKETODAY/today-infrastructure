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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 14:38
 */
public class DefinitionMetadataEqualsHashCodeTests {

  @Test
  public void rootBeanDefinition() {
    RootBeanDefinition master = new RootBeanDefinition(TestBean.class);
    RootBeanDefinition equal = new RootBeanDefinition(TestBean.class);
    RootBeanDefinition notEqual = new RootBeanDefinition(String.class);
    RootBeanDefinition subclass = new RootBeanDefinition(TestBean.class) {
    };
    setBaseProperties(master);
    setBaseProperties(equal);
    setBaseProperties(notEqual);
    setBaseProperties(subclass);

    assertEqualsAndHashCodeContracts(master, equal, notEqual, subclass);
  }

  /**
   * @see <a href="https://jira.spring.io/browse/SPR-11420">SPR-11420</a>
   * @since 3.2.8
   */
  @Test
  public void rootBeanDefinitionAndMethodOverridesWithDifferentOverloadedValues() {
    RootBeanDefinition master = new RootBeanDefinition(TestBean.class);
    RootBeanDefinition equal = new RootBeanDefinition(TestBean.class);

    setBaseProperties(master);
    setBaseProperties(equal);

    // Simulate AbstractBeanDefinition.validate() which delegates to
    // AbstractBeanDefinition.prepareMethodOverrides():
    master.getMethodOverrides().getOverrides().iterator().next().setOverloaded(false);
    // But do not simulate validation of the 'equal' bean. As a consequence, a method
    // override in 'equal' will be marked as overloaded, but the corresponding
    // override in 'master' will not. But... the bean definitions should still be
    // considered equal.

    assertThat(equal).as("Should be equal").isEqualTo(master);
    assertThat(equal.hashCode()).as("Hash code for equal instances must match").isEqualTo(master.hashCode());
  }

  @Test
  public void childBeanDefinition() {
    ChildBeanDefinition master = new ChildBeanDefinition("foo");
    ChildBeanDefinition equal = new ChildBeanDefinition("foo");
    ChildBeanDefinition notEqual = new ChildBeanDefinition("bar");
    ChildBeanDefinition subclass = new ChildBeanDefinition("foo") {
    };
    setBaseProperties(master);
    setBaseProperties(equal);
    setBaseProperties(notEqual);
    setBaseProperties(subclass);

    assertEqualsAndHashCodeContracts(master, equal, notEqual, subclass);
  }

  @Test
  public void runtimeBeanReference() {
    RuntimeBeanReference master = new RuntimeBeanReference("name");
    RuntimeBeanReference equal = new RuntimeBeanReference("name");
    RuntimeBeanReference notEqual = new RuntimeBeanReference("someOtherName");
    RuntimeBeanReference subclass = new RuntimeBeanReference("name") {
    };
    assertEqualsAndHashCodeContracts(master, equal, notEqual, subclass);
  }

  private void setBaseProperties(AbstractBeanDefinition definition) {
    definition.setAbstract(true);
    definition.setAttribute("foo", "bar");
    definition.setAutowireCandidate(false);
    definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    // definition.getConstructorArgumentValues().addGenericArgumentValue("foo");
    definition.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
    definition.setDependsOn(new String[] { "foo", "bar" });
    definition.setDestroyMethodName("destroy");
    definition.setEnforceDestroyMethod(false);
    definition.setEnforceInitMethod(true);
    definition.setFactoryBeanName("factoryBean");
    definition.setFactoryMethodName("factoryMethod");
    definition.setInitMethodName("init");
    definition.setLazyInit(true);
    definition.getMethodOverrides().addOverride(new LookupOverride("foo", "bar"));
    definition.getMethodOverrides().addOverride(new ReplaceOverride("foo", "bar"));
    definition.getPropertyValues().add("foo", "bar");
    definition.setResourceDescription("desc");
    definition.setRole(BeanDefinition.ROLE_APPLICATION);
    definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    definition.setSource("foo");
  }

  private void assertEqualsAndHashCodeContracts(Object master, Object equal, Object notEqual, Object subclass) {
    assertThat(equal).as("Should be equal").isEqualTo(master);
    assertThat(equal.hashCode()).as("Hash code for equal instances should match").isEqualTo(master.hashCode());

    assertThat(notEqual).as("Should not be equal").isNotEqualTo(master);
    assertThat(notEqual.hashCode()).as("Hash code for non-equal instances should not match").isNotEqualTo(master.hashCode());

    assertThat(subclass).as("Subclass should be equal").isEqualTo(master);
    assertThat(subclass.hashCode()).as("Hash code for subclass should match").isEqualTo(master.hashCode());
  }

}
