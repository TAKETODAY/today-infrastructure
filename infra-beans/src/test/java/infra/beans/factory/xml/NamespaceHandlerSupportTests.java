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
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class NamespaceHandlerSupportTests {

  private static final String NAMESPACE = "urn:example:test";

  private final XmlReaderContext readerContext = mock(XmlReaderContext.class);

  private final ParserContext context = new ParserContext(readerContext, new BeanDefinitionParserDelegate(readerContext));

  private final NamespaceHandlerSupport handler = new NamespaceHandlerSupport() {
    @Override
    public void init() {
    }
  };

  @Test
  void delegatesParsingByLocalName() throws ParserConfigurationException {
    var element = newDocument().createElementNS(NAMESPACE, "test:widget");
    var parser = mock(BeanDefinitionParser.class);
    var definition = new RootBeanDefinition(String.class);
    given(parser.parse(element, context)).willReturn(definition);
    handler.registerBeanDefinitionParser("widget", parser);

    assertThat(handler.parse(element, context)).isSameAs(definition);
  }

  @Test
  void reportsMissingParserAndReturnsNullWhenReporterContinues() throws ParserConfigurationException {
    var element = newDocument().createElementNS(NAMESPACE, "test:missing");

    assertThat(handler.parse(element, context)).isNull();
    then(readerContext).should().fatal("Cannot locate BeanDefinitionParser for element [missing]", element);
  }

  @Test
  void dispatchesElementAndAttributeDecoratorsWithTheSameLocalNameSeparately() throws ParserConfigurationException {
    var document = newDocument();
    var element = document.createElementNS(NAMESPACE, "test:widget");
    var attribute = document.createAttributeNS(NAMESPACE, "test:widget");
    var original = new BeanDefinitionHolder(new RootBeanDefinition(String.class), "original");
    var decoratedElement = new BeanDefinitionHolder(original.getBeanDefinition(), "element");
    var decoratedAttribute = new BeanDefinitionHolder(original.getBeanDefinition(), "attribute");
    var elementDecorator = mock(BeanDefinitionDecorator.class);
    var attributeDecorator = mock(BeanDefinitionDecorator.class);
    given(elementDecorator.decorate(element, original, context)).willReturn(decoratedElement);
    given(attributeDecorator.decorate(attribute, original, context)).willReturn(decoratedAttribute);
    handler.registerBeanDefinitionDecorator("widget", elementDecorator);
    handler.registerBeanDefinitionDecoratorForAttribute("widget", attributeDecorator);

    assertThat(handler.decorate(element, original, context)).isSameAs(decoratedElement);
    assertThat(handler.decorate(attribute, original, context)).isSameAs(decoratedAttribute);
  }

  @Test
  void reportsMissingDecoratorsByNodeKind() throws ParserConfigurationException {
    var document = newDocument();
    var element = document.createElementNS(NAMESPACE, "test:missing");
    var attribute = document.createAttributeNS(NAMESPACE, "test:missing");
    var original = new BeanDefinitionHolder(new RootBeanDefinition(String.class), "original");

    assertThat(handler.decorate(element, original, context)).isNull();
    assertThat(handler.decorate(attribute, original, context)).isNull();
    then(readerContext).should().fatal("Cannot locate BeanDefinitionDecorator for element [missing]", element);
    then(readerContext).should().fatal("Cannot locate BeanDefinitionDecorator for attribute [missing]", attribute);
  }

  @Test
  void reportsUnsupportedDecoratorNode() throws ParserConfigurationException {
    var text = newDocument().createTextNode("text");
    var original = new BeanDefinitionHolder(new RootBeanDefinition(String.class), "original");

    assertThat(handler.decorate(text, original, context)).isNull();
    then(readerContext).should().fatal("Cannot decorate based on Nodes of type [" + text.getClass().getName() + "]", text);
  }

  private static Document newDocument() throws ParserConfigurationException {
    return DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder().newDocument();
  }

}
