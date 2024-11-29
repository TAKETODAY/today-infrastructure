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

package infra.util.xml;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DomUtils}.
 *
 * @author Stephane Nicoll
 * @author Kunal Jani
 */
class DomUtilsTests {

  private static final Element SCHOOL_ELEMENT = getDocumentElement("""
          <?xml version="1.0"?>
          <school>TestSchool
          	<class teacher="Happy Teacher">Test Teacher One</class>
          	<class teacher="Sad Teacher">Test Teacher Two</class>
          	<principal>Test Principal</principal>
          	<guard>Fox Test</guard>
          </school>""");

  @Test
  void getChildElementsByTagNameWithSeveralMatchingTags() {
    List<Element> childElements = DomUtils.getChildElementsByTagName(SCHOOL_ELEMENT, "class", "principal");
    assertThat(childElements).map(Element::getNodeName).containsExactly("class", "class", "principal");
  }

  @Test
  void getChildElementsByTagNameWhenTagDoesNotExist() {
    assertThat(DomUtils.getChildElementsByTagName(SCHOOL_ELEMENT, "teacher")).isEmpty();
  }

  @Test
  void getChildElementByTagNameWithMatchingTag() {
    Element principalElement = DomUtils.getChildElementByTagName(SCHOOL_ELEMENT, "principal");
    assertThat(principalElement).isNotNull();
    assertThat(principalElement.getTextContent()).isEqualTo("Test Principal");
  }

  @Test
  void getChildElementByTagNameWithNonMatchingTag() {
    assertThat(DomUtils.getChildElementByTagName(SCHOOL_ELEMENT, "teacher")).isNull();
  }

  @Test
  void getChildElementValueByTagName() {
    assertThat(DomUtils.getChildElementValueByTagName(SCHOOL_ELEMENT, "guard")).isEqualTo("Fox Test");
  }

  @Test
  void getChildElementValueByTagNameWithNonMatchingTag() {
    assertThat(DomUtils.getChildElementValueByTagName(SCHOOL_ELEMENT, "math tutor")).isNull();
  }

  @Test
  void getChildElements() {
    List<Element> childElements = DomUtils.getChildElements(SCHOOL_ELEMENT);
    assertThat(childElements).map(Element::getNodeName).containsExactly("class", "class", "principal", "guard");
  }

  @Test
  void getTextValueWithCharacterDataNode() {
    assertThat(DomUtils.getTextValue(SCHOOL_ELEMENT)).isEqualToIgnoringWhitespace("TestSchool");
  }

  @Test
  void getTextValueWithCommentInXml() {
    Element elementWithComment = getDocumentElement("""
            <?xml version="1.0"?>
            <state>
            	<!-- This is a comment -->
            	<person>Alice</person>
            </state>""");
    assertThat(DomUtils.getTextValue(elementWithComment)).isBlank();
  }

  @Test
  void getTextValueWithEntityReference() {
    Element elementWithEntityReference = getDocumentElement("""
            <?xml version="1.0"?>
            <state>
            	&amp;
            	<person>Alice</person>
            </state>""");
    assertThat(DomUtils.getTextValue(elementWithEntityReference)).contains("&");
  }

  @Test
  void getTextValueWithEmptyElement() {
    Element emptyElement = getDocumentElement("""
            <?xml version="1.0"?>
            <person></person>""");
    assertThat(DomUtils.getTextValue(emptyElement)).isBlank();
  }

  @Test
  void nodeNameEqualsWhenTrue() {
    assertThat(DomUtils.nodeNameEquals(SCHOOL_ELEMENT, "school")).isTrue();
  }

  @Test
  void nodeNameEqualsWhenFalse() {
    assertThat(DomUtils.nodeNameEquals(SCHOOL_ELEMENT, "college")).isFalse();
  }

  private static Element getDocumentElement(String xmlContent) {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
      return document.getDocumentElement();
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to parse xml content:%n%s".formatted(xmlContent), ex);
    }
  }

}
