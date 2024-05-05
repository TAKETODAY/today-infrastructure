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

package cn.taketoday.test.web.mock.request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.beans.Mergeable;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpInputMessage;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.FormHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpSession;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.http.Cookie;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpSession;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.UriUtils;

/**
 * Default builder for {@link HttpMockRequestImpl} required as input to
 * perform requests in {@link MockMvc}.
 *
 * <p>Application tests will typically access this builder through the static
 * factory methods in {@link MockMvcRequestBuilders}.
 *
 * <p>This class is not open for extension. To apply custom initialization to
 * the created {@code MockHttpServletRequest}, please use the
 * {@link #with(RequestPostProcessor)} extension point.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 4.0
 */
public class MockHttpServletRequestBuilder implements ConfigurableSmartRequestBuilder<MockHttpServletRequestBuilder>, Mergeable {

  private final String method;

  private final URI url;

  private String contextPath = "";

  private String servletPath = "";

  @Nullable
  private String pathInfo = "";

  @Nullable
  private Boolean secure;

  @Nullable
  private Principal principal;

  @Nullable
  private String remoteAddress;

  @Nullable
  private MockHttpSession session;

  @Nullable
  private String characterEncoding;

  @Nullable
  private byte[] content;

  @Nullable
  private String contentType;

  private final MultiValueMap<String, Object> headers = new LinkedMultiValueMap<>();

  private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

  private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

  private final List<Cookie> cookies = new ArrayList<>();

  private final List<Locale> locales = new ArrayList<>();

  private final Map<String, Object> requestAttributes = new LinkedHashMap<>();

  private final Map<String, Object> sessionAttributes = new LinkedHashMap<>();

  private final Map<String, Object> flashAttributes = new LinkedHashMap<>();

  private final List<RequestPostProcessor> postProcessors = new ArrayList<>();

  /**
   * Package private constructor. To get an instance, use static factory
   * methods in {@link MockMvcRequestBuilders}.
   * <p>Although this class cannot be extended, additional ways to initialize
   * the {@code MockHttpServletRequest} can be plugged in via
   * {@link #with(RequestPostProcessor)}.
   *
   * @param httpMethod the HTTP method (GET, POST, etc)
   * @param url a URL template; the resulting URL will be encoded
   * @param vars zero or more URI variables
   */
  MockHttpServletRequestBuilder(HttpMethod httpMethod, String url, Object... vars) {
    this(httpMethod.name(), initUri(url, vars));
  }

  private static URI initUri(String url, Object[] vars) {
    Assert.notNull(url, "'url' is required");
    Assert.isTrue(url.isEmpty() || url.startsWith("/") || url.startsWith("http://") || url.startsWith("https://"),
            () -> "'url' should start with a path or be a complete HTTP URL: " + url);
    String uriString = (url.isEmpty() ? "/" : url);
    return UriComponentsBuilder.fromUriString(uriString).buildAndExpand(vars).encode().toUri();
  }

  /**
   * Alternative to {@link #MockHttpServletRequestBuilder(HttpMethod, String, Object...)}
   * with a pre-built URI.
   *
   * @param httpMethod the HTTP method (GET, POST, etc)
   * @param url the URL
   */
  MockHttpServletRequestBuilder(HttpMethod httpMethod, URI url) {
    this(httpMethod.name(), url);
  }

  /**
   * Alternative constructor for custom HTTP methods.
   *
   * @param httpMethod the HTTP method (GET, POST, etc)
   * @param url the URL
   */
  MockHttpServletRequestBuilder(String httpMethod, URI url) {
    Assert.notNull(httpMethod, "'httpMethod' is required");
    Assert.notNull(url, "'url' is required");
    this.method = httpMethod;
    this.url = url;
  }

