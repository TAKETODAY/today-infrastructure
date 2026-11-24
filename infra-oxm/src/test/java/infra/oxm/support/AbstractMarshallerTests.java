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

package infra.oxm.support;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import infra.oxm.UnmarshallingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AbstractMarshallerTests {

  private final AbstractMarshaller marshaller = mock(AbstractMarshaller.class, CALLS_REAL_METHODS);

  @Test
  void createsAndReusesDocumentFactory() throws Exception {
    DOMResult first = new DOMResult();
    DOMResult second = new DOMResult();
    marshaller.marshal("graph", first);
    marshaller.marshal("graph", second);
    assertThat(first.getNode()).isNotNull().isNotSameAs(second.getNode());
    verify(marshaller).marshalDomNode("graph", first.getNode());
    DOMSource source = new DOMSource();
    marshaller.unmarshal(source);
    assertThat(source.getNode()).isNotNull();
    verify(marshaller).unmarshalDomNode(source.getNode());
  }

  @Test
  void documentConfigurationFailureIsTranslated() throws Exception {
    ParserConfigurationException failure = new ParserConfigurationException("broken factory");
    given(marshaller.createDocumentBuilderFactory()).willThrow(failure);
    assertThatThrownBy(() -> marshaller.marshal("graph", new DOMResult()))
            .isInstanceOf(UnmarshallingFailureException.class).hasCause(failure);
  }

  @Test
  void parserOptionsResetCachedFactories() throws Exception {
    assertThat(marshaller.isSupportDtd()).isFalse();
    assertThat(marshaller.isProcessExternalEntities()).isFalse();
    XMLReader first = marshaller.createXmlReader();
    assertThat(first.getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isTrue();
    assertThat(first.getEntityResolver().resolveEntity(null, "file:///not-read").getCharacterStream().read()).isEqualTo(-1);
    marshaller.createXmlReader();
    marshaller.buildDocument();

    marshaller.setProcessExternalEntities(true);
    assertThat(marshaller.isSupportDtd()).isTrue();
    assertThat(marshaller.createXmlReader().getFeature("http://xml.org/sax/features/external-general-entities")).isTrue();
    assertThat(marshaller.createDocumentBuilder(marshaller.createDocumentBuilderFactory())).isNotNull();
    marshaller.setProcessExternalEntities(false);
    marshaller.setSupportDtd(false);
    assertThat(marshaller.createXmlReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isTrue();
    assertThat(marshaller.createDocumentBuilderFactory().isNamespaceAware()).isTrue();
  }

  @Test
  void emptySaxSourceGetsReaderAndInputSource() throws Exception {
    SAXSource source = new SAXSource();
    marshaller.unmarshal(source);
    assertThat(source.getXMLReader()).isNotNull();
    assertThat(source.getInputSource()).isNotNull();
    verify(marshaller).unmarshalSaxReader(source.getXMLReader(), source.getInputSource());
  }

  @Test
  void saxReaderCreationFailureIsTranslated() throws Exception {
    SAXException failure = new SAXException("broken parser");
    given(marshaller.createXmlReader()).willThrow(failure);
    assertThatThrownBy(() -> marshaller.unmarshal(new SAXSource()))
            .isInstanceOf(UnmarshallingFailureException.class).hasCause(failure);
  }

  @Test
  void streamSourcesUseSecureSaxByDefault() throws Exception {
    var stream = new ByteArrayInputStream(new byte[0]);
    var reader = new StringReader("");
    given(marshaller.unmarshalSaxReader(any(), any())).willAnswer(invocation -> invocation.getArgument(1));
    InputSource bytes = (InputSource) marshaller.unmarshal(new StreamSource(stream));
    assertThat(bytes.getByteStream()).isSameAs(stream);
    assertThat(bytes.getEncoding()).isNull();
    InputSource chars = (InputSource) marshaller.unmarshal(new StreamSource(reader));
    assertThat(chars.getCharacterStream()).isSameAs(reader);
    InputSource url = (InputSource) marshaller.unmarshal(new StreamSource("file:///not-read"));
    assertThat(url.getSystemId()).isEqualTo("file:///not-read");
  }

  @Test
  void enabledExternalEntitiesAllowDirectStreamDelegation() throws Exception {
    marshaller.setProcessExternalEntities(true);
    var stream = new ByteArrayInputStream(new byte[0]);
    var reader = new StringReader("");
    marshaller.unmarshal(new StreamSource(stream));
    marshaller.unmarshal(new StreamSource(reader));
    verify(marshaller).unmarshalInputStream(stream);
    verify(marshaller).unmarshalReader(reader);
    marshaller.setSupportDtd(false);
    marshaller.unmarshal(new StreamSource(stream));
    verify(marshaller).unmarshalSaxReader(any(), any());
  }

  @Test
  void nullPointerFailuresAreTranslatedOnlyWhenDtdIsDisabled() throws Exception {
    NullPointerException failure = new NullPointerException("provider failure");
    var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    given(marshaller.unmarshalDomNode(document)).willThrow(failure);
    given(marshaller.unmarshalSaxReader(any(), any())).willThrow(failure);
    assertThatThrownBy(() -> marshaller.unmarshal(new DOMSource(document)))
            .isInstanceOf(UnmarshallingFailureException.class).hasCause(failure);
    assertThatThrownBy(() -> marshaller.unmarshal(new SAXSource()))
            .isInstanceOf(UnmarshallingFailureException.class).hasMessageContaining("DTD");
    marshaller.setSupportDtd(true);
    assertThatThrownBy(() -> marshaller.unmarshal(new DOMSource(document))).isSameAs(failure);
    assertThatThrownBy(() -> marshaller.unmarshal(new SAXSource())).isSameAs(failure);
  }

  @Test
  void rejectsUnsupportedOrIncompleteSourcesAndResults() {
    assertThatThrownBy(() -> marshaller.marshal("graph", mock(Result.class)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown Result");
    assertThatThrownBy(() -> marshaller.unmarshal(mock(Source.class)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Unknown Source");
    assertThatThrownBy(() -> marshaller.marshal("graph", new StreamResult()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neither OutputStream nor Writer");
    assertThatThrownBy(() -> marshaller.marshal("graph", new SAXResult()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ContentHandler");
    assertThatThrownBy(() -> marshaller.marshalStaxResult("graph", mock(javax.xml.transform.stax.StAXResult.class)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neither XMLStreamWriter");
    assertThatThrownBy(() -> marshaller.unmarshalStaxSource(mock(javax.xml.transform.stax.StAXSource.class)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neither XMLStreamReader");
  }

  @Test
  void delegatesEventReader() throws Exception {
    XMLEventReader reader = javax.xml.stream.XMLInputFactory.newFactory()
            .createXMLEventReader(new StringReader("<root/>"));
    Source source = infra.util.xml.StaxUtils.createStaxSource(reader);
    marshaller.unmarshal(source);
    verify(marshaller).unmarshalXmlEventReader(reader);
    marshaller.marshal("graph", new StreamResult(new StringWriter()));
  }
}
