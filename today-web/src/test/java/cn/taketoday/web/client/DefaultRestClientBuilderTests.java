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

package cn.taketoday.web.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestInitializer;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.JdkClientHttpRequestFactory;
import cn.taketoday.http.client.support.BasicAuthenticationInterceptor;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/1/21 20:43
 */
class DefaultRestClientBuilderTests {

  @SuppressWarnings("unchecked")
  @Test
  void createFromRestTemplate() {
    ClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
    DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("baseUri");
    ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler();
    List<HttpMessageConverter<?>> restTemplateMessageConverters = List.of(new StringHttpMessageConverter());
    ClientHttpRequestInterceptor interceptor = new BasicAuthenticationInterceptor("foo", "bar");
    ClientHttpRequestInitializer initializer = request -> { };

    RestTemplate restTemplate = new RestTemplate(requestFactory);
    restTemplate.setUriTemplateHandler(uriBuilderFactory);
    restTemplate.setErrorHandler(errorHandler);
    restTemplate.setMessageConverters(restTemplateMessageConverters);
    restTemplate.setInterceptors(List.of(interceptor));
    restTemplate.setHttpRequestInitializers(List.of(initializer));

    RestClient.Builder builder = RestClient.builder(restTemplate);
    assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
    DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

    assertThat(fieldValue("requestFactory", defaultBuilder)).isSameAs(requestFactory);
    assertThat(fieldValue("uriBuilderFactory", defaultBuilder)).isSameAs(uriBuilderFactory);

    List<StatusHandler> statusHandlers = (List<StatusHandler>) fieldValue("statusHandlers", defaultBuilder);
    assertThat(statusHandlers).hasSize(1);

    List<HttpMessageConverter<?>> restClientMessageConverters =
            (List<HttpMessageConverter<?>>) fieldValue("messageConverters", defaultBuilder);
    assertThat(restClientMessageConverters).containsExactlyElementsOf(restClientMessageConverters);

    List<ClientHttpRequestInterceptor> interceptors =
            (List<ClientHttpRequestInterceptor>) fieldValue("interceptors", defaultBuilder);
    assertThat(interceptors).containsExactly(interceptor);

    List<ClientHttpRequestInitializer> initializers =
            (List<ClientHttpRequestInitializer>) fieldValue("initializers", defaultBuilder);
    assertThat(initializers).containsExactly(initializer);
  }

  @Test
  void defaultUriBuilderFactory() {
    RestTemplate restTemplate = new RestTemplate();

    RestClient.Builder builder = RestClient.builder(restTemplate);
    assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
    DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

    assertThat(fieldValue("uriBuilderFactory", defaultBuilder)).isNull();
  }

  @Nullable
  private static Object fieldValue(String name, DefaultRestClientBuilder instance) {
    try {
      Field field = DefaultRestClientBuilder.class.getDeclaredField(name);
      field.setAccessible(true);

      return field.get(instance);
    }
    catch (NoSuchFieldException | IllegalAccessException ex) {
      fail(ex.getMessage(), ex);
      return null;
    }
  }
}