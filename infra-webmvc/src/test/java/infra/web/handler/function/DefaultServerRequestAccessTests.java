/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRange;
import infra.http.MediaType;
import infra.http.converter.GenericHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.server.RequestPath;
import infra.util.LinkedMultiValueMap;
import infra.web.HttpContext;
import infra.web.HttpMediaTypeNotSupportedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DefaultServerRequestAccessTests {

  private final HttpContext context = mock(HttpContext.class);

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();

  @Test
  void returnsAllValuesForRepeatedParametersAndDescribesRequest() {
    params.add("sort", "name");
    params.add("sort", "date");
    given(context.getMethod()).willReturn(HttpMethod.GET);
    var request = request(List.of());

    assertThat(request.params("sort")).containsExactly("name", "date");
    assertThat(request.params("missing")).isEmpty();
    assertThat(request.toString()).isEqualTo("HTTP GET /items");
  }

  @Test
  void exposesTypedAndReadOnlyRequestHeaders() {
    InetSocketAddress host = InetSocketAddress.createUnresolved("example.test", 8080);
    headers.setHost(host);
    headers.setRange(List.of(HttpRange.createByteRange(5, 9)));
    headers.setAcceptLanguage(List.of(new Locale.LanguageRange("en-US")));
    var requestHeaders = request(List.of()).headers();

    assertThat(requestHeaders.host()).isEqualTo(host);
    assertThat(requestHeaders.range()).extracting(HttpRange::toString).containsExactly("5-9");
    assertThat(requestHeaders.acceptLanguage()).containsExactly(new Locale.LanguageRange("en-US"));
    assertThat(requestHeaders.toString()).isEqualTo(headers.toString());
    assertThatThrownBy(() -> requestHeaders.asHttpHeaders().set("X-Test", "value"))
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void usesGenericConverterWithFullTypeInformation() throws Exception {
    GenericHttpMessageConverter<?> converter = mock(GenericHttpMessageConverter.class);
    var bodyType = new ParameterizedTypeReference<List<String>>() { };
    headers.setContentType(MediaType.APPLICATION_JSON);
    given(converter.canRead(bodyType.getType(), List.class, MediaType.APPLICATION_JSON)).willReturn(true);
    given(converter.read(bodyType.getType(), List.class, context)).willAnswer(invocation -> List.of("sample"));

    assertThat(request(List.of(converter)).body(bodyType)).containsExactly("sample");
  }

  @Test
  void reportsSupportedMediaTypesWhenNoConverterCanReadTheBody() {
    HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
    headers.setContentType(MediaType.APPLICATION_JSON);
    given(context.getMethod()).willReturn(HttpMethod.POST);
    given(converter.getSupportedMediaTypes(String.class)).willReturn(List.of(MediaType.TEXT_PLAIN));

    assertThatThrownBy(() -> request(List.of(converter)).body(String.class))
            .isInstanceOfSatisfying(HttpMediaTypeNotSupportedException.class, failure ->
                    assertThat(failure.getSupportedMediaTypes()).containsExactly(MediaType.TEXT_PLAIN));
  }

  private DefaultServerRequest request(List<HttpMessageConverter<?>> converters) {
    given(context.getHeaders()).willReturn(headers);
    given(context.getParameters()).willReturn(params);
    given(context.getRequestPath()).willReturn(RequestPath.parse("/items", ""));
    return new DefaultServerRequest(context, converters);
  }

}
