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

package infra.messaging.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.diff.DifferenceEvaluator;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import infra.beans.TypeMismatchException;
import infra.core.testfixture.xml.XmlContent;
import infra.messaging.Message;
import infra.messaging.MessageHeaders;
import infra.messaging.support.GenericMessage;
import infra.messaging.support.MessageBuilder;
import infra.oxm.Marshaller;
import infra.oxm.Unmarshaller;
import infra.oxm.jaxb.Jaxb2Marshaller;
import infra.util.MimeType;
import jakarta.xml.bind.annotation.XmlRootElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.xmlunit.diff.ComparisonType.XML_STANDALONE;
import static org.xmlunit.diff.DifferenceEvaluators.Default;
import static org.xmlunit.diff.DifferenceEvaluators.chain;
import static org.xmlunit.diff.DifferenceEvaluators.downgradeDifferencesToEqual;

/**
 * @author Arjen Poutsma
 */
class MarshallingMessageConverterTests {

  private MarshallingMessageConverter converter;

  @BeforeEach
  void createMarshaller() throws Exception {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(MyBean.class);
    marshaller.afterPropertiesSet();

    this.converter = new MarshallingMessageConverter(marshaller);
  }

  @Test
  void fromMessage() {
    String payload = "<myBean><name>Foo</name></myBean>";
    Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
    MyBean actual = (MyBean) this.converter.fromMessage(message, MyBean.class);

    assertThat(actual).isNotNull();
    assertThat(actual.getName()).isEqualTo("Foo");
  }

  @Test
  void fromMessageInvalidXml() {
    String payload = "<myBean><name>Foo</name><myBean>";
    Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
    assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
            this.converter.fromMessage(message, MyBean.class));
  }

  @Test
  void fromMessageValidXmlWithUnknownProperty() {
    String payload = "<myBean><age>42</age><myBean>";
    Message<?> message = MessageBuilder.withPayload(payload.getBytes(StandardCharsets.UTF_8)).build();
    assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
            this.converter.fromMessage(message, MyBean.class));
  }

  @Test
  void toMessage() {
    MyBean payload = new MyBean();
    payload.setName("Foo");

    Message<?> message = this.converter.toMessage(payload, null);
    assertThat(message).isNotNull();
    String actual = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);

    DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
    assertThat(XmlContent.of(actual)).isSimilarTo("<myBean><name>Foo</name></myBean>", ev);
  }

  @Test
  void roundTripsStringPayloadsWithTheConfiguredXmlSubtype() {
    this.converter.setSerializedPayloadClass(String.class);
    var contentType = new MimeType("application", "orders+xml");
    var headers = new MessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE, contentType));
    var bean = new MyBean();
    bean.setName("München");

    Message<?> message = this.converter.toMessage(bean, headers);
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isInstanceOf(String.class);
    assertThat(message.getHeaders()).containsEntry(MessageHeaders.CONTENT_TYPE, contentType);
    assertThat(this.converter.fromMessage(message, MyBean.class)).isInstanceOfSatisfying(MyBean.class,
            result -> assertThat(result.getName()).isEqualTo("München"));
  }

  @Test
  void declinesConversionWithoutAMatchingMimeTypeOrCollaborator() {
    var message = new GenericMessage<>("<myBean/>");
    var jsonHeaders = new MessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE, MimeType.APPLICATION_JSON));
    var converter = new MarshallingMessageConverter();
    var bean = new MyBean();
    assertThat(converter.fromMessage(message, MyBean.class)).isNull();
    assertThat(converter.toMessage(bean, null)).isNull();

    converter.setMarshaller(this.converter.getMarshaller());
    converter.setUnmarshaller(this.converter.getUnmarshaller());
    assertThat(converter.getMarshaller()).isSameAs(this.converter.getMarshaller());
    assertThat(converter.getUnmarshaller()).isSameAs(this.converter.getUnmarshaller());
    assertThat(converter.toMessage(bean, jsonHeaders)).isNull();
    assertThat(converter.fromMessage(new GenericMessage<>("{}", jsonHeaders), MyBean.class)).isNull();
    assertThat(converter.toMessage("unsupported", null)).isNull();
    assertThat(converter.fromMessage(message, Integer.class)).isNull();
  }

  @Test
  void reportsADelegateReturningAnIncompatibleType() throws Exception {
    Unmarshaller unmarshaller = mock();
    given(unmarshaller.supports(MyBean.class)).willReturn(true);
    given(unmarshaller.unmarshal(any(Source.class))).willReturn("wrong type");
    var converter = new MarshallingMessageConverter();
    converter.setUnmarshaller(unmarshaller);
    var message = new GenericMessage<>("<myBean/>");

    assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() -> converter.fromMessage(message, MyBean.class))
            .withMessageContaining("Could not unmarshal XML").withCauseInstanceOf(TypeMismatchException.class)
            .satisfies(ex -> assertThat(ex.getFailedMessage()).isSameAs(message));
  }

  @Test
  void wrapsMarshallerFailureAndDoesNotUseWriteOnlyMarshallerForRead() throws Exception {
    Marshaller marshaller = mock();
    var converter = new MarshallingMessageConverter(marshaller);
    var bean = new MyBean();
    var failure = new IllegalStateException("marshaller failed");
    given(marshaller.supports(MyBean.class)).willReturn(true);
    willThrow(failure).given(marshaller).marshal(same(bean), any(Result.class));

    assertThat(converter.getUnmarshaller()).isNull();
    assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() -> converter.toMessage(bean, null))
            .withMessageContaining("Could not marshal XML").withCause(failure);
  }

  @XmlRootElement
  public static class MyBean {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}
