/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.util.ForwardedHeaderUtils;
import cn.taketoday.web.util.UriComponents;

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
          Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(10, Locale.ENGLISH));

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
      while (endIndex > 1 && rawPrefix.charAt(endIndex - 1) == '/') {
        endIndex--;
      }
      prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
    }
    return prefix.toString();
  }

}