  /**
   * Specify the portion of the requestURI that represents the pathInfo.
   * <p>If left unspecified (recommended), the pathInfo will be automatically derived
   * by removing the contextPath and the servletPath from the requestURI and using any
   * remaining part. If specified here, the pathInfo must start with a "/".
   * <p>If specified, the pathInfo will be used as-is.
   *
   * @see HttpMockRequest#getPathInfo()
   */
  public MockHttpServletRequestBuilder pathInfo(@Nullable String pathInfo) {
    if (StringUtils.hasText(pathInfo)) {
      Assert.isTrue(pathInfo.startsWith("/"), "Path info must start with a '/'");
    }
    this.pathInfo = pathInfo;
    return this;
  }

  /**
   * Set the secure property of the {@link MockRequest} indicating use of a
   * secure channel, such as HTTPS.
   *
   * @param secure whether the request is using a secure channel
   */
  public MockHttpServletRequestBuilder secure(boolean secure) {
    this.secure = secure;
    return this;
  }

  /**
   * Set the character encoding of the request.
   *
   * @param encoding the character encoding
   * @see StandardCharsets
   * @see #characterEncoding(String)
   */
  public MockHttpServletRequestBuilder characterEncoding(Charset encoding) {
    return this.characterEncoding(encoding.name());
  }

  /**
   * Set the character encoding of the request.
   *
   * @param encoding the character encoding
   */
  public MockHttpServletRequestBuilder characterEncoding(String encoding) {
    this.characterEncoding = encoding;
    return this;
  }

  /**
   * Set the request body.
   * <p>If content is provided and {@link #contentType(MediaType)} is set to
   * {@code application/x-www-form-urlencoded}, the content will be parsed
   * and used to populate the {@link #param(String, String...) request
   * parameters} map.
   *
   * @param content the body content
   */
  public MockHttpServletRequestBuilder content(byte[] content) {
    this.content = content;
    return this;
  }

  /**
   * Set the request body as a UTF-8 String.
   * <p>If content is provided and {@link #contentType(MediaType)} is set to
   * {@code application/x-www-form-urlencoded}, the content will be parsed
   * and used to populate the {@link #param(String, String...) request
   * parameters} map.
   *
   * @param content the body content
   */
  public MockHttpServletRequestBuilder content(String content) {
    this.content = content.getBytes(StandardCharsets.UTF_8);
    return this;
  }

  /**
   * Set the 'Content-Type' header of the request.
   * <p>If content is provided and {@code contentType} is set to
   * {@code application/x-www-form-urlencoded}, the content will be parsed
   * and used to populate the {@link #param(String, String...) request
   * parameters} map.
   *
   * @param contentType the content type
   */
  public MockHttpServletRequestBuilder contentType(MediaType contentType) {
    Assert.notNull(contentType, "'contentType' is required");
    this.contentType = contentType.toString();
    return this;
  }

  /**
   * Set the 'Content-Type' header of the request as a raw String value,
   * possibly not even well-formed (for testing purposes).
   *
   * @param contentType the content type
   */
  public MockHttpServletRequestBuilder contentType(String contentType) {
    Assert.notNull(contentType, "'contentType' is required");
    this.contentType = contentType;
    return this;
  }

  /**
   * Set the 'Accept' header to the given media type(s).
   *
   * @param mediaTypes one or more media types
   */
  public MockHttpServletRequestBuilder accept(MediaType... mediaTypes) {
    Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
    this.headers.set("Accept", MediaType.toString(Arrays.asList(mediaTypes)));
    return this;
  }

  /**
   * Set the {@code Accept} header using raw String values, possibly not even
   * well-formed (for testing purposes).
   *
   * @param mediaTypes one or more media types; internally joined as
   * comma-separated String
   */
  public MockHttpServletRequestBuilder accept(String... mediaTypes) {
    Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
    this.headers.set("Accept", String.join(", ", mediaTypes));
    return this;
  }

  /**
   * Add a header to the request. Values are always added.
   *
   * @param name the header name
   * @param values one or more header values
   */
  public MockHttpServletRequestBuilder header(String name, Object... values) {
    addToMultiValueMap(this.headers, name, values);
    return this;
  }

