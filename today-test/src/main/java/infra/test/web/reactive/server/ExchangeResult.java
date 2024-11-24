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

package infra.test.web.reactive.server;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.client.reactive.ClientHttpRequest;
import infra.http.client.reactive.ClientHttpResponse;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * Container for request and response details for exchanges performed through
 * {@link WebTestClient}.
 *
 * <p>Note that a decoded response body is not exposed at this level since the
 * body may not have been decoded and consumed yet. Subtypes
 * {@link EntityExchangeResult} and {@link FluxExchangeResult} provide access
 * to a decoded response entity and a decoded (but not consumed) response body
 * respectively.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see EntityExchangeResult
 * @see FluxExchangeResult
 * @since 4.0
 */
public class ExchangeResult {

  private static final Logger logger = LoggerFactory.getLogger(ExchangeResult.class);

  private static final List<MediaType> PRINTABLE_MEDIA_TYPES = List.of(
          MediaType.parseMediaType("application/*+json"), MediaType.APPLICATION_XML,
          MediaType.parseMediaType("text/*"), MediaType.APPLICATION_FORM_URLENCODED);

  private final ClientHttpRequest request;

  private final ClientHttpResponse response;

  private final Mono<byte[]> requestBody;

  private final Mono<byte[]> responseBody;

  private final Duration timeout;

  @Nullable
  private final String uriTemplate;

  @Nullable
  private final Object mockServerResult;

  /** Ensure single logging, e.g. for expectAll. */
  private boolean diagnosticsLogged;

  /**
   * Create an instance with an HTTP request and response along with promises
   * for the serialized request and response body content.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param requestBody capture of serialized request body content
   * @param responseBody capture of serialized response body content
   * @param timeout how long to wait for content to materialize
   * @param uriTemplate the URI template used to set up the request, if any
   * @param serverResult the result of a mock server exchange if applicable.
   */
  ExchangeResult(ClientHttpRequest request, ClientHttpResponse response,
          Mono<byte[]> requestBody, Mono<byte[]> responseBody, Duration timeout, @Nullable String uriTemplate,
          @Nullable Object serverResult) {

    Assert.notNull(request, "ClientHttpRequest is required");
    Assert.notNull(response, "ClientHttpResponse is required");
    Assert.notNull(requestBody, "'requestBody' is required");
    Assert.notNull(responseBody, "'responseBody' is required");

    this.request = request;
    this.response = response;
    this.requestBody = requestBody;
    this.responseBody = responseBody;
    this.timeout = timeout;
    this.uriTemplate = uriTemplate;
    this.mockServerResult = serverResult;
  }

  /**
   * Copy constructor to use after body is decoded and/or consumed.
   */
  ExchangeResult(ExchangeResult other) {
    this.request = other.request;
    this.response = other.response;
    this.requestBody = other.requestBody;
    this.responseBody = other.responseBody;
    this.timeout = other.timeout;
    this.uriTemplate = other.uriTemplate;
    this.mockServerResult = other.mockServerResult;
    this.diagnosticsLogged = other.diagnosticsLogged;
  }

  /**
   * Return the method of the request.
   */
  public HttpMethod getMethod() {
    return this.request.getMethod();
  }

  /**
   * Return the URI of the request.
   */
  public URI getUrl() {
    return this.request.getURI();
  }

  /**
   * Return the original URI template used to prepare the request, if any.
   */
  @Nullable
  public String getUriTemplate() {
    return this.uriTemplate;
  }

  /**
   * Return the request headers sent to the server.
   */
  public HttpHeaders getRequestHeaders() {
    return this.request.getHeaders();
  }

  /**
   * Return the raw request body content written through the request.
   * <p><strong>Note:</strong> If the request content has not been consumed
   * for any reason yet, use of this method will trigger consumption.
   *
   * @throws IllegalStateException if the request body has not been fully written.
   */
  @Nullable
  public byte[] getRequestBodyContent() {
    return this.requestBody.block(this.timeout);
  }

  /**
   * Return the HTTP status code as an {@link HttpStatusCode} value.
   */
  public HttpStatusCode getStatus() {
    return this.response.getStatusCode();
  }

  /**
   * Return the HTTP status code as an integer.
   */
  public int getRawStatusCode() {
    return getStatus().value();
  }

  /**
   * Return the response headers received from the server.
   */
  public HttpHeaders getResponseHeaders() {
    return this.response.getHeaders();
  }

  /**
   * Return response cookies received from the server.
   */
  public MultiValueMap<String, ResponseCookie> getResponseCookies() {
    return this.response.getCookies();
  }

  /**
   * Return the raw request body content written to the response.
   * <p><strong>Note:</strong> If the response content has not been consumed
   * yet, use of this method will trigger consumption.
   *
   * @throws IllegalStateException if the response has not been fully read.
   */
  @Nullable
  public byte[] getResponseBodyContent() {
    return this.responseBody.block(this.timeout);
  }

  /**
   * Return the result from the mock server exchange, if applicable, for
   * further assertions on the state of the server response.
   *
   * @see MockMvcWebTestClient#resultActionsFor(ExchangeResult)
   */
  @Nullable
  public Object getMockServerResult() {
    return this.mockServerResult;
  }

  /**
   * Execute the given Runnable, catch any {@link AssertionError}, log details
   * about the request and response at ERROR level under the class log
   * category, and after that re-throw the error.
   */
  public void assertWithDiagnostics(Runnable assertion) {
    try {
      assertion.run();
    }
    catch (AssertionError ex) {
      if (!this.diagnosticsLogged && logger.isErrorEnabled()) {
        this.diagnosticsLogged = true;
        logger.error("Request details for assertion failure:\n" + this);
      }
      throw ex;
    }
  }

  @Override
  public String toString() {
    return "\n" +
            "> " + getMethod() + " " + getUrl() + "\n" +
            "> " + formatHeaders(getRequestHeaders(), "\n> ") + "\n" +
            "\n" +
            formatBody(getRequestHeaders().getContentType(), this.requestBody) + "\n" +
            "\n" +
            "< " + formatStatus(getStatus()) + "\n" +
            "< " + formatHeaders(getResponseHeaders(), "\n< ") + "\n" +
            "\n" +
            formatBody(getResponseHeaders().getContentType(), this.responseBody) + "\n" +
            formatMockServerResult();
  }

  private String formatStatus(HttpStatusCode statusCode) {
    String result = statusCode.toString();
    if (statusCode instanceof HttpStatus status) {
      result += " " + status.getReasonPhrase();
    }
    return result;
  }

  private String formatHeaders(HttpHeaders headers, String delimiter) {
    return headers.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining(delimiter));
  }

  @Nullable
  private String formatBody(@Nullable MediaType contentType, Mono<byte[]> body) {
    return body
            .map(bytes -> {
              if (contentType == null) {
                return bytes.length + " bytes of content (unknown content-type).";
              }
              Charset charset = contentType.getCharset();
              if (charset != null) {
                return new String(bytes, charset);
              }
              if (PRINTABLE_MEDIA_TYPES.stream().anyMatch(contentType::isCompatibleWith)) {
                return new String(bytes, StandardCharsets.UTF_8);
              }
              return bytes.length + " bytes of content.";
            })
            .defaultIfEmpty("No content")
            .onErrorResume(ex -> Mono.just("Failed to obtain content: " + ex.getMessage()))
            .block(this.timeout);
  }

  private String formatMockServerResult() {
    return (this.mockServerResult != null ?
            "\n======================  MockMvc (Server) ===============================\n" +
                    this.mockServerResult + "\n" : "");
  }

}
