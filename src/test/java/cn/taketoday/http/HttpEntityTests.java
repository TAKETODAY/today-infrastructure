/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http;



import org.junit.jupiter.api.Test;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author TODAY 2021/4/15 14:24
 */
public class HttpEntityTests {

  @Test
  public void noHeaders() {
    String body = "foo";
    HttpEntity<String> entity = new HttpEntity<>(body);
    assertThat(entity.getBody()).isSameAs(body);
    assertThat(entity.getHeaders().isEmpty()).isTrue();
  }

  @Test
  public void httpHeaders() {
    HttpHeaders headers = HttpHeaders.create();
    headers.setContentType(MediaType.TEXT_PLAIN);
    String body = "foo";
    HttpEntity<String> entity = new HttpEntity<>(body, headers);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(entity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
  }

  @Test
  public void multiValueMap() {
    MultiValueMap<String, String> map = new DefaultMultiValueMap<>();
    map.set("Content-Type", "text/plain");
    String body = "foo";
    HttpEntity<String> entity = new HttpEntity<>(body, map);
    assertThat(entity.getBody()).isEqualTo(body);
    assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(entity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
  }

  @Test
  public void testEquals() {
    MultiValueMap<String, String> map1 = new DefaultMultiValueMap<>();
    map1.set("Content-Type", "text/plain");

    MultiValueMap<String, String> map2 = new DefaultMultiValueMap<>();
    map2.set("Content-Type", "application/json");

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
    HttpHeaders headers = HttpHeaders.create();
    headers.setContentType(MediaType.TEXT_PLAIN);
    String body = "foo";
    HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
    ResponseEntity<String> responseEntity = new ResponseEntity<>(body, headers, HttpStatus.OK);
    ResponseEntity<String> responseEntity2 = new ResponseEntity<>(body, headers, HttpStatus.OK);

    assertThat(responseEntity.getBody()).isEqualTo(body);
    assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(responseEntity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");
    assertThat(responseEntity.getHeaders().getFirst("Content-Type")).isEqualTo("text/plain");

    assertThat(httpEntity.equals(responseEntity)).isFalse();
    assertThat(responseEntity.equals(httpEntity)).isFalse();
    assertThat(responseEntity.equals(responseEntity2)).isTrue();
    assertThat(responseEntity2.equals(responseEntity)).isTrue();
  }

}
