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
