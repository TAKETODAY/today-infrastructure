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

package infra.restclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.ServerSentEvent;
import infra.http.StreamingHttpOutputMessage;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpResponse;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.restclient.DefaultRestClient.AbstractResponseSpec;
import infra.restclient.DefaultRestClient.DefaultClientResponse;
import infra.restclient.DefaultRestClient.DefaultRequestBodyUriSpec;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.client.ApiVersionInserter;
import infra.web.testfixture.http.MockClientHttpRequest;
import infra.web.testfixture.http.MockClientHttpResponse;
import infra.web.util.UriBuilderFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 16:01
 */
class DefaultRestClientTests {

  private static final String URL = "https://example.com";

  public static final String BODY = "Hello World";

  private final ClientHttpRequestFactory requestFactory = mock();

  private final ClientHttpRequest request = mock();

  private final ClientHttpResponse response = mock();

  private RestClient client;

  @BeforeEach
  void setup() {
    this.client = RestClient.builder()
            .requestFactory(this.requestFactory)
            .build();
  }

  @Test
  void requestDefaultsCanBeOverriddenWithoutChangingOriginalClient() throws IOException {
    RestClient original = this.client.mutate()
            .baseURI(URL)
            .defaultUriVariables(Map.of("region", "eu"))
            .defaultHeader("X-Source", "original")
            .defaultCookie("session", "default")
            .defaultCookie("theme", "light")
            .defaultApiVersion("1")
            .apiVersionInserter(ApiVersionInserter.forHeader("API-Version"))
            .defaultRequest(spec -> spec.header("X-Trace", "default"))
            .defaultRequest(spec -> spec.attribute("trace", "enabled"))
            .requestInitializer(request -> request.getHeaders().add("X-Stage", "initializer"))
            .build();
    RestClient derived = original.mutate().defaultHeader("X-Source", "derived").build();
    MockClientHttpRequest originalRequest = stubRequest(HttpMethod.GET, URL);
    MockClientHttpRequest derivedRequest = stubRequest(HttpMethod.GET, URL + "/eu/items/42");

    original.get().retrieve().toBodiless();
    derived.get().uri("/{region}/items/{id}", Map.of("id", 42))
            .header("X-Trace", "explicit")
            .cookie("session", "first").cookie("session", "second")
            .apiVersion("2")
            .attribute("item", 42)
            .httpRequest(request -> request.getHeaders().add("X-Stage", "first callback"))
            .httpRequest(request -> request.getHeaders().add("X-Stage", "second callback"))
            .retrieve().toBodiless();

    assertThat(originalRequest.getHeaders().getFirst("X-Source")).isEqualTo("original");
    assertThat(originalRequest.getHeaders().getFirst("X-Trace")).isEqualTo("default");
    assertThat(originalRequest.getHeaders().getFirst("API-Version")).isEqualTo("1");
    assertThat(originalRequest.getHeaders().getFirst(HttpHeaders.COOKIE)).isEqualTo("session=default; theme=light");
    assertThat(derivedRequest.getHeaders().getFirst("X-Source")).isEqualTo("derived");
    assertThat(derivedRequest.getHeaders().getFirst("X-Trace")).isEqualTo("explicit");
    assertThat(derivedRequest.getHeaders().getFirst("API-Version")).isEqualTo("2");
    assertThat(derivedRequest.getHeaders().getFirst(HttpHeaders.COOKIE)).isEqualTo("session=first; session=second; theme=light");
    assertThat(derivedRequest.getHeaders().get("X-Stage")).containsExactly("initializer", "first callback", "second callback");
    assertThat(derivedRequest.getAttributes()).containsEntry("trace", "enabled").containsEntry("item", 42);
  }

  @Test
  void interceptorCanReadBufferedResponseBeforeReturningItToCaller() throws IOException {
    RestClient client = this.client.mutate().baseURI(URL)
            .bufferContent(request -> request.getURI().getPath().startsWith("/items/"))
            .requestInterceptor((request, body, execution) -> {
              request.getHeaders().set("X-Intercepted", "true");
              ClientHttpResponse response = execution.execute(request, body);
              assertThat(response.getContentAsString(UTF_8)).isEqualTo(BODY);
              return response;
            })
            .build();

    for (int id : List.of(1, 2)) {
      MockClientHttpRequest request = stubRequest(HttpMethod.GET, URL + "/items/" + id);
      MockClientHttpResponse response = new MockClientHttpResponse(BODY.getBytes(UTF_8), HttpStatus.OK);
      response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
      request.setResponse(response);

      String result = client.get().uri("/items/{id}", builder -> builder.build(id))
              .cookie("view", "summary").retrieve().requiredBody(String.class);

      assertThat(result).isEqualTo(BODY);
      assertThat(request.getHeaders().getFirst("X-Intercepted")).isEqualTo("true");
      assertThat(request.getHeaders().getFirst(HttpHeaders.COOKIE)).isEqualTo("view=summary");
    }
  }

