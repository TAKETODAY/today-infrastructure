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

package cn.taketoday.web.service.invoker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.service.annotation.GetExchange;
import cn.taketoday.web.service.annotation.PostExchange;
import cn.taketoday.web.util.UriComponents;
import cn.taketoday.web.util.UriComponentsBuilder;

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

  private final Service service =
          HttpServiceProxyFactory.forAdapter(this.client).build().createClient(Service.class);

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
    UriComponents uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode();
    assertThat(uri.getQuery()).isEqualTo("param1=value%201&param2=1,2,3");
  }

  private interface Service {

    @PostExchange(contentType = "application/x-www-form-urlencoded")
    void postForm(@RequestParam String param1, @RequestParam String param2);

    @GetExchange
    void getWithParams(@RequestParam String param1, @RequestParam List<String> param2);
  }

}
