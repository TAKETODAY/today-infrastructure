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

package infra.http.service.invoker;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpEntity;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.util.UriBuilderFactory;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/4/4 18:37
 */
class ReactiveHttpRequestValuesTests {

  @Test
  void publisherBodyAndElementType() {
    Flux<String> bodyPublisher = Flux.just("data1", "data2");
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setBodyPublisher(bodyPublisher, elementType)
            .build();

    assertThat(requestValues.getBodyPublisher()).isEqualTo(bodyPublisher);
    assertThat(requestValues.getBodyPublisherElementType()).isEqualTo(elementType);
    assertThat(requestValues.getBodyValue()).isNull();
  }

  @Test
  void multipartWithPublisherPart() {
    Flux<byte[]> fileContent = Flux.just("file content".getBytes());
    ParameterizedTypeReference<byte[]> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .addRequestPartPublisher("file", fileContent, elementType)
            .build();

    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
  }

  @Test
  void bodyValueAndPublisherAreMutuallyExclusive() {
    Flux<String> bodyPublisher = Flux.just("data");
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setBodyValue("string body")
            .setBodyPublisher(bodyPublisher, elementType)
            .build();

    assertThat(requestValues.getBody()).isEqualTo(bodyPublisher);
    assertThat(requestValues.getBodyValue()).isNull();
  }

  @Test
  void multipleRequestPartsWithMixedTypes() {
    Flux<String> textContent = Flux.just("text content");
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .addRequestPart("text", "plain text")
            .addRequestPartPublisher("stream", textContent, elementType)
            .build();

    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
  }

