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

package infra.http.converter.xml;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;

import infra.beans.TypeMismatchException;
import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.oxm.Marshaller;
import infra.oxm.MarshallingFailureException;
import infra.oxm.Unmarshaller;
import infra.oxm.UnmarshallingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/4/8 11:24
 */
class MarshallingHttpMessageConverterTests {

  @Test
  public void canRead() {
    Unmarshaller unmarshaller = mock();

    given(unmarshaller.supports(Integer.class)).willReturn(false);
    given(unmarshaller.supports(String.class)).willReturn(true);

    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();
    converter.setUnmarshaller(unmarshaller);

    assertThat(converter.canRead(Boolean.class, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(converter.canRead(Integer.class, MediaType.TEXT_XML)).isFalse();
    assertThat(converter.canRead(String.class, MediaType.TEXT_XML)).isTrue();
  }

  @Test
  public void canWrite() {
    Marshaller marshaller = mock();

    given(marshaller.supports(Integer.class)).willReturn(false);
    given(marshaller.supports(String.class)).willReturn(true);

    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();
    converter.setMarshaller(marshaller);

    assertThat(converter.canWrite(Boolean.class, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(converter.canWrite(Integer.class, MediaType.TEXT_XML)).isFalse();
    assertThat(converter.canWrite(String.class, MediaType.TEXT_XML)).isTrue();
  }

  @Test
  public void read() throws Exception {
    String body = "<root>Hello World</root>";
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));

    Unmarshaller unmarshaller = mock();
    given(unmarshaller.unmarshal(isA(StreamSource.class))).willReturn(body);

    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();
    converter.setUnmarshaller(unmarshaller);

    String result = (String) converter.read(Object.class, inputMessage);
    assertThat(result).as("Invalid result").isEqualTo(body);
  }

  @Test
  public void readWithTypeMismatchException() throws Exception {
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);

    Marshaller marshaller = mock();
    Unmarshaller unmarshaller = mock();
    given(unmarshaller.unmarshal(isA(StreamSource.class))).willReturn(3);

    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller, unmarshaller);
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> converter.read(String.class, inputMessage))
            .withCauseInstanceOf(TypeMismatchException.class);
  }

  @Test
  public void readWithMarshallingFailureException() throws Exception {
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
    UnmarshallingFailureException ex = new UnmarshallingFailureException("forced", null);

    Unmarshaller unmarshaller = mock();
    given(unmarshaller.unmarshal(isA(StreamSource.class))).willThrow(ex);

    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();
    converter.setUnmarshaller(unmarshaller);

    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> converter.read(Object.class, inputMessage)).withCause(ex);
  }

  @Test
  public void write() throws Exception {
    String body = "<root>Hello World</root>";
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

    Marshaller marshaller = mock();
    willDoNothing().given(marshaller).marshal(eq(body), isA(Result.class));

    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller);
    converter.write(body, null, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType())
            .as("Invalid content-type").isEqualTo(new MediaType("application", "xml"));
  }

  @Test
  public void writeWithMarshallingFailureException() throws Exception {
    String body = "<root>Hello World</root>";
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MarshallingFailureException ex = new MarshallingFailureException("forced", null);

    Marshaller marshaller = mock();
    willThrow(ex).given(marshaller).marshal(eq(body), isA(Result.class));

    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller);
    assertThatExceptionOfType(HttpMessageNotWritableException.class)
            .isThrownBy(() -> converter.write(body, null, outputMessage)).withCause(ex);
  }

  @Test
  public void supports() {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            new MarshallingHttpMessageConverter().supports(Object.class));
  }
}