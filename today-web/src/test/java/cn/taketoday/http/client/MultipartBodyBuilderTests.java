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

package cn.taketoday.http.client;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.client.MultipartBodyBuilder.PublisherEntity;
import cn.taketoday.util.DefaultMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class MultipartBodyBuilderTests {

  @Test
  public void builder() {

    MultipartBodyBuilder builder = new MultipartBodyBuilder();

    MultiValueMap<String, String> multipartData = new DefaultMultiValueMap<>();
    multipartData.add("form field", "form value");
    builder.part("key", multipartData).header("foo", "bar");

    Resource logo = new ClassPathResource("/cn/taketoday/http/converter/logo.jpg");
    builder.part("logo", logo).header("baz", "qux");

    HttpHeaders entityHeaders = HttpHeaders.create();
    entityHeaders.add("foo", "bar");
    HttpEntity<String> entity = new HttpEntity<>("body", entityHeaders);
    builder.part("entity", entity).header("baz", "qux");

    Publisher<String> publisher = Flux.just("foo", "bar", "baz");
    builder.asyncPart("publisherClass", publisher, String.class).header("baz", "qux");
    builder.asyncPart("publisherPtr", publisher, new TypeReference<String>() { }).header("baz", "qux");

    MultiValueMap<String, HttpEntity<?>> result = builder.build();

    assertThat(result.size()).isEqualTo(5);
    HttpEntity<?> resultEntity = result.getFirst("key");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(multipartData);
    assertThat(resultEntity.getHeaders().getFirst("foo")).isEqualTo("bar");

    resultEntity = result.getFirst("logo");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(logo);
    assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");

    resultEntity = result.getFirst("entity");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo("body");
    assertThat(resultEntity.getHeaders().getFirst("foo")).isEqualTo("bar");
    assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");

    resultEntity = result.getFirst("publisherClass");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(publisher);
    assertThat(((PublisherEntity<?, ?>) resultEntity).getResolvableType()).isEqualTo(ResolvableType.forClass(String.class));
    assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");

    resultEntity = result.getFirst("publisherPtr");
    assertThat(resultEntity).isNotNull();
    assertThat(resultEntity.getBody()).isEqualTo(publisher);
    assertThat(((PublisherEntity<?, ?>) resultEntity).getResolvableType()).isEqualTo(ResolvableType.forClass(String.class));
    assertThat(resultEntity.getHeaders().getFirst("baz")).isEqualTo("qux");
  }

  @Test // SPR-16601
  public void publisherEntityAcceptedAsInput() {

    Publisher<String> publisher = Flux.just("foo", "bar", "baz");
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.asyncPart("publisherClass", publisher, String.class).header("baz", "qux");
    HttpEntity<?> entity = builder.build().getFirst("publisherClass");

    assertThat(entity).isNotNull();
    assertThat(entity.getClass()).isEqualTo(PublisherEntity.class);

    // Now build a new MultipartBodyBuilder, as BodyInserters.fromMultipartData would do...

    builder = new MultipartBodyBuilder();
    builder.part("publisherClass", entity);
    entity = builder.build().getFirst("publisherClass");

    assertThat(entity).isNotNull();
    assertThat(entity.getClass()).isEqualTo(PublisherEntity.class);
  }

}
