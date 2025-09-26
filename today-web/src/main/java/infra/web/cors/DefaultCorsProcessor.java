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
package infra.web.cors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.lang.Constant;
import infra.lang.Modifiable;
import infra.lang.Nullable;
import infra.lang.Unmodifiable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.web.RequestContext;

/**
 * The default implementation of {@link CorsProcessor}, as defined by the
 * <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>.
 *
 * <p>
 * Note that when input {@link CorsConfiguration} is {@code null}, this
 * implementation does not reject simple or actual requests outright but simply
 * avoid adding CORS headers to the response. CORS processing is also skipped if
 * the response already contains CORS headers.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author TODAY 2019-12-23 21:02
 */
public class DefaultCorsProcessor implements CorsProcessor {

  private static final Logger log = LoggerFactory.getLogger(DefaultCorsProcessor.class);

  /**
   * The {@code Access-Control-Request-Private-Network} request header field name.
   *
   * @see <a href="https://wicg.github.io/private-network-access/">Private Network Access specification</a>
   */
  static final String ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK = "Access-Control-Request-Private-Network";

  /**
   * The {@code Access-Control-Allow-Private-Network} response header field name.
   *
   * @see <a href="https://wicg.github.io/private-network-access/">Private Network Access specification</a>
   */
  static final String ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK = "Access-Control-Allow-Private-Network";

  @Override
  public boolean process(@Nullable CorsConfiguration config, RequestContext context) throws IOException {
    if (config == null) {
      if (log.isDebugEnabled() && context.isCorsRequest()) {
        log.debug("Skip: no CORS configuration has been provided");
      }
      return true;
    }

    HttpHeaders responseHeaders = context.responseHeaders();

    List<String> varyHeaders = responseHeaders.getVary();
    if (!varyHeaders.contains(HttpHeaders.ORIGIN)) {
      responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    }
    if (!varyHeaders.contains(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)) {
      responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    }
    if (!varyHeaders.contains(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS)) {
      responseHeaders.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
    }

    try {
      if (!context.isCorsRequest()) {
        return true;
      }
    }
    catch (IllegalArgumentException ex) {
      log.debug("Reject: origin is malformed");
      rejectRequest(context);
      return false;
    }

    if (responseHeaders.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
      log.trace("Skip: response already contains \"Access-Control-Allow-Origin\"");
      return true;
    }
    return handleInternal(context, config, context.isPreFlightRequest());
  }

  /**
   * Invoked when one of the CORS checks failed. The default implementation sets
   * the response status to 403 and writes "Invalid CORS request" to the response.
   */
  protected void rejectRequest(RequestContext context) throws IOException {

    context.setStatus(HttpStatus.FORBIDDEN);
    context.getOutputStream()
            .write("Invalid CORS request".getBytes(Constant.DEFAULT_CHARSET));
    context.flush();
  }

  /**
   * Handle the given request.
   */
  protected boolean handleInternal(RequestContext context, CorsConfiguration config, boolean preFlightRequest) throws IOException {
    String requestOrigin = context.requestHeaders().getOrigin();

    String allowOrigin = checkOrigin(config, requestOrigin);

    if (allowOrigin == null) {
      log.debug("Reject: '{}' origin is not allowed", requestOrigin);
      rejectRequest(context);
      return false;
    }

    HttpMethod requestMethod = getMethodToUse(context, preFlightRequest);
    List<HttpMethod> allowMethods = checkMethods(config, requestMethod);
    if (allowMethods == null) {
      log.debug("Reject: HTTP '{}' is not allowed", requestMethod);
      rejectRequest(context);
      return false;
    }

    List<String> requestHeaders = getHeadersToUse(context, preFlightRequest);
    List<String> allowHeaders = checkHeaders(config, requestHeaders);
    if (preFlightRequest && allowHeaders == null) {
      log.debug("Reject: headers '{}' are not allowed", requestHeaders);
      rejectRequest(context);
      return false;
    }

    HttpHeaders responseHeaders = context.responseHeaders();
    responseHeaders.setAccessControlAllowOrigin(allowOrigin);

    if (preFlightRequest) {
      responseHeaders.setAccessControlAllowMethods(allowMethods);
    }

    if (preFlightRequest && !allowHeaders.isEmpty()) {
      responseHeaders.setAccessControlAllowHeaders(allowHeaders);
    }

    if (CollectionUtils.isNotEmpty(config.getExposedHeaders())) {
      responseHeaders.setAccessControlExposeHeaders(config.getExposedHeaders());
    }

    if (Boolean.TRUE.equals(config.getAllowCredentials())) {
      responseHeaders.setAccessControlAllowCredentials(Boolean.TRUE);
    }

    if (Boolean.TRUE.equals(config.getAllowPrivateNetwork())
            && Boolean.parseBoolean(context.getHeaders().getFirst(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK))) {
      responseHeaders.set(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK, Boolean.toString(true));
    }

    if (preFlightRequest && config.getMaxAge() != null) {
      responseHeaders.setAccessControlMaxAge(config.getMaxAge());
    }

    context.flush();
    return true;
  }

  /**
   * Check the origin and determine the origin for the response. The default
   * implementation simply delegates to
   * {@link CorsConfiguration#checkOrigin(String)}.
   */
  @Nullable
  protected String checkOrigin(CorsConfiguration config, @Nullable String requestOrigin) {
    return config.checkOrigin(requestOrigin);
  }

  /**
   * Check the HTTP method and determine the methods for the response of a
   * pre-flight request. The default implementation simply delegates to
   * {@link CorsConfiguration#checkOrigin(String)}.
   */
  @Nullable
  @Unmodifiable
  protected List<HttpMethod> checkMethods(CorsConfiguration config, @Nullable HttpMethod method) {
    return config.checkHttpMethod(method);
  }

  @Nullable
  private HttpMethod getMethodToUse(RequestContext request, boolean isPreFlight) {
    return isPreFlight ? request.getHeaders().getAccessControlRequestMethod() : request.getMethod();
  }

  /**
   * Check the headers and determine the headers for the response of a pre-flight
   * request. The default implementation simply delegates to
   * {@link CorsConfiguration#checkHeaders}.
   */
  @Nullable
  @Modifiable
  protected List<String> checkHeaders(CorsConfiguration config, List<String> requestHeaders) {
    return config.checkHeaders(requestHeaders);
  }

  private List<String> getHeadersToUse(RequestContext context, boolean isPreFlight) {
    if (isPreFlight) {
      return context.requestHeaders().getAccessControlRequestHeaders();
    }
    return new ArrayList<>(context.requestHeaders().keySet());
  }

}