  @Test
  void requestBodyUsesParameterizedTypeWhenWritingJson() throws IOException {
    MockClientHttpRequest request = stubRequest(HttpMethod.POST, URL + "/items");
    RestClient client = this.client.mutate().baseURI(URL).build();

    client.post().uri(builder -> builder.path("/items").build()).contentType(MediaType.APPLICATION_JSON)
            .body(List.of("first", "second"), new ParameterizedTypeReference<List<String>>() { })
            .retrieve().toBodiless();

    assertThat(request.getBodyAsString(UTF_8)).isEqualTo("[\"first\",\"second\"]");
  }

  @Test
  void streamingBodyWritesDirectlyToNonStreamingRequest() throws IOException {
    mockSentRequest(HttpMethod.POST, URL);
    mockResponseStatus(HttpStatus.NO_CONTENT);
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    given(this.request.getBody()).willReturn(body);

    this.client.post().uri(URL).body(output -> output.write(BODY.getBytes(UTF_8))).retrieve().toBodiless();

    assertThat(body.toString(UTF_8)).isEqualTo(BODY);
    verify(this.response).close();
  }

  @Test
  void unsupportedRequestBodyFailsBeforeExecutingRequest() throws IOException {
    MockClientHttpRequest request = stubRequest(HttpMethod.POST, URL);
    RestClient client = this.client.mutate().messageConverters(List.of(new StringHttpMessageConverter())).build();

    assertThatThrownBy(() -> client.post().uri(URL).contentType(MediaType.APPLICATION_JSON).body(42).retrieve())
            .isInstanceOf(RestClientException.class)
            .hasMessage("No HttpMessageConverter for java.lang.Integer and content type \"application/json\"");
    assertThat(request.isExecuted()).isFalse();
  }

  @Test
  void unsupportedResponseBodyPreservesStatusHeadersAndPayload() throws IOException {
    MockClientHttpRequest request = stubRequest(HttpMethod.GET, URL);
    MockClientHttpResponse response = new MockClientHttpResponse(BODY.getBytes(UTF_8), HttpStatus.ACCEPTED);
    response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    response.getHeaders().set("X-Trace", "trace-1");
    request.setResponse(response);
    RestClient client = this.client.mutate().messageConverters(List.of(new StringHttpMessageConverter())).build();

    assertThatExceptionOfType(UnknownContentTypeException.class)
            .isThrownBy(() -> client.get().uri(URL).retrieve().body(Integer.class))
            .satisfies(error -> {
              assertThat(error.getTargetType()).isEqualTo(Integer.class);
              assertThat(error.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
              assertThat(error.getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
              assertThat(error.getResponseHeaders().getFirst("X-Trace")).isEqualTo("trace-1");
              assertThat(error.getResponseBodyAsString()).isEqualTo(BODY);
            });
  }

  @Test
  void bodilessResponseClosesWhenStatusHandlerFails() throws IOException {
    mockSentRequest(HttpMethod.GET, URL);
    mockResponseStatus(HttpStatus.BAD_REQUEST);
    IOException failure = new IOException("status handler failed");

    assertThatThrownBy(() -> this.client.get().uri(URL).retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
              throw failure;
            }).toBodiless())
            .isInstanceOf(ResourceAccessException.class)
            .hasMessage("Could not retrieve response status code: status handler failed")
            .cause().isSameAs(failure);

    verify(this.response).close();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("eventReaders")
  void readsServerSentEvents(String name, Function<RestClient.RequestHeadersSpec<?>, ServerSentEvents<?>> reader,
          Object expectedData) throws IOException {
    MockClientHttpRequest request = stubRequest(HttpMethod.GET, URL);
    MockClientHttpResponse response = spy(new MockClientHttpResponse("id: 3\ndata: [42]\n\n".getBytes(UTF_8), HttpStatus.OK));
    response.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
    request.setResponse(response);

    try (ServerSentEvents<?> events = reader.apply(this.client.get().uri(URL))) {
      verify(response, never()).close();
      ServerSentEvent<?> event = events.iterator().next();
      assertThat(event.id()).isEqualTo("3");
      assertThat(event.data()).isEqualTo(expectedData);
    }

    verify(response).close();
  }

  static Stream<Arguments> eventReaders() {
    ParameterizedTypeReference<List<Integer>> type = new ParameterizedTypeReference<>() { };
    return Stream.of(
            eventReader("raw events", spec -> spec.retrieve().events(), "[42]"),
            eventReader("typed events", spec -> spec.retrieve().events(type, MediaType.APPLICATION_JSON), List.of(42)),
            eventReader("async raw events", spec -> spec.async(Runnable::run).events().join(), "[42]"),
            eventReader("async typed events", spec -> spec.async(Runnable::run).events(type, MediaType.APPLICATION_JSON).join(), List.of(42))
    );
  }

  private static Arguments eventReader(String name, Function<RestClient.RequestHeadersSpec<?>, ServerSentEvents<?>> reader,
          Object expectedData) {
    return Arguments.of(name, reader, expectedData);
  }

  @Test
  void asyncEntityReadsParameterizedResponse() throws IOException {
    MockClientHttpRequest request = stubRequest(HttpMethod.GET, URL);
    MockClientHttpResponse response = new MockClientHttpResponse("[42]".getBytes(UTF_8), HttpStatus.OK);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    request.setResponse(response);

    ResponseEntity<List<Integer>> result = this.client.get().uri(URL).async(Runnable::run)
            .toEntity(new ParameterizedTypeReference<List<Integer>>() { }).join();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).containsExactly(42);
  }

