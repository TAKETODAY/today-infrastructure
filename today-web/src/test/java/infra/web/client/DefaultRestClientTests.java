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

package infra.web.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.StreamingHttpOutputMessage;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpResponse;
import infra.http.converter.HttpMessageConverter;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.client.DefaultRestClient.AbstractResponseSpec;
import infra.web.client.DefaultRestClient.DefaultClientResponse;
import infra.web.client.DefaultRestClient.DefaultRequestBodyUriSpec;
import infra.web.util.UriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 16:01
 */
class DefaultRestClientTests {

  @Test
  void getMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestHeadersUriSpec<?> spec = client.get();

    assertThat(spec).isNotNull();
  }

  @Test
  void headMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestHeadersUriSpec<?> spec = client.head();

    assertThat(spec).isNotNull();
  }

  @Test
  void postMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestBodyUriSpec spec = client.post();

    assertThat(spec).isNotNull();
  }

  @Test
  void putMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestBodyUriSpec spec = client.put();

    assertThat(spec).isNotNull();
  }

  @Test
  void patchMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestBodyUriSpec spec = client.patch();

    assertThat(spec).isNotNull();
  }

  @Test
  void deleteMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestBodyUriSpec spec = client.delete();

    assertThat(spec).isNotNull();
  }

  @Test
  void optionsMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestHeadersUriSpec<?> spec = client.options();

    assertThat(spec).isNotNull();
  }

  @Test
  void methodWithValidHttpMethodCreatesCorrectSpec() {
    DefaultRestClient client = createBasicClient();

    RestClient.RequestBodyUriSpec spec = client.method(HttpMethod.GET);

    assertThat(spec).isNotNull();
  }

  @Test
  void methodWithNullHttpMethodThrowsException() {
    DefaultRestClient client = createBasicClient();

    assertThatThrownBy(() -> client.method(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("HttpMethod is required");
  }

  @Test
  void mutateReturnsNewBuilder() {
    DefaultRestClient client = createBasicClient();

    RestClient.Builder builder = client.mutate();

    assertThat(builder).isNotNull();
    assertThat(builder).isNotSameAs(client.builder);
  }

  @Test
  void bodyClassWithClassType() {
    Class<String> result = DefaultRestClient.bodyClass(String.class);

    assertThat(result).isEqualTo(String.class);
  }

  @Test
  void bodyClassWithParameterizedType() throws Exception {
    ParameterizedType type = (ParameterizedType) new ParameterizedTypeReference<List<String>>() { }.getType();

    Class<List> result = DefaultRestClient.bodyClass(type);

    assertThat(result).isEqualTo(List.class);
  }

  @Test
  void bodyClassWithObjectClass() {
    Class<Object> result = DefaultRestClient.bodyClass(Object.class);

    assertThat(result).isEqualTo(Object.class);
  }

  @Test
  void bodyClassWithRawClassType() {
    Class<String> result = DefaultRestClient.bodyClass(String.class);
    assertThat(result).isEqualTo(String.class);
  }

  @Test
  void bodyClassWithParameterizedTypeReference() {
    ParameterizedTypeReference<List<String>> typeRef = new ParameterizedTypeReference<List<String>>() { };
    Class<List> result = DefaultRestClient.bodyClass(typeRef.getType());
    assertThat(result).isEqualTo(List.class);
  }

  @Test
  void bodyClassWithObjectClassType() {
    Class<Object> result = DefaultRestClient.bodyClass(Object.class);
    assertThat(result).isEqualTo(Object.class);
  }

  @Test
  void bodyClassWithComplexParameterizedType() throws Exception {
    ParameterizedType type = (ParameterizedType) new ParameterizedTypeReference<Map<String, List<Integer>>>() { }.getType();
    Class<Map> result = DefaultRestClient.bodyClass(type);
    assertThat(result).isEqualTo(Map.class);
  }

  @Test
  void getContentTypeReturnsApplicationOctetStreamWhenNull() {
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    when(response.getHeaders()).thenReturn(headers);

    MediaType result = DefaultRestClient.getContentType(response);

    assertThat(result).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
  }

  @Test
  void getContentTypeReturnsActualContentTypeWhenPresent() {
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setContentType(MediaType.APPLICATION_JSON);
    when(response.getHeaders()).thenReturn(headers);

    MediaType result = DefaultRestClient.getContentType(response);

    assertThat(result).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void defaultRequestBodyUriSpecConstructorSetsHttpMethod() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.POST);

    // Access private field via reflection for testing
    assertThat(spec.httpMethod).isEqualTo(HttpMethod.POST);
  }

  @Test
  void defaultRequestBodyUriSpecUriWithAbsoluteURI() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    URI testUri = URI.create("http://example.com/test");

    RestClient.RequestBodySpec result = spec.uri(testUri);

    assertThat(result).isSameAs(spec);
    assertThat(spec.uri).isEqualTo(testUri);
  }

  @Test
  void defaultRequestBodyUriSpecUriWithRelativeURI() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    URI baseUri = URI.create("http://example.com/");
    URI relativeUri = URI.create("/test");

    // Mock UriBuilderFactory to return base URI
    when(client.uriBuilderFactory.expand("")).thenReturn(baseUri);

    RestClient.RequestBodySpec result = spec.uri(relativeUri);

    assertThat(result).isSameAs(spec);
    assertThat(spec.uri).isEqualTo(URI.create("http://example.com/test"));
  }

  @Test
  void defaultRequestBodyUriSpecApiVersion() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    String version = "v1.0";

    RestClient.RequestBodySpec result = spec.apiVersion(version);

    assertThat(result).isSameAs(spec);
    assertThat(spec.apiVersion).isEqualTo(version);
  }

  @Test
  void defaultRequestBodyUriSpecHeaderAddsHeader() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    String headerName = "X-Test";
    String[] headerValues = { "value1", "value2" };

    DefaultRequestBodyUriSpec result = spec.header(headerName, headerValues);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.get(headerName)).containsExactly(headerValues);
  }

  @Test
  void defaultRequestBodyUriSpecHeadersWithConsumer() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    Consumer<HttpHeaders> headersConsumer = headers -> headers.set("X-Test", "value");

    DefaultRequestBodyUriSpec result = spec.headers(headersConsumer);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void defaultRequestBodyUriSpecHeadersWithHeadersObject() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("X-Test", "value");

    RestClient.RequestBodySpec result = spec.headers(headers);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void defaultRequestBodyUriSpecCookieAddsCookie() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    String cookieName = "testCookie";
    String cookieValue = "value";

    RestClient.RequestBodySpec result = spec.cookie(cookieName, cookieValue);

    assertThat(result).isSameAs(spec);
    assertThat(spec.cookies).isNotNull();
    assertThat(spec.cookies.getFirst(cookieName)).isEqualTo(cookieValue);
  }

  @Test
  void defaultRequestBodyUriSpecCookiesWithConsumer() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    Consumer<MultiValueMap<String, String>> cookiesConsumer = cookies -> cookies.add("testCookie", "value");

    RestClient.RequestBodySpec result = spec.cookies(cookiesConsumer);

    assertThat(result).isSameAs(spec);
    assertThat(spec.cookies).isNotNull();
    assertThat(spec.cookies.getFirst("testCookie")).isEqualTo("value");
  }

  @Test
  void defaultRequestBodyUriSpecCookiesWithCookiesObject() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();
    cookies.add("testCookie", "value");

    RestClient.RequestBodySpec result = spec.cookies(cookies);

    assertThat(result).isSameAs(spec);
    assertThat(spec.cookies).isNotNull();
    assertThat(spec.cookies.getFirst("testCookie")).isEqualTo("value");
  }

  @Test
  void defaultRequestBodyUriSpecAttributeAddsAttribute() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    String attributeName = "testAttribute";
    String attributeValue = "value";

    RestClient.RequestBodySpec result = spec.attribute(attributeName, attributeValue);

    assertThat(result).isSameAs(spec);
    assertThat(spec.attributes).isNotNull();
    assertThat(spec.attributes.get(attributeName)).isEqualTo(attributeValue);
  }

  @Test
  void defaultRequestBodyUriSpecAttributesWithConsumer() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    Consumer<Map<String, Object>> attributesConsumer = attributes -> attributes.put("testAttribute", "value");

    RestClient.RequestBodySpec result = spec.attributes(attributesConsumer);

    assertThat(result).isSameAs(spec);
    assertThat(spec.attributes).isNotNull();
    assertThat(spec.attributes.get("testAttribute")).isEqualTo("value");
  }

  @Test
  void defaultRequestBodyUriSpecAttributesWithAttributesMap() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    Map<String, Object> attributes = Map.of("testAttribute", "value");

    RestClient.RequestBodySpec result = spec.attributes(attributes);

    assertThat(result).isSameAs(spec);
    assertThat(spec.attributes).isNotNull();
    assertThat(spec.attributes.get("testAttribute")).isEqualTo("value");
  }

  @Test
  void defaultRequestBodyUriSpecAcceptSetsAcceptHeader() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    MediaType[] acceptTypes = { MediaType.APPLICATION_JSON, MediaType.TEXT_XML };

    DefaultRequestBodyUriSpec result = spec.accept(acceptTypes);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getAccept()).containsExactly(acceptTypes);
  }

  @Test
  void defaultRequestBodyUriSpecAcceptCharsetSetsAcceptCharsetHeader() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    Charset[] charsets = { Charset.forName("UTF-8"), Charset.forName("ISO-8859-1") };

    DefaultRequestBodyUriSpec result = spec.acceptCharset(charsets);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getAcceptCharset()).containsExactly(charsets);
  }

  @Test
  void defaultRequestBodyUriSpecContentTypeSetsContentTypeHeader() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    MediaType contentType = MediaType.APPLICATION_JSON;

    DefaultRequestBodyUriSpec result = spec.contentType(contentType);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getContentType()).isEqualTo(contentType);
  }

  @Test
  void defaultRequestBodyUriSpecContentLengthSetsContentLengthHeader() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    long contentLength = 1024L;

    DefaultRequestBodyUriSpec result = spec.contentLength(contentLength);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getContentLength()).isEqualTo(contentLength);
  }

  @Test
  void defaultRequestBodyUriSpecIfModifiedSinceSetsHeader() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    ZonedDateTime ifModifiedSince = ZonedDateTime.now();

    DefaultRequestBodyUriSpec result = spec.ifModifiedSince(ifModifiedSince);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getIfModifiedSince() / 1000).isEqualTo(ifModifiedSince.toEpochSecond());
  }

  @Test
  void defaultRequestBodyUriSpecIfNoneMatchSetsHeader() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.GET);
    String[] ifNoneMatches = { "\"etag1\"", "\"etag2\"" };

    DefaultRequestBodyUriSpec result = spec.ifNoneMatch(ifNoneMatches);

    assertThat(result).isSameAs(spec);
    assertThat(spec.headers).isNotNull();
    assertThat(spec.headers.getIfNoneMatch()).containsExactly(ifNoneMatches);
  }

  @Test
  void defaultRequestBodyUriSpecBodyWithObject() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.POST);
    String body = "test body";

    RestClient.RequestBodySpec result = spec.body(body);

    assertThat(result).isSameAs(spec);
    assertThat(spec.body).isNotNull();
  }

  @Test
  void defaultRequestBodyUriSpecBodyWithParameterizedTypeReference() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.POST);
    List<String> body = List.of("item1", "item2");
    ParameterizedTypeReference<List<String>> typeReference = new ParameterizedTypeReference<List<String>>() { };

    RestClient.RequestBodySpec result = spec.body(body, typeReference);

    assertThat(result).isSameAs(spec);
    assertThat(spec.body).isNotNull();
  }

  @Test
  void defaultRequestBodyUriSpecBodyWithStreamingBody() {
    DefaultRestClient client = createBasicClient();
    DefaultRequestBodyUriSpec spec = client.new DefaultRequestBodyUriSpec(HttpMethod.POST);
    StreamingHttpOutputMessage.Body body = outputStream -> { };

    RestClient.RequestBodySpec result = spec.body(body);

    assertThat(result).isSameAs(spec);
    assertThat(spec.body).isNotNull();
  }

  @Test
  void abstractResponseSpecOnStatusWithPredicateAndHandler() {
    ClientHttpRequest request = mock(ClientHttpRequest.class);
    DefaultRestClient client = createBasicClient();
    AbstractResponseSpec<?> spec = client.new AbstractResponseSpec(request) { };

    Predicate<HttpStatusCode> statusPredicate = status -> status.is4xxClientError();
    RestClient.ErrorHandler errorHandler = (req, resp) -> { };

    AbstractResponseSpec<?> result = spec.onStatus(statusPredicate, errorHandler);

    assertThat(result).isSameAs(spec);
    assertThat(spec.statusHandlers).hasSize(1);
  }

  @Test
  void abstractResponseSpecOnStatusWithResponseErrorHandler() {
    ClientHttpRequest request = mock(ClientHttpRequest.class);
    DefaultRestClient client = createBasicClient();
    AbstractResponseSpec<?> spec = client.new AbstractResponseSpec(request) { };
    ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);

    AbstractResponseSpec<?> result = spec.onStatus(errorHandler);

    assertThat(result).isSameAs(spec);
    assertThat(spec.statusHandlers).hasSize(1);
  }

  @Test
  void abstractResponseSpecIgnoreStatus() {
    ClientHttpRequest request = mock(ClientHttpRequest.class);
    DefaultRestClient client = createBasicClient();
    AbstractResponseSpec<?> spec = client.new AbstractResponseSpec(request) { };

    AbstractResponseSpec<?> result = spec.ignoreStatus(true);

    assertThat(result).isSameAs(spec);
    assertThat(spec.ignoreStatus).isTrue();
  }

  @Test
  void defaultClientResponseConstructor() {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate);

    assertThat(response.delegate).isSameAs(delegate);
  }

  @Test
  void defaultClientResponseGetHeaders() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("X-Test", "value");
    when(delegate.getHeaders()).thenReturn(headers);

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate);

    HttpHeaders result = response.getHeaders();

    assertThat(result).isSameAs(headers);
    assertThat(result.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void defaultClientResponseGetStatusCode() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    when(delegate.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate);

    HttpStatusCode result = response.getStatusCode();

    assertThat(result).isEqualTo(HttpStatusCode.valueOf(200));
  }

  @Test
  void defaultClientResponseGetRawStatusCode() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    when(delegate.getRawStatusCode()).thenReturn(200);

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate);

    int result = response.getRawStatusCode();

    assertThat(result).isEqualTo(200);
  }

  @Test
  void defaultClientResponseGetStatusText() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    when(delegate.getStatusText()).thenReturn("OK");

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate);

    String result = response.getStatusText();

    assertThat(result).isEqualTo("OK");
  }

  @Test
  void defaultClientResponseClose() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate);

    response.close();

    verify(delegate).close();
  }

  private DefaultRestClient createBasicClient() {
    ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
    UriBuilderFactory uriBuilderFactory = mock(UriBuilderFactory.class);
    List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

    return new DefaultRestClient(
            requestFactory,
            null,
            null,
            uriBuilderFactory,
            null,
            null,
            null,
            null,
            null,
            messageConverters,
            mock(DefaultRestClientBuilder.class),
            false,
            false,
            null,
            null
    );
  }

}