  /**
   * Add all headers to the request. Values are always added.
   *
   * @param httpHeaders the headers and values to add
   */
  public MockHttpServletRequestBuilder headers(HttpHeaders httpHeaders) {
    httpHeaders.forEach(this.headers::addAll);
    return this;
  }

  /**
   * Add a request parameter to {@link HttpMockRequestImpl#getParameterMap()}.
   * <p>In the Servlet API, a request parameter may be parsed from the query
   * string and/or from the body of an {@code application/x-www-form-urlencoded}
   * request. This method simply adds to the request parameter map. You may
   * also use add Servlet request parameters by specifying the query or form
   * data through one of the following:
   * <ul>
   * <li>Supply a URL with a query to {@link MockMvcRequestBuilders}.
   * <li>Add query params via {@link #queryParam} or {@link #queryParams}.
   * <li>Provide {@link #content} with {@link #contentType}
   * {@code application/x-www-form-urlencoded}.
   * </ul>
   *
   * @param name the parameter name
   * @param values one or more values
   */
  public MockHttpServletRequestBuilder param(String name, String... values) {
    addToMultiValueMap(this.parameters, name, values);
    return this;
  }

  /**
   * Variant of {@link #param(String, String...)} with a {@link MultiValueMap}.
   *
   * @param params the parameters to add
   * @since 4.0.4
   */
  public MockHttpServletRequestBuilder params(MultiValueMap<String, String> params) {
    params.forEach((name, values) -> {
      for (String value : values) {
        this.parameters.add(name, value);
      }
    });
    return this;
  }

  /**
   * Append to the query string and also add to the
   * {@link #param(String, String...) request parameters} map. The parameter
   * name and value are encoded when they are added to the query string.
   *
   * @param name the parameter name
   * @param values one or more values
   * @since 4.0
   */
  public MockHttpServletRequestBuilder queryParam(String name, String... values) {
    param(name, values);
    this.queryParams.addAll(name, Arrays.asList(values));
    return this;
  }

  /**
   * Append to the query string and also add to the
   * {@link #params(MultiValueMap) request parameters} map. The parameter
   * name and value are encoded when they are added to the query string.
   *
   * @param params the parameters to add
   * @since 4.0
   */
  public MockHttpServletRequestBuilder queryParams(MultiValueMap<String, String> params) {
    params(params);
    this.queryParams.addAll(params);
    return this;
  }

  /**
   * Add the given cookies to the request. Cookies are always added.
   *
   * @param cookies the cookies to add
   */
  public MockHttpServletRequestBuilder cookie(Cookie... cookies) {
    Assert.notEmpty(cookies, "'cookies' must not be empty");
    this.cookies.addAll(Arrays.asList(cookies));
    return this;
  }

  /**
   * Add the specified locales as preferred request locales.
   *
   * @param locales the locales to add
   * @see #locale(Locale)
   */
  public MockHttpServletRequestBuilder locale(Locale... locales) {
    Assert.notEmpty(locales, "'locales' must not be empty");
    this.locales.addAll(Arrays.asList(locales));
    return this;
  }

  /**
   * Set the locale of the request, overriding any previous locales.
   *
   * @param locale the locale, or {@code null} to reset it
   * @see #locale(Locale...)
   */
  public MockHttpServletRequestBuilder locale(@Nullable Locale locale) {
    this.locales.clear();
    if (locale != null) {
      this.locales.add(locale);
    }
    return this;
  }

  /**
   * Set a request attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public MockHttpServletRequestBuilder requestAttr(String name, Object value) {
    addToMap(this.requestAttributes, name, value);
    return this;
  }

  /**
   * Set a session attribute.
   *
   * @param name the session attribute name
   * @param value the session attribute value
   */
  public MockHttpServletRequestBuilder sessionAttr(String name, Object value) {
    addToMap(this.sessionAttributes, name, value);
    return this;
  }

  /**
   * Set session attributes.
   *
   * @param sessionAttributes the session attributes
   */
  public MockHttpServletRequestBuilder sessionAttrs(Map<String, Object> sessionAttributes) {
    Assert.notEmpty(sessionAttributes, "'sessionAttributes' must not be empty");
    sessionAttributes.forEach(this::sessionAttr);
    return this;
  }

