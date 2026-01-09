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

package infra.web.socket.server.support;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import infra.http.HttpStatus;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.web.RequestContext;
import infra.web.cors.CorsConfiguration;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.server.HandshakeInterceptor;

/**
 * An interceptor to check request {@code Origin} header value against a
 * collection of allowed origins.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OriginHandshakeInterceptor implements HandshakeInterceptor {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final CorsConfiguration corsConfiguration = new CorsConfiguration();

  /**
   * Default constructor with only same origin requests allowed.
   */
  public OriginHandshakeInterceptor() {
  }

  /**
   * Constructor using the specified allowed origin values.
   *
   * @see #setAllowedOrigins(List)
   */
  public OriginHandshakeInterceptor(@Nullable List<String> allowedOrigins) {
    setAllowedOrigins(allowedOrigins);
  }

  /**
   * Set the origins for which cross-origin requests are allowed from a browser.
   * Please, refer to {@link CorsConfiguration#setAllowedOrigins(List)} for
   * format details and considerations, and keep in mind that the CORS spec
   * does not allow use of {@code "*"} with {@code allowCredentials=true}.
   * For more flexible origin patterns use {@link #setAllowedOriginPatterns}
   * instead.
   *
   * <p>By default, no origins are allowed. When
   * {@link #setAllowedOriginPatterns(List) allowedOriginPatterns} is also
   * set, then that takes precedence over this property.
   *
   * @see #setAllowedOriginPatterns(List)
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
   */
  public void setAllowedOrigins(@Nullable List<String> allowedOrigins) {
    corsConfiguration.setAllowedOrigins(allowedOrigins);
  }

  /**
   * Return the {@link #setAllowedOriginPatterns(List) configured} allowed origins.
   */
  public Collection<String> getAllowedOrigins() {
    List<String> allowedOrigins = this.corsConfiguration.getAllowedOrigins();
    return CollectionUtils.isEmpty(allowedOrigins) ? Collections.emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(allowedOrigins));
  }

  /**
   * Alternative to {@link #setAllowedOrigins(List)} that supports more
   * flexible patterns for specifying the origins for which cross-origin
   * requests are allowed from a browser. Please, refer to
   * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
   * details and other considerations.
   * <p>By default this is not set.
   */
  public void setAllowedOriginPatterns(@Nullable List<String> allowedOriginPatterns) {
    this.corsConfiguration.setAllowedOriginPatterns(allowedOriginPatterns);
  }

  /**
   * Return the {@link #setAllowedOriginPatterns(List) configured} allowed origin patterns.
   */
  public Collection<String> getAllowedOriginPatterns() {
    List<String> allowedOriginPatterns = this.corsConfiguration.getAllowedOriginPatterns();
    return CollectionUtils.isEmpty(allowedOriginPatterns)
            ? Collections.emptySet()
            : Collections.unmodifiableSet(new LinkedHashSet<>(allowedOriginPatterns));
  }

  @Override
  public boolean beforeHandshake(RequestContext request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
    if (request.isCorsRequest() && corsConfiguration.checkOrigin(request.getHeaders().getOrigin()) == null) {
      request.setStatus(HttpStatus.FORBIDDEN);
      if (logger.isDebugEnabled()) {
        logger.debug("Handshake request rejected, Origin header value {} not allowed", request.getHeaders().getOrigin());
      }
      return false;
    }
    return true;
  }

  @Override
  public void afterHandshake(RequestContext request, WebSocketHandler wsHandler, @Nullable Throwable exception) {

  }

}
