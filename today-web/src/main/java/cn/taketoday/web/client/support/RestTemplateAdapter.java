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

package cn.taketoday.web.client.support;

import java.net.URI;
import java.util.ArrayList;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.service.invoker.HttpExchangeAdapter;
import cn.taketoday.web.service.invoker.HttpRequestValues;
import cn.taketoday.web.service.invoker.HttpServiceProxyFactory;
import cn.taketoday.web.util.UriBuilderFactory;

/**
 * {@link HttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link RestTemplate} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@link HttpServiceProxyFactory} configured with the given {@link RestTemplate}.
 *
 * @author Olga Maciaszek-Sharma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class RestTemplateAdapter implements HttpExchangeAdapter {

  private final RestTemplate restTemplate;

  private RestTemplateAdapter(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public boolean supportsRequestAttributes() {
    return false;
  }

  @Override
  public void exchange(HttpRequestValues values) {
    this.restTemplate.exchange(newRequest(values), Void.class);
  }

  @Override
  public HttpHeaders exchangeForHeaders(HttpRequestValues values) {
    return this.restTemplate.exchange(newRequest(values), Void.class).getHeaders();
  }

  @Override
  public <T> T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
    return this.restTemplate.exchange(newRequest(values), bodyType).getBody();
  }

  @Override
  public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
    return this.restTemplate.exchange(newRequest(values), Void.class);
  }

  @Override
  public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
    return this.restTemplate.exchange(newRequest(values), bodyType);
  }

  private RequestEntity<?> newRequest(HttpRequestValues values) {
    HttpMethod httpMethod = values.getHttpMethod();
    Assert.notNull(httpMethod, "HttpMethod is required");

    RequestEntity.BodyBuilder builder;

    if (values.getUri() != null) {
      builder = RequestEntity.method(httpMethod, values.getUri());
    }
    else if (values.getUriTemplate() != null) {
      UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
      if (uriBuilderFactory != null) {
        URI expanded = uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables());
        builder = RequestEntity.method(httpMethod, expanded);
      }
      else {
        builder = RequestEntity.method(httpMethod, values.getUriTemplate(), values.getUriVariables());
      }
    }
    else {
      throw new IllegalStateException("Neither full URL nor URI template");
    }

    builder.headers(values.getHeaders());

    if (!values.getCookies().isEmpty()) {
      ArrayList<String> cookies = new ArrayList<>();
      for (var entry : values.getCookies().entrySet()) {
        String name = entry.getKey();
        for (String value : entry.getValue()) {
          HttpCookie cookie = new HttpCookie(name, value);
          cookies.add(cookie.toString());
        }
      }
      builder.header(HttpHeaders.COOKIE, String.join("; ", cookies));
    }

    if (values.getBodyValue() != null) {
      return builder.body(values.getBodyValue());
    }

    return builder.build();
  }

  /**
   * Create a {@link RestTemplateAdapter} for the given {@link RestTemplate}.
   */
  public static RestTemplateAdapter create(RestTemplate restTemplate) {
    return new RestTemplateAdapter(restTemplate);
  }

}