  @Test
  void reactiveBuilderInheritsHttpRequestValuesBuilder() {
    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setHttpMethod(HttpMethod.POST)
            .setUriTemplate("/api/stream")
            .addHeader("X-Custom", "value")
            .addAttribute("key", "value")
            .build();

    assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.POST);
    assertThat(requestValues.getUriTemplate()).isEqualTo("/api/stream");
    assertThat(requestValues.getHeaders().getFirst("X-Custom")).isEqualTo("value");
    assertThat(requestValues.getAttributes().get("key")).isEqualTo("value");
  }

  @Test
  void setBodyAndSetBodyPublisherAreEquivalent() {
    Flux<String> bodyPublisher = Flux.just("data");
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues1 = ReactiveHttpRequestValues.builder()
            .setBodyPublisher(bodyPublisher, elementType)
            .build();

    ReactiveHttpRequestValues requestValues2 = ReactiveHttpRequestValues.builder()
            .setBody(bodyPublisher, elementType)
            .build();

    assertThat(requestValues1.getBodyPublisher()).isEqualTo(requestValues2.getBodyPublisher());
    assertThat(requestValues1.getBodyPublisherElementType()).isEqualTo(requestValues2.getBodyPublisherElementType());
  }

  @Test
  void nullUriAndMethodCreateValidRequest() {
    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setURI(null)
            .setHttpMethod(null)
            .build();

    assertThat(requestValues.getURI()).isNull();
    assertThat(requestValues.getHttpMethod()).isNull();
  }

  @Test
  void emptyRequestWithOnlyBaseUri() {
    URI baseUri = URI.create("https://api.example.com");
    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setURI(baseUri)
            .build();

    assertThat(requestValues.getURI()).isEqualTo(baseUri);
  }

  @Test
  void completeRequestWithAllUriComponents() {
    URI uri = URI.create("https://api.example.com/users");
    UriBuilderFactory uriBuilderFactory = mock(UriBuilderFactory.class);

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setURI(uri)
            .setUriBuilderFactory(uriBuilderFactory)
            .setUriTemplate("/users/{id}")
            .setUriVariable("id", "123")
            .build();

    assertThat(requestValues.getURI()).isEqualTo(uri);
    assertThat(requestValues.getUriTemplate()).isEqualTo("/users/{id}");
    assertThat(requestValues.getUriVariables()).containsEntry("id", "123");
  }

  @Test
  void requestWithCustomUriBuilderFactory() {
    UriBuilderFactory customFactory = mock(UriBuilderFactory.class);

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setUriBuilderFactory(customFactory)
            .setUriTemplate("/api")
            .build();

    assertThat(requestValues.getUriBuilderFactory()).isEqualTo(customFactory);
  }

  @Test
  void malformedUriHandling() {
    String invalidUri = "http://[invalid";

    assertThatThrownBy(() -> ReactiveHttpRequestValues.builder()
            .setURI(new URI(invalidUri))
            .build()).isInstanceOf(URISyntaxException.class);
  }

  @Test
  void uriTemplateWithMultipleVariables() {
    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setUriTemplate("/api/{version}/users/{userId}/posts/{postId}")
            .setUriVariable("version", "v1")
            .setUriVariable("userId", "123")
            .setUriVariable("postId", "456")
            .build();

    assertThat(requestValues.getUriTemplate()).isEqualTo("/api/{version}/users/{userId}/posts/{postId}");
    assertThat(requestValues.getUriVariables())
            .containsEntry("version", "v1")
            .containsEntry("userId", "123")
            .containsEntry("postId", "456");
  }

  @Test
  void requestPartPublisherWithNullValues() {
    assertThatThrownBy(() -> ReactiveHttpRequestValues.builder()
            .addRequestPartPublisher(null, Flux.just("data"), new ParameterizedTypeReference<String>() { })
            .build())
            .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> ReactiveHttpRequestValues.builder()
            .addRequestPartPublisher("name", null, new ParameterizedTypeReference<String>() { })
            .build())
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void mixedBodyTypesWithMultipart() {
    Flux<String> textPublisher = Flux.just("text");
    Flux<byte[]> filePublisher = Flux.just("file".getBytes());

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .addRequestPart("text", "plain text")
            .addRequestPartPublisher("stream1", textPublisher, new ParameterizedTypeReference<String>() { })
            .addRequestPartPublisher("stream2", filePublisher, new ParameterizedTypeReference<byte[]>() { })
            .build();

    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
  }

  @Test
  void builderMethodChaining() {
    URI uri = URI.create("https://api.example.com");
    UriBuilderFactory factory = mock(UriBuilderFactory.class);
    Flux<String> publisher = Flux.just("data");

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setHttpMethod(HttpMethod.POST)
            .setURI(uri)
            .setUriBuilderFactory(factory)
            .setUriTemplate("/path")
            .setUriVariable("var", "value")
            .setContentType(MediaType.APPLICATION_JSON)
            .addHeader("X-Custom", "value")
            .addCookie("session", "123")
            .addAttribute("attr", "value")
            .setBodyPublisher(publisher, new ParameterizedTypeReference<String>() { })
            .build();

    assertThat(requestValues.getHttpMethod()).isEqualTo(HttpMethod.POST);
    assertThat(requestValues.getURI()).isEqualTo(uri);
    assertThat(requestValues.getUriBuilderFactory()).isEqualTo(factory);
    assertThat(requestValues.getBodyPublisher()).isEqualTo(publisher);
  }

  @Test
  void multipleRequestPartsPublishers() {
    Flux<String> pub1 = Flux.just("data1");
    Flux<String> pub2 = Flux.just("data2");

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .addRequestPartPublisher("part1", pub1, new ParameterizedTypeReference<String>() { })
            .addRequestPartPublisher("part2", pub2, new ParameterizedTypeReference<String>() { })
            .build();

    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
  }

  @Test
  void clearBodyPublisherWhenSettingBodyValue() {
    Flux<String> publisher = Flux.just("data");

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setBodyPublisher(publisher, new ParameterizedTypeReference<String>() { })
            .setBodyValue("value")
            .build();

    assertThat(requestValues.getBodyPublisher()).isNull();
    assertThat(requestValues.getBodyValue()).isEqualTo("value");
  }

  @Test
  void multipartRequestWithEmptyParts() {
    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .addRequestPart("empty1", "")
            .addRequestPart("empty2", "")
            .build();

    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
    @SuppressWarnings("unchecked")
    MultiValueMap<String, Object> parts = (MultiValueMap<String, Object>) requestValues.getBodyValue();
    assertThat(parts.get("empty1")).containsExactly(new HttpEntity<>(""));
    assertThat(parts.get("empty2")).containsExactly(new HttpEntity<>(""));
  }

  @Test
  void bodyPublisherWithCustomElementType() {
    class CustomType {
      String value;
    }

    CustomType obj = new CustomType();
    obj.value = "test";
    Flux<CustomType> publisher = Flux.just(obj);
    ParameterizedTypeReference<CustomType> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setBodyPublisher(publisher, elementType)
            .build();

    assertThat(requestValues.getBodyPublisher()).isEqualTo(publisher);
    assertThat(requestValues.getBodyPublisherElementType()).isEqualTo(elementType);
  }

  @Test
  void requestPartPublisherWithDifferentElementTypes() {
    Flux<String> stringPublisher = Flux.just("text");
    Flux<Integer> intPublisher = Flux.just(123);
    Flux<byte[]> bytesPublisher = Flux.just("bytes".getBytes());

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .addRequestPartPublisher("string", stringPublisher, new ParameterizedTypeReference<String>() { })
            .addRequestPartPublisher("integer", intPublisher, new ParameterizedTypeReference<Integer>() { })
            .addRequestPartPublisher("bytes", bytesPublisher, new ParameterizedTypeReference<byte[]>() { })
            .build();

    assertThat(requestValues.getBodyValue()).isInstanceOf(MultiValueMap.class);
  }

  @Test
  void bodyPublisherWithEmptyFlux() {
    Flux<String> emptyPublisher = Flux.empty();
    ParameterizedTypeReference<String> elementType = new ParameterizedTypeReference<>() { };

    ReactiveHttpRequestValues requestValues = ReactiveHttpRequestValues.builder()
            .setBodyPublisher(emptyPublisher, elementType)
            .build();

    assertThat(requestValues.getBodyPublisher()).isEqualTo(emptyPublisher);
    assertThat(requestValues.getBodyPublisherElementType()).isEqualTo(elementType);
    assertThat(requestValues.getBodyValue()).isNull();
  }

}