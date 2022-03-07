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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * With Spring 3.1, bean id attributes (and all other id attributes across the
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
 * @since 3.1
 */
public class DuplicateBeanIdTests {

  @Test
  public void duplicateBeanIdsWithinSameNestingLevelRaisesError() {
    StandardBeanFactory bf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    assertThatExceptionOfType(Exception.class).as("duplicate ids in same nesting level").isThrownBy(() ->
            reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-sameLevel-context.xml", this.getClass())));
  }

  @Test
  public void duplicateBeanIdsAcrossNestingLevels() {
    StandardBeanFactory bf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    reader.loadBeanDefinitions(new ClassPathResource("DuplicateBeanIdTests-multiLevel-context.xml", this.getClass()));
    TestBean testBean = bf.getBean(TestBean.class); // there should be only one
    assertThat(testBean.getName()).isEqualTo("nested");
  }
}
