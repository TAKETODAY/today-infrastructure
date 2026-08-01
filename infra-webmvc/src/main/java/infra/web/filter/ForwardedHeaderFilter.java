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

package infra.web.filter;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.LinkedCaseInsensitiveMap;
import infra.util.StringUtils;
import infra.web.DecoratingHttpContext;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;
import infra.web.util.ForwardedHeaderUtils;
import infra.web.util.UriComponents;
import infra.web.util.UriComponentsBuilder;

/**
 * Extract values from the standard "Forwarded" header or the "X-Forwarded-*"
 * alternative header, wrap the request and response, and make them reflect
 * the originating client's perspective.
 *
 * <p>An application cannot know if forwarded headers were added by a
 * trusted proxy or by a malicious client. It is imperative that a proxy at the
 * edge of trust is configured to drop forwarded headers from the outside,
 * including both the standard "Forwarded" header and the "X-Forwarded-*"
 * alternative headers.
 *
 * <p>Proxies are typically configured to support either the standard "Forwarded"
 * header or the "X-Forwarded-*" header. Accordingly, an application must indicate
 * which of the two alternatives it expects through a constructor argument.
 *
 * <p>Support for "X-Forwarded-Prefix" is enabled separately via
 * {@link #setUseForwardedPrefix}.
 *
 * <p>You can configure this filter in {@link #setRemoveOnly removeOnly} mode,
 * in which case it hides the headers without using them.
 *
 * @author Rossen Stoyanchev
 * @author Eddú Meléndez
 * @author Rob Winch
 * @author Brian Clozel
 * @author Mengqi Xu
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 5.0
 */
