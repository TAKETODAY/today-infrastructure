/*
 * Copyright 2017 - 2025 the original author or authors.
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

import java.util.List;
import java.util.Map;

import infra.core.conversion.support.DefaultConversionService;
import infra.util.MultiValueMap;
import infra.web.service.annotation.GetExchange;
import infra.web.service.annotation.PostExchange;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/1 21:59
 */
class FallbackRequestParamArgumentResolverTests {

  private final TestExchangeAdapter client = new TestExchangeAdapter();

  @Test
  @SuppressWarnings("unchecked")
  void requestParam() {
    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .useDefaultFallbackArgumentResolver()
            .build().createClient(Service.class);
    service.postForm("value 1", "value 2");

    Object body = this.client.getRequestValues().getBodyValue();
    assertThat(body).isInstanceOf(MultiValueMap.class);
    assertThat((MultiValueMap<String, String>) body).hasSize(2)
            .containsEntry("param1", List.of("value 1"))
            .containsEntry("param2", List.of("value 2"));
  }

  @Test
  void requestParamWithDisabledFormattingCollectionValue() {
    RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService(), true);
    resolver.setFavorSingleValue(true);

    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .fallbackArgumentResolver(resolver)
            .build().createClient(Service.class);

    service.getWithParams("value 1", List.of("1", "2", "3"));

    HttpRequestValues values = this.client.getRequestValues();
    String uriTemplate = values.getUriTemplate();
    Map<String, String> uriVariables = values.getUriVariables();
    UriComponents uri = UriComponentsBuilder.forURIString(uriTemplate).buildAndExpand(uriVariables).encode();
    assertThat(uri.getQuery()).isEqualTo("param1=value%201&param2=1,2,3");
  }

  @Test
  void requestParamWithMultipleValues() {
    RequestParamArgumentResolver resolver = new RequestParamArgumentResolver(new DefaultConversionService(), true);
    resolver.setFavorSingleValue(false);

    Service service = HttpServiceProxyFactory.forAdapter(this.client)
            .fallbackArgumentResolver(resolver)
            .build().createClient(Service.class);

    service.getWithParams("value 1", List.of("1", "2", "3"));

    HttpRequestValues values = this.client.getRequestValues();
    String uriTemplate = values.getUriTemplate();
    Map<String, String> uriVariables = values.getUriVariables();
    UriComponents uri = UriComponentsBuilder.forURIString(uriTemplate).buildAndExpand(uriVariables).encode();
    assertThat(uri.getQuery()).isEqualTo("param1=value%201&param2=1,2,3");
  }

  private interface Service {

    @PostExchange(contentType = "application/x-www-form-urlencoded")
    void postForm(String param1, String param2);

    @GetExchange
    void getWithParams(String param1, List<String> param2);
  }

}