  /**
   * Set an "input" flash attribute.
   *
   * @param name the flash attribute name
   * @param value the flash attribute value
   */
  public MockHttpServletRequestBuilder flashAttr(String name, Object value) {
    addToMap(this.flashAttributes, name, value);
    return this;
  }

  /**
   * Set flash attributes.
   *
   * @param flashAttributes the flash attributes
   */
  public MockHttpServletRequestBuilder flashAttrs(Map<String, Object> flashAttributes) {
    Assert.notEmpty(flashAttributes, "'flashAttributes' must not be empty");
    flashAttributes.forEach(this::flashAttr);
    return this;
  }

  /**
   * Set the HTTP session to use, possibly re-used across requests.
   * <p>Individual attributes provided via {@link #sessionAttr(String, Object)}
   * override the content of the session provided here.
   *
   * @param session the HTTP session
   */
  public MockHttpServletRequestBuilder session(MockHttpSession session) {
    Assert.notNull(session, "'session' is required");
    this.session = session;
    return this;
  }

  /**
   * Set the principal of the request.
   *
   * @param principal the principal
   */
  public MockHttpServletRequestBuilder principal(Principal principal) {
    Assert.notNull(principal, "'principal' is required");
    this.principal = principal;
    return this;
  }

  /**
   * Set the remote address of the request.
   *
   * @param remoteAddress the remote address (IP)
   */
  public MockHttpServletRequestBuilder remoteAddress(String remoteAddress) {
    Assert.hasText(remoteAddress, "'remoteAddress' must not be null or blank");
    this.remoteAddress = remoteAddress;
    return this;
  }

  /**
   * An extension point for further initialization of {@link HttpMockRequestImpl}
   * in ways not built directly into the {@code MockHttpServletRequestBuilder}.
   * Implementation of this interface can have builder-style methods themselves
   * and be made accessible through static factory methods.
   *
   * @param postProcessor a post-processor to add
   */
  @Override
  public MockHttpServletRequestBuilder with(RequestPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "postProcessor is required");
    this.postProcessors.add(postProcessor);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @return always returns {@code true}.
   */
  @Override
  public boolean isMergeEnabled() {
    return true;
  }

  /**
   * Merges the properties of the "parent" RequestBuilder accepting values
   * only if not already set in "this" instance.
   *
   * @param parent the parent {@code RequestBuilder} to inherit properties from
   * @return the result of the merge
   */
  @Override
  public Object merge(@Nullable Object parent) {
    if (parent == null) {
      return this;
    }
    if (!(parent instanceof MockHttpServletRequestBuilder parentBuilder)) {
      throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
    }
    if (!StringUtils.hasText(this.contextPath)) {
      this.contextPath = parentBuilder.contextPath;
    }
    if (!StringUtils.hasText(this.servletPath)) {
      this.servletPath = parentBuilder.servletPath;
    }
    if ("".equals(this.pathInfo)) {
      this.pathInfo = parentBuilder.pathInfo;
    }

    if (this.secure == null) {
      this.secure = parentBuilder.secure;
    }
    if (this.principal == null) {
      this.principal = parentBuilder.principal;
    }
    if (this.session == null) {
      this.session = parentBuilder.session;
    }
    if (this.remoteAddress == null) {
      this.remoteAddress = parentBuilder.remoteAddress;
    }

    if (this.characterEncoding == null) {
      this.characterEncoding = parentBuilder.characterEncoding;
    }
    if (this.content == null) {
      this.content = parentBuilder.content;
    }
    if (this.contentType == null) {
      this.contentType = parentBuilder.contentType;
    }

    for (Map.Entry<String, List<Object>> entry : parentBuilder.headers.entrySet()) {
      String headerName = entry.getKey();
      if (!this.headers.containsKey(headerName)) {
        this.headers.put(headerName, entry.getValue());
      }
    }
    for (Map.Entry<String, List<String>> entry : parentBuilder.parameters.entrySet()) {
      String paramName = entry.getKey();
      if (!this.parameters.containsKey(paramName)) {
        this.parameters.put(paramName, entry.getValue());
      }
    }
    for (Map.Entry<String, List<String>> entry : parentBuilder.queryParams.entrySet()) {
      String paramName = entry.getKey();
      if (!this.queryParams.containsKey(paramName)) {
        this.queryParams.put(paramName, entry.getValue());
      }
    }
    for (Cookie cookie : parentBuilder.cookies) {
      if (!containsCookie(cookie)) {
        this.cookies.add(cookie);
      }
    }
    for (Locale locale : parentBuilder.locales) {
      if (!this.locales.contains(locale)) {
        this.locales.add(locale);
      }
    }

    for (Map.Entry<String, Object> entry : parentBuilder.requestAttributes.entrySet()) {
      String attributeName = entry.getKey();
      if (!this.requestAttributes.containsKey(attributeName)) {
        this.requestAttributes.put(attributeName, entry.getValue());
      }
    }
    for (Map.Entry<String, Object> entry : parentBuilder.sessionAttributes.entrySet()) {
      String attributeName = entry.getKey();
      if (!this.sessionAttributes.containsKey(attributeName)) {
        this.sessionAttributes.put(attributeName, entry.getValue());
      }
    }
    for (Map.Entry<String, Object> entry : parentBuilder.flashAttributes.entrySet()) {
      String attributeName = entry.getKey();
      if (!this.flashAttributes.containsKey(attributeName)) {
        this.flashAttributes.put(attributeName, entry.getValue());
      }
    }

    this.postProcessors.addAll(0, parentBuilder.postProcessors);

    return this;
  }

