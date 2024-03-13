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

package cn.taketoday.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.support.ApplicationObjectSupport;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.session.WebSessionRequiredException;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.servlet.ServletUtils;

/**
 * Convenient superclass for any kind of web content generator
 *
 * <p>Supports HTTP cache control options. The usage of corresponding HTTP
 * headers can be controlled via the {@link #setCacheSeconds "cacheSeconds"}
 * and {@link #setCacheControl "cacheControl"} properties.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setCacheSeconds
 * @see #setCacheControl
 * @see #setRequireSession
 * @since 4.0 2022/1/29 10:25
 */
public abstract class WebContentGenerator extends ApplicationObjectSupport {

  /** HTTP method "GET". */
  public static final String METHOD_GET = "GET";

  /** HTTP method "HEAD". */
  public static final String METHOD_HEAD = "HEAD";

  /** HTTP method "POST". */
  public static final String METHOD_POST = "POST";

  /** Set of supported HTTP methods. */
  @Nullable
  private Set<String> supportedMethods;

  @Nullable
  private String allowHeader;

  private boolean requireSession = false;

  @Nullable
  private CacheControl cacheControl;

  private int cacheSeconds = -1;

  @Nullable
  private String[] varyByRequestHeaders;

  /**
   * Create a new WebContentGenerator which supports
   * HTTP methods GET, HEAD and POST by default.
   */
  public WebContentGenerator() {
    this(true);
  }

  /**
   * Create a new WebContentGenerator.
   *
   * @param restrictDefaultSupportedMethods {@code true} if this
   * generator should support HTTP methods GET, HEAD and POST by default,
   * or {@code false} if it should be unrestricted
   */
  public WebContentGenerator(boolean restrictDefaultSupportedMethods) {
    if (restrictDefaultSupportedMethods) {
      this.supportedMethods = new LinkedHashSet<>(4);
      this.supportedMethods.add(METHOD_GET);
      this.supportedMethods.add(METHOD_HEAD);
      this.supportedMethods.add(METHOD_POST);
    }
    initAllowHeader();
  }

  /**
   * Create a new WebContentGenerator.
   *
   * @param supportedMethods the supported HTTP methods for this content generator
   */
  public WebContentGenerator(String... supportedMethods) {
    setSupportedMethods(supportedMethods);
  }

  /**
   * Set the HTTP methods that this content generator should support.
   * <p>Default is GET, HEAD and POST for simple form controller types;
   * unrestricted for general controllers and interceptors.
   */
  public final void setSupportedMethods(@Nullable String... methods) {
    if (ObjectUtils.isNotEmpty(methods)) {
      this.supportedMethods = new LinkedHashSet<>(Arrays.asList(methods));
    }
    else {
      this.supportedMethods = null;
    }
    initAllowHeader();
  }

  /**
   * Return the HTTP methods that this content generator supports.
   */
  @Nullable
  public final String[] getSupportedMethods() {
    return supportedMethods != null ? StringUtils.toStringArray(supportedMethods) : null;
  }

  private void initAllowHeader() {
    Collection<String> allowedMethods;
    if (this.supportedMethods == null) {
      allowedMethods = new ArrayList<>(HttpMethod.values().length - 1);
      for (HttpMethod method : HttpMethod.values()) {
        if (method != HttpMethod.TRACE) {
          allowedMethods.add(method.name());
        }
      }
    }
    else if (this.supportedMethods.contains(HttpMethod.OPTIONS.name())) {
      allowedMethods = this.supportedMethods;
    }
    else {
      allowedMethods = new ArrayList<>(this.supportedMethods);
      allowedMethods.add(HttpMethod.OPTIONS.name());

    }
    this.allowHeader = StringUtils.collectionToCommaDelimitedString(allowedMethods);
  }

  /**
   * Return the "Allow" header value to use in response to an HTTP OPTIONS request
   * based on the configured {@link #setSupportedMethods supported methods} also
   * automatically adding "OPTIONS" to the list even if not present as a supported
   * method. This means subclasses don't have to explicitly list "OPTIONS" as a
   * supported method as long as HTTP OPTIONS requests are handled before making a
   * call to {@link #checkRequest(RequestContext)}.
   */
  @Nullable
  protected String getAllowHeader() {
    return this.allowHeader;
  }

  /**
   * Set whether a session should be required to handle requests.
   */
  public final void setRequireSession(boolean requireSession) {
    this.requireSession = requireSession;
  }

  /**
   * Return whether a session is required to handle requests.
   */
  public final boolean isRequireSession() {
    return this.requireSession;
  }

  /**
   * Set the {@link CacheControl} instance to build
   * the Cache-Control HTTP response header.
   */
  public final void setCacheControl(@Nullable CacheControl cacheControl) {
    this.cacheControl = cacheControl;
  }

  /**
   * Get the {@link CacheControl} instance
   * that builds the Cache-Control HTTP response header.
   */
  @Nullable
  public final CacheControl getCacheControl() {
    return this.cacheControl;
  }