public class ForwardedHeaderFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(ForwardedHeaderFilter.class);

  private static final Set<String> FORWARDED_HEADER_NAMES =
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

  private boolean relativeRedirects;

  /**
   * A default constructor with the historic behavior so far, which is to check
   * both the standard "Forwarded" header and the "X-Forwarded-*" alternative
   * headers in that order, also with "X-Forwarded-Prefix" enabled by default.
   * <p>This behavior depends on proxies being configured correctly
   * to clear both standard "Forwarded" and "X-Forwarded-*" header values coming
   * from the outside. Going forward, applications must explicitly declare which
   * forwarded headers are expected.
   */
  public ForwardedHeaderFilter() {
    this.useStandardHeader = null;
    this.useForwardedPrefix = true;
  }

  /**
   * Create an instance of the filter and specify whether it should use the
   * standard "Forwarded" header or the "X-Forwarded-*" alternative headers.
   * <p>"X-Forwarded-Prefix" is enabled separately via {@link #setUseForwardedPrefix}.
   *
   * @param useStandardHeader whether to use the standard "Forwarded" header
   * (true), or the "X-Forwarded-*" alternative headers (false).
   */
  public ForwardedHeaderFilter(boolean useStandardHeader) {
    this.useStandardHeader = useStandardHeader;
  }

  /**
   * Enable use of "X-Forwarded-Prefix" to determine the context path.
   * <p>By default, this is set to "false" in which case the header is ignored.
   */
  public void setUseForwardedPrefix(boolean useForwardedPrefix) {
    this.useForwardedPrefix = useForwardedPrefix;
  }

  /**
   * Enables mode in which any "Forwarded" or "X-Forwarded-*" headers are
   * removed only and the information in them ignored.
   *
   * @param removeOnly whether to discard and ignore forwarded headers
   */
  public void setRemoveOnly(boolean removeOnly) {
    this.removeOnly = removeOnly;
  }

  /**
   * Use this property to enable relative redirects as explained in
   * {@link RelativeRedirectFilter}, and also using the same response wrapper
   * as that filter does, or if both are configured, only one will wrap.
   * <p>By default, if this property is set to false, in which case calls to
   * {@link HttpContext#sendRedirect(String)} are overridden in order
   * to turn relative into absolute URLs, also taking into account forwarded
   * headers.
   *
   * @param relativeRedirects whether to use relative redirects
   */
  public void setRelativeRedirects(boolean relativeRedirects) {
    this.relativeRedirects = relativeRedirects;
  }

  protected boolean shouldNotFilter(HttpContext request) {
    for (String headerName : FORWARDED_HEADER_NAMES) {
      if (request.containsHeader(headerName)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void doFilter(HttpContext context, FilterChain filterChain) throws Exception {
    if (shouldNotFilter(context)) {
      filterChain.doFilter(context);
    }
    else {
      if (this.removeOnly) {
        ForwardedHeaderRemovingRequest wrappedRequest = new ForwardedHeaderRemovingRequest(context);
        filterChain.doFilter(wrappedRequest);
      }
      else {
        HttpContext wrappedRequest;
        try {
          wrappedRequest = new ForwardedHeaderExtractingContext(context, this.useStandardHeader, this.useForwardedPrefix);
          if (relativeRedirects) {
            wrappedRequest = RelativeRedirectResponseWrapper.wrapIfNecessary(wrappedRequest, HttpStatus.SEE_OTHER);
          }
        }
        catch (Throwable ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Failed to apply forwarded headers to {}", formatRequest(context), ex);
          }
          context.sendError(HttpStatus.BAD_REQUEST);
          return;
        }
        filterChain.doFilter(wrappedRequest);
      }
    }
  }

  /**
   * Format the request for logging purposes including HTTP method and URL.
   *
   * @param request the request to format
   * @return the String to display, never empty or {@code null}
   */
  protected String formatRequest(HttpContext request) {
    return "HTTP " + request.getMethod() + " \"" + request.getRequestURI() + "\"";
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

  /**
   * Hide "Forwarded" or "X-Forwarded-*" headers.
   */
  private static class ForwardedHeaderRemovingRequest extends DecoratingHttpContext {

    private final Set<String> headerNames;

    public ForwardedHeaderRemovingRequest(HttpContext delegate) {
      super(delegate);
      this.headerNames = headerNames(delegate);
    }

    private static Set<String> headerNames(HttpContext request) {
      Set<String> headerNames = Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(Locale.ROOT));
      for (String name : request.getHeaderNames()) {
        if (!FORWARDED_HEADER_NAMES.contains(name)) {
          headerNames.add(name);
        }
      }
      return Collections.unmodifiableSet(headerNames);
    }

    // Override header accessors to not expose forwarded headers

    @Override
    public @Nullable String getHeader(String name) {
      if (FORWARDED_HEADER_NAMES.contains(name)) {
        return null;
      }
      return super.getHeader(name);
    }

    @Override
    public List<String> getHeaders(String name) {
      if (FORWARDED_HEADER_NAMES.contains(name)) {
        return Collections.emptyList();
      }
      return super.getHeaders(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
      return this.headerNames;
    }
  }

  /**
   * Extract and use "Forwarded" or "X-Forwarded-*" headers.
   */
  private static class ForwardedHeaderExtractingContext extends ForwardedHeaderRemovingRequest {

    private static final String FOLDER_SEPARATOR = "/";

    private final @Nullable String scheme;

    private final boolean secure;

    private final @Nullable String host;

    private final int port;

    private final @Nullable InetSocketAddress remoteAddress;

    private final @Nullable InetSocketAddress localAddress;

    private final ForwardedPrefixExtractor forwardedPrefixExtractor;

    private final @Nullable Boolean useStandardHeader;

    ForwardedHeaderExtractingContext(HttpContext context, @Nullable Boolean useStandardHeader, boolean useForwardedPrefix) {
      super(context);
      this.useStandardHeader = useStandardHeader;
      var info = getForwardedInfo(useStandardHeader, context.getURI(), context.getHeaders(),
              context.remoteAddress(), (InetSocketAddress) context.localAddress());

      UriComponents uriComponents = info.uriComponentsBuilder().build();
      int port = uriComponents.getPort();

      this.scheme = uriComponents.getScheme();
      this.secure = "https".equals(this.scheme) || "wss".equals(this.scheme);
      this.host = uriComponents.getHost();
      this.port = (port == -1 ? (this.secure ? 443 : 80) : port);

      this.remoteAddress = info.forAddress();
      this.localAddress = info.byAddress();

      // Use Supplier as Tomcat updates delegate request on FORWARD
      Supplier<HttpContext> requestSupplier = this::delegate;

      this.forwardedPrefixExtractor = new ForwardedPrefixExtractor(
              requestSupplier, (this.scheme + "://" + this.host + (port == -1 ? "" : ":" + port)),
              useForwardedPrefix);
    }

    @Override
    public String getScheme() {
      return this.scheme != null ? scheme : super.getScheme();
    }

    @Override
    public String getServerName() {
      return this.host != null ? host : super.getServerName();
    }

    @Override
    public int getServerPort() {
      return this.port;
    }

    @Override
    public boolean isSecure() {
      return this.secure;
    }

    @Override
    public String getRequestURI() {
      return this.forwardedPrefixExtractor.getRequestUri();
    }

    @Override
    public String getRequestURL() {
      return this.forwardedPrefixExtractor.getRequestUrl();
    }

    @Override
    public String getRemoteAddress() {
      return (this.remoteAddress != null ? this.remoteAddress.getHostString() : super.getRemoteAddress());
    }

    @Override
    public int getRemotePort() {
      return (this.remoteAddress != null ? this.remoteAddress.getPort() : super.getRemotePort());
    }

    @Override
    public SocketAddress localAddress() {
      return this.localAddress != null ? this.localAddress : super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
      return remoteAddress != null ? remoteAddress : super.remoteAddress();
    }

    @Override
    public void sendRedirect(String location) throws IOException {
      UriComponentsBuilder builder = UriComponentsBuilder.forURIString(location);
      UriComponents uriComponents = builder.build();

      // Absolute location
      if (uriComponents.getScheme() != null) {
        super.sendRedirect(location);
        return;
      }

      // Network-path reference
      if (location.startsWith("//")) {
        String scheme = getScheme();
        super.sendRedirect(builder.scheme(scheme).toUriString());
        return;
      }

      String path = uriComponents.getPath();
      if (path != null) {
        // Relative to Servlet container root or to current request
        path = (path.startsWith(FOLDER_SEPARATOR) ? path :
                StringUtils.applyRelativePath(getRequestURI(), path));
      }

      URI uri = getURI();
      HttpHeaders headers = getHeaders();

      var info = getForwardedInfo(useStandardHeader, uri, headers, null, null);

      String result = info.uriComponentsBuilder()
              .replacePath(path)
              .replaceQuery(uriComponents.getQuery())
              .fragment(uriComponents.getFragment())
              .build().normalize().toUriString();

      super.sendRedirect(result);
    }

  }

  /**
   * Responsible for the contextPath, requestURI, and requestURL with forwarded
   * headers in mind, and also taking into account changes to the path of the
   * underlying delegate request (for example, on a Servlet FORWARD).
   */
  private static class ForwardedPrefixExtractor {

    private final Supplier<HttpContext> delegate;

    private final String baseUrl;

    private String actualRequestUri;

    private final @Nullable String forwardedPrefix;

    private @Nullable String requestUri;

    private String requestUrl;

    /**
     * Constructor with required information.
     *
     * @param delegate supplier for the current
     * {@link DecoratingHttpContext#delegate() delegate request} which
     * may change during a forward (for example, Tomcat.
     * @param baseUrl the host, scheme, and port based on forwarded headers
     * @param useForwardedPrefix whether to use "X-Forwarded-Prefix"
     */
    public ForwardedPrefixExtractor(Supplier<HttpContext> delegate, String baseUrl, boolean useForwardedPrefix) {
      this.delegate = delegate;
      this.baseUrl = baseUrl;
      this.actualRequestUri = delegate.get().getRequestURI();

      // Keep call order
      this.forwardedPrefix = (useForwardedPrefix ? initForwardedPrefix(delegate.get()) : null);
      this.requestUri = initRequestUri();
      this.requestUrl = initRequestUrl();
    }

    private static @Nullable String initForwardedPrefix(HttpContext request) {
      String result = null;
      Collection<String> names = request.getHeaderNames();
      for (String name : names) {
        if ("X-Forwarded-Prefix".equalsIgnoreCase(name)) {
          result = request.getHeader(name);
        }
      }
      if (result != null) {
        StringBuilder prefix = new StringBuilder(result.length());
        String[] rawPrefixes = StringUtils.tokenizeToStringArray(result, ",");
        for (String rawPrefix : rawPrefixes) {
          int endIndex = rawPrefix.length();
          while (endIndex > 0 && rawPrefix.charAt(endIndex - 1) == '/') {
            endIndex--;
          }
          prefix.append((endIndex != rawPrefix.length() ? rawPrefix.substring(0, endIndex) : rawPrefix));
        }
        return prefix.toString();
      }
      return null;
    }

    private @Nullable String initRequestUri() {
      if (this.forwardedPrefix != null) {
        return this.forwardedPrefix + this.delegate.get().getRequestURI();
      }
      return null;
    }

    private String initRequestUrl() {
      return (this.baseUrl + (this.requestUri != null ? this.requestUri : this.delegate.get().getRequestURI()));
    }

    public String getRequestUri() {
      if (this.requestUri == null) {
        return this.delegate.get().getRequestURI();
      }
      recalculatePathsIfNecessary();
      return this.requestUri;
    }

    public String getRequestUrl() {
      recalculatePathsIfNecessary();
      return requestUrl;
    }

    private void recalculatePathsIfNecessary() {
      // Path of delegate request changed, for example, FORWARD on Tomcat
      if (!this.actualRequestUri.equals(this.delegate.get().getRequestURI())) {
        this.actualRequestUri = this.delegate.get().getRequestURI();
        // Keep call order
        this.requestUri = initRequestUri();
        this.requestUrl = initRequestUrl();
      }
    }

  }

}
