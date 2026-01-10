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

package infra.aop.config;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.parsing.BeanDefinitionParsingException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
class AopNamespaceHandlerPointcutErrorTests {

  @Test
  void duplicatePointcutConfig() {
    StandardBeanFactory bf = new StandardBeanFactory();
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
                    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
                            qualifiedResource(getClass(), "pointcutDuplication.xml")))
            .satisfies(ex -> ex.contains(BeanDefinitionParsingException.class));
  }

  @Test
  void missingPointcutConfig() {
    StandardBeanFactory bf = new StandardBeanFactory();
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
                    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
                            qualifiedResource(getClass(), "pointcutMissing.xml")))
            .satisfies(ex -> ex.contains(BeanDefinitionParsingException.class));
  }

}