  /**
   * Cache content for the given number of seconds, by writing
   * cache-related HTTP headers to the response:
   * <ul>
   * <li>seconds == -1 (default value): no generation cache-related headers</li>
   * <li>seconds == 0: "Cache-Control: no-store" will prevent caching</li>
   * <li>seconds &gt; 0: "Cache-Control: max-age=seconds" will ask to cache content</li>
   * </ul>
   * <p>For more specific needs, a custom {@link CacheControl}
   * should be used.
   *
   * @see #setCacheControl
   */
  public final void setCacheSeconds(int seconds) {
    this.cacheSeconds = seconds;
  }

  /**
   * Return the number of seconds that content is cached.
   */
  public final int getCacheSeconds() {
    return this.cacheSeconds;
  }

  /**
   * Configure one or more request header names (e.g. "Accept-Language") to
   * add to the "Vary" response header to inform clients that the response is
   * subject to content negotiation and variances based on the value of the
   * given request headers. The configured request header names are added only
   * if not already present in the response "Vary" header.
   *
   * @param varyByRequestHeaders one or more request header names
   */
  public final void setVaryByRequestHeaders(@Nullable String... varyByRequestHeaders) {
    this.varyByRequestHeaders = varyByRequestHeaders;
  }

  /**
   * Return the configured request header names for the "Vary" response header.
   */
  @Nullable
  public final String[] getVaryByRequestHeaders() {
    return this.varyByRequestHeaders;
  }

  /**
   * Check the given request for supported methods and a required session, if any.
   *
   * @param request current HTTP request
   */
  protected final void checkRequest(RequestContext request) {
    // Check whether we should support the request method.
    String method = request.getMethodValue();
    if (this.supportedMethods != null && !this.supportedMethods.contains(method)) {
      throw new HttpRequestMethodNotSupportedException(method, this.supportedMethods);
    }

    // Check whether a session is required.
    if (this.requireSession && RequestContextUtils.getSession(request, false) == null) {
      throw new WebSessionRequiredException("Pre-existing session required but none found");
    }
  }

  /**
   * Prepare the given response according to the settings of this generator.
   * Applies the number of cache seconds specified for this generator.
   *
   * @param response current HTTP response
   */
  protected final void prepareResponse(RequestContext response) {
    if (this.cacheControl != null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Applying default {}", getCacheControl());
      }
      applyCacheControl(response, this.cacheControl);
    }
    else {
      if (logger.isTraceEnabled()) {
        logger.trace("Applying default cacheSeconds={}", this.cacheSeconds);
      }
      applyCacheSeconds(response, this.cacheSeconds);
    }
    if (this.varyByRequestHeaders != null) {
      response.responseHeaders().setVary(getVaryRequestHeadersToAdd(response, this.varyByRequestHeaders));
    }
  }

  /**
   * Set the HTTP Cache-Control header according to the given settings.
   *
   * @param response current HTTP response
   * @param cacheControl the pre-configured cache control settings
   */
  protected final void applyCacheControl(RequestContext response, CacheControl cacheControl) {
    String ccValue = cacheControl.getHeaderValue();
    if (ccValue != null) {
      // Set computed HTTP 1.1 Cache-Control header
      if (ServletDetector.runningInServlet(response)) {
        ServletUtils.getServletResponse(response)
                .setHeader(HttpHeaders.CACHE_CONTROL, ccValue);
      }
      else {
        response.setHeader(HttpHeaders.CACHE_CONTROL, ccValue);
      }
    }
  }

  /**
   * Apply the given cache seconds and generate corresponding HTTP headers,
   * i.e. allow caching for the given number of seconds in case of a positive
   * value, prevent caching if given a 0 value, do nothing else.
   * Does not tell the browser to revalidate the resource.
   *
   * @param response current HTTP response
   * @param cacheSeconds positive number of seconds into the future that the
   * response should be cacheable for, 0 to prevent caching
   */
  protected final void applyCacheSeconds(RequestContext response, int cacheSeconds) {
    CacheControl cControl;
    if (cacheSeconds > 0) {
      cControl = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS);
    }
    else if (cacheSeconds == 0) {
      cControl = CacheControl.noStore();
    }
    else {
      cControl = CacheControl.empty();
    }
    applyCacheControl(response, cControl);
  }

  private Collection<String> getVaryRequestHeadersToAdd(RequestContext response, String[] varyByRequestHeaders) {
    if (!response.containsResponseHeader(HttpHeaders.VARY)) {
      return Arrays.asList(varyByRequestHeaders);
    }
    ArrayList<String> result = new ArrayList<>(varyByRequestHeaders.length);
    Collections.addAll(result, varyByRequestHeaders);

    for (String existing : response.responseHeaders().getVary()) {
      if ("*".equals(existing)) {
        return Collections.emptyList();
      }
      for (String value : varyByRequestHeaders) {
        if (value.equalsIgnoreCase(existing)) {
          result.remove(value);
        }
      }
    }

    return result;
  }

}
