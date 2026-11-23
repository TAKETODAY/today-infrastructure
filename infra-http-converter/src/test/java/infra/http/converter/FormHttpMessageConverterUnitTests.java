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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.ByteArrayResource;
import infra.core.io.InputStreamResource;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.http.StreamingHttpOutputMessage;
import infra.util.MultiValueMap;

import static infra.http.MediaType.APPLICATION_FORM_URLENCODED;
import static infra.http.MediaType.MULTIPART_FORM_DATA;
import static infra.http.MediaType.MULTIPART_MIXED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

class FormHttpMessageConverterUnitTests {

  private final FormHttpMessageConverter converter = new FormHttpMessageConverter();

  @Test
  void distinguishesUrlEncodedMapsFromWritableMultipartMaps() {
    var type = ResolvableType.forClass(MultiValueMap.class);
    assertThat(this.converter.canRead(type, APPLICATION_FORM_URLENCODED)).isTrue();
    assertThat(this.converter.canRead(MultiValueMap.class, null)).isTrue();
    assertThat(this.converter.canRead(MultiValueMap.class, MULTIPART_FORM_DATA)).isFalse();
    assertThat(this.converter.canRead(String.class, APPLICATION_FORM_URLENCODED)).isFalse();
    assertThat(this.converter.canRead(Map.class, APPLICATION_FORM_URLENCODED)).isTrue();
    assertThat(this.converter.canRead(Map.class, null)).isFalse();
    assertThat(this.converter.canWrite(type, MultiValueMap.class, MULTIPART_MIXED)).isTrue();
    assertThat(this.converter.canWrite(MultiValueMap.class, MediaType.ALL)).isTrue();
    assertThat(this.converter.canWrite(Map.class, APPLICATION_FORM_URLENCODED)).isTrue();
    assertThat(this.converter.canWrite(Map.class, MULTIPART_MIXED)).isFalse();
    assertThat(this.converter.canWrite(MultiValueMap.class, MediaType.APPLICATION_JSON)).isFalse();
  }

