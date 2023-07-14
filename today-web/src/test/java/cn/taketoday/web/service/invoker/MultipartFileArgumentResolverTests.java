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

package cn.taketoday.web.service.invoker;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.service.annotation.PostExchange;
import cn.taketoday.web.testfixture.servlet.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultipartFileArgumentResolver}.
 * Tests for base class functionality of this resolver can be found in
 * {@link NamedValueArgumentResolverTests}.
 *
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
class MultipartFileArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  private final MultipartService multipartService =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(MultipartService.class);

  @Test
  void multipartFile() {
    String fileName = "testFileName";
    String originalFileName = "originalTestFileName";
    MultipartFile testFile = new MockMultipartFile(fileName, originalFileName, "text/plain", "test".getBytes());

    this.multipartService.postMultipartFile(testFile);
    Object value = this.client.getRequestValues().getBodyValue();

    assertThat(value).isInstanceOf(MultiValueMap.class);
    MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) value;
    assertThat(map).hasSize(1);

    HttpEntity<?> entity = map.getFirst("file");
    assertThat(entity).isNotNull();
    assertThat(entity.getBody()).isEqualTo(testFile.getResource());

    HttpHeaders headers = entity.getHeaders();
    assertThat(headers.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(headers.getContentDisposition().getType()).isEqualTo("form-data");
    assertThat(headers.getContentDisposition().getName()).isEqualTo("file");
    assertThat(headers.getContentDisposition().getFilename()).isEqualTo(originalFileName);
  }

  @Test
  void optionalMultipartFile() {
    this.multipartService.postOptionalMultipartFile(Optional.empty(), "anotherPart");
    Object value = client.getRequestValues().getBodyValue();

    assertThat(value).isInstanceOf(MultiValueMap.class);
    MultiValueMap<String, HttpEntity<?>> map = (MultiValueMap<String, HttpEntity<?>>) value;
    assertThat(map).containsOnlyKeys("anotherPart");
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private interface MultipartService {

    @PostExchange
    void postMultipartFile(MultipartFile file);

    @PostExchange
    void postOptionalMultipartFile(Optional<MultipartFile> file, @RequestPart String anotherPart);

  }

}
