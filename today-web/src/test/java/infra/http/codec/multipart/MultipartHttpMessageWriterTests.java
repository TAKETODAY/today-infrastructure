/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.http.codec.multipart;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.core.ResolvableType;
import infra.core.codec.StringDecoder;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.client.MultipartBodyBuilder;
import infra.http.codec.ClientCodecConfigurer;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MultipartHttpMessageWriter}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class MultipartHttpMessageWriterTests extends AbstractLeakCheckingTests {

  private final MultipartHttpMessageWriter writer =
          new MultipartHttpMessageWriter(ClientCodecConfigurer.create().getWriters());

  private final MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);

  @Test
  public void canWrite() {
    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
            MediaType.MULTIPART_FORM_DATA)).isTrue();
    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
            MediaType.MULTIPART_FORM_DATA)).isTrue();
    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
            MediaType.MULTIPART_MIXED)).isTrue();
    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
            MediaType.MULTIPART_RELATED)).isTrue();
    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
            MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

    assertThat(this.writer.canWrite(
            ResolvableType.forClassWithGenerics(Map.class, String.class, Object.class),
            MediaType.MULTIPART_FORM_DATA)).isFalse();
  }

  @Test
  public void writeMultipartFormData() throws Exception {
    Resource logo = new ClassPathResource("/infra/http/converter/logo.jpg");
    Resource utf8 = new ClassPathResource("/infra/http/converter/logo.jpg") {
      @Override
      public String getName() {
        //
        return "Hall\u00F6le.jpg";
      }
    };

    Flux<DataBuffer> bufferPublisher = Flux.just(
            this.bufferFactory.wrap("Aa".getBytes(StandardCharsets.UTF_8)),
            this.bufferFactory.wrap("Bb".getBytes(StandardCharsets.UTF_8)),
            this.bufferFactory.wrap("Cc".getBytes(StandardCharsets.UTF_8))
    );
    FilePart mockPart = mock();
    HttpHeaders partHeaders = HttpHeaders.forWritable();
    partHeaders.setContentType(MediaType.TEXT_PLAIN);
    partHeaders.setContentDispositionFormData("foo", "file.txt");
    partHeaders.add("foo", "bar");
    given(mockPart.headers()).willReturn(partHeaders);
    given(mockPart.content()).willReturn(bufferPublisher);

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("name 1", "value 1");
    bodyBuilder.part("name 2", "value 2+1");
    bodyBuilder.part("name 2", "value 2+2");
    bodyBuilder.part("logo", logo);
    bodyBuilder.part("utf8", utf8);
    bodyBuilder.part("json", new Foo("bar"), MediaType.APPLICATION_JSON);
    bodyBuilder.asyncPart("publisher", Flux.just("foo", "bar", "baz"), String.class);
    bodyBuilder.part("filePublisher", mockPart);
    Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

    Map<String, Object> hints = Collections.emptyMap();
    this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, this.response, hints)
            .block(Duration.ofSeconds(5));

    MultiValueMap<String, Part> requestParts = parse(this.response, hints);
    assertThat(requestParts).hasSize(7);

    Part part = requestParts.getFirst("name 1");
    assertThat(part).isInstanceOf(FormFieldPart.class);
    assertThat(part.name()).isEqualTo("name 1");
    assertThat(((FormFieldPart) part).value()).isEqualTo("value 1");

    List<Part> parts2 = requestParts.get("name 2");
    assertThat(parts2).hasSize(2);
    part = parts2.get(0);
    assertThat(part).isInstanceOf(FormFieldPart.class);
    assertThat(part.name()).isEqualTo("name 2");
    assertThat(((FormFieldPart) part).value()).isEqualTo("value 2+1");
    part = parts2.get(1);
    assertThat(part).isInstanceOf(FormFieldPart.class);
    assertThat(part.name()).isEqualTo("name 2");
    assertThat(((FormFieldPart) part).value()).isEqualTo("value 2+2");

    part = requestParts.getFirst("logo");
    assertThat(part).isInstanceOf(FilePart.class);
    assertThat(part.name()).isEqualTo("logo");
    assertThat(((FilePart) part).filename()).isEqualTo("logo.jpg");
    assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());

    part = requestParts.getFirst("utf8");
    assertThat(part).isInstanceOf(FilePart.class);
    assertThat(part.name()).isEqualTo("utf8");
    assertThat(((FilePart) part).filename()).isEqualTo("Hall\u00F6le.jpg");
    assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    assertThat(part.headers().getContentLength()).isEqualTo(utf8.getFile().length());

    part = requestParts.getFirst("json");
    assertThat(part).isNotNull();
    assertThat(part.name()).isEqualTo("json");
    assertThat(part.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    String value = decodeToString(part);
    assertThat(value).isEqualTo("{\"bar\":\"bar\"}");

    part = requestParts.getFirst("publisher");
    assertThat(part).isNotNull();
    assertThat(part.name()).isEqualTo("publisher");
    value = decodeToString(part);
    assertThat(value).isEqualTo("foobarbaz");

    part = requestParts.getFirst("filePublisher");
    assertThat(part).isNotNull();
    assertThat(part.name()).isEqualTo("filePublisher");
    assertThat(part.headers()).containsEntry("foo", Collections.singletonList("bar"));
    assertThat(((FilePart) part).filename()).isEqualTo("file.txt");
    value = decodeToString(part);
    assertThat(value).isEqualTo("AaBbCc");
  }

  @Test  // gh-24582
  public void writeMultipartRelated() {
    MediaType mediaType = MediaType.parseMediaType("multipart/related;type=foo");

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("name 1", "value 1");
    bodyBuilder.part("name 2", "value 2");
    Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

    Map<String, Object> hints = Collections.emptyMap();
    this.writer.write(result, null, mediaType, this.response, hints)
            .block(Duration.ofSeconds(5));

    MediaType contentType = this.response.getHeaders().getContentType();
    assertThat(contentType).isNotNull();
    assertThat(contentType.isCompatibleWith(mediaType)).isTrue();
    assertThat(contentType.getParameter("type")).isEqualTo("foo");
    assertThat(contentType.getParameter("boundary")).isNotEmpty();
    assertThat(contentType.getParameter("charset")).isNull();

    MultiValueMap<String, Part> requestParts = parse(this.response, hints);
    assertThat(requestParts).hasSize(2);
    assertThat(requestParts.getFirst("name 1").name()).isEqualTo("name 1");
    assertThat(requestParts.getFirst("name 2").name()).isEqualTo("name 2");
  }

  @SuppressWarnings("ConstantConditions")
  private String decodeToString(Part part) {
    return StringDecoder.textPlainOnly().decodeToMono(part.content(),
            ResolvableType.forClass(String.class), MediaType.TEXT_PLAIN,
            Collections.emptyMap()).block(Duration.ZERO);
  }

  @Test
  public void singleSubscriberWithResource() throws IOException {
    Sinks.Many<Resource> sink = Sinks.many().unicast().onBackpressureBuffer();
    Resource logo = new ClassPathResource("/infra/http/converter/logo.jpg");
    sink.tryEmitNext(logo);

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.asyncPart("logo", sink.asFlux(), Resource.class);

    Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

    Map<String, Object> hints = Collections.emptyMap();
    this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, this.response, hints).block();

    MultiValueMap<String, Part> requestParts = parse(this.response, hints);
    assertThat(requestParts).hasSize(1);

    Part part = requestParts.getFirst("logo");
    assertThat(part.name()).isEqualTo("logo");
    assertThat(part).isInstanceOf(FilePart.class);
    assertThat(((FilePart) part).filename()).isEqualTo("logo.jpg");
    assertThat(part.headers().getContentType()).isEqualTo(MediaType.IMAGE_JPEG);
    assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());
  }

  @Test
  public void singleSubscriberWithStrings() {
    AtomicBoolean subscribed = new AtomicBoolean();
    Flux<String> publisher = Flux.just("foo", "bar", "baz")
            .doOnSubscribe(subscription ->
                    assertThat(subscribed.compareAndSet(false, true)).isTrue());

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.asyncPart("name", publisher, String.class);

    Mono<MultiValueMap<String, HttpEntity<?>>> result = Mono.just(bodyBuilder.build());

    this.writer.write(result, null, MediaType.MULTIPART_FORM_DATA, this.response, Collections.emptyMap())
            .block(Duration.ofSeconds(5));

    // Make sure body is consumed to avoid leak reports
    this.response.getBodyAsString().block(Duration.ofSeconds(5));
  }

  @Test
  public void customContentDisposition() throws IOException {
    Resource logo = new ClassPathResource("/infra/http/converter/logo.jpg");
    Flux<DataBuffer> buffers = DataBufferUtils.read(logo, DefaultDataBufferFactory.sharedInstance, 1024);
    long contentLength = logo.contentLength();

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("resource", logo)
            .headers(h -> h.setContentDispositionFormData("resource", "spring.jpg"));
    bodyBuilder.asyncPart("buffers", buffers, DataBuffer.class)
            .headers(h -> {
              h.setContentDispositionFormData("buffers", "buffers.jpg");
              h.setContentType(MediaType.IMAGE_JPEG);
              h.setContentLength(contentLength);
            });

    MultiValueMap<String, HttpEntity<?>> multipartData = bodyBuilder.build();

    Map<String, Object> hints = Collections.emptyMap();
    this.writer.write(Mono.just(multipartData), null, MediaType.MULTIPART_FORM_DATA,
            this.response, hints).block();

    MultiValueMap<String, Part> requestParts = parse(this.response, hints);
    assertThat(requestParts).hasSize(2);

    Part part = requestParts.getFirst("resource");
    assertThat(part).isInstanceOf(FilePart.class);
    assertThat(((FilePart) part).filename()).isEqualTo("spring.jpg");
    assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());

    part = requestParts.getFirst("buffers");
    assertThat(part).isInstanceOf(FilePart.class);
    assertThat(((FilePart) part).filename()).isEqualTo("buffers.jpg");
    assertThat(part.headers().getContentLength()).isEqualTo(logo.getFile().length());
  }

  public static MultiValueMap<String, Part> parse(MockServerHttpResponse response, Map<String, Object> hints) {
    MediaType contentType = response.getHeaders().getContentType();
    assertThat(contentType.getParameter("boundary")).as("No boundary found").isNotNull();

    // see if we can read what we wrote
    DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
    MultipartHttpMessageReader reader = new MultipartHttpMessageReader(partReader);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(MediaType.parseMediaType(contentType.toString()))
            .body(response.getBody());

    ResolvableType elementType = ResolvableType.forClassWithGenerics(
            MultiValueMap.class, String.class, Part.class);

    MultiValueMap<String, Part> result = reader.readMono(elementType, request, hints)
            .block(Duration.ofSeconds(5));

    assertThat(result).isNotNull();
    return result;
  }

  @SuppressWarnings("unused")
  private static class Foo {

    private String bar;

    public Foo() {
    }

    public Foo(String bar) {
      this.bar = bar;
    }

    public String getBar() {
      return this.bar;
    }

    public void setBar(String bar) {
      this.bar = bar;
    }
  }

}
