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
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;

import javax.xml.transform.sax.SAXResult;

import infra.oxm.Marshaller;
import infra.oxm.support.MarshallingSource.MarshallingXMLReader;
import infra.oxm.xstream.XStreamMarshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/20 11:54
 */
class MarshallingSourceTests {

  @Test
  void thrownBy() {
    XStreamMarshaller marshaller = new XStreamMarshaller();
    MarshallingSource marshallingSource = new MarshallingSource(marshaller, "");

    assertThatThrownBy(() -> marshallingSource.setInputSource(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("setInputSource is not supported");

    assertThatThrownBy(() -> marshallingSource.setXMLReader(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("setXMLReader is not supported");

    XMLReader xmlReader = marshallingSource.getXMLReader();
    assertThat(xmlReader)
            .isInstanceOf(MarshallingXMLReader.class);

    assertThatThrownBy(() -> xmlReader.getFeature("test"))
            .isInstanceOf(SAXNotRecognizedException.class)
            .hasMessage("test");

    assertThatThrownBy(() -> xmlReader.setFeature("test", false))
            .isInstanceOf(SAXNotRecognizedException.class)
            .hasMessage("test");
  }

  @Test
  void marshallingSource() {
    XStreamMarshaller marshaller = new XStreamMarshaller();
    MarshallingSource marshallingSource = new MarshallingSource(marshaller, "");

    assertThat(marshallingSource.getMarshaller())
            .isInstanceOf(XStreamMarshaller.class)
            .isSameAs(marshaller);

    assertThat(marshallingSource.getContent()).isEqualTo("");
  }

  @Test
  void nullMarshallerThrowsException() {
    assertThatThrownBy(() -> new MarshallingSource(null, "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'marshaller' is required");
  }

  @Test
  void nullContentThrowsException() {
    Marshaller marshaller = mock(Marshaller.class);
    assertThatThrownBy(() -> new MarshallingSource(marshaller, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'content' is required");
  }

  @Test
  void marshalFailureTriggersErrorHandler() throws Exception {
    Marshaller marshaller = mock(Marshaller.class);
    ErrorHandler errorHandler = mock(ErrorHandler.class);
    IOException ioException = new IOException("Marshal failed");

    doThrow(ioException).when(marshaller).marshal(any(), any(SAXResult.class));

    MarshallingSource.MarshallingXMLReader reader = new MarshallingSource.MarshallingXMLReader(marshaller, "content");
    reader.setErrorHandler(errorHandler);
    reader.parse("");

    verify(errorHandler).fatalError(any(SAXParseException.class));
  }

  @Test
  void marshalFailureWithoutErrorHandlerThrowsException() throws IOException {
    Marshaller marshaller = mock(Marshaller.class);
    doThrow(new IOException("Marshal failed")).when(marshaller).marshal(any(), any(SAXResult.class));

    MarshallingSource.MarshallingXMLReader reader = new MarshallingSource.MarshallingXMLReader(marshaller, "content");

    assertThatThrownBy(() -> reader.parse(""))
            .isInstanceOf(SAXParseException.class)
            .hasMessage("Marshal failed");
  }

  @Test
  void setsAndGetsLexicalHandlerProperty() throws Exception {
    Marshaller marshaller = mock(Marshaller.class);
    LexicalHandler lexicalHandler = mock(LexicalHandler.class);

    MarshallingSource.MarshallingXMLReader reader = new MarshallingSource.MarshallingXMLReader(marshaller, "content");
    reader.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler);

    assertThat(reader.getProperty("http://xml.org/sax/properties/lexical-handler"))
            .isSameAs(lexicalHandler);
  }

  @Test
  void invalidPropertyNameThrowsException() {
    Marshaller marshaller = mock(Marshaller.class);
    MarshallingSource.MarshallingXMLReader reader = new MarshallingSource.MarshallingXMLReader(marshaller, "content");

    assertThatThrownBy(() -> reader.getProperty("invalid-property"))
            .isInstanceOf(SAXNotRecognizedException.class)
            .hasMessage("invalid-property");

    assertThatThrownBy(() -> reader.setProperty("invalid-property", "value"))
            .isInstanceOf(SAXNotRecognizedException.class)
            .hasMessage("invalid-property");
  }

  @Test
  void handlersAreProperlySetAndRetrieved() {
    Marshaller marshaller = mock(Marshaller.class);
    var reader = new MarshallingXMLReader(marshaller, "content");

    ContentHandler contentHandler = mock(ContentHandler.class);
    DTDHandler dtdHandler = mock(DTDHandler.class);
    EntityResolver entityResolver = mock(EntityResolver.class);
    ErrorHandler errorHandler = mock(ErrorHandler.class);

    reader.setContentHandler(contentHandler);
    reader.setDTDHandler(dtdHandler);
    reader.setEntityResolver(entityResolver);
    reader.setErrorHandler(errorHandler);

    assertThat(reader.getContentHandler()).isSameAs(contentHandler);
    assertThat(reader.getDTDHandler()).isSameAs(dtdHandler);
    assertThat(reader.getEntityResolver()).isSameAs(entityResolver);
    assertThat(reader.getErrorHandler()).isSameAs(errorHandler);
  }

  @Test
  void parseDelegatesCorrectlyToMarshaller() throws Exception {
    Marshaller marshaller = mock(Marshaller.class);
    ContentHandler contentHandler = mock(ContentHandler.class);
    String content = "test content";

    MarshallingSource.MarshallingXMLReader reader = new MarshallingXMLReader(marshaller, content);
    reader.setContentHandler(contentHandler);
    reader.parse(new InputSource());

    verify(marshaller).marshal(eq(content), any(SAXResult.class));
  }

  @Test
  void parseWithEmptySystemIdSucceeds() throws Exception {
    Marshaller marshaller = mock(Marshaller.class);
    ContentHandler contentHandler = mock(ContentHandler.class);

    MarshallingSource.MarshallingXMLReader reader = new MarshallingSource.MarshallingXMLReader(marshaller, "content");
    reader.setContentHandler(contentHandler);
    reader.parse("");

    verify(marshaller).marshal(eq("content"), any(SAXResult.class));
  }

  @Test
  void parseWithNullInputSourceSucceeds() throws Exception {
    Marshaller marshaller = mock(Marshaller.class);
    ContentHandler contentHandler = mock(ContentHandler.class);

    MarshallingSource.MarshallingXMLReader reader = new MarshallingSource.MarshallingXMLReader(marshaller, "content");
    reader.setContentHandler(contentHandler);
    reader.parse((InputSource) null);

    verify(marshaller).marshal(eq("content"), any(SAXResult.class));
  }

  @Test
  void marshallingSourceRetainsOriginalContent() {
    Object content = new Object();
    Marshaller marshaller = mock(Marshaller.class);
    MarshallingSource source = new MarshallingSource(marshaller, content);

    assertThat(source.getContent()).isSameAs(content);
  }

  @Test
  void marshallingSourceRetainsOriginalMarshaller() {
    Marshaller marshaller = mock(Marshaller.class);
    MarshallingSource source = new MarshallingSource(marshaller, "content");

    assertThat(source.getMarshaller()).isSameAs(marshaller);
  }

  @Test
  void setInputSourceOnMarshallingSourceThrowsUnsupportedException() {
    MarshallingSource source = new MarshallingSource(mock(Marshaller.class), "content");
    InputSource inputSource = new InputSource();

    assertThatThrownBy(() -> source.setInputSource(inputSource))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("setInputSource is not supported");
  }

  @Test
  void setXMLReaderOnMarshallingSourceThrowsUnsupportedException() {
    MarshallingSource source = new MarshallingSource(mock(Marshaller.class), "content");
    XMLReader reader = mock(XMLReader.class);

    assertThatThrownBy(() -> source.setXMLReader(reader))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("setXMLReader is not supported");
  }

  @Test
  void getXMLReaderReturnsMarshallingXMLReader() {
    Marshaller marshaller = mock(Marshaller.class);
    MarshallingSource source = new MarshallingSource(marshaller, "content");

    assertThat(source.getXMLReader())
            .isInstanceOf(MarshallingSource.MarshallingXMLReader.class);
  }

}