  @Test
  void asyncBodilessEntityKeepsStatusAndHeadersAndClosesResponse() throws IOException {
    MockClientHttpRequest request = stubRequest(HttpMethod.GET, URL);
    MockClientHttpResponse response = spy(new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT));
    response.getHeaders().setETag("\"v1\"");
    request.setResponse(response);

    ResponseEntity<Void> result = this.client.get().uri(URL).async(Runnable::run).toBodilessEntity().join();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(result.headers().getETag()).isEqualTo("\"v1\"");
    assertThat(result.hasBody()).isFalse();
    verify(response).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void asyncFailuresFromRequestCreationAndExecutionOmitQuery(boolean creationFailure) throws IOException {
    String url = URL + "/items?token=hidden";
    IOException failure = new IOException("connection failed");
    if (creationFailure) {
      given(this.requestFactory.createRequest(URI.create(url), HttpMethod.GET)).willThrow(failure);
    }
    else {
      MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create(url)) {
        @Override
        protected ClientHttpResponse executeInternal() throws IOException {
          throw failure;
        }
      };
      given(this.requestFactory.createRequest(URI.create(url), HttpMethod.GET)).willReturn(request);
    }

    var future = this.client.get().uri(url).async(Runnable::run).toBodiless();

    assertThatThrownBy(future::join).isInstanceOf(ResourceAccessException.class)
            .hasMessage("I/O error on GET request for \"https://example.com/items\": connection failed")
            .cause().isSameAs(failure);
  }

  @Test
  void exchangeLeavesReturnedStreamOpenForCaller() throws IOException {
    mockSentRequest(HttpMethod.GET, URL);
    mockResponseStatus(HttpStatus.OK);
    mockResponseBody(BODY, MediaType.APPLICATION_OCTET_STREAM);

    try (InputStream stream = this.client.get().uri(URL).exchange((request, response) -> response.getBody())) {
      verify(this.response, never()).close();
      assertThat(stream.readAllBytes()).isEqualTo(BODY.getBytes(UTF_8));
    }
  }

  private MockClientHttpRequest stubRequest(HttpMethod method, String url) throws IOException {
    URI uri = URI.create(url);
    MockClientHttpRequest request = new MockClientHttpRequest(method, uri);
    request.setResponse(new MockClientHttpResponse());
    given(this.requestFactory.createRequest(uri, method)).willReturn(request);
    return request;
  }

  @Test
  void requiredBodyWithClass() throws IOException {
    mockSentRequest(HttpMethod.GET, URL);
    mockResponseStatus(HttpStatus.OK);
    mockResponseBody(BODY, MediaType.TEXT_PLAIN);

    String result = this.client.get().uri(URL).retrieve().requiredBody(String.class);

    assertThat(result).isEqualTo(BODY);
  }

  @Test
  void requiredBodyWithClassAndNullBody() throws IOException {
    mockSentRequest(HttpMethod.GET, URL);
    mockResponseStatus(HttpStatus.OK);
    mockEmptyResponseBody();

    assertThatIllegalStateException().isThrownBy(() ->
            this.client.get().uri(URL).retrieve().requiredBody(String.class)
    );
  }

  @Test
  void requiredBodyWithParameterizedTypeReference() throws IOException {
    mockSentRequest(HttpMethod.GET, URL);
    mockResponseStatus(HttpStatus.OK);
    mockResponseBody(BODY, MediaType.TEXT_PLAIN);

    String result = this.client.get().uri(URL).retrieve()
            .requiredBody(new ParameterizedTypeReference<>() {

            });

    assertThat(result).isEqualTo(BODY);
  }

  @Test
  void requiredBodyWithParameterizedTypeReferenceAndNullBody() throws IOException {
    mockSentRequest(HttpMethod.GET, URL);
    mockResponseStatus(HttpStatus.OK);
    mockEmptyResponseBody();

    assertThatIllegalStateException().isThrownBy(() ->
            this.client.get().uri(URL).retrieve()
                    .requiredBody(new ParameterizedTypeReference<String>() { })
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("streamResponseBodies")
  void streamingBody(String typeName, Consumer<RestClient> clientConsumer) throws IOException {
    mockSentRequest(HttpMethod.GET, URL);
    mockResponseStatus(HttpStatus.OK);
    mockResponseBody(BODY, MediaType.APPLICATION_OCTET_STREAM);

    clientConsumer.accept(this.client);

    verify(this.response, times(0)).close();
  }

  static Stream<Arguments> streamResponseBodies() {
    return Stream.of(
            Arguments.of("InputStream", (Consumer<RestClient>) client -> {
              InputStream result = client.get().uri(URL).retrieve().requiredBody(InputStream.class);
              assertThat(result).isInstanceOf(InputStream.class);
            }),
            Arguments.of("ResponseEntity<Inpustream>", (Consumer<RestClient>) client -> {
              ResponseEntity<InputStream> result = client.get().uri(URL).retrieve().toEntity(InputStream.class);
              assertThat(result).isInstanceOf(ResponseEntity.class);
            })
    );
  }

  private void mockSentRequest(HttpMethod method, String uri) throws IOException {
    given(this.requestFactory.createRequest(URI.create(uri), method)).willReturn(this.request);
    given(this.request.getHeaders()).willReturn(HttpHeaders.forWritable());
    given(this.request.getMethod()).willReturn(method);
    given(this.request.getURI()).willReturn(URI.create(uri));
  }

  private void mockResponseStatus(HttpStatus responseStatus) throws IOException {
    given(this.request.execute()).willReturn(this.response);
    given(this.response.getStatusCode()).willReturn(responseStatus);
    given(this.response.getStatusText()).willReturn(responseStatus.getReasonPhrase());
  }

  private void mockResponseBody(String expectedBody, MediaType mediaType) throws IOException {
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setContentType(mediaType);
    responseHeaders.setContentLength(expectedBody.length());
    given(this.response.getHeaders()).willReturn(responseHeaders);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(expectedBody.getBytes()));
  }

  private void mockEmptyResponseBody() throws IOException {
    HttpHeaders responseHeaders = HttpHeaders.forWritable();
    responseHeaders.setContentLength(0);
    given(this.response.getHeaders()).willReturn(responseHeaders);
    given(this.response.getBody()).willReturn(new ByteArrayInputStream(new byte[0]));
  }

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
    when(response.getContentType()).thenReturn(MediaType.APPLICATION_JSON);

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
    DefaultClientResponse response = client.new DefaultClientResponse(delegate, null);

    assertThat(response.delegate).isSameAs(delegate);
  }

  @Test
  void defaultClientResponseGetHeaders() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.set("X-Test", "value");
    when(delegate.getHeaders()).thenReturn(headers);

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate, null);

    HttpHeaders result = response.getHeaders();

    assertThat(result).isSameAs(headers);
    assertThat(result.getFirst("X-Test")).isEqualTo("value");
  }

  @Test
  void defaultClientResponseGetStatusCode() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    when(delegate.getStatusCode()).thenReturn(HttpStatusCode.valueOf(200));

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate, null);

    HttpStatusCode result = response.getStatusCode();

    assertThat(result).isEqualTo(HttpStatusCode.valueOf(200));
  }

  @Test
  void defaultClientResponseGetRawStatusCode() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    when(delegate.getRawStatusCode()).thenReturn(200);

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate, null);

    int result = response.getRawStatusCode();

    assertThat(result).isEqualTo(200);
  }

  @Test
  void defaultClientResponseGetStatusText() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);
    when(delegate.getStatusText()).thenReturn("OK");

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate, null);

    String result = response.getStatusText();

    assertThat(result).isEqualTo("OK");
  }

  @Test
  void defaultClientResponseClose() throws IOException {
    ClientHttpResponse delegate = mock(ClientHttpResponse.class);

    DefaultRestClient client = createBasicClient();
    DefaultClientResponse response = client.new DefaultClientResponse(delegate, null);

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
