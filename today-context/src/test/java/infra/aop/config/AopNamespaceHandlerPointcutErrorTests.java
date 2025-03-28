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
