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

package cn.taketoday.test.web.client.response;

import java.io.IOException;
import java.net.URI;

import cn.taketoday.core.io.Resource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.client.ResponseCreator;

/**
 * Static factory methods for obtaining a {@link ResponseCreator} instance.
 *
 * <p><strong>Eclipse users:</strong> consider adding this class as a Java editor
 * favorite. To navigate, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class MockRestResponseCreators {

  /**
   * {@code ResponseCreator} for a 200 response (OK).
   */
  public static DefaultResponseCreator withSuccess() {
    return new DefaultResponseCreator(HttpStatus.OK);
  }

  /**
   * {@code ResponseCreator} for a 200 response (OK) with String body.
   *
   * @param body the response body, a "UTF-8" string
   * @param contentType the type of the content (may be {@code null})
   */
  public static DefaultResponseCreator withSuccess(String body, @Nullable MediaType contentType) {
    DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
    return (contentType != null ? creator.contentType(contentType) : creator);
  }

  /**
   * {@code ResponseCreator} for a 200 response (OK) with byte[] body.
   *
   * @param body the response body
   * @param contentType the type of the content (may be {@code null})
   */
  public static DefaultResponseCreator withSuccess(byte[] body, @Nullable MediaType contentType) {
    DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
    return (contentType != null ? creator.contentType(contentType) : creator);
  }

  /**
   * {@code ResponseCreator} for a 200 response (OK) content with {@link Resource}-based body.
   *
   * @param body the response body
   * @param contentType the type of the content (may be {@code null})
   */
  public static DefaultResponseCreator withSuccess(Resource body, @Nullable MediaType contentType) {
    DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
    return (contentType != null ? creator.contentType(contentType) : creator);
  }

  /**
   * {@code ResponseCreator} for a 201 response (CREATED) with a 'Location' header.
   *
   * @param location the value for the {@code Location} header
   */
  public static DefaultResponseCreator withCreatedEntity(URI location) {
    return new DefaultResponseCreator(HttpStatus.CREATED).location(location);
  }

  /**
   * {@code ResponseCreator} for a 202 response (ACCEPTED).
   */
  public static DefaultResponseCreator withAccepted() {
    return new DefaultResponseCreator(HttpStatus.ACCEPTED);
  }

  /**
   * {@code ResponseCreator} for a 204 response (NO_CONTENT).
   */
  public static DefaultResponseCreator withNoContent() {
    return new DefaultResponseCreator(HttpStatus.NO_CONTENT);
  }

  /**
   * {@code ResponseCreator} for a 400 response (BAD_REQUEST).
   */
  public static DefaultResponseCreator withBadRequest() {
    return new DefaultResponseCreator(HttpStatus.BAD_REQUEST);
  }

  /**
   * {@code ResponseCreator} for a 401 response (UNAUTHORIZED).
   */
  public static DefaultResponseCreator withUnauthorizedRequest() {
    return new DefaultResponseCreator(HttpStatus.UNAUTHORIZED);
  }

  /**
   * {@code ResponseCreator} for a 403 response (FORBIDDEN).
   */
  public static DefaultResponseCreator withForbiddenRequest() {
    return new DefaultResponseCreator(HttpStatus.FORBIDDEN);
  }

  /**
   * {@code ResponseCreator} for a 404 response (NOT_FOUND).
   */
  public static DefaultResponseCreator withResourceNotFound() {
    return new DefaultResponseCreator(HttpStatus.NOT_FOUND);
  }

  /**
   * {@code ResponseCreator} for a 409 response (CONFLICT).
   */
  public static DefaultResponseCreator withRequestConflict() {
    return new DefaultResponseCreator(HttpStatus.CONFLICT);
  }

  /**
   * {@code ResponseCreator} for a 429 ratelimited response (TOO_MANY_REQUESTS).
   */
  public static DefaultResponseCreator withTooManyRequests() {
    return new DefaultResponseCreator(HttpStatus.TOO_MANY_REQUESTS);
  }

  /**
   * {@code ResponseCreator} for a 429 rate-limited response (TOO_MANY_REQUESTS)
   * with a {@code Retry-After} header in seconds.
   */
  public static DefaultResponseCreator withTooManyRequests(int retryAfter) {
    return new DefaultResponseCreator(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfter));
  }

  /**
   * {@code ResponseCreator} for a 500 response (SERVER_ERROR).
   */
  public static DefaultResponseCreator withServerError() {
    return new DefaultResponseCreator(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * {@code ResponseCreator} for a 502 response (BAD_GATEWAY).
   */
  public static DefaultResponseCreator withBadGateway() {
    return new DefaultResponseCreator(HttpStatus.BAD_GATEWAY);
  }

  /**
   * {@code ResponseCreator} for a 503 response (SERVICE_UNAVAILABLE).
   */
  public static DefaultResponseCreator withServiceUnavailable() {
    return new DefaultResponseCreator(HttpStatus.SERVICE_UNAVAILABLE);
  }

  /**
   * {@code ResponseCreator} for a 504 response (GATEWAY_TIMEOUT).
   */
  public static DefaultResponseCreator withGatewayTimeout() {
    return new DefaultResponseCreator(HttpStatus.GATEWAY_TIMEOUT);
  }

  /**
   * {@code ResponseCreator} with a specific HTTP status.
   *
   * @param status the response status
   */
  public static DefaultResponseCreator withStatus(HttpStatusCode status) {
    return new DefaultResponseCreator(status);
  }

  /**
   * Variant of {@link #withStatus(HttpStatusCode)} with an integer.
   *
   * @param status the response status
   */
  public static DefaultResponseCreator withRawStatus(int status) {
    return new DefaultResponseCreator(status);
  }

  /**
   * {@code ResponseCreator} with an internal application {@code IOException}.
   * <p>For example, one could use this to simulate a {@code SocketTimeoutException}.
   *
   * @param ex the {@code Exception} to be thrown at HTTP call time
   */
  public static ResponseCreator withException(IOException ex) {
    return request -> {
      throw ex;
    };
  }

}
