/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.util;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.util.HierarchicalUriComponents.PathComponent;
import cn.taketoday.web.util.UriComponents.UriTemplateVariables;

/**
 * Builder for {@link UriComponents}.
 *
 * <p>Typical usage involves:
 * <ol>
 * <li>Create a {@code UriComponentsBuilder} with one of the static factory methods
 * (such as {@link #fromPath(String)} or {@link #fromUri(URI)})</li>
 * <li>Set the various URI components through the respective methods ({@link #scheme(String)},
 * {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 * {@link #pathSegment(String...)}, {@link #queryParam(String, Object...)}, and
 * {@link #fragment(String)}.</li>
 * <li>Build the {@link UriComponents} instance with the {@link #build()} method.</li>
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 * @since 4.0
 */
public class UriComponentsBuilder implements UriBuilder, Cloneable {

  private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

  private static final String SCHEME_PATTERN = "([^:/?#]+):";

  private static final String HTTP_PATTERN = "(?i)(http|https):";

  private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

  private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

  private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}:.]*[%\\p{Alnum}]*]";

  private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

  private static final String PORT_PATTERN = "(\\{[^}]+\\}?|[^/?#]*)";

  private static final String PATH_PATTERN = "([^?#]*)";

  private static final String QUERY_PATTERN = "([^#]*)";

  private static final String LAST_PATTERN = "(.*)";

  // Regex patterns that matches URIs. See RFC 3986, appendix B
  private static final Pattern URI_PATTERN = Pattern.compile(
          "^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
                  ")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

  private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
          "^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
                  PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

  private static final String FORWARDED_VALUE = "\"?([^;,\"]+)\"?";

  private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("(?i:host)=" + FORWARDED_VALUE);

  private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("(?i:proto)=" + FORWARDED_VALUE);

  private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("(?i:for)=" + FORWARDED_VALUE);

  private static final Object[] EMPTY_VALUES = new Object[0];

  @Nullable
  private String scheme;

  @Nullable
  private String ssp;

  @Nullable
  private String userInfo;

  @Nullable
  private String host;

  @Nullable
  private String port;

  private CompositePathComponentBuilder pathBuilder;

  private final DefaultMultiValueMap<String, String> queryParams = new DefaultMultiValueMap<>();

  @Nullable
  private String fragment;

  private final HashMap<String, Object> uriVariables = new HashMap<>(4);

  private boolean encodeTemplate;

  private Charset charset = StandardCharsets.UTF_8;

  /**
   * Default constructor. Protected to prevent direct instantiation.
   *
   * @see #newInstance()
   * @see #fromPath(String)
   * @see #fromUri(URI)
   */
  protected UriComponentsBuilder() {
    this.pathBuilder = new CompositePathComponentBuilder();
  }

  /**
   * Create a deep copy of the given UriComponentsBuilder.
   *
   * @param other the other builder to copy from
   */
  protected UriComponentsBuilder(UriComponentsBuilder other) {
    this.scheme = other.scheme;
    this.ssp = other.ssp;
    this.userInfo = other.userInfo;
    this.host = other.host;
    this.port = other.port;
    this.pathBuilder = other.pathBuilder.cloneBuilder();
    this.uriVariables.putAll(other.uriVariables);
    this.queryParams.addAll(other.queryParams);
    this.fragment = other.fragment;
    this.encodeTemplate = other.encodeTemplate;
    this.charset = other.charset;
  }

  // Factory methods

  /**
   * Create a new, empty builder.
   *
   * @return the new {@code UriComponentsBuilder}
   */
  public static UriComponentsBuilder newInstance() {
    return new UriComponentsBuilder();
  }

  /**
   * Create a builder that is initialized with the given path.
   *
   * @param path the path to initialize with
   * @return the new {@code UriComponentsBuilder}
   */
  public static UriComponentsBuilder fromPath(String path) {
    UriComponentsBuilder builder = new UriComponentsBuilder();
    builder.path(path);
    return builder;
  }

  /**
   * Create a builder that is initialized from the given {@code URI}.
   * <p><strong>Note:</strong> the components in the resulting builder will be
   * in fully encoded (raw) form and further changes must also supply values
   * that are fully encoded, for example via methods in {@link UriUtils}.
   * In addition please use {@link #build(boolean)} with a value of "true" to
   * build the {@link UriComponents} instance in order to indicate that the
   * components are encoded.
   *
   * @param uri the URI to initialize with
   * @return the new {@code UriComponentsBuilder}
   */
  public static UriComponentsBuilder fromUri(URI uri) {
    UriComponentsBuilder builder = new UriComponentsBuilder();
    builder.uri(uri);
    return builder;
  }

  /**
   * Create a builder that is initialized with the given URI string.
   * <p><strong>Note:</strong> The presence of reserved characters can prevent
   * correct parsing of the URI string. For example if a query parameter
   * contains {@code '='} or {@code '&'} characters, the query string cannot
   * be parsed unambiguously. Such values should be substituted for URI
   * variables to enable correct parsing:
   * <pre class="code">
   * String uriString = &quot;/hotels/42?filter={value}&quot;;
   * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
   * </pre>
   *
   * @param uri the URI string to initialize with
   * @return the new {@code UriComponentsBuilder}
   */
  public static UriComponentsBuilder fromUriString(String uri) {
    Assert.notNull(uri, "URI must not be null");
    Matcher matcher = URI_PATTERN.matcher(uri);
    if (matcher.matches()) {
      UriComponentsBuilder builder = new UriComponentsBuilder();
      String scheme = matcher.group(2);
      String userInfo = matcher.group(5);
      String host = matcher.group(6);
      String port = matcher.group(8);
      String path = matcher.group(9);
      String query = matcher.group(11);
      String fragment = matcher.group(13);
      boolean opaque = false;
      if (StringUtils.isNotEmpty(scheme)) {
        String rest = uri.substring(scheme.length());
        if (!rest.startsWith(":/")) {
          opaque = true;
        }
      }
      builder.scheme(scheme);
      if (opaque) {
        String ssp = uri.substring(scheme.length() + 1);
        if (StringUtils.isNotEmpty(fragment)) {
          ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
        }
        builder.schemeSpecificPart(ssp);
      }
      else {
        if (StringUtils.isNotEmpty(scheme) && scheme.startsWith("http") && StringUtils.isEmpty(host)) {
          throw new IllegalArgumentException("[" + uri + "] is not a valid HTTP URL");
        }
        builder.userInfo(userInfo);
        builder.host(host);
        if (StringUtils.isNotEmpty(port)) {
          builder.port(port);
        }
        builder.path(path);
        builder.query(query);
      }
      if (StringUtils.hasText(fragment)) {
        builder.fragment(fragment);
      }
      return builder;
    }
    else {
      throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
    }
  }

  /**
   * Create a URI components builder from the given HTTP URL String.
   * <p><strong>Note:</strong> The presence of reserved characters can prevent
   * correct parsing of the URI string. For example if a query parameter
   * contains {@code '='} or {@code '&'} characters, the query string cannot
   * be parsed unambiguously. Such values should be substituted for URI
   * variables to enable correct parsing:
   * <pre class="code">
   * String urlString = &quot;https://example.com/hotels/42?filter={value}&quot;;
   * UriComponentsBuilder.fromHttpUrl(urlString).buildAndExpand(&quot;hot&amp;cold&quot;);
   * </pre>
   *
   * @param httpUrl the source URI
   * @return the URI components of the URI
   */
  public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
    Assert.notNull(httpUrl, "HTTP URL must not be null");
    Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
    if (matcher.matches()) {
      UriComponentsBuilder builder = new UriComponentsBuilder();
      String scheme = matcher.group(1);
      builder.scheme(scheme != null ? scheme.toLowerCase() : null);
      builder.userInfo(matcher.group(4));
      String host = matcher.group(5);
      if (StringUtils.isNotEmpty(scheme) && StringUtils.isEmpty(host)) {
        throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
      }
      builder.host(host);
      String port = matcher.group(7);
      if (StringUtils.isNotEmpty(port)) {
        builder.port(port);
      }
      builder.path(matcher.group(8));
      builder.query(matcher.group(10));
      String fragment = matcher.group(12);
      if (StringUtils.hasText(fragment)) {
        builder.fragment(fragment);
      }
      return builder;
    }
    else {
      throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
    }
  }

  /**
   * Create a new {@code UriComponents} object from the URI associated with
   * the given HttpRequest while also overlaying with values from the headers
   * "Forwarded" (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>),
   * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
   * "Forwarded" is not found.
   *
   * @param request the source request
   * @return the URI components of the URI
   * @see #parseForwardedFor(HttpRequest, InetSocketAddress)
   */
  public static UriComponentsBuilder fromHttpRequest(HttpRequest request) {
    return fromUri(request.getURI()).adaptFromForwardedHeaders(request.getHeaders());
  }

  /**
   * Parse the first "Forwarded: for=..." or "X-Forwarded-For" header value to
   * an {@code InetSocketAddress} representing the address of the client.
   *
   * @param request a request with headers that may contain forwarded headers
   * @param remoteAddress the current remoteAddress
   * @return an {@code InetSocketAddress} with the extracted host and port, or
   * {@code null} if the headers are not present.
   * @see <a href="https://tools.ietf.org/html/rfc7239#section-5.2">RFC 7239, Section 5.2</a>
   */
  @Nullable
  public static InetSocketAddress parseForwardedFor(
          HttpRequest request, @Nullable InetSocketAddress remoteAddress) {

    int port = (remoteAddress != null ?
                remoteAddress.getPort() : "https".equals(request.getURI().getScheme()) ? 443 : 80);

    String forwardedHeader = request.getHeaders().getFirst("Forwarded");
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
            port = Integer.parseInt(value.substring(portSeparatorIdx + 1));
          }
          catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Failed to parse a port from \"forwarded\"-type header value: " + value);
          }
        }
        return InetSocketAddress.createUnresolved(host, port);
      }
    }

    String forHeader = request.getHeaders().getFirst("X-Forwarded-For");
    if (StringUtils.hasText(forHeader)) {
      String host = StringUtils.tokenizeToStringArray(forHeader, ",")[0];
      return InetSocketAddress.createUnresolved(host, port);
    }
    return null;
  }

  /**
   * Create an instance by parsing the "Origin" header of an HTTP request.
   *
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
   */
  public static UriComponentsBuilder fromOriginHeader(String origin) {
    Matcher matcher = URI_PATTERN.matcher(origin);
    if (matcher.matches()) {
      UriComponentsBuilder builder = new UriComponentsBuilder();
      String scheme = matcher.group(2);
      String host = matcher.group(6);
      String port = matcher.group(8);
      if (StringUtils.isNotEmpty(scheme)) {
        builder.scheme(scheme);
      }
      builder.host(host);
      if (StringUtils.isNotEmpty(port)) {
        builder.port(port);
      }
      return builder;
    }
    else {
      throw new IllegalArgumentException("[" + origin + "] is not a valid \"Origin\" header value");
    }
  }

  // Encode methods

  /**
   * Request to have the URI template pre-encoded at build time, and
   * URI variables encoded separately when expanded.
   * <p>In comparison to {@link UriComponents#encode()}, this method has the
   * same effect on the URI template, i.e. each URI component is encoded by
   * replacing non-ASCII and illegal (within the URI component type) characters
   * with escaped octets. However URI variables are encoded more strictly, by
   * also escaping characters with reserved meaning.
   * <p>For most cases, this method is more likely to give the expected result
   * because in treats URI variables as opaque data to be fully encoded, while
   * {@link UriComponents#encode()} is useful when intentionally expanding URI
   * variables that contain reserved characters.
   * <p>For example ';' is legal in a path but has reserved meaning. This
   * method replaces ";" with "%3B" in URI variables but not in the URI
   * template. By contrast, {@link UriComponents#encode()} never replaces ";"
   * since it is a legal character in a path.
   * <p>When not expanding URI variables at all, prefer use of
   * {@link UriComponents#encode()} since that will also encode anything that
   * incidentally looks like a URI variable.
   */
  public final UriComponentsBuilder encode() {
    return encode(StandardCharsets.UTF_8);
  }

  /**
   * A variant of {@link #encode()} with a charset other than "UTF-8".
   *
   * @param charset the charset to use for encoding
   */
  public UriComponentsBuilder encode(Charset charset) {
    this.encodeTemplate = true;
    this.charset = charset;
    return this;
  }

  // Build methods

  /**
   * Build a {@code UriComponents} instance from the various components contained in this builder.
   *
   * @return the URI components
   */
  public UriComponents build() {
    return build(false);
  }

  /**
   * Variant of {@link #build()} to create a {@link UriComponents} instance
   * when components are already fully encoded. This is useful for example if
   * the builder was created via {@link UriComponentsBuilder#fromUri(URI)}.
   *
   * @param encoded whether the components in this builder are already encoded
   * @return the URI components
   * @throws IllegalArgumentException if any of the components contain illegal
   * characters that should have been encoded.
   */
  public UriComponents build(boolean encoded) {
    return buildInternal(encoded ? EncodingHint.FULLY_ENCODED :
                         (this.encodeTemplate ? EncodingHint.ENCODE_TEMPLATE : EncodingHint.NONE));
  }

  private UriComponents buildInternal(EncodingHint hint) {
    UriComponents result;
    if (this.ssp != null) {
      result = new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
    }
    else {
      MultiValueMap<String, String> queryParams = MultiValueMap.from(this.queryParams);
      HierarchicalUriComponents uric = new HierarchicalUriComponents(
              this.scheme, this.fragment, this.userInfo, this.host,
              this.port, this.pathBuilder.build(), queryParams, hint == EncodingHint.FULLY_ENCODED);
      result = (hint == EncodingHint.ENCODE_TEMPLATE ? uric.encodeTemplate(this.charset) : uric);
    }
    if (!this.uriVariables.isEmpty()) {
      result = result.expand(name -> this.uriVariables.getOrDefault(name, UriTemplateVariables.SKIP_VALUE));
    }
    return result;
  }

  /**
   * Build a {@code UriComponents} instance and replaces URI template variables
   * with the values from a map. This is a shortcut method which combines
   * calls to {@link #build()} and then {@link UriComponents#expand(Map)}.
   *
   * @param uriVariables the map of URI variables
   * @return the URI components with expanded values
   */
  public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
    return build().expand(uriVariables);
  }

  /**
   * Build a {@code UriComponents} instance and replaces URI template variables
   * with the values from an array. This is a shortcut method which combines
   * calls to {@link #build()} and then {@link UriComponents#expand(Object...)}.
   *
   * @param uriVariableValues the URI variable values
   * @return the URI components with expanded values
   */
  public UriComponents buildAndExpand(Object... uriVariableValues) {
    return build().expand(uriVariableValues);
  }

  @Override
  public URI build(Object... uriVariables) {
    return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
  }

  @Override
  public URI build(Map<String, ?> uriVariables) {
    return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
  }

  /**
   * Build a URI String.
   * <p>Effectively, a shortcut for building, encoding, and returning the
   * String representation:
   * <pre class="code">
   * String uri = builder.build().encode().toUriString()
   * </pre>
   * <p>However if {@link #uriVariables(Map) URI variables} have been provided
   * then the URI template is pre-encoded separately from URI variables (see
   * {@link #encode()} for details), i.e. equivalent to:
   * <pre>
   * String uri = builder.encode().build().toUriString()
   * </pre>
   *
   * @see UriComponents#toUriString()
   */
  public String toUriString() {
    return (this.uriVariables.isEmpty() ? build().encode().toUriString() :
            buildInternal(EncodingHint.ENCODE_TEMPLATE).toUriString());
  }

  // Instance methods

  /**
   * Initialize components of this builder from components of the given URI.
   *
   * @param uri the URI
   * @return this UriComponentsBuilder
   */
  public UriComponentsBuilder uri(URI uri) {
    Assert.notNull(uri, "URI must not be null");
    this.scheme = uri.getScheme();
    if (uri.isOpaque()) {
      this.ssp = uri.getRawSchemeSpecificPart();
      resetHierarchicalComponents();
    }
    else {
      if (uri.getRawUserInfo() != null) {
        this.userInfo = uri.getRawUserInfo();
      }
      if (uri.getHost() != null) {
        this.host = uri.getHost();
      }
      if (uri.getPort() != -1) {
        this.port = String.valueOf(uri.getPort());
      }
      if (StringUtils.isNotEmpty(uri.getRawPath())) {
        this.pathBuilder = new CompositePathComponentBuilder();
        this.pathBuilder.addPath(uri.getRawPath());
      }
      if (StringUtils.isNotEmpty(uri.getRawQuery())) {
        this.queryParams.clear();
        query(uri.getRawQuery());
      }
      resetSchemeSpecificPart();
    }
    if (uri.getRawFragment() != null) {
      this.fragment = uri.getRawFragment();
    }
    return this;
  }

  /**
   * Set or append individual URI components of this builder from the values
   * of the given {@link UriComponents} instance.
   * <p>For the semantics of each component (i.e. set vs append) check the
   * builder methods on this class. For example {@link #host(String)} sets
   * while {@link #path(String)} appends.
   *
   * @param uriComponents the UriComponents to copy from
   * @return this UriComponentsBuilder
   */
  public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
    Assert.notNull(uriComponents, "UriComponents must not be null");
    uriComponents.copyToUriComponentsBuilder(this);
    return this;
  }

  @Override
  public UriComponentsBuilder scheme(@Nullable String scheme) {
    this.scheme = scheme;
    return this;
  }

  /**
   * Set the URI scheme-specific-part. When invoked, this method overwrites
   * {@linkplain #userInfo(String) user-info}, {@linkplain #host(String) host},
   * {@linkplain #port(int) port}, {@linkplain #path(String) path}, and
   * {@link #query(String) query}.
   *
   * @param ssp the URI scheme-specific-part, may contain URI template parameters
   * @return this UriComponentsBuilder
   */
  public UriComponentsBuilder schemeSpecificPart(String ssp) {
    this.ssp = ssp;
    resetHierarchicalComponents();
    return this;
  }

  @Override
  public UriComponentsBuilder userInfo(@Nullable String userInfo) {
    this.userInfo = userInfo;
    resetSchemeSpecificPart();
    return this;
  }

  @Override
  public UriComponentsBuilder host(@Nullable String host) {
    this.host = host;
    if (host != null) {
      resetSchemeSpecificPart();
    }
    return this;
  }

  @Override
  public UriComponentsBuilder port(int port) {
    Assert.isTrue(port >= -1, "Port must be >= -1");
    this.port = String.valueOf(port);
    if (port > -1) {
      resetSchemeSpecificPart();
    }
    return this;
  }

  @Override
  public UriComponentsBuilder port(@Nullable String port) {
    this.port = port;
    if (port != null) {
      resetSchemeSpecificPart();
    }
    return this;
  }

  @Override
  public UriComponentsBuilder path(String path) {
    this.pathBuilder.addPath(path);
    resetSchemeSpecificPart();
    return this;
  }

  @Override
  public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
    this.pathBuilder.addPathSegments(pathSegments);
    resetSchemeSpecificPart();
    return this;
  }

  @Override
  public UriComponentsBuilder replacePath(@Nullable String path) {
    this.pathBuilder = new CompositePathComponentBuilder();
    if (path != null) {
      this.pathBuilder.addPath(path);
    }
    resetSchemeSpecificPart();
    return this;
  }

  @Override
  public UriComponentsBuilder query(@Nullable String query) {
    if (query != null) {
      Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
      while (matcher.find()) {
        String name = matcher.group(1);
        String eq = matcher.group(2);
        String value = matcher.group(3);
        queryParam(name, (value != null ? value : (StringUtils.isNotEmpty(eq) ? "" : null)));
      }
      resetSchemeSpecificPart();
    }
    else {
      this.queryParams.clear();
    }
    return this;
  }

  @Override
  public UriComponentsBuilder replaceQuery(@Nullable String query) {
    this.queryParams.clear();
    if (query != null) {
      query(query);
      resetSchemeSpecificPart();
    }
    return this;
  }

  @Override
  public UriComponentsBuilder queryParam(String name, Object... values) {
    Assert.notNull(name, "Name must not be null");
    if (ObjectUtils.isNotEmpty(values)) {
      for (Object value : values) {
        String valueAsString = getQueryParamValue(value);
        this.queryParams.add(name, valueAsString);
      }
    }
    else {
      this.queryParams.add(name, null);
    }
    resetSchemeSpecificPart();
    return this;
  }

  @Nullable
  private String getQueryParamValue(@Nullable Object value) {
    if (value != null) {
      return (value instanceof Optional ?
              ((Optional<?>) value).map(Object::toString).orElse(null) :
              value.toString());
    }
    return null;
  }

  @Override
  public UriComponentsBuilder queryParam(String name, @Nullable Collection<?> values) {
    return queryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
  }

  @Override
  public UriComponentsBuilder queryParamIfPresent(String name, Optional<?> value) {
    value.ifPresent(o -> {
      if (o instanceof Collection) {
        queryParam(name, (Collection<?>) o);
      }
      else {
        queryParam(name, o);
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UriComponentsBuilder queryParams(@Nullable MultiValueMap<String, String> params) {
    if (params != null) {
      this.queryParams.addAll(params);
      resetSchemeSpecificPart();
    }
    return this;
  }

  @Override
  public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
    Assert.notNull(name, "Name must not be null");
    this.queryParams.remove(name);
    if (ObjectUtils.isNotEmpty(values)) {
      queryParam(name, values);
    }
    resetSchemeSpecificPart();
    return this;
  }

  @Override
  public UriComponentsBuilder replaceQueryParam(String name, @Nullable Collection<?> values) {
    return replaceQueryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UriComponentsBuilder replaceQueryParams(@Nullable MultiValueMap<String, String> params) {
    this.queryParams.clear();
    if (params != null) {
      this.queryParams.putAll(params);
    }
    return this;
  }

  @Override
  public UriComponentsBuilder fragment(@Nullable String fragment) {
    if (fragment != null) {
      Assert.hasLength(fragment, "Fragment must not be empty");
      this.fragment = fragment;
    }
    else {
      this.fragment = null;
    }
    return this;
  }

  /**
   * Configure URI variables to be expanded at build time.
   * <p>The provided variables may be a subset of all required ones. At build
   * time, the available ones are expanded, while unresolved URI placeholders
   * are left in place and can still be expanded later.
   * <p>In contrast to {@link UriComponents#expand(Map)} or
   * {@link #buildAndExpand(Map)}, this method is useful when you need to
   * supply URI variables without building the {@link UriComponents} instance
   * just yet, or perhaps pre-expand some shared default values such as host
   * and port.
   *
   * @param uriVariables the URI variables to use
   * @return this UriComponentsBuilder
   */
  public UriComponentsBuilder uriVariables(Map<String, Object> uriVariables) {
    this.uriVariables.putAll(uriVariables);
    return this;
  }

  /**
   * Adapt this builder's scheme+host+port from the given headers, specifically
   * "Forwarded" (<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>,
   * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
   * "Forwarded" is not found.
   * <p><strong>Note:</strong> this method uses values from forwarded headers,
   * if present, in order to reflect the client-originated protocol and address.
   * Consider using the {@code ForwardedHeaderFilter} in order to choose from a
   * central place whether to extract and use, or to discard such headers.
   * See the Spring Framework reference for more on this filter.
   *
   * @param headers the HTTP headers to consider
   * @return this UriComponentsBuilder
   */
  UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
    try {
      String forwardedHeader = headers.getFirst("Forwarded");
      if (StringUtils.hasText(forwardedHeader)) {
        Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedHeader);
        if (matcher.find()) {
          scheme(matcher.group(1).trim());
          port(null);
        }
        else if (isForwardedSslOn(headers)) {
          scheme("https");
          port(null);
        }
        matcher = FORWARDED_HOST_PATTERN.matcher(forwardedHeader);
        if (matcher.find()) {
          adaptForwardedHost(matcher.group(1).trim());
        }
      }
      else {
        String protocolHeader = headers.getFirst("X-Forwarded-Proto");
        if (StringUtils.hasText(protocolHeader)) {
          scheme(StringUtils.tokenizeToStringArray(protocolHeader, ",")[0]);
          port(null);
        }
        else if (isForwardedSslOn(headers)) {
          scheme("https");
          port(null);
        }
        String hostHeader = headers.getFirst("X-Forwarded-Host");
        if (StringUtils.hasText(hostHeader)) {
          adaptForwardedHost(StringUtils.tokenizeToStringArray(hostHeader, ",")[0]);
        }
        String portHeader = headers.getFirst("X-Forwarded-Port");
        if (StringUtils.hasText(portHeader)) {
          port(Integer.parseInt(StringUtils.tokenizeToStringArray(portHeader, ",")[0]));
        }
      }
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
              "Failed to parse a port from \"forwarded\"-type headers. " +
                      "If not behind a trusted proxy, consider using ForwardedHeaderFilter " +
                      "with the removeOnly=true. Request headers: " + headers);
    }

    if (this.scheme != null &&
            (((this.scheme.equals("http") || this.scheme.equals("ws")) && "80".equals(this.port)) ||
                    ((this.scheme.equals("https") || this.scheme.equals("wss")) && "443".equals(this.port)))) {
      port(null);
    }

    return this;
  }

  private boolean isForwardedSslOn(HttpHeaders headers) {
    String forwardedSsl = headers.getFirst("X-Forwarded-Ssl");
    return StringUtils.hasText(forwardedSsl) && forwardedSsl.equalsIgnoreCase("on");
  }

  private void adaptForwardedHost(String rawValue) {
    int portSeparatorIdx = rawValue.lastIndexOf(':');
    int squareBracketIdx = rawValue.lastIndexOf(']');
    if (portSeparatorIdx > squareBracketIdx) {
      if (squareBracketIdx == -1 && rawValue.indexOf(':') != portSeparatorIdx) {
        throw new IllegalArgumentException("Invalid IPv4 address: " + rawValue);
      }
      host(rawValue.substring(0, portSeparatorIdx));
      port(Integer.parseInt(rawValue.substring(portSeparatorIdx + 1)));
    }
    else {
      host(rawValue);
      port(null);
    }
  }

  private void resetHierarchicalComponents() {
    this.userInfo = null;
    this.host = null;
    this.port = null;
    this.pathBuilder = new CompositePathComponentBuilder();
    this.queryParams.clear();
  }

  private void resetSchemeSpecificPart() {
    this.ssp = null;
  }

  /**
   * Public declaration of Object's {@code clone()} method.
   * Delegates to {@link #cloneBuilder()}.
   */
  @Override
  public Object clone() {
    return cloneBuilder();
  }

  /**
   * Clone this {@code UriComponentsBuilder}.
   *
   * @return the cloned {@code UriComponentsBuilder} object
   */
  public UriComponentsBuilder cloneBuilder() {
    return new UriComponentsBuilder(this);
  }

  private interface PathComponentBuilder {

    @Nullable
    PathComponent build();

    PathComponentBuilder cloneBuilder();
  }

  private static class CompositePathComponentBuilder implements PathComponentBuilder {
    private final ArrayDeque<PathComponentBuilder> builders = new ArrayDeque<>();

    public void addPathSegments(String... pathSegments) {
      if (ObjectUtils.isNotEmpty(pathSegments)) {
        PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
        FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
        if (psBuilder == null) {
          psBuilder = new PathSegmentComponentBuilder();
          this.builders.add(psBuilder);
          if (fpBuilder != null) {
            fpBuilder.removeTrailingSlash();
          }
        }
        psBuilder.append(pathSegments);
      }
    }

    public void addPath(String path) {
      if (StringUtils.hasText(path)) {
        PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
        FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
        if (psBuilder != null) {
          path = (path.startsWith("/") ? path : "/" + path);
        }
        if (fpBuilder == null) {
          fpBuilder = new FullPathComponentBuilder();
          this.builders.add(fpBuilder);
        }
        fpBuilder.append(path);
      }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T getLastBuilder(Class<T> builderClass) {
      if (!this.builders.isEmpty()) {
        PathComponentBuilder last = this.builders.getLast();
        if (builderClass.isInstance(last)) {
          return (T) last;
        }
      }
      return null;
    }

    @Override
    public PathComponent build() {
      int size = this.builders.size();
      List<PathComponent> components = new ArrayList<>(size);
      for (PathComponentBuilder componentBuilder : this.builders) {
        PathComponent pathComponent = componentBuilder.build();
        if (pathComponent != null) {
          components.add(pathComponent);
        }
      }
      if (components.isEmpty()) {
        return HierarchicalUriComponents.NULL_PATH_COMPONENT;
      }
      if (components.size() == 1) {
        return components.get(0);
      }
      return new HierarchicalUriComponents.PathComponentComposite(components);
    }

    @Override
    public CompositePathComponentBuilder cloneBuilder() {
      CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
      for (PathComponentBuilder builder : this.builders) {
        compositeBuilder.builders.add(builder.cloneBuilder());
      }
      return compositeBuilder;
    }
  }

  private static class FullPathComponentBuilder implements PathComponentBuilder {

    private final StringBuilder path = new StringBuilder();

    public void append(String path) {
      this.path.append(path);
    }

    @Override
    public PathComponent build() {
      if (this.path.length() == 0) {
        return null;
      }
      String sanitized = getSanitizedPath(this.path);
      return new HierarchicalUriComponents.FullPathComponent(sanitized);
    }

    private static String getSanitizedPath(final StringBuilder path) {
      int index = path.indexOf("//");
      if (index >= 0) {
        StringBuilder sanitized = new StringBuilder(path);
        while (index != -1) {
          sanitized.deleteCharAt(index);
          index = sanitized.indexOf("//", index);
        }
        return sanitized.toString();
      }
      return path.toString();
    }

    public void removeTrailingSlash() {
      int index = this.path.length() - 1;
      if (this.path.charAt(index) == '/') {
        this.path.deleteCharAt(index);
      }
    }

    @Override
    public FullPathComponentBuilder cloneBuilder() {
      FullPathComponentBuilder builder = new FullPathComponentBuilder();
      builder.append(this.path.toString());
      return builder;
    }
  }

  private static class PathSegmentComponentBuilder implements PathComponentBuilder {

    private final List<String> pathSegments = new ArrayList<>();

    public void append(String... pathSegments) {
      for (String pathSegment : pathSegments) {
        if (StringUtils.hasText(pathSegment)) {
          this.pathSegments.add(pathSegment);
        }
      }
    }

    @Override
    public PathComponent build() {
      return (this.pathSegments.isEmpty() ? null :
              new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
    }

    @Override
    public PathSegmentComponentBuilder cloneBuilder() {
      PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
      builder.pathSegments.addAll(this.pathSegments);
      return builder;
    }
  }

  private enum EncodingHint {
    ENCODE_TEMPLATE, FULLY_ENCODED, NONE
  }

}
