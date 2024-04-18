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

package cn.taketoday.web.util;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Utility class to assist with processing "Forwarded" and "X-Forwarded-*" headers.
 *
 * <p><strong>Note:</strong> There are security considerations surrounding the use
 * of forwarded headers. Those should not be used unless the application is
 * behind a trusted proxy that inserts them and also explicitly removes any such
 * headers coming from an external source.
 *
 * <p>In most cases, you should not use this class directly but rather rely on
 * {@link cn.taketoday.web.servlet.filter.ForwardedHeaderFilter} for Web MVC or
 * {@link cn.taketoday.http.server.reactive.ForwardedHeaderTransformer} in
 * order to extract the information from the headers as early as possible and discard
 * such headers. Underlying servers such as Tomcat, Jetty, and Reactor Netty also
 * provide options to handle forwarded headers even earlier.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ForwardedHeaderUtils {

  private static final String FORWARDED_VALUE = "\"?([^;,\"]+)\"?";

  private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("(?i:host)=" + FORWARDED_VALUE);

  private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("(?i:proto)=" + FORWARDED_VALUE);

  private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("(?i:for)=" + FORWARDED_VALUE);

  /**
   * Adapt the scheme+host+port of the given {@link URI} from the "Forwarded" header
   * (see <a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>) or from the
   * "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" headers if
   * "Forwarded" is not present.
   *
   * @param uri the request {@code URI}
   * @param headers the HTTP headers to consider
   * @return a {@link UriComponentsBuilder} that reflects the request URI and
   * additional updates from forwarded headers
   */
  public static UriComponentsBuilder adaptFromForwardedHeaders(URI uri, HttpHeaders headers) {
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUri(uri);
    try {
      String forwardedHeader = headers.getFirst("Forwarded");
      if (StringUtils.hasText(forwardedHeader)) {
        Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedHeader);
        if (matcher.find()) {
          uriComponentsBuilder.scheme(matcher.group(1).trim());
          uriComponentsBuilder.port(null);
        }
        else if (isForwardedSslOn(headers)) {
          uriComponentsBuilder.scheme("https");
          uriComponentsBuilder.port(null);
        }
        matcher = FORWARDED_HOST_PATTERN.matcher(forwardedHeader);
        if (matcher.find()) {
          adaptForwardedHost(uriComponentsBuilder, matcher.group(1).trim());
        }
      }
      else {
        String protocolHeader = headers.getFirst("X-Forwarded-Proto");
        if (StringUtils.hasText(protocolHeader)) {
          uriComponentsBuilder.scheme(StringUtils.tokenizeToStringArray(protocolHeader, ",")[0]);
          uriComponentsBuilder.port(null);
        }
        else if (isForwardedSslOn(headers)) {
          uriComponentsBuilder.scheme("https");
          uriComponentsBuilder.port(null);
        }
        String hostHeader = headers.getFirst("X-Forwarded-Host");
        if (StringUtils.hasText(hostHeader)) {
          adaptForwardedHost(uriComponentsBuilder, StringUtils.tokenizeToStringArray(hostHeader, ",")[0]);
        }
        String portHeader = headers.getFirst("X-Forwarded-Port");
        if (StringUtils.hasText(portHeader)) {
          uriComponentsBuilder.port(Integer.parseInt(StringUtils.tokenizeToStringArray(portHeader, ",")[0]));
        }
      }
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
              "Failed to parse a port from \"forwarded\"-type headers. If not behind a trusted proxy, consider using ForwardedHeaderFilter with removeOnly=true. Request headers: %s"
                      .formatted(headers));
    }

    uriComponentsBuilder.resetPortIfDefaultForScheme();

    return uriComponentsBuilder;
  }

  private static boolean isForwardedSslOn(HttpHeaders headers) {
    String forwardedSsl = headers.getFirst("X-Forwarded-Ssl");
    return StringUtils.hasText(forwardedSsl) && forwardedSsl.equalsIgnoreCase("on");
  }

  private static void adaptForwardedHost(UriComponentsBuilder uriComponentsBuilder, String rawValue) {
    int portSeparatorIdx = rawValue.lastIndexOf(':');
    int squareBracketIdx = rawValue.lastIndexOf(']');
    if (portSeparatorIdx > squareBracketIdx) {
      if (squareBracketIdx == -1 && rawValue.indexOf(':') != portSeparatorIdx) {
        throw new IllegalArgumentException("Invalid IPv4 address: " + rawValue);
      }
      uriComponentsBuilder.host(rawValue.substring(0, portSeparatorIdx));
      uriComponentsBuilder.port(Integer.parseInt(rawValue, portSeparatorIdx + 1, rawValue.length(), 10));
    }
    else {
      uriComponentsBuilder.host(rawValue);
      uriComponentsBuilder.port(null);
    }
  }

  /**
   * Parse the first "Forwarded: for=..." or "X-Forwarded-For" header value to
   * an {@code InetSocketAddress} representing the address of the client.
   *
   * @param uri the request {@code URI}
   * @param headers the request headers that may contain forwarded headers
   * @param remoteAddress the current remote address
   * @return an {@code InetSocketAddress} with the extracted host and port, or
   * {@code null} if the headers are not present
   * @see <a href="https://tools.ietf.org/html/rfc7239#section-5.2">RFC 7239, Section 5.2</a>
   */
  @Nullable
  public static InetSocketAddress parseForwardedFor(
          URI uri, HttpHeaders headers, @Nullable InetSocketAddress remoteAddress) {

    int port = (remoteAddress != null ?
            remoteAddress.getPort() : "https".equals(uri.getScheme()) ? 443 : 80);

    String forwardedHeader = headers.getFirst("Forwarded");
    if (StringUtils.hasText(forwardedHeader)) {
      String forwardedToUse = StringUtils.tokenizeToStringArray(forwardedHeader, ",")[0];
      Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwardedToUse);
      if (matcher.find()) {
        String value = matcher.group(1).trim();
        String host = value;
        int portSeparatorIdx = value.lastIndexOf(':');
        int squareBracketIdx = value.lastIndexOf(']');
        if (portSeparatorIdx > squareBracketIdx) {
          if (squareBracketIdx == -1 && value.indexOf(':') != portSeparatorIdx) {
            throw new IllegalArgumentException("Invalid IPv4 address: " + value);
          }
          host = value.substring(0, portSeparatorIdx);
          try {
            port = Integer.parseInt(value, portSeparatorIdx + 1, value.length(), 10);
          }
          catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Failed to parse a port from \"forwarded\"-type header value: " + value);
          }
        }
        return InetSocketAddress.createUnresolved(host, port);
      }
    }

    String forHeader = headers.getFirst("X-Forwarded-For");
    if (StringUtils.hasText(forHeader)) {
      String host = StringUtils.tokenizeToStringArray(forHeader, ",")[0];
      return InetSocketAddress.createUnresolved(host, port);
    }

    return null;
  }

}
