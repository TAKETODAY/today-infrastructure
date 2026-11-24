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

package infra.oxm.xstream;

import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.basic.StringConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import infra.oxm.MarshallingFailureException;
import infra.oxm.UncategorizedMappingException;
import infra.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class XStreamMarshallerConfigurationTests {

  @Test
  void supportsAllClassesByDefaultAndAssignableConfiguredClasses() {
    var marshaller = new XStreamMarshaller();
    assertThat(marshaller.supports(String.class)).isTrue();
    marshaller.setSupportedClasses(Number.class, CharSequence.class);
    assertThat(marshaller.supports(Integer.class)).isTrue();
    assertThat(marshaller.supports(String.class)).isTrue();
    assertThat(marshaller.supports(Object.class)).isFalse();
    marshaller.setSupportedClasses();
    assertThat(marshaller.supports(Object.class)).isTrue();
    assertThat(marshaller.getDefaultEncoding()).isEqualTo("UTF-8");
  }

  @Test
  void customMapperAndClassLoaderAreUsed() {
    var marshaller = new XStreamMarshaller();
    Mapper mapper = mock();
    marshaller.setMapper(mapper);
    marshaller.setBeanClassLoader(getClass().getClassLoader());
    marshaller.afterPropertiesSet();
    assertThat(marshaller.getXStream().getMapper()).isSameAs(mapper);
    assertThat(marshaller.getXStream().getClassLoader()).isSameAs(getClass().getClassLoader());
  }

  @Test
  @SuppressWarnings("unchecked")
  void wrapsMapperWithEitherSupportedConstructor() {
    var marshaller = new XStreamMarshaller();
    marshaller.setMapperWrappers(MapperConstructor.class, WrapperConstructor.class);
    Mapper mapper = marshaller.getXStream().getMapper();
    assertThat(mapper.lookupMapperOfType(MapperConstructor.class)).isInstanceOf(MapperConstructor.class);
    assertThat(mapper.lookupMapperOfType(WrapperConstructor.class)).isInstanceOf(WrapperConstructor.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void rejectsMissingOrFailingMapperConstructors() {
    var missing = new XStreamMarshaller();
    missing.setMapperWrappers(MissingConstructor.class);
    assertThatThrownBy(missing::getXStream).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No appropriate MapperWrapper constructor");
    var failing = new XStreamMarshaller();
    failing.setMapperWrappers(FailingConstructor.class);
    assertThatThrownBy(failing::getXStream).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to construct MapperWrapper");
  }

  @Test
  void singleValueConverterAndAnnotationDetection() throws Exception {
    var marshaller = new XStreamMarshaller();
    marshaller.setConverters(new StringConverter());
    marshaller.setAutodetectAnnotations(true);
    StringWriter writer = new StringWriter();
    marshaller.marshal("hello", new StreamResult(writer));
    assertThat(writer.toString()).isEqualTo("<string>hello</string>");
  }

  @Test
  void rejectsInvalidConvertersAliasesAndAttributeMappings() {
    var converter = new XStreamMarshaller();
    converter.setConverters(mock(ConverterMatcher.class));
    assertThatThrownBy(converter::getXStream).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid ConverterMatcher");
    var alias = new XStreamMarshaller();
    alias.setAliases(Map.of("invalid", 42));
    assertThatThrownBy(alias::getXStream).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expected String or Class");
    var missingClass = new XStreamMarshaller();
    missingClass.setAliases(Map.of("invalid", "no.such.Type"));
    assertThatThrownBy(missingClass::getXStream).isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(ClassNotFoundException.class);
    var field = new XStreamMarshaller();
    field.setFieldAliases(Map.of("missingSeparator", "alias"));
    assertThatThrownBy(field::getXStream).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not contain '.'");
    for (Map<?, ?> attributes : List.of(Map.of("field", "not a class"),
            Map.of(Flight.class, 42), Map.of(42, "field"))) {
      var marshaller = new XStreamMarshaller();
      marshaller.setUseAttributeFor(attributes);
      assertThatThrownBy(marshaller::getXStream).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("useAttributesFor");
    }
  }

  @Test
  void customDriverSupportsByteStreams() throws Exception {
    var marshaller = new XStreamMarshaller();
    marshaller.setStreamDriver(new DomDriver());
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    marshaller.marshalOutputStream("hello", output);
    assertThat(marshaller.unmarshalInputStream(new ByteArrayInputStream(output.toByteArray())))
            .isEqualTo("hello");
  }

  @Test
  void rejectsUnsupportedDomNodesAndEmptyStreams() throws Exception {
    var marshaller = new XStreamMarshaller();
    var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    var text = document.createTextNode("invalid");
    assertThatThrownBy(() -> marshaller.marshal("graph", new DOMResult(text)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neither Document nor Element");
    assertThatThrownBy(() -> marshaller.unmarshal(new DOMSource(text)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neither Document nor Element");
    assertThatThrownBy(() -> marshaller.unmarshal(new StreamSource()))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neither InputStream nor Reader");
    assertThatThrownBy(() -> marshaller.unmarshal(new SAXSource()))
            .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("SAX XMLReaders");
  }

  @Test
  void unmarshalsElementAndEventReader() throws Exception {
    var marshaller = new XStreamMarshaller();
    var document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new org.xml.sax.InputSource(new StringReader("<string>hello</string>")));
    assertThat(marshaller.unmarshal(new DOMSource(document.getDocumentElement()))).isEqualTo("hello");
    var reader = javax.xml.stream.XMLInputFactory.newFactory()
            .createXMLEventReader(new StringReader("<string>hello</string>"));
    assertThat(marshaller.unmarshal(StaxUtils.createStaxSource(reader))).isEqualTo("hello");
  }

  @Test
  void translatesStaxFailuresAndUnknownExceptions() throws Exception {
    var marshaller = new XStreamMarshaller();
    XMLStreamWriter writer = mock();
    var driver = mock(com.thoughtworks.xstream.io.xml.StaxDriver.class);
    given(driver.createStaxWriter(writer)).willThrow(new XMLStreamException("broken writer"));
    marshaller.setStreamDriver(driver);
    assertThatThrownBy(() -> marshaller.marshalXmlStreamWriter("hello", writer))
            .isInstanceOf(UncategorizedMappingException.class).hasCauseInstanceOf(XMLStreamException.class);
    XMLEventReader reader = mock();
    given(reader.nextEvent()).willThrow(new XMLStreamException("broken reader"));
    assertThatThrownBy(() -> marshaller.unmarshalXmlEventReader(reader))
            .isInstanceOf(UncategorizedMappingException.class).hasCauseInstanceOf(XMLStreamException.class);
    var failure = new IllegalStateException("unknown");
    assertThat(marshaller.convertXStreamException(failure, true))
            .isInstanceOf(UncategorizedMappingException.class).hasCause(failure);
  }

  @Test
  void flushFailureDoesNotMaskMarshallingFailure() throws Exception {
    com.thoughtworks.xstream.XStream delegate = mock();
    var marshaller = new XStreamMarshaller() {
      @Override
      protected com.thoughtworks.xstream.XStream buildXStream() {
        return delegate;
      }
    };
    HierarchicalStreamDriver driver = mock();
    HierarchicalStreamWriter writer = mock();
    given(driver.createWriter(any(java.io.Writer.class))).willReturn(writer);
    doThrow(new com.thoughtworks.xstream.io.StreamException("write failed"))
            .when(delegate).marshal("hello", writer, null);
    doThrow(new IllegalStateException("flush failed")).when(writer).flush();
    marshaller.setStreamDriver(driver);
    assertThatThrownBy(() -> marshaller.marshalWriter("hello", new StringWriter()))
            .isInstanceOf(MarshallingFailureException.class).hasMessageContaining("marshalling");
    verify(writer).flush();
  }

  public static class MapperConstructor extends MapperWrapper {
    public MapperConstructor(Mapper mapper) {
      super(mapper);
    }
  }

  public static class WrapperConstructor extends MapperWrapper {
    public WrapperConstructor(MapperWrapper mapper) {
      super(mapper);
    }
  }

  public static class MissingConstructor extends MapperWrapper {
    public MissingConstructor() {
      super(null);
    }
  }

  public static class FailingConstructor extends MapperWrapper {
    public FailingConstructor(Mapper mapper) {
      super(mapper);
      throw new IllegalStateException("broken constructor");
    }
  }
}
