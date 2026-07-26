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

package infra.web.reactive.server;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import infra.http.HttpHeaders;
import infra.http.reactive.server.ServerHttpRequest;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.StringUtils;
import infra.web.util.ForwardedHeaderUtils;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers to override
 * the request URI (i.e. {@link ServerHttpRequest#getURI()}) so it reflects
 * the client-originated protocol and address.
 *
 * <p>There are security considerations for forwarded headers since an application
 * cannot know if the headers were added by a proxy, as intended, or by a malicious
 * client. This is why a proxy at the boundary of trust should be configured to
 * remove untrusted Forwarded headers that come from the outside.
 *
 * <p>You can also configure the ForwardedHeaderFilter with {@link #setRemoveOnly removeOnly},
 * in which case it removes but does not use the headers.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 4.0 2022/10/21 12:19
 */
public class ForwardedHeaderTransformer implements Function<ServerHttpRequest, ServerHttpRequest> {

  static final Set<String> FORWARDED_HEADER_NAMES =
          Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(10, Locale.ROOT));

  static {
    FORWARDED_HEADER_NAMES.add("Forwarded");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-For");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
  }

  private final @Nullable Boolean useStandardHeader;

  private boolean useForwardedPrefix;

  private boolean removeOnly;

  /**
   * A default constructor with the historic behavior so far, which is to check
   * both the standard "Forwarded" header and the "X-Forwarded-*" alternative
   * headers in that order, also with "X-Forwarded-Prefix" enabled by default.
   * <p>This behavior depends on proxies being configured correctly
   * to clear both standard "Forwarded" and "X-Forwarded-*" header values coming
   * from the outside. Going forward, applications must explicitly declare which
   * forwarded headers are expected.
   */
  public ForwardedHeaderTransformer() {
    this.useStandardHeader = null;
    this.useForwardedPrefix = true;
  }

  /**
   * Create an instance of the transformer and specify whether it should use the
   * standard "Forwarded" header or the "X-Forwarded-*" alternative headers.
   * <p>"X-Forwarded-Prefix" is enabled separately via {@link #setUseForwardedPrefix}.
   *
   * @param useStandardHeader whether to use the standard "Forwarded" header
   * (true), or the "X-Forwarded-*" alternative headers (false).
   * @since 5.0
   */
  public ForwardedHeaderTransformer(boolean useStandardHeader) {
    this.useStandardHeader = useStandardHeader;
  }

  /**
   * Enable use of "X-Forwarded-Prefix" to determine the context path.
   * <p>By default, this is set to "false" in which case the header is ignored.
   *
   * @since 5.0
   */
  public void setUseForwardedPrefix(boolean useForwardedPrefix) {
    this.useForwardedPrefix = useForwardedPrefix;
  }

  /**
   * Enable mode in which any "Forwarded" or "X-Forwarded-*" headers are
   * removed only and the information in them ignored.
   *
   * @param removeOnly whether to discard and ignore forwarded headers
   */
  public void setRemoveOnly(boolean removeOnly) {
    this.removeOnly = removeOnly;
  }

  /**
   * Whether the "remove only" mode is on.
   *
   * @see #setRemoveOnly
   */
  public boolean isRemoveOnly() {
    return this.removeOnly;
  }

  /**
   * Apply and remove, or remove Forwarded type headers.
   *
   * @param request the request
   */
  @Override
  public ServerHttpRequest apply(ServerHttpRequest request) {
    if (hasForwardedHeaders(request)) {
      ServerHttpRequest.Builder builder = request.mutate();
      if (!this.removeOnly) {
        URI originalUri = request.getURI();
        HttpHeaders headers = request.getHeaders();
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        InetSocketAddress localAddress = request.getLocalAddress();

        var info = getForwardedInfo(
                this.useStandardHeader, originalUri, headers, remoteAddress, localAddress);

        URI uri = info.uri();
        builder.uri(uri);
        if (this.useForwardedPrefix) {
          String prefix = getForwardedPrefix(request);
          if (prefix != null) {
            builder.path(prefix + uri.getRawPath());
            builder.contextPath(prefix);
          }
        }
        remoteAddress = info.forAddress();
        if (remoteAddress != null) {
          builder.remoteAddress(remoteAddress);
        }
        localAddress = info.byAddress();
        if (localAddress != null) {
          builder.localAddress(localAddress);
        }
      }
      removeForwardedHeaders(builder);
      request = builder.build();
    }
    return request;
  }

  /**
   * Whether the request has any Forwarded headers.
   *
   * @param request the request
   */
  protected boolean hasForwardedHeaders(ServerHttpRequest request) {
    for (String name : FORWARDED_HEADER_NAMES) {
      if (request.containsHeader(name)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("removal")
  private static ForwardedHeaderUtils.ForwardedInfo getForwardedInfo(
          @Nullable Boolean useStandardHeader, URI uri, HttpHeaders headers,
          @Nullable InetSocketAddress remoteAddress, @Nullable InetSocketAddress localAddress) {

    if (useStandardHeader == null) {
      return new ForwardedHeaderUtils.ForwardedInfo(
              ForwardedHeaderUtils.adaptFromForwardedHeaders(uri, headers),
              ForwardedHeaderUtils.parseForwardedFor(uri, headers, remoteAddress),
              ForwardedHeaderUtils.parseForwardedBy(uri, headers, localAddress));
    }
    else {
      return (useStandardHeader ?
              ForwardedHeaderUtils.parseStandardHeader(uri, headers, remoteAddress, localAddress) :
              ForwardedHeaderUtils.parseXForwardedHeaders(uri, headers, remoteAddress, localAddress));
    }
  }

  private void removeForwardedHeaders(ServerHttpRequest.Builder builder) {
    builder.headers(map -> FORWARDED_HEADER_NAMES.forEach(map::remove));
  }

  private static @Nullable String getForwardedPrefix(ServerHttpRequest request) {
    String header = request.getHeader("X-Forwarded-Prefix");
    if (header == null) {
      return null;
    }
    StringBuilder prefix = new StringBuilder(header.length());
    String[] rawPrefixes = StringUtils.tokenizeToStringArray(header, ",");
    for (String rawPrefix : rawPrefixes) {
      int endIndex = rawPrefix.length();
      while (endIndex > 0 && rawPrefix.charAt(endIndex - 1) == '/') {
        endIndex--;
      }
      prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
    }
    return prefix.toString();
  }

}
