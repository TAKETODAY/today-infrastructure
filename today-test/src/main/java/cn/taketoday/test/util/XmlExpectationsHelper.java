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

package cn.taketoday.test.util;

import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A helper class for assertions on XML content.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class XmlExpectationsHelper {

  /**
   * Parse the content as {@link Node} and apply a {@link Matcher}.
   */
  public void assertNode(String content, Matcher<? super Node> matcher) throws Exception {
    Document document = parseXmlString(content);
    assertThat("Body content", document, matcher);
  }

  private Document parseXmlString(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder documentBuilder = factory.newDocumentBuilder();
    InputSource inputSource = new InputSource(new StringReader(xml));
    return documentBuilder.parse(inputSource);
  }

  /**
   * Parse the content as {@link DOMSource} and apply a {@link Matcher}.
   *
   * @see <a href="https://github.com/davidehringer/xml-matchers">xml-matchers</a>
   */
  public void assertSource(String content, Matcher<? super Source> matcher) throws Exception {
    Document document = parseXmlString(content);
    assertThat("Body content", new DOMSource(document), matcher);
  }

  /**
   * Parse the expected and actual content strings as XML and assert that the
   * two are "similar" -- i.e. they contain the same elements and attributes
   * regardless of order.
   * <p>Use of this method assumes the
   * <a href="https://github.com/xmlunit/xmlunit">XMLUnit</a> library is available.
   *
   * @param expected the expected XML content
   * @param actual the actual XML content
   */
  public void assertXmlEqual(String expected, String actual) throws Exception {
    XmlUnitDiff diff = new XmlUnitDiff(expected, actual);
    if (diff.hasDifferences()) {
      AssertionErrors.fail("Body content " + diff);
    }
  }

  /**
   * Inner class to prevent hard dependency on XML Unit.
   */
  private static class XmlUnitDiff {

    private final Diff diff;

    XmlUnitDiff(String expected, String actual) {
      this.diff = DiffBuilder.compare(expected).withTest(actual)
              .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
              .ignoreWhitespace().ignoreComments()
              .checkForSimilar()
              .build();
    }

    public boolean hasDifferences() {
      return this.diff.hasDifferences();
    }

    @Override
    public String toString() {
      return this.diff.toString();
    }

  }

}
