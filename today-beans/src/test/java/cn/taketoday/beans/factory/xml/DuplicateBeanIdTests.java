/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory.xml;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThatException;

/**
 * With , bean id attributes (and all other id attributes across the
 * core schemas) are no longer typed as xsd:id, but as xsd:string.  This allows
 * for using the same bean id within nested &lt;beans&gt; elements.
 *
 * Duplicate ids *within the same level of nesting* will still be treated as an
 * error through the ProblemReporter, as this could never be an intended/valid
 * situation.
 *
 * @author Chris Beams
 * @see cn.taketoday.beans.factory.xml.XmlBeanFactoryTests#withDuplicateName
 * @see cn.taketoday.beans.factory.xml.XmlBeanFactoryTests#withDuplicateNameInAlias
 * @since 4.0
 */
class DuplicateBeanIdTests {

  @Test
  void duplicateBeanIdsWithinSameNestingLevelRaisesError() {
    StandardBeanFactory bf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    assertThatException().as("duplicate ids in same nesting level").isThrownBy(() ->
            reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-sameLevel-context.xml", this.getClass())));
  }

  @Test
  void duplicateBeanIdsAcrossNestingLevels() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setAllowBeanDefinitionOverriding(true);
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-multiLevel-context.xml", this.getClass()));
    TestBean testBean = bf.getBean(TestBean.class); // there should be only one
    Assertions.assertThat(testBean.getName()).isEqualTo("nested");
  }

}
