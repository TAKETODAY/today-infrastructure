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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import infra.core.testfixture.xml.XmlContent;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class XMLEventStreamReaderTests {

  private static final String XML =
          "<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2'>content</prefix:child></root>";

  private XMLEventStreamReader streamReader;

  @BeforeEach
  void createStreamReader() throws Exception {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
    streamReader = new XMLEventStreamReader(eventReader);
  }

  @Test
  void readAll() throws Exception {
    while (streamReader.hasNext()) {
      streamReader.next();
    }
  }

  @Test
  void readCorrect() throws Exception {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    StAXSource source = new StAXSource(streamReader);
    StringWriter writer = new StringWriter();
    transformer.transform(source, new StreamResult(writer));
    Predicate<Node> nodeFilter = n ->
            n.getNodeType() != Node.DOCUMENT_TYPE_NODE && n.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE;
    assertThat(XmlContent.from(writer)).isSimilarTo(XML, nodeFilter);
  }

}
