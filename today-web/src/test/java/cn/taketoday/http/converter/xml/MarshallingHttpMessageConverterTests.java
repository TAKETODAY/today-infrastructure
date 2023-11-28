/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.converter.xml;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;

import cn.taketoday.beans.TypeMismatchException;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.http.converter.HttpMessageNotReadableException;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.oxm.Marshaller;
import cn.taketoday.oxm.MarshallingFailureException;
import cn.taketoday.oxm.Unmarshaller;
import cn.taketoday.oxm.UnmarshallingFailureException;

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
    UnmarshallingFailureException ex = new UnmarshallingFailureException("forced");

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
    MarshallingFailureException ex = new MarshallingFailureException("forced");

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