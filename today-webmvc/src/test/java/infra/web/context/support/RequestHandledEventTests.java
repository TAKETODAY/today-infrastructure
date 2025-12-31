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

package infra.web.context.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 22:19
 */
class RequestHandledEventTests {

  @Test
  void constructorInitializesAllFields() {
    Object source = new Object();
    String requestUrl = "/test";
    String clientAddress = "127.0.0.1";
    String method = "GET";
    String sessionId = "session123";
    long processingTimeMillis = 100L;
    Throwable failureCause = new RuntimeException("error");
    int statusCode = 200;

    RequestHandledEvent event = new RequestHandledEvent(source, requestUrl, clientAddress, method,
            sessionId, processingTimeMillis, failureCause, statusCode);

    assertThat(event.getSource()).isSameAs(source);
    assertThat(event.getRequestUrl()).isEqualTo(requestUrl);
    assertThat(event.getClientAddress()).isEqualTo(clientAddress);
    assertThat(event.getMethod()).isEqualTo(method);
    assertThat(event.getSessionId()).isEqualTo(sessionId);
    assertThat(event.getProcessingTimeMillis()).isEqualTo(processingTimeMillis);
    assertThat(event.getFailureCause()).isSameAs(failureCause);
    assertThat(event.getStatusCode()).isEqualTo(statusCode);
  }

  @Test
  void getProcessingTimeMillisReturnsCorrectValue() {
    Object source = new Object();
    long processingTime = 150L;

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", null, processingTime, null, 200);

    assertThat(event.getProcessingTimeMillis()).isEqualTo(processingTime);
  }

  @Test
  void getSessionIdReturnsCorrectValue() {
    Object source = new Object();
    String sessionId = "test-session-id";

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", sessionId, 100L, null, 200);

    assertThat(event.getSessionId()).isEqualTo(sessionId);
  }

  @Test
  void getSessionIdReturnsNullWhenNoSession() {
    Object source = new Object();

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", null, 100L, null, 200);

    assertThat(event.getSessionId()).isNull();
  }

  @Test
  void wasFailureReturnsTrueWhenFailureOccurred() {
    Object source = new Object();
    Throwable failure = new RuntimeException();

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", null, 100L, failure, 500);

    assertThat(event.wasFailure()).isTrue();
  }

  @Test
  void wasFailureReturnsFalseWhenNoFailure() {
    Object source = new Object();

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", null, 100L, null, 200);

    assertThat(event.wasFailure()).isFalse();
  }

  @Test
  void getFailureCauseReturnsCorrectValue() {
    Object source = new Object();
    Throwable failure = new RuntimeException("test error");

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", null, 100L, failure, 500);

    assertThat(event.getFailureCause()).isSameAs(failure);
  }

  @Test
  void getFailureCauseReturnsNullWhenNoFailure() {
    Object source = new Object();

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", null, 100L, null, 200);

    assertThat(event.getFailureCause()).isNull();
  }

  @Test
  void getRequestUrlReturnsCorrectValue() {
    Object source = new Object();
    String url = "/api/users";

    RequestHandledEvent event = new RequestHandledEvent(source, url, "", "", null, 100L, null, 200);

    assertThat(event.getRequestUrl()).isEqualTo(url);
  }

  @Test
  void getClientAddressReturnsCorrectValue() {
    Object source = new Object();
    String clientAddress = "192.168.1.100";

    RequestHandledEvent event = new RequestHandledEvent(source, "", clientAddress, "", null, 100L, null, 200);

    assertThat(event.getClientAddress()).isEqualTo(clientAddress);
  }

  @Test
  void getMethodReturnsCorrectValue() {
    Object source = new Object();
    String method = "POST";

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", method, null, 100L, null, 200);

    assertThat(event.getMethod()).isEqualTo(method);
  }

  @Test
  void getStatusCodeReturnsCorrectValue() {
    Object source = new Object();
    int statusCode = 404;

    RequestHandledEvent event = new RequestHandledEvent(source, "", "", "", null, 100L, null, statusCode);

    assertThat(event.getStatusCode()).isEqualTo(statusCode);
  }

  @Test
  void getShortDescriptionReturnsFormattedString() {
    Object source = new Object();
    String url = "/test";
    String client = "127.0.0.1";
    String session = "session123";

    RequestHandledEvent event = new RequestHandledEvent(source, url, client, "GET", session, 100L, null, 200);

    String description = event.getShortDescription();
    assertThat(description).contains("url=[/test]")
            .contains("client=[127.0.0.1]")
            .contains("session=[session123]");
  }

  @Test
  void getDescriptionReturnsFullFormattedStringForSuccess() {
    Object source = new Object();
    String url = "/api/test";
    String client = "192.168.1.1";
    String method = "POST";
    String session = "session456";
    long time = 250L;

    RequestHandledEvent event = new RequestHandledEvent(source, url, client, method, session, time, null, 201);

    String description = event.getDescription();
    assertThat(description).contains("url=[/api/test]")
            .contains("client=[192.168.1.1]")
            .contains("method=[POST]")
            .contains("session=[session456]")
            .contains("time=[250ms]")
            .contains("status=[OK]");
  }

  @Test
  void getDescriptionReturnsFullFormattedStringForFailure() {
    Object source = new Object();
    Throwable failure = new RuntimeException("database error");

    RequestHandledEvent event = new RequestHandledEvent(source, "/api/error", "192.168.1.1", "GET", null, 150L, failure, 500);

    String description = event.getDescription();
    assertThat(description).contains("url=[/api/error]")
            .contains("client=[192.168.1.1]")
            .contains("method=[GET]")
            .contains("time=[150ms]")
            .contains("status=[failed: ")
            .contains("database error");
  }

  @Test
  void toStringReturnsFormattedDescription() {
    Object source = new Object();

    RequestHandledEvent event = new RequestHandledEvent(source, "/test", "127.0.0.1", "GET", null, 100L, null, 200);

    String stringRepresentation = event.toString();
    assertThat(stringRepresentation).startsWith("RequestHandledEvent: ")
            .contains("url=[/test]")
            .contains("client=[127.0.0.1]")
            .contains("method=[GET]")
            .contains("status=[OK]");
  }

}