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

package infra.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.beans.PropertyValue;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Rob Harrop
 */
public class MetadataAttachmentTests {

  private StandardBeanFactory beanFactory;

  @BeforeEach
  public void setUp() throws Exception {
    this.beanFactory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
            new ClassPathResource("withMeta.xml", getClass()));
  }

  @Test
  public void metadataAttachment() throws Exception {
    BeanDefinition beanDefinition1 = this.beanFactory.getMergedBeanDefinition("testBean1");
    assertThat(beanDefinition1.getAttribute("foo")).isEqualTo("bar");
  }

  @Test
  public void metadataIsInherited() throws Exception {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("testBean2");
    assertThat(beanDefinition.getAttribute("foo")).as("Metadata not inherited").isEqualTo("bar");
    assertThat(beanDefinition.getAttribute("abc")).as("Child metdata not attached").isEqualTo("123");
  }

  @Test
  public void propertyMetadata() throws Exception {
    BeanDefinition beanDefinition = this.beanFactory.getMergedBeanDefinition("testBean3");
    PropertyValue pv = beanDefinition.getPropertyValues().get("name");
    assertThat(pv.getAttribute("surname")).isEqualTo("Harrop");
  }

}
