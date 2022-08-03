/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http.codec.multipart;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariables;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.core.codec.DecodingException;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.ContentDisposition;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.server.reactive.MockServerHttpRequest;
import io.netty.buffer.PooledByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static cn.taketoday.core.ResolvableType.fromClass;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/22 9:47
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "file CRLF problem")
class PartEventHttpMessageReaderTests {

  private static final int BUFFER_SIZE = 64;

  private static final DataBufferFactory bufferFactory = new NettyDataBufferFactory(new PooledByteBufAllocator());

  private static final MediaType TEXT_PLAIN_ASCII = new MediaType("text", "plain", StandardCharsets.US_ASCII);

  private final PartEventHttpMessageReader reader = new PartEventHttpMessageReader();

  @Test
  public void canRead() {
    assertThat(this.reader.canRead(fromClass(PartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
    assertThat(this.reader.canRead(fromClass(PartEvent.class), null)).isTrue();
  }

  @Test
  public void simple() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("simple.multipart", getClass()), "simple-boundary");

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .assertNext(form(headers -> Assertions.assertThat(headers).isEmpty(), "This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak."))
            .assertNext(form(headers -> Assertions.assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN_ASCII),
                    "This is explicitly typed plain ASCII text.\r\nIt DOES end with a linebreak.\r\n"))
            .verifyComplete();
  }

  @Test
  public void noHeaders() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("no-header.multipart", getClass()), "boundary");
    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .assertNext(data(headers -> Assertions.assertThat(headers).isEmpty(), bodyText("a"), true))
            .verifyComplete();
  }

  @Test
  public void noEndBoundary() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("no-end-boundary.multipart", getClass()), "boundary");

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DecodingException.class)
            .verify();
  }

  @Test
  public void garbage() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("garbage-1.multipart", getClass()), "boundary");

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DecodingException.class)
            .verify();
  }

  @Test
  public void noEndHeader() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("no-end-header.multipart", getClass()), "boundary");
    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DecodingException.class)
            .verify();
  }

  @Test
  public void noEndBody() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("no-end-body.multipart", getClass()), "boundary");
    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .expectError(DecodingException.class)
            .verify();
  }

  @Test
  public void noBody() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("no-body.multipart", getClass()), "boundary");
    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .assertNext(form(headers -> Assertions.assertThat(headers).contains(entry("Part", List.of("1"))), ""))
            .assertNext(data(headers -> Assertions.assertThat(headers).contains(entry("Part", List.of("2"))), bodyText("a"), true))
            .verifyComplete();
  }

  @Test
  public void cancel() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("simple.multipart", getClass()), "simple-boundary");
    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result, 3)
            .assertNext(form(headers -> Assertions.assertThat(headers).isEmpty(),
                    "This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak."))
            .thenCancel()
            .verify();
  }

  @Test
  public void firefox() {

    MockServerHttpRequest request = createRequest(new ClassPathResource("firefox.multipart", getClass()),
            "---------------------------18399284482060392383840973206");

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());
    StepVerifier.create(result)
            .assertNext(data(headersFormField("text1"), bodyText("a"), true))
            .assertNext(data(headersFormField("text2"), bodyText("b"), true))
            .assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, true))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
            .verifyComplete();
  }

  @Test
  public void chrome() {

    MockServerHttpRequest request = createRequest(new ClassPathResource("chrome.multipart", getClass()),
            "----WebKitFormBoundaryEveBLvRT65n21fwU");

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());
    StepVerifier.create(result)
            .assertNext(data(headersFormField("text1"), bodyText("a"), true))
            .assertNext(data(headersFormField("text2"), bodyText("b"), true))
            .assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, true))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
            .verifyComplete();
  }

  @Test
  public void safari() {

    MockServerHttpRequest request = createRequest(new ClassPathResource("safari.multipart", getClass()),
            "----WebKitFormBoundaryG8fJ50opQOML0oGD");

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());
    StepVerifier.create(result)
            .assertNext(data(headersFormField("text1"), bodyText("a"), true))
            .assertNext(data(headersFormField("text2"), bodyText("b"), true))
            .assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, true))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
            .verifyComplete();
  }

  @Test
  public void utf8Headers() {
    MockServerHttpRequest request = createRequest(
            new ClassPathResource("utf8.multipart", getClass()), "\"simple-boundary\"");

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .assertNext(data(headers -> Assertions.assertThat(headers).containsEntry("Føø", List.of("Bår")),
                    bodyText("This is plain ASCII text."), true))
            .verifyComplete();
  }

  @Test
  public void exceedHeaderLimit() {
    Flux<DataBuffer> body = DataBufferUtils
            .readByteChannel((new ClassPathResource("files.multipart", getClass()))::readableChannel, bufferFactory,
                    282);

    MediaType contentType = new MediaType("multipart", "form-data",
            Collections.singletonMap("boundary", "----WebKitFormBoundaryG8fJ50opQOML0oGD"));
    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(contentType)
            .body(body);

    this.reader.setMaxHeadersSize(230);

    Flux<PartEvent> result = this.reader.read(fromClass(PartEvent.class), request, emptyMap());

    StepVerifier.create(result)
            .assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
            .assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
            .verifyComplete();
  }

  private MockServerHttpRequest createRequest(Resource resource, String boundary) {
    Flux<DataBuffer> body = DataBufferUtils
            .readByteChannel(resource::readableChannel, bufferFactory, BUFFER_SIZE);

    MediaType contentType = new MediaType("multipart", "form-data", Collections.singletonMap("boundary", boundary));
    return MockServerHttpRequest.post("/")
            .contentType(contentType)
            .body(body);
  }

  private static Consumer<PartEvent> form(Consumer<HttpHeaders> headersConsumer, String value) {
    return data -> {
      headersConsumer.accept(data.headers());
      String actual = data.content().toString(UTF_8);
      assertThat(actual).isEqualTo(value);
      assertThat(data.isLast()).isTrue();
    };
  }

  private static Consumer<PartEvent> data(Consumer<HttpHeaders> headersConsumer, Consumer<DataBuffer> bufferConsumer, boolean isLast) {
    return data -> {
      headersConsumer.accept(data.headers());
      bufferConsumer.accept(data.content());
      assertThat(data.isLast()).isEqualTo(isLast);
    };
  }

  private static Consumer<HttpHeaders> headersFormField(String expectedName) {
    return headers -> {
      ContentDisposition cd = headers.getContentDisposition();
      assertThat(cd.isFormData()).isTrue();
      assertThat(cd.getName()).isEqualTo(expectedName);
    };
  }

  private static Consumer<HttpHeaders> headersFile(String expectedName, String expectedFilename) {
    return headers -> {
      ContentDisposition cd = headers.getContentDisposition();
      assertThat(cd.isFormData()).isTrue();
      assertThat(cd.getName()).isEqualTo(expectedName);
      assertThat(cd.getFilename()).isEqualTo(expectedFilename);
    };
  }

  private static Consumer<DataBuffer> bodyText(String expected) {
    return buffer -> {
      String s = buffer.toString(UTF_8);
      DataBufferUtils.release(buffer);
      assertThat(s).isEqualTo(expected);
    };
  }

}
