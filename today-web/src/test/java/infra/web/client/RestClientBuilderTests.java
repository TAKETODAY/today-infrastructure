/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.web.client;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInitializer;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.support.BasicAuthenticationInterceptor;
import infra.http.converter.AllEncompassingFormHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/1/21 20:43
 */
class RestClientBuilderTests {

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

    List<ResponseErrorHandler> statusHandlers = (List<ResponseErrorHandler>) fieldValue("statusHandlers", defaultBuilder);
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

  @Test
  void defaultUri() {
    URI baseUrl = URI.create("https://example.org");
    RestClient.Builder builder = RestClient.builder();
    builder.baseURI(baseUrl);

    assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
    DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

    assertThat(fieldValue("baseURI", defaultBuilder)).isEqualTo(baseUrl);
  }

  @Test
  void messageConvertersList() {
    StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
    RestClient.Builder builder = RestClient.builder();
    builder.messageConverters(List.of(stringConverter));

    assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
    DefaultRestClientBuilder defaultBuilder = (DefaultRestClientBuilder) builder;

    assertThat(fieldValue("messageConverters", defaultBuilder))
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactly(stringConverter);
  }

  @Test
  void messageConvertersListEmpty() {
    RestClient.Builder builder = RestClient.builder();
    List<HttpMessageConverter<?>> converters = Collections.emptyList();
    assertThatIllegalArgumentException().isThrownBy(() -> builder.messageConverters(converters));
  }

  @Test
  void messageConvertersListWithNullElement() {
    RestClient.Builder builder = RestClient.builder();
    List<HttpMessageConverter<?>> converters = new ArrayList<>();
    converters.add(null);
    assertThatIllegalArgumentException().isThrownBy(() -> builder.messageConverters(converters));
  }

  @Test
  void configureMessageConverters() {
    StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
    RestClient.Builder builder = RestClient.builder();
    builder.configureMessageConverters(clientBuilder -> clientBuilder.addCustomConverter(stringConverter));
    assertThat(builder).isInstanceOf(DefaultRestClientBuilder.class);
    DefaultRestClient restClient = (DefaultRestClient) builder.build();

    assertThat(fieldValue("messageConverters", restClient))
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .hasExactlyElementsOfTypes(StringHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
  }

  @Test
  void defaultCookieAddsCookieToDefaultCookiesMap() {
    RestClient.Builder builder = RestClient.builder();

    builder.defaultCookie("myCookie", "testValue");

    assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(Map.entry("myCookie", List.of("testValue")));
  }

  @Test
  void defaultCookieWithMultipleValuesAddsCookieToDefaultCookiesMap() {
    RestClient.Builder builder = RestClient.builder();

    builder.defaultCookie("myCookie", "testValue1", "testValue2");

    assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(Map.entry("myCookie", List.of("testValue1", "testValue2")));
  }

  @Test
  void defaultCookiesAllowsToAddCookie() {
    RestClient.Builder builder = RestClient.builder();
    builder.defaultCookie("firstCookie", "firstValue");

    builder.defaultCookies(cookies -> cookies.add("secondCookie", "secondValue"));

    assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(
                    Map.entry("firstCookie", List.of("firstValue")),
                    Map.entry("secondCookie", List.of("secondValue"))
            );
  }

  @Test
  void defaultCookiesAllowsToRemoveCookie() {
    RestClient.Builder builder = RestClient.builder();
    builder.defaultCookie("firstCookie", "firstValue");
    builder.defaultCookie("secondCookie", "secondValue");

    builder.defaultCookies(cookies -> cookies.remove("firstCookie"));

    assertThat(fieldValue("defaultCookies", (DefaultRestClientBuilder) builder))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(Map.entry("secondCookie", List.of("secondValue")));
  }

  @Test
  void copyConstructorCopiesDefaultCookies() {
    DefaultRestClientBuilder sourceBuilder = new DefaultRestClientBuilder();
    sourceBuilder.defaultCookie("firstCookie", "firstValue");
    sourceBuilder.defaultCookie("secondCookie", "secondValue");

    DefaultRestClientBuilder copiedBuilder = new DefaultRestClientBuilder(sourceBuilder);

    assertThat(fieldValue("defaultCookies", copiedBuilder))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(
                    Map.entry("firstCookie", List.of("firstValue")),
                    Map.entry("secondCookie", List.of("secondValue"))
            );
  }

  @Test
  void copyConstructorCopiesDefaultCookiesImmutable() {
    DefaultRestClientBuilder sourceBuilder = new DefaultRestClientBuilder();
    sourceBuilder.defaultCookie("firstCookie", "firstValue");
    sourceBuilder.defaultCookie("secondCookie", "secondValue");
    DefaultRestClientBuilder copiedBuilder = new DefaultRestClientBuilder(sourceBuilder);

    sourceBuilder.defaultCookie("thirdCookie", "thirdValue");

    assertThat(fieldValue("defaultCookies", copiedBuilder))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(
                    Map.entry("firstCookie", List.of("firstValue")),
                    Map.entry("secondCookie", List.of("secondValue"))
            );
  }

  @Test
  void buildCopiesDefaultCookies() {
    RestClient.Builder builder = RestClient.builder();
    builder.defaultCookie("firstCookie", "firstValue");
    builder.defaultCookie("secondCookie", "secondValue");

    RestClient restClient = builder.build();

    assertThat(fieldValue("defaultCookies", restClient))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(
                    Map.entry("firstCookie", List.of("firstValue")),
                    Map.entry("secondCookie", List.of("secondValue"))
            );
  }

  @Test
  void buildCopiesDefaultCookiesImmutable() {
    RestClient.Builder builder = RestClient.builder();
    builder.defaultCookie("firstCookie", "firstValue");
    builder.defaultCookie("secondCookie", "secondValue");
    RestClient restClient = builder.build();

    builder.defaultCookie("thirdCookie", "thirdValue");
    builder.defaultCookie("firstCookie", "fourthValue");

    assertThat(fieldValue("defaultCookies", restClient))
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsExactly(
                    Map.entry("firstCookie", List.of("firstValue")),
                    Map.entry("secondCookie", List.of("secondValue"))
            );
  }

  @Test
  void ignoreStatus() {
    RestClient restClient = RestClient.builder()
            .ignoreStatus()
            .build();

    assertThat(fieldValue("ignoreStatusHandlers", restClient))
            .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

    restClient = RestClient.builder()
            .ignoreStatus(false)
            .build();

    assertThat(fieldValue("ignoreStatusHandlers", restClient))
            .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
            .isFalse();
  }

  @Test
  void detectEmptyMessageBody() {
    RestClient restClient = RestClient.builder()
            .build();

    assertThat(fieldValue("detectEmptyMessageBody", restClient))
            .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
            .isTrue();

    restClient = RestClient.builder()
            .detectEmptyMessageBody(false)
            .build();

    assertThat(fieldValue("detectEmptyMessageBody", restClient))
            .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
            .isFalse();
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

  @Nullable
  private static Object fieldValue(String name, RestClient instance) {
    try {
      Field field = DefaultRestClient.class.getDeclaredField(name);
      field.setAccessible(true);

      return field.get(instance);
    }
    catch (NoSuchFieldException | IllegalAccessException ex) {
      fail(ex.getMessage(), ex);
      return null;
    }
  }
}