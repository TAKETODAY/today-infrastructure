/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.web.client;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.http.converter.FormHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJacksonValue;

import static cn.taketoday.http.HttpMethod.POST;
import static cn.taketoday.http.MediaType.MULTIPART_MIXED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Integration tests for {@link RestTemplate}.
 *
 * <h3>Logging configuration for {@code MockWebServer}</h3>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre class="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Sam Brannen
 */
class RestTemplateIntegrationTests extends AbstractMockWebServerTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("clientHttpRequestFactories")
  @interface ParameterizedRestTemplateTest {
  }

  @SuppressWarnings("deprecation")
  static Stream<ClientHttpRequestFactory> clientHttpRequestFactories() {
    return Stream.of(
            new SimpleClientHttpRequestFactory(),
            new HttpComponentsClientHttpRequestFactory(),
            new OkHttp3ClientHttpRequestFactory()
    );
  }

  private RestTemplate template;

  private ClientHttpRequestFactory clientHttpRequestFactory;

  /**
   * Custom JUnit Jupiter extension that handles exceptions thrown by test methods.
   *
   * <p>If the test method throws an {@link HttpServerErrorException}, this
   * extension will throw an {@link AssertionError} that wraps the
   * {@code HttpServerErrorException} using the
   * {@link HttpServerErrorException#getResponseBodyAsString() response body}
   * as the failure message.
   *
   * <p>This mechanism provides an actually meaningful failure message if the
   * test fails due to an {@code AssertionError} on the server.
   */
  @RegisterExtension
  TestExecutionExceptionHandler serverErrorToAssertionErrorConverter = (context, throwable) -> {
    if (throwable instanceof HttpServerErrorException ex) {
      String responseBody = ex.getResponseBodyAsString();
      String prefix = AssertionError.class.getName() + ": ";
      if (responseBody.startsWith(prefix)) {
        responseBody = responseBody.substring(prefix.length());
      }
      throw new AssertionError(responseBody, ex);
    }
    // Else throw as-is in order to comply with the contract of TestExecutionExceptionHandler.
    throw throwable;
  };

  private void setUpClient(ClientHttpRequestFactory clientHttpRequestFactory) {
    this.clientHttpRequestFactory = clientHttpRequestFactory;
    this.template = new RestTemplate(this.clientHttpRequestFactory);
  }

  @ParameterizedRestTemplateTest
  void getString(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    String s = template.getForObject(baseUrl + "/{method}", String.class, "get");
    assertThat(s).as("Invalid content").isEqualTo(helloWorld);
  }

  @ParameterizedRestTemplateTest
  void getEntity(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    ResponseEntity<String> entity = template.getForEntity(baseUrl + "/{method}", String.class, "get");
    assertThat(entity.getBody()).as("Invalid content").isEqualTo(helloWorld);
    assertThat(entity.getHeaders().isEmpty()).as("No headers").isFalse();
    assertThat(entity.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(textContentType);
    assertThat(entity.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
  }

  @ParameterizedRestTemplateTest
  void getNoResponse(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    String s = template.getForObject(baseUrl + "/get/nothing", String.class);
    assertThat(s).as("Invalid content").isNull();
  }

  @ParameterizedRestTemplateTest
  void getNoContentTypeHeader(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    byte[] bytes = template.getForObject(baseUrl + "/get/nocontenttype", byte[].class);
    assertThat(bytes).as("Invalid content").isEqualTo(helloWorld.getBytes(StandardCharsets.UTF_8));
  }

  @ParameterizedRestTemplateTest
  void getNoContent(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    String s = template.getForObject(baseUrl + "/status/nocontent", String.class);
    assertThat(s).as("Invalid content").isNull();

    ResponseEntity<String> entity = template.getForEntity(baseUrl + "/status/nocontent", String.class);
    assertThat(entity.getStatusCode()).as("Invalid response code").isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(entity.getBody()).as("Invalid content").isNull();
  }

  @ParameterizedRestTemplateTest
  void getNotModified(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    String s = template.getForObject(baseUrl + "/status/notmodified", String.class);
    assertThat(s).as("Invalid content").isNull();

    ResponseEntity<String> entity = template.getForEntity(baseUrl + "/status/notmodified", String.class);
    assertThat(entity.getStatusCode()).as("Invalid response code").isEqualTo(HttpStatus.NOT_MODIFIED);
    assertThat(entity.getBody()).as("Invalid content").isNull();
  }

  @ParameterizedRestTemplateTest
  void postForLocation(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    URI location = template.postForLocation(baseUrl + "/{method}", helloWorld, "post");
    assertThat(location).as("Invalid location").isEqualTo(new URI(baseUrl + "/post/1"));
  }

  @ParameterizedRestTemplateTest
  void postForLocationEntity(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    HttpHeaders entityHeaders = HttpHeaders.create();
    entityHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
    HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
    URI location = template.postForLocation(baseUrl + "/{method}", entity, "post");
    assertThat(location).as("Invalid location").isEqualTo(new URI(baseUrl + "/post/1"));
  }

  @ParameterizedRestTemplateTest
  void postForObject(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    String s = template.postForObject(baseUrl + "/{method}", helloWorld, String.class, "post");
    assertThat(s).as("Invalid content").isEqualTo(helloWorld);
  }

  @ParameterizedRestTemplateTest
  void patchForObject(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    Assumptions.assumeFalse(clientHttpRequestFactory instanceof SimpleClientHttpRequestFactory,
            "JDK client does not support the PATCH method");

    setUpClient(clientHttpRequestFactory);

    String s = template.patchForObject(baseUrl + "/{method}", helloWorld, String.class, "patch");
    assertThat(s).as("Invalid content").isEqualTo(helloWorld);
  }

  @ParameterizedRestTemplateTest
  void notFound(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
                    template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null))
            .satisfies(ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(ex.getStatusText()).isNotNull();
              assertThat(ex.getResponseBodyAsString()).isNotNull();
            });
  }

  @ParameterizedRestTemplateTest
  void badRequest(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
                    template.execute(baseUrl + "/status/badrequest", HttpMethod.GET, null, null))
            .satisfies(ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
              assertThat(ex.getMessage()).isEqualTo("400 Client Error: [no body]");
            });
  }

  @ParameterizedRestTemplateTest
  void serverError(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() ->
                    template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null))
            .satisfies(ex -> {
              assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
              assertThat(ex.getStatusText()).isNotNull();
              assertThat(ex.getResponseBodyAsString()).isNotNull();
            });
  }

  @ParameterizedRestTemplateTest
  void optionsForAllow(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    Set<HttpMethod> allowed = template.optionsForAllow(new URI(baseUrl + "/get"));
    assertThat(allowed).as("Invalid response").isEqualTo(EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE));
  }

  @ParameterizedRestTemplateTest
  void uri(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    String result = template.getForObject(baseUrl + "/uri/{query}", String.class, "Z\u00fcrich");
    assertThat(result).as("Invalid request URI").isEqualTo("/uri/Z%C3%BCrich");

    result = template.getForObject(baseUrl + "/uri/query={query}", String.class, "foo@bar");
    assertThat(result).as("Invalid request URI").isEqualTo("/uri/query=foo@bar");

    result = template.getForObject(baseUrl + "/uri/query={query}", String.class, "T\u014dky\u014d");
    assertThat(result).as("Invalid request URI").isEqualTo("/uri/query=T%C5%8Dky%C5%8D");
  }

  @ParameterizedRestTemplateTest
  void multipartFormData(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    template.postForLocation(baseUrl + "/multipartFormData", createMultipartParts());
  }

  @ParameterizedRestTemplateTest
  void multipartMixed(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    HttpHeaders requestHeaders = HttpHeaders.create();
    requestHeaders.setContentType(MULTIPART_MIXED);
    template.postForLocation(baseUrl + "/multipartMixed", new HttpEntity<>(createMultipartParts(), requestHeaders));
  }

  @ParameterizedRestTemplateTest
  void multipartRelated(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    addSupportedMediaTypeToFormHttpMessageConverter(MULTIPART_RELATED);

    HttpHeaders requestHeaders = HttpHeaders.create();
    requestHeaders.setContentType(MULTIPART_RELATED);
    template.postForLocation(baseUrl + "/multipartRelated", new HttpEntity<>(createMultipartParts(), requestHeaders));
  }

  private MultiValueMap<String, Object> createMultipartParts() {
    MultiValueMap<String, Object> parts = MultiValueMap.fromLinkedHashMap();
    parts.add("name 1", "value 1");
    parts.add("name 2", "value 2+1");
    parts.add("name 2", "value 2+2");
    Resource logo = new ClassPathResource("/cn/taketoday/http/converter/logo.jpg");
    parts.add("logo", logo);
    return parts;
  }

  private void addSupportedMediaTypeToFormHttpMessageConverter(MediaType mediaType) {
    this.template.getMessageConverters().stream()
            .filter(FormHttpMessageConverter.class::isInstance)
            .map(FormHttpMessageConverter.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to find FormHttpMessageConverter"))
            .addSupportedMediaTypes(mediaType);
  }

  @ParameterizedRestTemplateTest
  void form(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    MultiValueMap<String, String> form = new DefaultMultiValueMap<>();
    form.add("name 1", "value 1");
    form.add("name 2", "value 2+1");
    form.add("name 2", "value 2+2");

    template.postForLocation(baseUrl + "/form", form);
  }

  @ParameterizedRestTemplateTest
  void exchangeGet(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    HttpHeaders requestHeaders = HttpHeaders.create();
    requestHeaders.set("MyHeader", "MyValue");
    HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
    ResponseEntity<String> response =
            template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity, String.class, "get");
    assertThat(response.getBody()).as("Invalid content").isEqualTo(helloWorld);
  }

  @ParameterizedRestTemplateTest
  void exchangePost(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    HttpHeaders requestHeaders = HttpHeaders.create();
    requestHeaders.set("MyHeader", "MyValue");
    requestHeaders.setContentType(MediaType.TEXT_PLAIN);
    HttpEntity<String> entity = new HttpEntity<>(helloWorld, requestHeaders);
    HttpEntity<Void> result = template.exchange(baseUrl + "/{method}", POST, entity, Void.class, "post");
    assertThat(result.getHeaders().getLocation()).as("Invalid location").isEqualTo(new URI(baseUrl + "/post/1"));
    assertThat(result.hasBody()).isFalse();
  }

  @ParameterizedRestTemplateTest
  void jsonPostForObject(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    HttpHeaders entityHeaders = HttpHeaders.create();
    entityHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
    MySampleBean bean = new MySampleBean();
    bean.setWith1("with");
    bean.setWith2("with");
    bean.setWithout("without");
    HttpEntity<MySampleBean> entity = new HttpEntity<>(bean, entityHeaders);
    String s = template.postForObject(baseUrl + "/jsonpost", entity, String.class);
    assertThat(s.contains("\"with1\":\"with\"")).isTrue();
    assertThat(s.contains("\"with2\":\"with\"")).isTrue();
    assertThat(s.contains("\"without\":\"without\"")).isTrue();
  }

  @ParameterizedRestTemplateTest
  void jsonPostForObjectWithJacksonView(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    HttpHeaders entityHeaders = HttpHeaders.create();
    entityHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
    MySampleBean bean = new MySampleBean("with", "with", "without");
    MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
    jacksonValue.setSerializationView(MyJacksonView1.class);
    HttpEntity<MappingJacksonValue> entity = new HttpEntity<>(jacksonValue, entityHeaders);
    String s = template.postForObject(baseUrl + "/jsonpost", entity, String.class);
    assertThat(s.contains("\"with1\":\"with\"")).isTrue();
    assertThat(s.contains("\"with2\":\"with\"")).isFalse();
    assertThat(s.contains("\"without\":\"without\"")).isFalse();
  }

  @ParameterizedRestTemplateTest
    // SPR-12123
  void serverPort(ClientHttpRequestFactory clientHttpRequestFactory) {
    setUpClient(clientHttpRequestFactory);

    String s = template.getForObject("http://localhost:{port}/get", String.class, port);
    assertThat(s).as("Invalid content").isEqualTo(helloWorld);
  }

  @ParameterizedRestTemplateTest
    // SPR-13154
  void jsonPostForObjectWithJacksonTypeInfoList(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    List<ParentClass> list = new ArrayList<>();
    list.add(new Foo("foo"));
    list.add(new Bar("bar"));
    TypeReference<?> typeReference = new TypeReference<List<ParentClass>>() { };
    RequestEntity<List<ParentClass>> entity = RequestEntity
            .post(new URI(baseUrl + "/jsonpost"))
            .contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
            .body(list, typeReference.getType());
    String content = template.exchange(entity, String.class).getBody();
    assertThat(content.contains("\"type\":\"foo\"")).isTrue();
    assertThat(content.contains("\"type\":\"bar\"")).isTrue();
  }

  @ParameterizedRestTemplateTest
    // SPR-15015
  void postWithoutBody(ClientHttpRequestFactory clientHttpRequestFactory) throws Exception {
    setUpClient(clientHttpRequestFactory);

    assertThat(template.postForObject(baseUrl + "/jsonpost", null, String.class)).isNull();
  }

  public interface MyJacksonView1 { }

  public interface MyJacksonView2 { }

  public static class MySampleBean {

    @JsonView(MyJacksonView1.class)
    private String with1;

    @JsonView(MyJacksonView2.class)
    private String with2;

    private String without;

    private MySampleBean() {
    }

    private MySampleBean(String with1, String with2, String without) {
      this.with1 = with1;
      this.with2 = with2;
      this.without = without;
    }

    public String getWith1() {
      return with1;
    }

    public void setWith1(String with1) {
      this.with1 = with1;
    }

    public String getWith2() {
      return with2;
    }

    public void setWith2(String with2) {
      this.with2 = with2;
    }

    public String getWithout() {
      return without;
    }

    public void setWithout(String without) {
      this.without = without;
    }
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "cn/taketoday/core/testfixture/type")
  public static class ParentClass {

    private String parentProperty;

    public ParentClass() {
    }

    public ParentClass(String parentProperty) {
      this.parentProperty = parentProperty;
    }

    public String getParentProperty() {
      return parentProperty;
    }

    public void setParentProperty(String parentProperty) {
      this.parentProperty = parentProperty;
    }
  }

  @JsonTypeName("foo")
  public static class Foo extends ParentClass {

    public Foo() {
    }

    public Foo(String parentProperty) {
      super(parentProperty);
    }
  }

  @JsonTypeName("bar")
  public static class Bar extends ParentClass {

    public Bar() {
    }

    public Bar(String parentProperty) {
      super(parentProperty);
    }
  }

}