  private boolean containsCookie(Cookie cookie) {
    for (Cookie cookieToCheck : this.cookies) {
      if (ObjectUtils.nullSafeEquals(cookieToCheck.getName(), cookie.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Build a {@link HttpMockRequestImpl}.
   */
  @Override
  public final HttpMockRequestImpl buildRequest(MockContext mockContext) {
    HttpMockRequestImpl request = createServletRequest(mockContext);

    request.setAsyncSupported(true);
    request.setMethod(this.method);

    String requestUri = this.url.getRawPath();
    request.setRequestURI(requestUri);

    if (this.url.getScheme() != null) {
      request.setScheme(this.url.getScheme());
    }
    if (this.url.getHost() != null) {
      request.setServerName(this.url.getHost());
    }
    if (this.url.getPort() != -1) {
      request.setServerPort(this.url.getPort());
    }

    updatePathRequestProperties(request, requestUri);

    if (this.secure != null) {
      request.setSecure(this.secure);
    }
    if (this.principal != null) {
      request.setUserPrincipal(this.principal);
    }
    if (this.session != null) {
      request.setSession(this.session);
    }
    if (this.remoteAddress != null) {
      request.setRemoteAddr(this.remoteAddress);
    }
    request.setCharacterEncoding(this.characterEncoding);
    request.setContent(this.content);
    request.setContentType(this.contentType);

    this.headers.forEach((name, values) -> {
      for (Object value : values) {
        request.addHeader(name, value);
      }
    });

    if (ObjectUtils.isNotEmpty(this.content) &&
            !this.headers.containsKey(HttpHeaders.CONTENT_LENGTH) &&
            !this.headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {

      request.addHeader(HttpHeaders.CONTENT_LENGTH, this.content.length);
    }

    String query = this.url.getRawQuery();
    if (!this.queryParams.isEmpty()) {
      String str = UriComponentsBuilder.newInstance().queryParams(this.queryParams).build().encode().getQuery();
      query = StringUtils.isNotEmpty(query) ? (query + "&" + str) : str;
    }
    if (query != null) {
      request.setQueryString(query);
    }
    addRequestParams(request, UriComponentsBuilder.fromUri(this.url).build().getQueryParams());

    this.parameters.forEach((name, values) -> {
      for (String value : values) {
        request.addParameter(name, value);
      }
    });

    if (this.content != null && this.content.length > 0) {
      String requestContentType = request.getContentType();
      if (requestContentType != null) {
        try {
          MediaType mediaType = MediaType.parseMediaType(requestContentType);
          if (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType)) {
            addRequestParams(request, parseFormData(mediaType));
          }
        }
        catch (Exception ex) {
          // Must be invalid, ignore..
        }
      }
    }

    if (ObjectUtils.isNotEmpty(this.cookies)) {
      request.setCookies(this.cookies.toArray(new Cookie[0]));
    }
    if (ObjectUtils.isNotEmpty(this.locales)) {
      request.setPreferredLocales(this.locales);
    }

    this.requestAttributes.forEach(request::setAttribute);
    this.sessionAttributes.forEach((name, attribute) -> {
      HttpSession session = request.getSession();
      Assert.state(session != null, "No HttpSession");
      session.setAttribute(name, attribute);
    });

    RedirectModel flashMap = new RedirectModel();
    flashMap.putAll(this.flashAttributes);
//    RedirectModelManager flashMapManager = getFlashMapManager(request);
//    flashMapManager.saveRedirectModel(new ServletRequestContext(
//            null, request, new MockHttpServletResponse()), flashMap);
    request.setAttribute(RedirectModel.INPUT_ATTRIBUTE, flashMap);
    return request;
  }

  /**
   * Create a new {@link HttpMockRequestImpl} based on the supplied
   * {@code MockContext}.
   * <p>Can be overridden in subclasses.
   */
  protected HttpMockRequestImpl createServletRequest(MockContext mockContext) {
    return new HttpMockRequestImpl(mockContext);
  }

  /**
   * Update the contextPath, servletPath, and pathInfo of the request.
   */
  private void updatePathRequestProperties(HttpMockRequestImpl request, String requestUri) {
    if ("".equals(this.pathInfo)) {
      if (!requestUri.startsWith(this.contextPath + this.servletPath)) {
        throw new IllegalArgumentException(
                "Invalid servlet path [" + this.servletPath + "] for request URI [" + requestUri + "]");
      }
      String extraPath = requestUri.substring(this.contextPath.length() + this.servletPath.length());
      this.pathInfo = (StringUtils.hasText(extraPath) ?
              UriUtils.decode(extraPath, StandardCharsets.UTF_8) : null);
    }
    request.setPathInfo(this.pathInfo);
  }

  private void addRequestParams(HttpMockRequestImpl request, MultiValueMap<String, String> map) {
    map.forEach((key, values) -> values.forEach(value -> {
      value = (value != null ? UriUtils.decode(value, StandardCharsets.UTF_8) : null);
      request.addParameter(UriUtils.decode(key, StandardCharsets.UTF_8), value);
    }));
  }

  private MultiValueMap<String, String> parseFormData(MediaType mediaType) {
    HttpInputMessage message = new HttpInputMessage() {
      @Override
      public InputStream getBody() {
        return (content != null ? new ByteArrayInputStream(content) : InputStream.nullInputStream());
      }

      @Override
      public HttpHeaders getHeaders() {
        HttpHeaders headers = HttpHeaders.forWritable();
        headers.setContentType(mediaType);
        return headers;
      }
    };

    try {
      return new FormHttpMessageConverter().read(null, message);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Failed to parse form data in request body", ex);
    }
  }

  @Override
  public HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request) {
    for (RequestPostProcessor postProcessor : this.postProcessors) {
      request = postProcessor.postProcessRequest(request);
    }
    return request;
  }

  private static void addToMap(Map<String, Object> map, String name, Object value) {
    Assert.hasLength(name, "'name' must not be empty");
    Assert.notNull(value, "'value' is required");
    map.put(name, value);
  }

  private static <T> void addToMultiValueMap(MultiValueMap<String, T> map, String name, T[] values) {
    Assert.hasLength(name, "'name' must not be empty");
    Assert.notEmpty(values, "'values' must not be empty");
    for (T value : values) {
      map.add(name, value);
    }
  }

}
