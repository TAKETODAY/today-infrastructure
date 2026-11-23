/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.converter;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.http.StreamingHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ObjectToStringHttpMessageConverterTests {

  @Test
  void checksBothTheMediaTypeAndConversionDirection() throws IOException {
    ConversionService conversion = mock();
    given(conversion.canConvert(String.class, Integer.class)).willReturn(true);
    given(conversion.canConvert(Long.class, String.class)).willReturn(true);
    var converter = new ObjectToStringHttpMessageConverter(conversion);

    assertThat(converter.canRead(Integer.class, MediaType.TEXT_PLAIN)).isTrue();
    assertThat(converter.canRead(Long.class, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(converter.canRead(Integer.class, MediaType.APPLICATION_JSON)).isFalse();
    assertThat(converter.canWrite(Long.class, MediaType.TEXT_PLAIN)).isTrue();
    assertThat(converter.canWrite(Integer.class, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(converter.canWrite(Long.class, MediaType.APPLICATION_JSON)).isFalse();
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> converter.supports(Long.class));
    var input = new MockHttpInputMessage(new byte[0]);
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> converter.read(Integer.class, input))
            .withMessageContaining("Unexpected null conversion result")
            .satisfies(error -> assertThat(error.getHttpInputMessage()).isSameAs(input));
    var output = new MockHttpOutputMessage();
    converter.write(5L, null, output);
    assertThat(output.getContentLength()).isZero();
    assertThat(output.getBodyAsBytes()).isEmpty();
  }

  @Test
  void readsObjectsAndWritesTextWithTheConfiguredCharset() throws IOException {
    var charset = StandardCharsets.ISO_8859_1;
    var conversion = new DefaultConversionService();
    conversion.addConverter(Integer.class, String.class, number -> "n°" + number);
    var converter = new ObjectToStringHttpMessageConverter(conversion, charset);
    converter.setWriteAcceptCharset(true);
    var input = new MockHttpInputMessage("42".getBytes(charset));
    assertThat(converter.read(Integer.class, input)).isEqualTo(42);

    var output = new MockHttpOutputMessage();
    converter.write(42, MediaType.APPLICATION_OCTET_STREAM, output);
    assertThat(output.getContentType()).isEqualTo(MediaType.TEXT_PLAIN.withCharset(charset));
    assertThat(output.getContentLength()).isEqualTo(4);
    assertThat(output.getHeaders().getAcceptCharset()).contains(charset);
    assertThat(output.getBodyAsString(charset)).isEqualTo("n°42");
  }

  @Test
  void streamsRepeatableTextAndPreservesExistingFramingHeaders() throws IOException {
    var converter = new ObjectToStringHttpMessageConverter(new DefaultConversionService());
    converter.setSupportedMediaTypes(List.of(MediaType.TEXT_PLAIN));
    var output = mock(StreamingHttpOutputMessage.class);
    var headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
    given(output.getHeaders()).willReturn(headers);
    given(output.getContentTypeAsString()).willReturn(MediaType.TEXT_PLAIN_VALUE);
    given(output.getContentLength()).willReturn(-1L);
    given(output.containsHeader(HttpHeaders.TRANSFER_ENCODING)).willReturn(true);

    converter.write(42, null, output);
    var captor = ArgumentCaptor.forClass(StreamingHttpOutputMessage.Body.class);
    verify(output).setBody(captor.capture());
    var body = captor.getValue();
    var bytes = new ByteArrayOutputStream();
    body.writeTo(bytes);
    body.writeTo(bytes);
    assertThat(body.repeatable()).isTrue();
    assertThat(bytes.toString(StandardCharsets.UTF_8)).isEqualTo("4242");
    assertThat(headers.getContentLength()).isEqualTo(-1);
  }

}
