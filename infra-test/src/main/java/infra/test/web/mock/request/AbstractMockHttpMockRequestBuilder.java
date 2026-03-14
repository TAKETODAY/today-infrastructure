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

package infra.test.web.mock.request;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

import infra.beans.Mergeable;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpMethod;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.FormHttpMessageConverter;
import infra.lang.Assert;
import infra.mock.api.MockContext;
import infra.mock.api.MockRequest;
import infra.mock.api.http.Cookie;
import infra.mock.api.http.HttpSession;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpSession;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.RequestBuilder;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.RedirectModel;
import infra.web.client.ApiVersionFormatter;
import infra.web.client.ApiVersionInserter;
import infra.web.util.UriComponentsBuilder;
import infra.web.util.UriUtils;

/**
 * Base builder for {@link HttpMockRequestImpl} required as input to
 * perform requests in {@link MockMvc}.
 *
 * @param <B> a self reference to the builder type
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 5.0
 */
public abstract class AbstractMockHttpMockRequestBuilder<B extends AbstractMockHttpMockRequestBuilder<B>>
        implements ConfigurableSmartRequestBuilder<B>, Mergeable {

  private static final SimpleDateFormat simpleDateFormat;

  static {
    simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private final HttpMethod method;

  private @Nullable String uriTemplate;

  private @Nullable URI uri;

  private @Nullable String pathInfo = "";

  private @Nullable Boolean secure;

  private @Nullable Principal principal;

  private @Nullable MockHttpSession session;

  private @Nullable String remoteAddress;

  private @Nullable String characterEncoding;

  private byte @Nullable [] content;

  private @Nullable String contentType;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

  private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

  private final MultiValueMap<String, String> formFields = new LinkedMultiValueMap<>();

  private final List<Cookie> cookies = new ArrayList<>();

  private final List<Locale> locales = new ArrayList<>();

  private @Nullable Object version;

  private @Nullable ApiVersionInserter versionInserter;

  private final Map<String, Object> requestAttributes = new LinkedHashMap<>();

  private final Map<String, Object> sessionAttributes = new LinkedHashMap<>();

  private final Map<String, Object> flashAttributes = new LinkedHashMap<>();

  private final List<RequestPostProcessor> postProcessors = new ArrayList<>();

  /**
   * Create a new instance using the specified {@link HttpMethod}.
   *
   * @param httpMethod the HTTP method (GET, POST, etc.)
   */
  protected AbstractMockHttpMockRequestBuilder(HttpMethod httpMethod) {
    Assert.notNull(httpMethod, "'httpMethod' is required");
    this.method = httpMethod;
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }

  /**
   * Specify the URI using an absolute, fully constructed {@link java.net.URI}.
   */
  public B uri(URI uri) {
    return updateUri(uri, null);
  }

  /**
   * Specify the URI for the request using a URI template and URI variables.
   */
  public B uri(String uriTemplate, @Nullable Object... uriVariables) {
    return updateUri(initUri(uriTemplate, uriVariables), uriTemplate);
  }

  private B updateUri(URI uri, @Nullable String uriTemplate) {
    this.uri = uri;
    this.uriTemplate = uriTemplate;
    return self();
  }

  private static URI initUri(String uri, @Nullable Object[] vars) {
    Assert.notNull(uri, "'uri' must not be null");
    Assert.isTrue(uri.isEmpty() || uri.startsWith("/") || uri.startsWith("http://") || uri.startsWith("https://"),
            () -> "'uri' should start with a path or be a complete HTTP URI: " + uri);
    String uriString = (uri.isEmpty() ? "/" : uri);
    return UriComponentsBuilder.forURIString(uriString).buildAndExpand(vars).encode().toURI();
  }

  /**
   * Specify the portion of the requestURI that represents the pathInfo.
   * <p>If left unspecified (recommended), the pathInfo will be automatically derived
   * by removing the contextPath and the servletPath from the requestURI and using any
   * remaining part. If specified here, the pathInfo must start with a "/".
   * <p>If specified, the pathInfo will be used as-is.
   */
  public B pathInfo(@Nullable String pathInfo) {
    if (StringUtils.hasText(pathInfo)) {
      Assert.isTrue(pathInfo.startsWith("/"), "Path info must start with a '/'");
    }
    this.pathInfo = pathInfo;
    return self();
  }

  /**
   * Set the secure property of the {@link MockRequest} indicating use of a
   * secure channel, such as HTTPS.
   *
   * @param secure whether the request is using a secure channel
   */
  public B secure(boolean secure) {
    this.secure = secure;
    return self();
  }

  /**
   * Set the character encoding of the request.
   *
   * @param encoding the character encoding
   * @see StandardCharsets
   * @see #characterEncoding(String)
   */
  public B characterEncoding(Charset encoding) {
    return characterEncoding(encoding.name());
  }

  /**
   * Set the character encoding of the request.
   *
   * @param encoding the character encoding
   */
  public B characterEncoding(String encoding) {
    this.characterEncoding = encoding;
    return self();
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
  public B content(byte[] content) {
    this.content = content;
    return self();
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
  public B content(String content) {
    this.content = content.getBytes(StandardCharsets.UTF_8);
    return self();
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
  public B contentType(MediaType contentType) {
    Assert.notNull(contentType, "'contentType' must not be null");
    this.contentType = contentType.toString();
    return self();
  }

  /**
   * Set the 'Content-Type' header of the request as a raw String value,
   * possibly not even well-formed (for testing purposes).
   *
   * @param contentType the content type
   */
  public B contentType(String contentType) {
    Assert.notNull(contentType, "'contentType' must not be null");
    this.contentType = contentType;
    return self();
  }

  /**
   * Set the 'Accept' header to the given media type(s).
   *
   * @param mediaTypes one or more media types
   */
  public B accept(MediaType... mediaTypes) {
    Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
    this.headers.setAccept(Arrays.asList(mediaTypes));
    return self();
  }

  /**
   * Set the {@code Accept} header using raw String values, possibly not even
   * well-formed (for testing purposes).
   *
   * @param mediaTypes one or more media types; internally joined as
   * comma-separated String
   */
  public B accept(String... mediaTypes) {
    Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
    this.headers.set("Accept", String.join(", ", mediaTypes));
    return self();
  }

  /**
   * Set the list of acceptable {@linkplain Charset charsets}, as specified
   * by the {@code Accept-Charset} header.
   *
   * @param acceptableCharsets the acceptable charsets
   */
  public B acceptCharset(Charset... acceptableCharsets) {
    this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
    return self();
  }

  /**
   * Set the value of the {@code If-Modified-Since} header.
   *
   * @param ifModifiedSince the new value of the header
   */
  public B ifModifiedSince(ZonedDateTime ifModifiedSince) {
    this.headers.setIfModifiedSince(ifModifiedSince);
    return self();
  }

  /**
   * Set the values of the {@code If-None-Match} header.
   *
   * @param ifNoneMatches the new value of the header
   */
  public B ifNoneMatch(String... ifNoneMatches) {
    this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
    return self();
  }

  /**
   * Add a header to the request. Values are always added.
   *
   * @param name the header name
   * @param values one or more header values
   */
  public B header(String name, Object... values) {

    // Prior to 7.0, header values were passed as Objects to HttpMockRequestImpl.
    // Here we add some formatting for backwards compatibility.

    if (values.length == 1) {
      Object value = values[0];
      if (value instanceof Collection<?> collection) {
        values = collection.toArray();
      }
    }

    for (Object value : values) {
      if (value instanceof Date date) {
        this.headers.add(name, simpleDateFormat.format(date));
      }
      else {
        this.headers.add(name, String.valueOf(value));
      }
    }

    return self();
  }

  /**
   * Add all headers to the request. Values are always added.
   *
   * @param httpHeaders the headers and values to add
   */
  public B headers(HttpHeaders httpHeaders) {
    headers.addAll(httpHeaders);
    return self();
  }

  /**
   * Provides access to every header declared so far with the possibility
   * to add, replace, or remove values.
   *
   * @param headersConsumer the consumer to provide access to
   * @return this builder
   */
  public B headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return self();
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
  public B param(String name, String... values) {
    this.parameters.addAll(name, Arrays.asList(values));
    return self();
  }

  /**
   * Variant of {@link #param(String, String...)} with a {@link MultiValueMap}.
   *
   * @param params the parameters to add
   */
  public B params(MultiValueMap<String, String> params) {
    params.forEach((name, values) -> {
      for (String value : values) {
        this.parameters.add(name, value);
      }
    });
    return self();
  }

  /**
   * Append to the query string and also add to the
   * {@link #param(String, String...) request parameters} map. The parameter
   * name and value are encoded when they are added to the query string.
   *
   * @param name the parameter name
   * @param values one or more values
   */
  public B queryParam(String name, String... values) {
    param(name, values);
    this.queryParams.addAll(name, Arrays.asList(values));
    return self();
  }

  /**
   * Append to the query string and also add to the
   * {@link #params(MultiValueMap) request parameters} map. The parameter
   * name and value are encoded when they are added to the query string.
   *
   * @param params the parameters to add
   */
  public B queryParams(MultiValueMap<String, String> params) {
    params(params);
    this.queryParams.addAll(params);
    return self();
  }

  /**
   * Append the given value(s) to the given form field and also add them to the
   * {@linkplain #param(String, String...) request parameters} map.
   *
   * @param name the field name
   * @param values one or more values
   */
  public B formField(String name, String... values) {
    param(name, values);
    this.formFields.addAll(name, Arrays.asList(values));
    return self();
  }

  /**
   * Variant of {@link #formField(String, String...)} with a {@link MultiValueMap}.
   *
   * @param formFields the form fields to add
   */
  public B formFields(MultiValueMap<String, String> formFields) {
    params(formFields);
    this.formFields.addAll(formFields);
    return self();
  }

  /**
   * Add the given cookies to the request. Cookies are always added.
   *
   * @param cookies the cookies to add
   */
  public B cookie(Cookie... cookies) {
    Assert.notEmpty(cookies, "'cookies' must not be empty");
    this.cookies.addAll(Arrays.asList(cookies));
    return self();
  }

  /**
   * Add the specified locales as preferred request locales.
   *
   * @param locales the locales to add
   * @see #locale(Locale)
   */
  public B locale(Locale... locales) {
    Assert.notEmpty(locales, "'locales' must not be empty");
    this.locales.addAll(Arrays.asList(locales));
    return self();
  }

  /**
   * Set the locale of the request, overriding any previous locales.
   *
   * @param locale the locale, or {@code null} to reset it
   * @see #locale(Locale...)
   */
  public B locale(@Nullable Locale locale) {
    this.locales.clear();
    if (locale != null) {
      this.locales.add(locale);
    }
    return self();
  }

  /**
   * Set an API version for the request. The version is inserted into the
   * request by the {@link #apiVersionInserter(ApiVersionInserter) configured}
   * {@code ApiVersionInserter}.
   *
   * @param version the API version of the request; this can be a String or
   * some Object that can be formatted the inserter, e.g. through an
   * {@link ApiVersionFormatter}.
   */
  public B apiVersion(Object version) {
    this.version = version;
    return self();
  }

  /**
   * Configure an {@link ApiVersionInserter} to abstract how an API version
   * specified via {@link #apiVersion(Object)} is inserted into the request.
   * An inserter may typically be set once (more centrally) via
   * {@link infra.test.web.mock.setup.ConfigurableMockMvcBuilder#defaultRequest(RequestBuilder)}, or
   * {@link infra.test.web.mock.setup.ConfigurableMockMvcBuilder#apiVersionInserter(ApiVersionInserter)}.
   * <p>{@code ApiVersionInserter} exposes shortcut methods for several
   * built-in inserter implementation types. See the class-level Javadoc
   * of {@link ApiVersionInserter} for a list of choices.
   *
   * @param versionInserter the inserter to use
   */
  public B apiVersionInserter(@Nullable ApiVersionInserter versionInserter) {
    this.versionInserter = versionInserter;
    return self();
  }

  /**
   * Set a request attribute.
   *
   * @param name the attribute name
   * @param value the attribute value
   */
  public B requestAttr(String name, Object value) {
    addToMap(this.requestAttributes, name, value);
    return self();
  }

  /**
   * Set a session attribute.
   *
   * @param name the session attribute name
   * @param value the session attribute value
   */
  public B sessionAttr(String name, Object value) {
    addToMap(this.sessionAttributes, name, value);
    return self();
  }

  /**
   * Set session attributes.
   *
   * @param sessionAttributes the session attributes
   */
  public B sessionAttrs(Map<String, Object> sessionAttributes) {
    Assert.notEmpty(sessionAttributes, "'sessionAttributes' must not be empty");
    sessionAttributes.forEach(this::sessionAttr);
    return self();
  }

  /**
   * Set an "input" flash attribute.
   *
   * @param name the flash attribute name
   * @param value the flash attribute value
   */
  public B flashAttr(String name, Object value) {
    addToMap(this.flashAttributes, name, value);
    return self();
  }

  /**
   * Set flash attributes.
   *
   * @param flashAttributes the flash attributes
   */
  public B flashAttrs(Map<String, Object> flashAttributes) {
    Assert.notEmpty(flashAttributes, "'flashAttributes' must not be empty");
    flashAttributes.forEach(this::flashAttr);
    return self();
  }

  /**
   * Set the HTTP session to use, possibly re-used across requests.
   * <p>Individual attributes provided via {@link #sessionAttr(String, Object)}
   * override the content of the session provided here.
   *
   * @param session the HTTP session
   */
  public B session(MockHttpSession session) {
    Assert.notNull(session, "'session' must not be null");
    this.session = session;
    return self();
  }

  /**
   * Set the principal of the request.
   *
   * @param principal the principal
   */
  public B principal(Principal principal) {
    Assert.notNull(principal, "'principal' must not be null");
    this.principal = principal;
    return self();
  }

  /**
   * Set the remote address of the request.
   *
   * @param remoteAddress the remote address (IP)
   */
  public B remoteAddress(String remoteAddress) {
    Assert.hasText(remoteAddress, "'remoteAddress' must not be null or blank");
    this.remoteAddress = remoteAddress;
    return self();
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
  public B with(RequestPostProcessor postProcessor) {
    Assert.notNull(postProcessor, "postProcessor is required");
    this.postProcessors.add(postProcessor);
    return self();
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
    if (!(parent instanceof AbstractMockHttpMockRequestBuilder<?> parentBuilder)) {
      throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
    }
    if (this.uri == null) {
      this.uri = parentBuilder.uri;
      this.uriTemplate = parentBuilder.uriTemplate;
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

    parentBuilder.headers.forEach((headerName, values) -> {
      values.forEach(value -> {
        if (!this.headers.contains(headerName)) {
          this.headers.set(headerName, values);
        }
      });
    });
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
    for (Map.Entry<String, List<String>> entry : parentBuilder.formFields.entrySet()) {
      String paramName = entry.getKey();
      if (!this.formFields.containsKey(paramName)) {
        this.formFields.put(paramName, entry.getValue());
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

    if (this.version == null) {
      this.version = parentBuilder.version;
    }

    if (this.versionInserter == null) {
      this.versionInserter = parentBuilder.versionInserter;
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
  public final HttpMockRequestImpl buildRequest(MockContext servletContext) {

    URI uri = this.uri;
    Assert.notNull(uri, "'uri' is required");

    if (this.version != null) {
      Assert.state(this.versionInserter != null, "No ApiVersionInserter");
      uri = this.versionInserter.insertVersion(this.version, uri);
    }

    HttpMockRequestImpl request = createMockRequest(servletContext);

    request.setAsyncSupported(true);
    request.setMethod(this.method.name());

    request.setUriTemplate(this.uriTemplate);

    String requestUri = uri.getRawPath();
    request.setRequestURI(requestUri);

    if (uri.getScheme() != null) {
      request.setScheme(uri.getScheme());
    }
    if (uri.getHost() != null) {
      request.setServerName(uri.getHost());
    }
    if (uri.getPort() != -1) {
      request.setServerPort(uri.getPort());
    }

    updatePathRequestProperties(request, requestUri);

    if (this.secure != null) {
      request.setSecure(this.secure);
    }
    if (this.principal != null) {
      request.setUserPrincipal(this.principal);
    }
    if (this.remoteAddress != null) {
      request.setRemoteAddr(this.remoteAddress);
    }
    if (this.session != null) {
      request.setSession(this.session);
    }

    request.setCharacterEncoding(this.characterEncoding);
    request.setContent(this.content);
    request.setContentType(this.contentType);

    if (this.version != null) {
      Assert.state(this.versionInserter != null, "No ApiVersionInserter");
      this.versionInserter.insertVersion(this.version, this.headers);
    }

    this.headers.forEach((name, values) -> {
      for (Object value : values) {
        request.addHeader(name, value);
      }
    });

    if (ObjectUtils.isNotEmpty(this.content) &&
            !this.headers.contains(HttpHeaders.CONTENT_LENGTH) &&
            !this.headers.contains(HttpHeaders.TRANSFER_ENCODING)) {

      request.addHeader(HttpHeaders.CONTENT_LENGTH, this.content.length);
    }

    String query = uri.getRawQuery();
    if (!this.queryParams.isEmpty()) {
      String str = UriComponentsBuilder.create().queryParams(this.queryParams).build().encode().getQuery();
      query = StringUtils.isNotEmpty(query) ? (query + "&" + str) : str;
    }
    if (query != null) {
      request.setQueryString(query);
    }

    addRequestParams(request, UriComponentsBuilder.forURI(uri).build().getQueryParams());
    this.parameters.forEach((name, values) -> request.addParameter(name, values.toArray(new String[0])));

    if (!this.formFields.isEmpty()) {
      if (this.content != null && this.content.length > 0) {
        throw new IllegalStateException("Could not write form data with an existing body");
      }
      Charset charset = (this.characterEncoding != null ?
              Charset.forName(this.characterEncoding) : StandardCharsets.UTF_8);
      MediaType mediaType = (request.getContentType() != null ?
              MediaType.parseMediaType(request.getContentType()) :
              new MediaType(MediaType.APPLICATION_FORM_URLENCODED, charset));
      if (!mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
        throw new IllegalStateException("Invalid content type: '" + mediaType +
                "' is not compatible with '" + MediaType.APPLICATION_FORM_URLENCODED + "'");
      }
      request.setContent(writeFormData(mediaType, charset));
      if (request.getContentType() == null) {
        request.setContentType(mediaType.toString());
      }
    }
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
          // Must be invalid, ignore
        }
      }
    }

    if (!ObjectUtils.isEmpty(this.cookies)) {
      request.setCookies(this.cookies.toArray(new Cookie[0]));
    }
    if (!ObjectUtils.isEmpty(this.locales)) {
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
//    flashMapManager.saveRedirectModel(new MockRequestContext(
//            null, request, new MockHttpMockResponse()), flashMap);
    request.setAttribute(RedirectModel.INPUT_ATTRIBUTE, flashMap);
    return request;
  }

  /**
   * Create a new {@link HttpMockRequestImpl} based on the supplied
   * {@code MockContext}.
   * <p>Can be overridden in subclasses.
   */
  protected HttpMockRequestImpl createMockRequest(MockContext mockContext) {
    return new HttpMockRequestImpl(mockContext);
  }

  /**
   * Update the pathInfo of the request.
   */
  private void updatePathRequestProperties(HttpMockRequestImpl request, String requestUri) {
    String path = this.pathInfo;
    if ("".equals(path)) {
      path = StringUtils.hasText(requestUri) ? UriUtils.decode(requestUri, StandardCharsets.UTF_8) : null;
    }
    request.setPathInfo(path);
  }

  private void addRequestParams(HttpMockRequestImpl request, MultiValueMap<String, String> map) {
    map.forEach((key, values) ->
            request.addParameter(
                    UriUtils.decode(key, StandardCharsets.UTF_8),
                    values.stream()
                            .map(value -> value != null ? UriUtils.decode(value, StandardCharsets.UTF_8) : null)
                            .toArray(String[]::new)));
  }

  private byte[] writeFormData(MediaType mediaType, Charset charset) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    HttpOutputMessage message = new HttpOutputMessage() {
      @Override
      public OutputStream getBody() {
        return out;
      }

      @Override
      public HttpHeaders getHeaders() {
        HttpHeaders headers = HttpHeaders.forWritable();
        headers.setContentType(mediaType);
        return headers;
      }
    };
    try {
      FormHttpMessageConverter messageConverter = new FormHttpMessageConverter();
      messageConverter.setCharset(charset);
      messageConverter.write(this.formFields, mediaType, message);
      return out.toByteArray();
    }
    catch (IOException ex) {
      throw new IllegalStateException("Failed to write form data to request body", ex);
    }
  }

  @SuppressWarnings("unchecked")
  private MultiValueMap<String, String> parseFormData(MediaType mediaType) {
    HttpInputMessage message = new HttpInputMessage() {
      @Override
      public InputStream getBody() {
        byte[] bodyContent = AbstractMockHttpMockRequestBuilder.this.content;
        return (bodyContent != null ? new ByteArrayInputStream(bodyContent) : InputStream.nullInputStream());
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
    Assert.notNull(value, "'value' must not be null");
    map.put(name, value);
  }

}
