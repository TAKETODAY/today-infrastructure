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

package infra.web.accept;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
    handler.handleVersion("1.1", context);
    context.requestCompleted();

    assertThat(response.getHeader("Deprecation")).isEqualTo("@1688169599");
    assertThat(response.getHeader("Sunset")).isEqualTo(sunsetDate);
    assertThat(response.getHeaders("Link")).containsExactlyInAnyOrder(
            "<" + deprecationUrl + ">; rel=\"deprecation\"; type=\"text/html\"",
            "<" + sunsetUrl + ">; rel=\"sunset\"; type=\"text/html\""
    );
  }

  private static ZonedDateTime getDate(String date) {
    return ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME);
  }

}