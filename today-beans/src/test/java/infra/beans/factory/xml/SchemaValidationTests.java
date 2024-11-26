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

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import infra.beans.BeansException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Rob Harrop
 */
public class SchemaValidationTests {

  @Test
  public void withAutodetection() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
                    reader.loadBeanDefinitions(new ClassPathResource("invalidPerSchema.xml", getClass())))
            .withCauseInstanceOf(SAXParseException.class);
  }

  @Test
  public void withExplicitValidationMode() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
    assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
                    reader.loadBeanDefinitions(new ClassPathResource("invalidPerSchema.xml", getClass())))
            .withCauseInstanceOf(SAXParseException.class);
  }

  @Test
  public void loadDefinitions() throws Exception {
    StandardBeanFactory bf = new StandardBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(bf);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
    reader.loadBeanDefinitions(new ClassPathResource("schemaValidated.xml", getClass()));

    TestBean foo = (TestBean) bf.getBean("fooBean");
    assertThat(foo.getSpouse()).as("Spouse is null").isNotNull();
    assertThat(foo.getFriends().size()).as("Incorrect number of friends").isEqualTo(2);
  }

}
