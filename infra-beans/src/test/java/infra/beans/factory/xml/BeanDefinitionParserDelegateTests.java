/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Element;

import infra.beans.factory.config.RuntimeBeanReference;
import infra.beans.factory.config.TypedStringValue;
import infra.beans.factory.parsing.BeanDefinitionParsingException;
import infra.beans.factory.support.ManagedMap;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.SimpleBeanDefinitionRegistry;
import infra.core.io.DescriptiveResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BeanDefinitionParserDelegateTests {

  private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(new SimpleBeanDefinitionRegistry());

  private final XmlReaderContext context = reader.createReaderContext(new DescriptiveResource("beans.xml"));

  private final BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(context);

  @Test
  void parsesQualifierValueAndCustomAttributes() {
    var definition = new RootBeanDefinition(String.class);
    delegate.parseQualifierElement(element("""
            <qualifier type="Region" value="west">
              <attribute key="zone" value="two"/>
            </qualifier>
            """), definition);

    var qualifier = definition.getQualifier("Region");
    assertThat(qualifier).isNotNull();
    assertThat(qualifier.getAttribute("value")).isEqualTo("west");
    assertThat(qualifier.getAttribute("zone")).isEqualTo("two");
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
          <qualifier/>                                            | must have a 'type' attribute
          <qualifier type="Region"><attribute key="zone"/></qualifier> | must have a 'key' and 'value'
          """)
  void rejectsIncompleteQualifiers(String xml, String message) {
    var definition = new RootBeanDefinition(String.class);

    assertThatExceptionOfType(BeanDefinitionParsingException.class)
            .isThrownBy(() -> delegate.parseQualifierElement(element(xml), definition))
            .withMessageContaining(message);
    assertThat(definition.getQualifiers()).isEmpty();
  }

  @Test
  void parsesMapReferencesWithIndependentKeyAndValueTypes() {
    var map = (ManagedMap<?, ?>) delegate.parseMapElement(element("""
            <map key-type="java.lang.Integer" value-type="java.lang.Boolean" merge="true">
              <entry key-ref="key" value="true"/>
              <entry key="42" value-ref="target"/>
              <entry key="7" value="8" value-type="java.lang.Long"/>
            </map>
            """), null);

    assertThat(map.isMergeEnabled()).isTrue();
    assertThat(map.getKeyTypeName()).isEqualTo("java.lang.Integer");
    assertThat(map.getValueTypeName()).isEqualTo("java.lang.Boolean");
    assertThat(map).hasSize(3);
    assertThat(map.get(new RuntimeBeanReference("key"))).isEqualTo(new TypedStringValue("true", "java.lang.Boolean"));
    assertThat(map.get(new TypedStringValue("42", "java.lang.Integer"))).isEqualTo(new RuntimeBeanReference("target"));
    assertThat(map.get(new TypedStringValue("7", "java.lang.Integer"))).isEqualTo(new TypedStringValue("8", "java.lang.Long"));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
          <entry key="a" key-ref="k" value="b"/>         | a 'key' attribute OR a 'key-ref' attribute
          <entry key-ref=" " value="b"/>                | contains empty 'key-ref' attribute
          <entry key="a" value="b" value-ref="v"/>      | 'value' attribute OR 'value-ref' attribute
          <entry key="a" value-ref="v" value-type="t"/> | only allowed to contain a 'value-type'
          <entry key="a" value-ref=" "/>                | contains empty 'value-ref' attribute
          <entry key="a"><value>b</value><null/></entry> | must not contain more than one value sub-element
          """)
  void rejectsInvalidMapEntries(String entry, String message) {
    Element map = element("<map>" + entry + "</map>");

    assertThatExceptionOfType(BeanDefinitionParsingException.class)
            .isThrownBy(() -> delegate.parseMapElement(map, null))
            .withMessageContaining(message);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', quoteCharacter = '`', textBlock = """
          <property ref="a" value="b"/>                 | is only allowed to contain either 'ref' attribute
          <property ref=" "/>                           | contains empty 'ref' attribute
          <property><description>note</description></property> | must specify a ref or value
          <property><value>a</value><null/></property>   | must not contain more than one sub-element
          """)
  void rejectsAmbiguousOrMissingPropertyValues(String xml, String message) {
    var definition = new RootBeanDefinition(String.class);

    assertThatExceptionOfType(BeanDefinitionParsingException.class)
            .isThrownBy(() -> delegate.parsePropertyValue(element(xml), definition, "sample"))
            .withMessageContaining("property 'sample'", message);
  }

  private Element element(String xml) {
    reader.setValidating(false);
    return context.readDocumentFromString(xml).getDocumentElement();
  }

}
