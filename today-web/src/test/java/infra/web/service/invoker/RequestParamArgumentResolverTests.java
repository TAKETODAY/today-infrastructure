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

package infra.web.service.invoker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.core.conversion.support.DefaultConversionService;
import infra.util.MultiValueMap;
import infra.web.annotation.RequestParam;
import infra.web.service.annotation.GetExchange;
import infra.web.service.annotation.PostExchange;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestParamArgumentResolver}.
 *
 * <p>Additional tests for this resolver:
 * <ul>
 * <li>Base class functionality in {@link NamedValueArgumentResolverTests}
 * <li>Form data vs query params in {@link HttpRequestValuesTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
class RequestParamArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  @Test
  @SuppressWarnings("unchecked")
  void requestParam() {
    Service service = HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);
    service.postForm("value 1", "value 2");

    Object body = this.client.getRequestValues().getBodyValue();
    assertThat(body).isInstanceOf(MultiValueMap.class);
    assertThat((MultiValueMap<String, String>) body).hasSize(2)
            .containsEntry("param1", List.of("value 1"))
            .containsEntry("param2", List.of("value 2"));
  }

  @Test
  void requestParamWithDisabledFormattingCollectionValue() {
    RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService());
    resolver.setFavorSingleValue(true);

    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .customArgumentResolver(resolver).build().createClient(Service.class);

    service.getWithParams("value 1", List.of("1", "2", "3"));

    HttpRequestValues values = this.client.getRequestValues();
    String uriTemplate = values.getUriTemplate();
    Map<String, String> uriVariables = values.getUriVariables();
    UriComponents uri = UriComponentsBuilder.forURIString(uriTemplate).buildAndExpand(uriVariables).encode();
    assertThat(uri.getQuery()).isEqualTo("param1=value%201&param2=1,2,3");
  }

  @Test
  void favorSingleValueWithFormContent() {
    RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService());
    resolver.setFavorSingleValue(true);

    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .customArgumentResolver(resolver).build().createClient(Service.class);

    service.postForm("value1", "value2");

    HttpRequestValues values = this.client.getRequestValues();
    assertThat(values.getHeaders().getContentType().toString()).isEqualTo("application/x-www-form-urlencoded");
    assertThat(values.getBodyValue()).isInstanceOf(MultiValueMap.class);
  }

  @Test
  void favorSingleValueDisabled() {
    RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService());
    resolver.setFavorSingleValue(false);

    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .customArgumentResolver(resolver).build().createClient(Service.class);

    service.getWithParams("value", List.of("1", "2", "3"));

    HttpRequestValues values = this.client.getRequestValues();
    String uriTemplate = values.getUriTemplate();
    Map<String, String> uriVariables = values.getUriVariables();
    UriComponents uri = UriComponentsBuilder.forURIString(uriTemplate).buildAndExpand(uriVariables).encode();
    assertThat(uri.getQuery()).isEqualTo("param1=value&param2=1&param2=2&param2=3");
  }

  @Test
  void favorSingleValueWithEmptyList() {
    RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService());
    resolver.setFavorSingleValue(true);

    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .customArgumentResolver(resolver).build().createClient(Service.class);

    service.getWithParams("value", List.of());

    HttpRequestValues values = this.client.getRequestValues();
    String uriTemplate = values.getUriTemplate();
    Map<String, String> uriVariables = values.getUriVariables();
    UriComponents uri = UriComponentsBuilder.forURIString(uriTemplate).buildAndExpand(uriVariables).encode();
    assertThat(uri.getQuery()).isEqualTo("param1=value&param2=");
  }

  @Test
  void multipartContentType() {
    RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService());
    resolver.setFavorSingleValue(true);

    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .customArgumentResolver(resolver).build().createClient(Service.class);

    service.postMultipart("value1", List.of("1", "2"));

    HttpRequestValues values = this.client.getRequestValues();
    assertThat(values.getHeaders().getContentType().toString()).startsWith("multipart/");
  }

  private interface Service {

    @PostExchange(contentType = "application/x-www-form-urlencoded")
    void postForm(@RequestParam String param1, @RequestParam String param2);

    @GetExchange
    void getWithParams(@RequestParam String param1, @RequestParam List<String> param2);

    @PostExchange(contentType = "multipart/form-data")
    void postMultipart(@RequestParam String param1, @RequestParam List<String> param2);
  }

}
