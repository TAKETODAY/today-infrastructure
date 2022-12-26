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

package cn.taketoday.web.socket.server.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.server.HandshakeInterceptor;
import cn.taketoday.web.util.WebUtils;

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
  public OriginHandshakeInterceptor() { }

  /**
   * Constructor using the specified allowed origin values.
   *
   * @see #setAllowedOrigins(Collection)
   */
  public OriginHandshakeInterceptor(Collection<String> allowedOrigins) {
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
   * {@link #setAllowedOriginPatterns(Collection) allowedOriginPatterns} is also
   * set, then that takes precedence over this property.
   *
   * <p>Note when SockJS is enabled and origins are restricted, transport types
   * that do not allow to check request origin (Iframe based transports) are
   * disabled. As a consequence, IE 6 to 9 are not supported when origins are
   * restricted.
   *
   * @see #setAllowedOriginPatterns(Collection)
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
   */
  public void setAllowedOrigins(Collection<String> allowedOrigins) {
    Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
    this.corsConfiguration.setAllowedOrigins(new ArrayList<>(allowedOrigins));
  }

  /**
   * Return the {@link #setAllowedOriginPatterns(Collection) configured} allowed origins.
   */
  public Collection<String> getAllowedOrigins() {
    List<String> allowedOrigins = this.corsConfiguration.getAllowedOrigins();
    return (CollectionUtils.isEmpty(allowedOrigins) ? Collections.emptySet() :
            Collections.unmodifiableSet(new LinkedHashSet<>(allowedOrigins)));
  }

  /**
   * Alternative to {@link #setAllowedOrigins(Collection)} that supports more
   * flexible patterns for specifying the origins for which cross-origin
   * requests are allowed from a browser. Please, refer to
   * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
   * details and other considerations.
   * <p>By default this is not set.
   */
  public void setAllowedOriginPatterns(Collection<String> allowedOriginPatterns) {
    Assert.notNull(allowedOriginPatterns, "Allowed origin patterns Collection must not be null");
    this.corsConfiguration.setAllowedOriginPatterns(new ArrayList<>(allowedOriginPatterns));
  }

  /**
   * Return the {@link #setAllowedOriginPatterns(Collection) configured} allowed origin patterns.
   */
  public Collection<String> getAllowedOriginPatterns() {
    List<String> allowedOriginPatterns = this.corsConfiguration.getAllowedOriginPatterns();
    return (CollectionUtils.isEmpty(allowedOriginPatterns) ? Collections.emptySet() :
            Collections.unmodifiableSet(new LinkedHashSet<>(allowedOriginPatterns)));
  }

  @Override
  public boolean beforeHandshake(RequestContext request,
          WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

    if (!WebUtils.isSameOrigin(request) &&
            this.corsConfiguration.checkOrigin(request.getHeaders().getOrigin()) == null) {
      request.setStatus(HttpStatus.FORBIDDEN);
      if (logger.isDebugEnabled()) {
        logger.debug("Handshake request rejected, Origin header value {} not allowed", request.getHeaders().getOrigin());
      }
      return false;
    }
    return true;
  }

  @Override
  public void afterHandshake(RequestContext request,
          WebSocketHandler wsHandler, @Nullable Exception exception) {
  }

}
