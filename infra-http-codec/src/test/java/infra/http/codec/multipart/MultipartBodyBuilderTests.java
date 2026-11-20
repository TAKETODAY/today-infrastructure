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

package infra.http.codec.multipart;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.time.Duration;

import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.codec.multipart.MultipartBodyBuilder.PublisherEntity;
import infra.util.MappingMultiValueMap;
import infra.util.MultiValueMap;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 */
class MultipartBodyBuilderTests {

  @Test
  public void builder() {

    MultipartBodyBuilder builder = new MultipartBodyBuilder();

    MultiValueMap<String, String> multipartData = new MappingMultiValueMap<>();
    multipartData.add("form field", "form value");
    builder.part("key", multipartData).header("foo", "bar");

    Resource logo = new ClassPathResource("/infra/http/codec/logo.jpg");
    builder.part("logo", logo).header("baz", "qux");

    HttpHeaders entityHeaders = HttpHeaders.forWritable();
    entityHeaders.add("foo", "bar");
    HttpEntity<String> entity = new HttpEntity<>("body", entityHeaders);
    builder.part("entity", entity).header("baz", "qux");

    Publisher<String> publisher = Flux.just("foo", "bar", "baz");
    builder.asyncPart("publisherClass", publisher, String.class).header("baz", "qux");
    builder.asyncPart("publisherPtr", publisher, new ParameterizedTypeReference<String>() { }).header("baz", "qux");

    MultiValueMap<String, HttpEntity<?>> result = builder.build();

    assertThat(result.size()).isEqualTo(5);
    HttpEntity<?> resultEntity = result.getFirst("key");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(multipartData);
    assertThat(resultEntity.headers().getFirst("foo")).isEqualTo("bar");

    resultEntity = result.getFirst("logo");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(logo);
    assertThat(resultEntity.headers().getFirst("baz")).isEqualTo("qux");

    resultEntity = result.getFirst("entity");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo("body");
    assertThat(resultEntity.headers().getFirst("foo")).isEqualTo("bar");
    assertThat(resultEntity.headers().getFirst("baz")).isEqualTo("qux");

    resultEntity = result.getFirst("publisherClass");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(publisher);
    assertThat(((PublisherEntity<?, ?>) resultEntity).getResolvableType()).isEqualTo(ResolvableType.forClass(String.class));
    assertThat(resultEntity.headers().getFirst("baz")).isEqualTo("qux");

    resultEntity = result.getFirst("publisherPtr");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(publisher);
    assertThat(((PublisherEntity<?, ?>) resultEntity).getResolvableType()).isEqualTo(ResolvableType.forClass(String.class));
    assertThat(resultEntity.headers().getFirst("baz")).isEqualTo("qux");
  }

  @Test
  public void publisherEntityAcceptedAsInput() {

    Publisher<String> publisher = Flux.just("foo", "bar", "baz");
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.asyncPart("publisherClass", publisher, String.class).header("baz", "qux");
    HttpEntity<?> entity = builder.build().getFirst("publisherClass");

    assertThat(entity).isNotNull();
    assertThat(entity.getClass()).isEqualTo(PublisherEntity.class);

    // Now build a new MultipartBodyBuilder, as BodyInserters.fromMultipartData would do...

    builder = new MultipartBodyBuilder();
    builder.part("publisherClass", entity, MediaType.TEXT_PLAIN).filename("values.txt");
    entity = builder.build().getFirst("publisherClass");

    assertThat(entity).isNotNull();
    assertThat(entity.getClass()).isEqualTo(PublisherEntity.class);
    assertThat(entity.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(entity.headers().getContentDisposition().getFilename()).isEqualTo("values.txt");
    assertThat(entity.getBody()).isSameAs(publisher);
  }

  @Test
  void forwardsPartContentAndRenamesTheDisposition() {
    var builder = new MultipartBodyBuilder();
    var headers = HttpHeaders.forWritable();
    headers.setContentDispositionFormData("original", "notes.txt");
    headers.setContentType(MediaType.TEXT_PLAIN);
    var original = DefaultParts.part(headers, Flux.empty());
    var empty = DefaultParts.part(HttpHeaders.empty(), Flux.empty());
    builder.part("renamed", original, MediaType.APPLICATION_OCTET_STREAM);
    builder.part("empty", empty);

    var parts = builder.build();
    var renamed = parts.getFirst("renamed");
    assertThat(renamed.headers().getContentDisposition().getName()).isEqualTo("renamed");
    assertThat(renamed.headers().getContentDisposition().getFilename()).isEqualTo("notes.txt");
    assertThat(renamed.headers().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    assertThat(original.name()).isEqualTo("original");
    assertThat(Flux.from((Publisher<?>) renamed.getBody()).collectList().block(Duration.ofSeconds(5))).isEmpty();
    assertThat(parts.getFirst("empty").headers().isEmpty()).isTrue();
  }

  @Test
  void directsUntypedPublishersToAsyncPart() {
    var builder = new MultipartBodyBuilder();
    var publisher = Flux.just("value");
    assertThatIllegalArgumentException().isThrownBy(() -> builder.part("publisher", publisher))
            .withMessageContaining("Use asyncPart");
    assertThatIllegalArgumentException().isThrownBy(() -> builder.part("entity", new HttpEntity<>(publisher)))
            .withMessageContaining("Use asyncPart");
    assertThat(builder.build()).isEmpty();
  }

}
