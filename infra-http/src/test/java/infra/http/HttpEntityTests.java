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

package infra.http;

import org.junit.jupiter.api.Test;

import infra.util.MappingMultiValueMap;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author TODAY 2021/4/15 14:24
 */
class HttpEntityTests {

  @Test
  public void noHeaders() {
    String body = "foo";
    HttpEntity<String> entity = new HttpEntity<>(body);
    assertThat(entity.getBody()).isSameAs(body);
    assertThat(entity.headers().isEmpty()).isTrue();
  }

  @Test
  public void httpHeaders() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_PLAIN);
    String body = "foo";
    HttpEntity<String> entity = new HttpEntity<>(body, headers);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(entity.headers().getFirst("Content-Type")).isEqualTo("text/plain");
  }

  @Test
  public void multiValueMap() {
    MultiValueMap<String, String> map = new MappingMultiValueMap<>();
    map.setOrRemove("Content-Type", "text/plain");
    String body = "foo";
    HttpEntity<String> entity = new HttpEntity<>(body, map);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(entity.headers().getFirst("Content-Type")).isEqualTo("text/plain");
  }

  @Test
  public void testEquals() {
    MultiValueMap<String, String> map1 = new MappingMultiValueMap<>();
    map1.setOrRemove("Content-Type", "text/plain");

    MultiValueMap<String, String> map2 = new MappingMultiValueMap<>();
    map2.setOrRemove("Content-Type", "application/json");

    assertThat(new HttpEntity<>().equals(new HttpEntity<>())).isTrue();
    assertThat(new HttpEntity<>(map1).equals(new HttpEntity<>())).isFalse();
    assertThat(new HttpEntity<>().equals(new HttpEntity<>(map2))).isFalse();

    assertThat(new HttpEntity<>(map1).equals(new HttpEntity<>(map1))).isTrue();
    assertThat(new HttpEntity<>(map1).equals(new HttpEntity<>(map2))).isFalse();

    assertThat(new HttpEntity<String>(null, null).equals(new HttpEntity<String>(null, null))).isTrue();
    assertThat(new HttpEntity<>("foo", null).equals(new HttpEntity<String>(null, null))).isFalse();
    assertThat(new HttpEntity<String>(null, null).equals(new HttpEntity<>("bar", null))).isFalse();

    assertThat(new HttpEntity<>("foo", map1).equals(new HttpEntity<>("foo", map1))).isTrue();
    assertThat(new HttpEntity<>("foo", map1).equals(new HttpEntity<>("bar", map1))).isFalse();
  }

  @Test
  public void responseEntity() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_PLAIN);
    String body = "foo";
    HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
    ResponseEntity<String> responseEntity = new ResponseEntity<>(body, headers, HttpStatus.OK);
    ResponseEntity<String> responseEntity2 = new ResponseEntity<>(body, headers, HttpStatus.OK);

    assertThat(responseEntity.getBody()).isEqualTo(body);
    assertThat(responseEntity.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(responseEntity.headers().getFirst("Content-Type")).isEqualTo("text/plain");
    assertThat(responseEntity.headers().getFirst("Content-Type")).isEqualTo("text/plain");

    assertThat(httpEntity.equals(responseEntity)).isFalse();
    assertThat(responseEntity.equals(httpEntity)).isFalse();
    assertThat(responseEntity.equals(responseEntity2)).isTrue();
    assertThat(responseEntity2.equals(responseEntity)).isTrue();
  }

  @Test
  void headerAreMutable() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.TEXT_PLAIN);
    String body = "foo";
    HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
    httpEntity.getHeaders().setContentType(MediaType.APPLICATION_JSON);

  }

}
