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

package infra.web.service.invoker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.List;

import infra.http.HttpEntity;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.util.MultiValueMap;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpRequestValues}.
 *
 * @author Rossen Stoyanchev
 */
class HttpRequestValuesTests {

  @Test
  void defaultUri() {
    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.GET).build();

    assertThat(requestValues.getUri()).isNull();
    assertThat(requestValues.getUriTemplate()).isEmpty();
    assertThat(requestValues.getUriBuilderFactory()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = { "POST", "PUT", "PATCH" })
  @SuppressWarnings("unchecked")
  void formData(String httpMethod) {

    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.valueOf(httpMethod))
            .setContentType(MediaType.APPLICATION_FORM_URLENCODED)
            .addRequestParameter("param1", "1st value")
            .addRequestParameter("param2", "2nd value A", "2nd value B")
            .build();

    Object body = requestValues.getBodyValue();
    assertThat((MultiValueMap<String, String>) body).hasSize(2)
            .containsEntry("param1", List.of("1st value"))
            .containsEntry("param2", List.of("2nd value A", "2nd value B"));
  }

  @Test
  void queryParamsWithUriTemplate() {

    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
            .setUriTemplate("/path")
            .addRequestParameter("param1", "1st value")
            .addRequestParameter("param2", "2nd value A", "2nd value B")
            .build();

    String uriTemplate = requestValues.getUriTemplate();
    assertThat(uriTemplate).isNotNull();

    assertThat(uriTemplate)
            .isEqualTo("/path?" +
                    "{queryParam0}={queryParam0[0]}&" +
                    "{queryParam1}={queryParam1[0]}&" +
                    "{queryParam1}={queryParam1[1]}");

    assertThat(requestValues.getUriVariables())
            .containsOnlyKeys("queryParam0", "queryParam1", "queryParam0[0]", "queryParam1[0]", "queryParam1[1]")
            .containsEntry("queryParam0", "param1")
            .containsEntry("queryParam1", "param2")
            .containsEntry("queryParam0[0]", "1st value")
            .containsEntry("queryParam1[0]", "2nd value A")
            .containsEntry("queryParam1[1]", "2nd value B");

    URI uri = UriComponentsBuilder.forURIString(uriTemplate)
            .encode()
            .build(requestValues.getUriVariables());

    assertThat(uri.toString())
            .isEqualTo("/path?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
  }

  @Test
  void queryParamsWithPreparedUri() {

    URI uri = URI.create("/my%20path");

    HttpRequestValues requestValues = HttpRequestValues.builder().setHttpMethod(HttpMethod.POST)
            .setUri(uri)
            .addRequestParameter("param1", "1st value")
            .addRequestParameter("param2", "2nd value A", "2nd value B")
            .build();

    assertThat(requestValues.getUri().toString())
            .isEqualTo("/my%20path?param1=1st%20value&param2=2nd%20value%20A&param2=2nd%20value%20B");
  }

  @Test
  void requestPart() {
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.add("foo", "bar");
    HttpEntity<String> entity = new HttpEntity<>("body", headers);

    HttpRequestValues requestValues = HttpRequestValues.builder()
            .addRequestPart("form field", "form value")
            .addRequestPart("entity", entity)
            .build();

    @SuppressWarnings("unchecked")
    MultiValueMap<String, Object> map = (MultiValueMap<String, Object>) requestValues.getBodyValue();
    assertThat(map).hasSize(2);
    assertThat(map.getFirst("form field")).isEqualTo("form value");
    assertThat(map.getFirst("entity")).isEqualTo(entity);
  }

  @Test
  void requestPartAndRequestParam() {

    HttpRequestValues requestValues = HttpRequestValues.builder()
            .setUriTemplate("/path")
            .addRequestPart("form field", "form value")
            .addRequestParameter("query param", "query value")
            .build();

    String uriTemplate = requestValues.getUriTemplate();
    assertThat(uriTemplate).isNotNull();

    assertThat(uriTemplate).isEqualTo("/path?{queryParam0}={queryParam0[0]}");

    @SuppressWarnings("unchecked")
    MultiValueMap<String, Object> map = (MultiValueMap<String, Object>) requestValues.getBodyValue();
    assertThat(map).hasSize(1);
    assertThat(map.getFirst("form field")).isEqualTo("form value");
  }

}
