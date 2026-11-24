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

package infra.oxm.jaxb;

import org.junit.jupiter.api.Test;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.StringReader;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import infra.core.io.ClassPathResource;
import infra.oxm.MarshallingFailureException;
import infra.oxm.UncategorizedMappingException;
import infra.oxm.UnmarshallingFailureException;
import infra.oxm.ValidationFailureException;
import infra.oxm.jaxb.test.FlightType;
import infra.oxm.jaxb.test.Flights;
import infra.util.xml.StaxUtils;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.MarshalException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEventHandler;
import jakarta.xml.bind.ValidationException;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class Jaxb2MarshallerConfigurationTests {

  private static final String CONTEXT_PATH = "infra.oxm.jaxb.test";

  @Test
  void contextPathsAndPropertiesWorkWithBothClassLoaderChoices() throws Exception {
    for (boolean explicitLoader : new boolean[] { false, true }) {
      var marshaller = new Jaxb2Marshaller();
      marshaller.setContextPaths(CONTEXT_PATH);
      marshaller.setJaxbContextProperties(Map.of());
      if (explicitLoader) {
        marshaller.setBeanClassLoader(getClass().getClassLoader());
      }
      marshaller.afterPropertiesSet();
      assertThat(marshaller.getContextPath()).isEqualTo(CONTEXT_PATH);
      assertThat(marshaller.getJaxbContext()).isSameAs(marshaller.getJaxbContext());
      assertThat(marshaller.supports(Flights.class)).isTrue();
    }
    var marshaller = new Jaxb2Marshaller();
    marshaller.setContextPath(CONTEXT_PATH);
    marshaller.setBeanClassLoader(getClass().getClassLoader());
    marshaller.afterPropertiesSet();
    assertThat(marshaller.supports(Flights.class)).isTrue();
    assertThatThrownBy(() -> marshaller.setContextPaths()).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void contextPropertiesApplyToClassesAndScannedPackages() throws Exception {
    var classes = new Jaxb2Marshaller();
    classes.setClassesToBeBound(Flights.class);
    classes.setJaxbContextProperties(Map.of());
    classes.afterPropertiesSet();
    assertThat(classes.getClassesToBeBound()).containsExactly(Flights.class);
    var packages = new Jaxb2Marshaller();
    packages.setPackagesToScan(CONTEXT_PATH);
    packages.setJaxbContextProperties(Map.of());
    packages.afterPropertiesSet();
    assertThat(packages.getPackagesToScan()).containsExactly(CONTEXT_PATH);
    assertThat(packages.getClassesToBeBound()).contains(Flights.class, FlightType.class);
    assertThat(new Jaxb2Marshaller().getJaxbContext()).isNotNull();
  }

  @Test
  void rejectsConflictingContextConfiguration() {
    var pathAndClasses = new Jaxb2Marshaller();
    pathAndClasses.setContextPath(CONTEXT_PATH);
    pathAndClasses.setClassesToBeBound(Flights.class);
    assertThatThrownBy(pathAndClasses::afterPropertiesSet).isInstanceOf(IllegalArgumentException.class);
    var pathAndPackages = new Jaxb2Marshaller();
    pathAndPackages.setContextPath(CONTEXT_PATH);
    pathAndPackages.setPackagesToScan(CONTEXT_PATH);
    assertThatThrownBy(pathAndPackages::afterPropertiesSet).isInstanceOf(IllegalArgumentException.class);
    var classesAndPackages = new Jaxb2Marshaller();
    classesAndPackages.setClassesToBeBound(Flights.class);
    classesAndPackages.setPackagesToScan(CONTEXT_PATH);
    assertThatThrownBy(classesAndPackages::afterPropertiesSet).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rootElementCheckingCanBeDisabled() {
    var marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(FlightType.class);
    assertThat(marshaller.supports(FlightType.class)).isFalse();
    marshaller.setCheckForXmlRootElement(false);
    assertThat(marshaller.supports(FlightType.class)).isTrue();
    assertThat(new Jaxb2Marshaller().supports(Flights.class)).isFalse();
  }

  @Test
  void genericTypeSupportRejectsUnrecognizedShapes() {
    var marshaller = new Jaxb2Marshaller();
    GenericArrayType bytes = () -> byte.class;
    GenericArrayType strings = () -> String.class;
    assertThat(marshaller.supports(elementType(bytes))).isTrue();
    assertThat(marshaller.supports(elementType(strings))).isFalse();
    assertThat(marshaller.supports(elementType(mock(Type.class)))).isFalse();
    assertThat(marshaller.supports(mock(Type.class))).isFalse();
    ParameterizedType wrongArity = mock();
    given(wrongArity.getRawType()).willReturn(JAXBElement.class);
    given(wrongArity.getActualTypeArguments()).willReturn(new Type[0]);
    assertThat(marshaller.supports(wrongArity)).isFalse();
  }

  private static ParameterizedType elementType(Type argument) {
    ParameterizedType type = mock();
    given(type.getRawType()).willReturn(JAXBElement.class);
    given(type.getActualTypeArguments()).willReturn(new Type[] { argument });
    return type;
  }

  @Test
  void initializesNativeDelegatesWithPropertiesListenersAndAdapters() throws Exception {
    var marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(Flights.class);
    marshaller.setMarshallerProperties(Map.of(Marshaller.JAXB_FORMATTED_OUTPUT, true));
    marshaller.setUnmarshallerProperties(Map.of("custom", "value"));
    Marshaller.Listener marshalListener = mock();
    Unmarshaller.Listener unmarshalListener = mock();
    ValidationEventHandler handler = mock();
    var adapter = new StringAdapter();
    marshaller.setMarshallerListener(marshalListener);
    marshaller.setUnmarshallerListener(unmarshalListener);
    marshaller.setValidationEventHandler(handler);
    marshaller.setAdapters(adapter);
    Marshaller nativeMarshaller = marshaller.createMarshaller();
    assertThat(nativeMarshaller.getProperty(Marshaller.JAXB_FORMATTED_OUTPUT)).isEqualTo(true);
    assertThat(nativeMarshaller.getListener()).isSameAs(marshalListener);
    assertThat(nativeMarshaller.getAdapter(StringAdapter.class)).isSameAs(adapter);
    Unmarshaller nativeUnmarshaller = mock();
    marshaller.initJaxbUnmarshaller(nativeUnmarshaller);
    verify(nativeUnmarshaller).setProperty("custom", "value");
    verify(nativeUnmarshaller).setListener(unmarshalListener);
    verify(nativeUnmarshaller).setEventHandler(handler);
    verify(nativeUnmarshaller).setAdapter(adapter);
    assertThatThrownBy(marshaller::createUnmarshaller).isInstanceOf(UncategorizedMappingException.class);
    marshaller.setMarshallerProperties(Map.of("invalid", true));
    assertThatThrownBy(marshaller::createMarshaller).isInstanceOf(UncategorizedMappingException.class);
  }

  @Test
  void mappedClassWorksWithStreamAndBothStaxReaders() throws Exception {
    var marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(FlightType.class);
    marshaller.setMappedClass(FlightType.class);
    String xml = "<flight><number>42</number></flight>";
    var factory = XMLInputFactory.newFactory();
    for (Source source : new Source[] {
            new StreamSource(new StringReader(xml)),
            StaxUtils.createStaxSource(factory.createXMLStreamReader(new StringReader(xml))),
            StaxUtils.createStaxSource(factory.createXMLEventReader(new StringReader(xml))) }) {
      assertThat(marshaller.unmarshal(source)).isInstanceOf(FlightType.class);
    }
    assertThatThrownBy(() -> marshaller.unmarshalStaxSource(mock(Unmarshaller.class),
            mock(javax.xml.transform.stax.StAXSource.class)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neither XMLStreamReader");
  }

  @Test
  void schemaCanBeReloadedAndUsesConfiguredResolver() throws Exception {
    var marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(Flights.class);
    marshaller.setSchemaLanguage(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    marshaller.setSchemaResourceResolver(mock(LSResourceResolver.class));
    marshaller.setSchema(new ClassPathResource("schema/flight.xsd"));
    marshaller.afterPropertiesSet();
    marshaller.afterPropertiesSet();
    assertThat(marshaller.createMarshaller().getSchema()).isNotNull();
    marshaller.setSchema(new ClassPathResource("missing-schema.xsd"));
    assertThatThrownBy(marshaller::afterPropertiesSet).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Resource does not exist");
  }

  @Test
  void suppliedXmlReaderIsSecuredAndFailuresLeaveOriginalSource() throws Exception {
    Unmarshaller delegate = mock();
    var marshaller = withUnmarshaller(delegate);
    XMLReader reader = mock();
    SAXSource source = new SAXSource(reader, new InputSource());
    marshaller.unmarshal(source);
    var captor = org.mockito.ArgumentCaptor.forClass(org.xml.sax.EntityResolver.class);
    verify(reader).setEntityResolver(captor.capture());
    assertThat(captor.getValue().resolveEntity(null, "not-read").getCharacterStream().read()).isEqualTo(-1);
    doThrow(new IllegalStateException("unsupported resolver")).when(reader).setEntityResolver(any());
    marshaller.unmarshal(source);
    verify(delegate).unmarshal(source);
  }

  @Test
  void nullPointerFailuresAreTranslatedOnlyWithDtdDisabled() throws Exception {
    Unmarshaller delegate = mock();
    NullPointerException failure = new NullPointerException("provider failure");
    given(delegate.unmarshal(any(Source.class))).willThrow(failure);
    var marshaller = withUnmarshaller(delegate);
    assertThatThrownBy(() -> marshaller.unmarshal(new DOMSource()))
            .isInstanceOf(UnmarshallingFailureException.class).hasCause(failure);
    marshaller.setSupportDtd(true);
    assertThatThrownBy(() -> marshaller.unmarshal(new DOMSource())).isSameAs(failure);
  }

  @Test
  void nativeCreationFailuresAreTranslated() throws Exception {
    JAXBContext context = mock();
    var marshaller = new Jaxb2Marshaller() {
      @Override
      public JAXBContext getJaxbContext() {
        return context;
      }
    };
    given(context.createMarshaller()).willThrow(new MarshalException("marshal"));
    given(context.createUnmarshaller()).willThrow(new UnmarshalException("unmarshal"));
    assertThatThrownBy(marshaller::createMarshaller).isInstanceOf(MarshallingFailureException.class);
    assertThatThrownBy(marshaller::createUnmarshaller).isInstanceOf(UnmarshallingFailureException.class);
    assertThat(marshaller.convertJaxbException(new ValidationException("validation")))
            .isInstanceOf(ValidationFailureException.class);
    assertThat(marshaller.convertJaxbException(new JAXBException("other")))
            .isInstanceOf(UncategorizedMappingException.class);
  }

  private static Jaxb2Marshaller withUnmarshaller(Unmarshaller delegate) {
    return new Jaxb2Marshaller() {
      @Override
      public Unmarshaller createUnmarshaller() {
        return delegate;
      }
    };
  }

  public static class StringAdapter extends XmlAdapter<String, String> {
    @Override
    public String unmarshal(String value) {
      return value;
    }

    @Override
    public String marshal(String value) {
      return value;
    }
  }
}
