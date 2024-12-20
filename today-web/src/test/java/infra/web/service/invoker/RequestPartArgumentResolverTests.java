/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.service.invoker;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.annotation.RequestPart;
import infra.web.multipart.MultipartFile;
import infra.web.service.annotation.PostExchange;
import infra.web.testfixture.MockMultipartFile;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestPartArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
class RequestPartArgumentResolverTests {

  private static final MockMultipartFile mockMultipartFile =
          new MockMultipartFile("testFileName", "originalTestFileName", "text/plain", "test".getBytes());

  private final TestReactorExchangeAdapter client = new TestReactorExchangeAdapter();

  private final Service service = HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

  // Base class functionality should be tested in NamedValueArgumentResolverTests.
  // Form data vs query params tested in HttpRequestValuesTests.

  @Test
  void requestPart() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("foo", "bar");
    HttpEntity<String> part2 = new HttpEntity<>("part 2", headers);
    this.service.postMultipart("part 1", part2, Mono.just("part 3"), Optional.of("part 4"));

    Object body = this.client.getRequestValues().getBodyValue();
    assertThat(body).isInstanceOf(MultiValueMap.class);

    @SuppressWarnings("unchecked")
    MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) body;
    assertThat(map.getFirst("part1").getBody()).isEqualTo("part 1");
    assertThat(map.getFirst("part2")).isEqualTo(part2);
    assertThat(((Mono<?>) map.getFirst("part3").getBody()).block()).isEqualTo("part 3");
    assertThat(map.getFirst("optionalPart").getBody()).isEqualTo("part 4");
  }

  @Test
  void multipartFile() {
    this.service.postMultipartFile(mockMultipartFile);
    testMultipartFile(mockMultipartFile, "file");
  }

  @Test
  void requestPartMultipartFile() {
    this.service.postRequestPartMultipartFile(mockMultipartFile);
    testMultipartFile(mockMultipartFile, "myFile");
  }

  @Test
  void optionalMultipartFile() {
    this.service.postOptionalMultipartFile(Optional.empty(), "anotherPart");
    Object value = client.getRequestValues().getBodyValue();

    assertThat(value).isInstanceOf(MultiValueMap.class);

    @SuppressWarnings("unchecked")
    MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) value;
    assertThat(map).hasSize(1).containsKey("anotherPart");
  }

  private void testMultipartFile(MultipartFile testFile, String partName) {
    Object value = this.client.getRequestValues().getBodyValue();

    assertThat(value).isInstanceOf(MultiValueMap.class);

    @SuppressWarnings("unchecked")
    MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) value;
    assertThat(map).hasSize(1);

    HttpEntity<?> entity = map.getFirst(partName);
    assertThat(entity).isNotNull();
    assertThat(entity.getBody()).isEqualTo(testFile.getResource());

    HttpHeaders headers = entity.getHeaders();
    assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(headers.getContentDisposition().getType()).isEqualTo("form-data");
    assertThat(headers.getContentDisposition().getName()).isEqualTo(partName);
    assertThat(headers.getContentDisposition().getFilename()).isEqualTo(testFile.getOriginalFilename());
  }

  private interface Service {

    @PostExchange
    void postMultipart(
            @RequestPart String part1, @RequestPart HttpEntity<String> part2,
            @RequestPart Mono<String> part3,
            @RequestPart Optional<String> optionalPart);

    @PostExchange
    void postMultipartFile(MultipartFile file);

    @PostExchange
    void postRequestPartMultipartFile(@RequestPart(name = "myFile") MultipartFile file);

    @PostExchange
    void postOptionalMultipartFile(@RequestPart Optional<MultipartFile> file, @RequestPart String anotherPart);
  }

}