  @Test
  void configuresMediaTypesAndUpdatesOnlyCharsetAwarePartConverters() {
    var extra = MediaType.parseMediaType("multipart/custom");
    this.converter.setSupportedMediaTypes(List.of(APPLICATION_FORM_URLENCODED));
    this.converter.addSupportedMediaTypes(extra);
    assertThat(this.converter.getSupportedMediaTypes()).containsExactly(APPLICATION_FORM_URLENCODED, extra);
    assertThat(this.converter.canWrite(MultiValueMap.class, extra)).isTrue();

    var text = new StringHttpMessageConverter();
    var bytes = new ByteArrayHttpMessageConverter();
    HttpMessageConverter<?> custom = mock();
    this.converter.setPartConverters(new ArrayList<>(List.of(text, bytes)));
    this.converter.addPartConverter(custom);
    this.converter.setCharset(StandardCharsets.ISO_8859_1);
    this.converter.setCharset(StandardCharsets.ISO_8859_1);
    assertThat(text.getDefaultCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(bytes.getDefaultCharset()).isNull();
    this.converter.setCharset(null);
    assertThat(text.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(this.converter.getPartConverters()).containsExactly(text, bytes, custom);
  }

  @Test
  void readsRepeatedNamesAndDistinguishesMissingFromEmptyValues() throws IOException {
    String body = "tag=one+two&tag=caf%E9&empty=&flag";
    var message = input(body);
    message.getHeaders().setContentType(APPLICATION_FORM_URLENCODED.withCharset(StandardCharsets.ISO_8859_1));
    var result = (MultiValueMap<?, ?>) this.converter.read(ResolvableType.forClass(MultiValueMap.class), message, null);
    assertThat(result.get("tag")).isEqualTo(List.of("one two", "café"));
    assertThat(result.get("empty")).isEqualTo(List.of(""));
    assertThat(result.get("flag")).hasSize(1).allMatch(value -> value == null);

    assertThat(this.converter.read(Map.class, input("a=first&a=second")).get("a")).isEqualTo("first");
    assertThat(this.converter.read(null, input("a=%C3%A9"))).isInstanceOf(MultiValueMap.class);
  }

  @Test
  void reportsMalformedFormDataWithTheOriginalMessage() {
    var message = input("name=%GG");
    message.getHeaders().setContentType(APPLICATION_FORM_URLENCODED);
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> this.converter.read(Map.class, message))
            .withCauseInstanceOf(IllegalArgumentException.class)
            .satisfies(error -> assertThat(error.getHttpInputMessage()).isSameAs(message));
  }

  @Test
  void writesSingleValueMapsWithTheExplicitCharsetAndExactLength() throws IOException {
    var form = new LinkedHashMap<String, Object>();
    form.put("nom", "café");
    form.put("a b", 42);
    form.put("flag", null);
    form.put(null, "");
    var type = APPLICATION_FORM_URLENCODED.withCharset(StandardCharsets.ISO_8859_1);
    var output = new MockHttpOutputMessage();
    this.converter.write(form, ResolvableType.forClass(Map.class), type, output, null);
    assertThat(output.getBodyAsString()).isEqualTo("nom=caf%E9&a+b=42&flag");
    assertThat(output.getHeaders().getContentType()).isEqualTo(type);
    assertThat(output.getHeaders().getContentLength()).isEqualTo(output.getBodyAsBytes().length);
    form.put(null, null);
    var defaults = new MockHttpOutputMessage();
    this.converter.write(form, APPLICATION_FORM_URLENCODED, defaults);
    assertThat(defaults.getHeaders().getContentType()).isEqualTo(APPLICATION_FORM_URLENCODED);
    assertThat(defaults.getBodyAsString()).isEqualTo("nom=caf%C3%A9&a+b=42&flag");
    form.put(null, "ignored key with actual value");
    assertThatIllegalArgumentException().isThrownBy(() -> this.converter.write(form, null, new MockHttpOutputMessage()))
            .withMessageContaining("Null name");
  }

  @Test
  void writesRepeatedFormValuesAndAllowsAnEmptyListUnderANullName() throws IOException {
    MultiValueMap<String, Object> form = MultiValueMap.forLinkedHashMap();
    form.put(null, List.of());
    form.add("tag", "a+b");
    form.add("tag", "café");
    form.add("flag", null);
    var output = new MockHttpOutputMessage();
    this.converter.write(form, null, output);
    assertThat(output.getHeaders().getContentType()).isEqualTo(APPLICATION_FORM_URLENCODED);
    assertThat(output.getBodyAsString()).isEqualTo("tag=a%2Bb&tag=caf%C3%A9&flag");

    this.converter.setCharset(StandardCharsets.ISO_8859_1);
    var explicitType = new MockHttpOutputMessage();
    this.converter.write(form, APPLICATION_FORM_URLENCODED, explicitType);
    assertThat(explicitType.getHeaders().getContentType().getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
    assertThat(explicitType.getBodyAsString()).isEqualTo("tag=a%2Bb&tag=caf%E9&flag");

    form.put(null, List.of("invalid"));
    assertThatIllegalArgumentException().isThrownBy(() -> this.converter.write(form, null, new MockHttpOutputMessage()))
            .withMessageContaining("Null name");
  }

  @Test
  void infersMultipartFromBinaryDataAndWritesPartHeadersAndClosingBoundary() throws IOException {
    this.converter.setCharset(StandardCharsets.ISO_8859_1);
    var headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
    headers.add("X-Part", "first");
    headers.add("X-Part", "second");
    MultiValueMap<String, Object> parts = MultiValueMap.forLinkedHashMap();
    parts.add("skip", null);
    parts.add("text", "café");
    parts.add("binary", new HttpEntity<>(new byte[] { 65, 66 }, headers));
    var output = new MockHttpOutputMessage();
    this.converter.write(parts, null, output);

    var type = output.getHeaders().getContentType();
    assertThat(type.isCompatibleWith(MULTIPART_FORM_DATA)).isTrue();
    assertThat(type.getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
    String boundary = type.getParameter("boundary");
    assertThat(output.getBodyAsString(StandardCharsets.ISO_8859_1))
            .startsWith("--" + boundary + "\r\n")
            .contains("name=\"text\"", "\r\n\r\ncafé\r\n", "name=\"binary\"", "X-Part: first\r\nX-Part: second\r\n",
                    "\r\n\r\nAB\r\n")
            .doesNotContain("skip")
            .endsWith("--" + boundary + "--\r\n");
  }

  @Test
  void streamsRepeatablePartsAndEncodesResourceFilenames() throws IOException {
    this.converter.setCharset(StandardCharsets.ISO_8859_1);
    this.converter.setMultipartCharset(StandardCharsets.UTF_8);
    var file = new ByteArrayResource(new byte[] { 65 }) {
      @Override
      public String getName() {
        return "café.txt";
      }
    };
    MultiValueMap<String, Object> parts = MultiValueMap.forLinkedHashMap();
    parts.add("skip", null);
    parts.add("file", new HttpEntity<>(file));
    var output = new CapturingOutputMessage();
    this.converter.write(parts, MULTIPART_MIXED, output);

    assertThat(output.getHeaders().getContentType().getCharset()).isNull();
    assertThat(output.body.repeatable()).isTrue();
    var bytes = new ByteArrayOutputStream();
    output.body.writeTo(bytes);
    assertThat(bytes.toString(StandardCharsets.US_ASCII)).contains("filename*=UTF-8''caf%C3%A9.txt", "\r\n\r\nA\r\n");
    var again = new ByteArrayOutputStream();
    output.body.writeTo(again);
    assertThat(again.toByteArray()).isEqualTo(bytes.toByteArray());
  }

  @Test
  void doesNotMarkStreamBackedResourcesOrUnknownPartsAsRepeatable() throws IOException {
    this.converter.setCharset(StandardCharsets.US_ASCII);
    MultiValueMap<String, Object> parts = MultiValueMap.forLinkedHashMap();
    parts.add("file", new InputStreamResource(new ByteArrayInputStream(new byte[] { 65 })));
    var output = new CapturingOutputMessage();
    this.converter.write(parts, MULTIPART_MIXED, output);
    assertThat(output.getHeaders().getContentType().getCharset()).isNull();
    assertThat(output.body.repeatable()).isFalse();

    parts.clear();
    parts.add("unsupported", 42);
    var unsupported = new CapturingOutputMessage();
    this.converter.write(parts, MULTIPART_MIXED, unsupported);
    assertThat(unsupported.body.repeatable()).isFalse();
    assertThatExceptionOfType(HttpMessageNotWritableException.class)
            .isThrownBy(() -> unsupported.body.writeTo(new ByteArrayOutputStream()))
            .withMessageContaining(Integer.class.getName());
  }

  @Test
  void rejectsAnEntityWithoutABodyInBothOutputModes() {
    MultiValueMap<String, Object> parts = MultiValueMap.forLinkedHashMap();
    parts.add("empty", HttpEntity.EMPTY);
    assertThatIllegalStateException()
            .isThrownBy(() -> this.converter.write(parts, MULTIPART_MIXED, new CapturingOutputMessage()))
            .withMessageContaining("Empty body for part 'empty'");
    assertThatIllegalStateException()
            .isThrownBy(() -> this.converter.write(parts, MULTIPART_MIXED, new MockHttpOutputMessage()))
            .withMessageContaining("Empty body for part 'empty'");
  }

  @Test
  void keepsTheMultipartStreamOpenAndWritesHeadersOnlyOncePerPart() throws IOException {
    @SuppressWarnings("unchecked")
    HttpMessageConverter<String> partConverter = mock();
    given(partConverter.canWrite(String.class, null)).willReturn(true);
    willAnswer(invocation -> {
      HttpOutputMessage part = invocation.getArgument(2);
      var body = part.getBody();
      body.write('A');
      body.flush();
      body.close();
      assertThat(part.getBody()).isSameAs(body);
      assertThat(part.getHeaders().getContentDisposition().getName()).isEqualTo("first");
      body.write('B');
      return null;
    }).given(partConverter).write(eq("value"), isNull(), any());
    this.converter.setPartConverters(List.of(partConverter));
    MultiValueMap<String, Object> parts = MultiValueMap.forLinkedHashMap();
    parts.add("first", "value");
    var output = new CapturingOutputMessage();
    this.converter.write(parts, MULTIPART_FORM_DATA, output);
    assertThat(output.body.repeatable()).isFalse();

    var bytes = new ByteArrayOutputStream() {
      @Override
      public void flush() {
        throw new AssertionError("Part writer must not flush the multipart stream");
      }

      @Override
      public void close() {
        throw new AssertionError("Part writer must not close the multipart stream");
      }
    };
    output.body.writeTo(bytes);
    assertThat(bytes.toString(StandardCharsets.UTF_8)).contains("\r\n\r\nAB\r\n");
  }

  private MockHttpInputMessage input(String content) {
    return new MockHttpInputMessage(content.getBytes(StandardCharsets.US_ASCII));
  }

  private static final class CapturingOutputMessage extends MockHttpOutputMessage implements StreamingHttpOutputMessage {

    private Body body;

    @Override
    public void setBody(Body body) {
      this.body = body;
    }
  }

}
