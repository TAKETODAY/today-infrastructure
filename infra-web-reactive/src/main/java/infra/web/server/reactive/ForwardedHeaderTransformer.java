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

package infra.web.server.reactive;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import infra.http.HttpHeaders;
import infra.http.server.reactive.ServerHttpRequest;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.StringUtils;
import infra.web.util.ForwardedHeaderUtils;
import infra.web.util.UriComponents;

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
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
    FORWARDED_HEADER_NAMES.add("X-Forwarded-For");
  }

  private boolean removeOnly;

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
        URI uri = adaptFromForwardedHeaders(originalUri, headers);
        builder.uri(uri);
        String prefix = getForwardedPrefix(request);
        if (prefix != null) {
          builder.path(prefix + uri.getRawPath());
          builder.contextPath(prefix);
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        remoteAddress = ForwardedHeaderUtils.parseForwardedFor(originalUri, headers, remoteAddress);
        if (remoteAddress != null) {
          builder.remoteAddress(remoteAddress);
        }
        InetSocketAddress localAddress = request.getLocalAddress();
        localAddress = ForwardedHeaderUtils.parseForwardedBy(originalUri, headers, localAddress);
        if (localAddress != null) {
          builder.localAddress(localAddress);
        }
      }
      removeForwardedHeaders(builder);
      request = builder.build();
    }
    return request;
  }

  private static URI adaptFromForwardedHeaders(URI uri, HttpHeaders headers) {
    // GH-30137: assume URI is encoded, but avoid build(true) for more lenient handling
    UriComponents components = ForwardedHeaderUtils.adaptFromForwardedHeaders(uri, headers).build();
    try {
      return new URI(components.toUriString());
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
    }
  }

  /**
   * Whether the request has any Forwarded headers.
   *
   * @param request the request
   */
  protected boolean hasForwardedHeaders(ServerHttpRequest request) {
    HttpHeaders headers = request.getHeaders();
    for (String headerName : FORWARDED_HEADER_NAMES) {
      if (headers.containsKey(headerName)) {
        return true;
      }
    }
    return false;
  }

  private void removeForwardedHeaders(ServerHttpRequest.Builder builder) {
    builder.headers(map -> FORWARDED_HEADER_NAMES.forEach(map::remove));
  }

  @Nullable
  private static String getForwardedPrefix(ServerHttpRequest request) {
    HttpHeaders headers = request.getHeaders();
    String header = headers.getFirst("X-Forwarded-Prefix");
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
