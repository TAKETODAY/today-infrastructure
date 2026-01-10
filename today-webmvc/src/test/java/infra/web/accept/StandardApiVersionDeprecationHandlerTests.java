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

package infra.web.accept;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import infra.http.MediaType;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/6/27 21:31
 */
class StandardApiVersionDeprecationHandlerTests {

  private final HttpMockRequest request = new HttpMockRequestImpl();

  private final HttpMockResponse response = new MockHttpResponseImpl();

  private final Object handler = new Object();

  @Test
  void basic() {
    String deprecationUrl = "https://example.org/deprecation";
    String sunsetDate = "Wed, 11 Nov 2026 11:11:11 GMT";
    String sunsetUrl = "https://example.org/sunset";

    ApiVersionParser<String> parser = version -> version;
    StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

    handler.configureVersion("1.1")
            .setDeprecationDate(getDate("Fri, 30 Jun 2023 23:59:59 GMT"))
            .setDeprecationLink(URI.create(deprecationUrl))
            .setSunsetDate(getDate(sunsetDate))
            .setSunsetLink(URI.create(sunsetUrl));

    MockRequestContext context = new MockRequestContext(request, response);
    handler.handleVersion("1.1", this.handler, context);
    context.requestCompleted();

    assertThat(response.getHeader("Deprecation")).isEqualTo("@1688169599");
    assertThat(response.getHeader("Sunset")).isEqualTo(sunsetDate);
    assertThat(response.getHeaders("Link")).containsExactlyInAnyOrder(
            "<" + deprecationUrl + ">; rel=\"deprecation\"; type=\"text/html\"",
            "<" + sunsetUrl + ">; rel=\"sunset\"; type=\"text/html\""
    );

    assertThat(handler.toString()).startsWith("StandardApiVersionDeprecationHandler [VersionInfo[version=1.1,");
  }

  @Test
  void handleVersionWithNoMatchDoesNotSetHeaders() {
    ApiVersionParser<String> parser = version -> version;
    StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

    handler.configureVersion("1.0")
            .setDeprecationDate(getDate("Fri, 30 Jun 2023 23:59:59 GMT"));

    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v2.0/resource");
    HttpMockResponse response = new MockHttpResponseImpl();
    MockRequestContext context = new MockRequestContext(request, response);

    handler.handleVersion("2.0", this.handler, context);
    context.requestCompleted();

    assertThat(response.getHeader("Deprecation")).isNull();
  }

  @Test
  void handleVersionWithPredicateMismatchDoesNotSetHeaders() {
    ApiVersionParser<String> parser = version -> version;
    StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

    handler.configureVersion("1.0")
            .setRequestPredicate(request -> request.getRequestURL().toString().contains("internal"))
            .setDeprecationDate(getDate("Fri, 30 Jun 2023 23:59:59 GMT"));

    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v1.0/resource");
    HttpMockResponse response = new MockHttpResponseImpl();
    MockRequestContext context = new MockRequestContext(request, response);

    handler.handleVersion("1.0", this.handler, context);
    context.requestCompleted();

    assertThat(response.getHeader("Deprecation")).isNull();
  }

  @Test
  void setDeprecationLinkWithCustomMediaType() {
    ApiVersionParser<String> parser = version -> version;
    StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

    URI deprecationUri = URI.create("https://example.com/deprecation");
    MediaType customMediaType = MediaType.APPLICATION_JSON;

    handler.configureVersion("1.0")
            .setDeprecationLink(deprecationUri, customMediaType);

    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v1.0/resource");
    HttpMockResponse response = new MockHttpResponseImpl();
    MockRequestContext context = new MockRequestContext(request, response);

    handler.handleVersion("1.0", this.handler, context);
    context.requestCompleted();

    assertThat(response.getHeaders("Link")).containsExactly(
            "<" + deprecationUri + ">; rel=\"deprecation\"; type=\"" + customMediaType + "\""
    );
  }

  @Test
  void setSunsetLinkWithCustomMediaType() {
    ApiVersionParser<String> parser = version -> version;
    StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

    URI sunsetUri = URI.create("https://example.com/sunset");
    MediaType customMediaType = MediaType.APPLICATION_JSON;

    handler.configureVersion("1.0")
            .setSunsetLink(sunsetUri, customMediaType);

    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v1.0/resource");
    HttpMockResponse response = new MockHttpResponseImpl();
    MockRequestContext context = new MockRequestContext(request, response);

    handler.handleVersion("1.0", this.handler, context);
    context.requestCompleted();

    assertThat(response.getHeaders("Link")).containsExactly(
            "<" + sunsetUri + ">; rel=\"sunset\"; type=\"" + customMediaType + "\""
    );
  }

  @Test
  void handleVersionWithMultipleLinkHeaders() {
    ApiVersionParser<String> parser = version -> version;
    StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

    URI deprecationUri = URI.create("https://example.com/deprecation");
    URI sunsetUri = URI.create("https://example.com/sunset");

    handler.configureVersion("1.0")
            .setDeprecationLink(deprecationUri)
            .setSunsetLink(sunsetUri);

    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v1.0/resource");
    HttpMockResponse response = new MockHttpResponseImpl();
    MockRequestContext context = new MockRequestContext(request, response);

    handler.handleVersion("1.0", this.handler, context);
    context.requestCompleted();

    assertThat(response.getHeaders("Link")).containsExactlyInAnyOrder(
            "<" + deprecationUri + ">; rel=\"deprecation\"; type=\"text/html\"",
            "<" + sunsetUri + ">; rel=\"sunset\"; type=\"text/html\""
    );
  }

  @Test
  void handleVersionWithAllHeadersSet() {
    ApiVersionParser<String> parser = version -> version;
    StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

    ZonedDateTime deprecationDate = getDate("Fri, 30 Jun 2023 23:59:59 GMT");
    ZonedDateTime sunsetDate = getDate("Wed, 11 Nov 2026 11:11:11 GMT");
    URI deprecationUri = URI.create("https://example.com/deprecation");
    URI sunsetUri = URI.create("https://example.com/sunset");

    handler.configureVersion("1.0")
            .setDeprecationDate(deprecationDate)
            .setDeprecationLink(deprecationUri)
            .setSunsetDate(sunsetDate)
            .setSunsetLink(sunsetUri);

    HttpMockRequest request = new HttpMockRequestImpl("GET", "/api/v1.0/resource");
    HttpMockResponse response = new MockHttpResponseImpl();
    MockRequestContext context = new MockRequestContext(request, response);

    handler.handleVersion("1.0", this.handler, context);
    context.requestCompleted();

    assertThat(response.getHeader("Deprecation")).isEqualTo("@1688169599");
    assertThat(response.getHeader("Sunset")).isEqualTo("Wed, 11 Nov 2026 11:11:11 GMT");
    assertThat(response.getHeaders("Link")).containsExactlyInAnyOrder(
            "<" + deprecationUri + ">; rel=\"deprecation\"; type=\"text/html\"",
            "<" + sunsetUri + ">; rel=\"sunset\"; type=\"text/html\""
    );
  }

  private static ZonedDateTime getDate(String date) {
    return ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME);
  }

}