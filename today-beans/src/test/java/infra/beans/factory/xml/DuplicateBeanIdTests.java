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

package infra.beans.factory.xml;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ClassPathResource;

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
 * @see infra.beans.factory.xml.XmlBeanFactoryTests#withDuplicateName
 * @see infra.beans.factory.xml.XmlBeanFactoryTests#withDuplicateNameInAlias
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
