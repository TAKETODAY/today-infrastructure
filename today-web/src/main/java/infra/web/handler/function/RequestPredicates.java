/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.handler.function;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.server.PathContainer;
import infra.http.server.RequestPath;
import infra.lang.Assert;
import infra.lang.NonNull;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.MimeTypeUtils;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
import infra.validation.BindException;
import infra.web.HandlerMapping;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
import infra.web.bind.WebDataBinder;
import infra.web.multipart.Multipart;
import infra.web.util.UriBuilder;
import infra.web.util.UriUtils;
import infra.web.util.pattern.PathMatchInfo;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

/**
 * Implementations of {@link RequestPredicate} that implement various useful
 * request matching operations, such as matching based on path, HTTP method, etc.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class RequestPredicates {

  private static final Logger logger = LoggerFactory.getLogger(RequestPredicates.class);

  /**
   * Return a {@code RequestPredicate} that always matches.
   *
   * @return a predicate that always matches
   */
  public static RequestPredicate all() {
    return request -> true;
  }

  /**
   * Return a {@code RequestPredicate} that matches if the request's
   * HTTP method is equal to the given method.
   *
   * @param httpMethod the HTTP method to match against
   * @return a predicate that tests against the given HTTP method
   */
  public static RequestPredicate method(HttpMethod httpMethod) {
    return new HttpMethodPredicate(httpMethod);
  }

  /**
   * Return a {@code RequestPredicate} that matches if the request's
   * HTTP method is equal to one the of the given methods.
   *
   * @param httpMethods the HTTP methods to match against
   * @return a predicate that tests against the given HTTP methods
   */
  public static RequestPredicate methods(HttpMethod... httpMethods) {
    return new HttpMethodPredicate(httpMethods);
  }

  /**
   * Return a {@code RequestPredicate} that tests the request path
   * against the given path pattern.
   *
   * @param pattern the pattern to match to
   * @return a predicate that tests against the given path pattern
   */
  public static RequestPredicate path(String pattern) {
    Assert.notNull(pattern, "'pattern' is required");
    pattern = StringUtils.prependLeadingSlash(pattern);
    return pathPredicates(PathPatternParser.defaultInstance).apply(pattern);
  }

  /**
   * Return a function that creates new path-matching {@code RequestPredicates}
   * from pattern Strings using the given {@link PathPatternParser}.
   * <p>This method can be used to specify a non-default, customized
   * {@code PathPatternParser} when resolving path patterns.
   *
   * @param patternParser the parser used to parse patterns given to the returned function
   * @return a function that resolves a pattern String into a path-matching
   * {@code RequestPredicates} instance
   */
  public static Function<String, RequestPredicate> pathPredicates(PathPatternParser patternParser) {
    Assert.notNull(patternParser, "PathPatternParser is required");
    return pattern -> new PathPatternPredicate(patternParser.parse(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that tests the request's headers
   * against the given headers predicate.
   *
   * @param headersPredicate a predicate that tests against the request headers
   * @return a predicate that tests against the given header predicate
   */
  public static RequestPredicate headers(Predicate<ServerRequest.Headers> headersPredicate) {
    return new HeadersPredicate(headersPredicate);
  }

  /**
   * Return a {@code RequestPredicate} that tests if the request's
   * {@linkplain ServerRequest.Headers#contentType() content type} is
   * {@linkplain MediaType#includes(MediaType) included} by any of the given media types.
   *
   * @param mediaTypes the media types to match the request's content type against
   * @return a predicate that tests the request's content type against the given media types
   */
  public static RequestPredicate contentType(MediaType... mediaTypes) {
    Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
    return new ContentTypePredicate(mediaTypes);
  }

  /**
   * Return a {@code RequestPredicate} that tests if the request's
   * {@linkplain ServerRequest.Headers#accept() accept} header is
   * {@linkplain MediaType#isCompatibleWith(MediaType) compatible} with any of the given media types.
   *
   * @param mediaTypes the media types to match the request's accept header against
   * @return a predicate that tests the request's accept header against the given media types
   */
  public static RequestPredicate accept(MediaType... mediaTypes) {
    Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
    return new AcceptPredicate(mediaTypes);
  }

  /**
   * {@code RequestPredicate} to match to the request API version extracted
   * from and parsed with the configured {@link ApiVersionStrategy}.
   * <p>The version may be one of the following:
   * <ul>
   * <li>Fixed version ("1.2") -- match this version only.
   * <li>Baseline version ("1.2+") -- match this and subsequent versions.
   * </ul>
   * <p>A baseline version allows n endpoint route to continue to work in
   * subsequent versions if it remains compatible until an incompatible change
   * eventually leads to the creation of a new route.
   *
   * @param version the version to use
   * @return the created predicate instance
   * @since 5.0
   */
  public static RequestPredicate version(Object version) {
    return new ApiVersionPredicate(version);
  }

  /**
   * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code GET}
   * and the given {@code pattern} matches against the request path.
   *
   * @param pattern the path pattern to match against
   * @return a predicate that matches if the request method is GET and if the given pattern
   * matches against the request path
   */
  public static RequestPredicate GET(String pattern) {
    return method(HttpMethod.GET).and(path(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code HEAD}
   * and the given {@code pattern} matches against the request path.
   *
   * @param pattern the path pattern to match against
   * @return a predicate that matches if the request method is HEAD and if the given pattern
   * matches against the request path
   */
  public static RequestPredicate HEAD(String pattern) {
    return method(HttpMethod.HEAD).and(path(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code POST}
   * and the given {@code pattern} matches against the request path.
   *
   * @param pattern the path pattern to match against
   * @return a predicate that matches if the request method is POST and if the given pattern
   * matches against the request path
   */
  public static RequestPredicate POST(String pattern) {
    return method(HttpMethod.POST).and(path(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code PUT}
   * and the given {@code pattern} matches against the request path.
   *
   * @param pattern the path pattern to match against
   * @return a predicate that matches if the request method is PUT and if the given pattern
   * matches against the request path
   */
  public static RequestPredicate PUT(String pattern) {
    return method(HttpMethod.PUT).and(path(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code PATCH}
   * and the given {@code pattern} matches against the request path.
   *
   * @param pattern the path pattern to match against
   * @return a predicate that matches if the request method is PATCH and if the given pattern
   * matches against the request path
   */
  public static RequestPredicate PATCH(String pattern) {
    return method(HttpMethod.PATCH).and(path(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code DELETE}
   * and the given {@code pattern} matches against the request path.
   *
   * @param pattern the path pattern to match against
   * @return a predicate that matches if the request method is DELETE and if the given pattern
   * matches against the request path
   */
  public static RequestPredicate DELETE(String pattern) {
    return method(HttpMethod.DELETE).and(path(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that matches if request's HTTP method is {@code OPTIONS}
   * and the given {@code pattern} matches against the request path.
   *
   * @param pattern the path pattern to match against
   * @return a predicate that matches if the request method is OPTIONS and if the given pattern
   * matches against the request path
   */
  public static RequestPredicate OPTIONS(String pattern) {
    return method(HttpMethod.OPTIONS).and(path(pattern));
  }

  /**
   * Return a {@code RequestPredicate} that matches if the request's path has the given extension.
   *
   * @param extension the path extension to match against, ignoring case
   * @return a predicate that matches if the request's path has the given file extension
   */
  public static RequestPredicate pathExtension(String extension) {
    Assert.notNull(extension, "'extension' is required");
    return new PathExtensionPredicate(extension);
  }

  /**
   * Return a {@code RequestPredicate} that matches if the request's path matches the given
   * predicate.
   *
   * @param extensionPredicate the predicate to test against the request path extension
   * @return a predicate that matches if the given predicate matches against the request's path
   * file extension
   */
  public static RequestPredicate pathExtension(Predicate<String> extensionPredicate) {
    return new PathExtensionPredicate(extensionPredicate);
  }

  /**
   * Return a {@code RequestPredicate} that matches if the request's parameter of the given name
   * has the given value.
   *
   * @param name the name of the parameter to test against
   * @return a predicate that matches if the parameter has the given value
   * @see ServerRequest#param(String)
   * @see StringUtils#isBlank(String)
   */
  public static RequestPredicate param(String name) {
    return new ParamPredicate(name, StringUtils::isBlank);
  }

  /**
   * Return a {@code RequestPredicate} that matches if the request's parameter of the given name
   * has the given value.
   *
   * @param name the name of the parameter to test against
   * @param value the value of the parameter to test against
   * @return a predicate that matches if the parameter has the given value
   * @see ServerRequest#param(String)
   */
  public static RequestPredicate param(String name, String value) {
    return new ParamPredicate(name, value);
  }

  /**
   * Return a {@code RequestPredicate} that tests the request's parameter of the given name
   * against the given predicate.
   *
   * @param name the name of the parameter to test against
   * @param predicate the predicate to test against the parameter value
   * @return a predicate that matches the given predicate against the parameter of the given name
   * @see ServerRequest#param(String)
   */
  public static RequestPredicate param(String name, Predicate<String> predicate) {
    return new ParamPredicate(name, predicate);
  }

  private static void traceMatch(String prefix, Object desired, @Nullable Object actual, boolean match) {
    if (logger.isTraceEnabled()) {
      logger.trace("{} \"{}\" {} against value \"{}\"",
              prefix, desired, match ? "matches" : "does not match", actual);
    }
  }

  private static Map<String, String> mergePathVariables(Map<String, String> oldVariables, Map<String, String> newVariables) {
    if (!newVariables.isEmpty()) {
      Map<String, String> mergedVariables = new LinkedHashMap<>(oldVariables);
      mergedVariables.putAll(newVariables);
      return mergedVariables;
    }
    else {
      return oldVariables;
    }
  }

  private static PathPattern mergePatterns(@Nullable PathPattern oldPattern, PathPattern newPattern) {
    if (oldPattern != null) {
      return oldPattern.combine(newPattern);
    }
    else {
      return newPattern;
    }

  }

  /**
   * Receives notifications from the logical structure of request predicates.
   */
  public interface Visitor {

    /**
     * Receive notification of an HTTP method predicate.
     *
     * @param methods the HTTP methods that make up the predicate
     * @see RequestPredicates#method(HttpMethod)
     */
    void method(Set<HttpMethod> methods);

    /**
     * Receive notification of a path predicate.
     *
     * @param pattern the path pattern that makes up the predicate
     * @see RequestPredicates#path(String)
     */
    void path(String pattern);

    /**
     * Receive notification of a path extension predicate.
     *
     * @param extension the path extension that makes up the predicate
     * @see RequestPredicates#pathExtension(String)
     */
    void pathExtension(String extension);

    /**
     * Receive notification of an HTTP header predicate.
     *
     * @param name the name of the HTTP header to check
     * @param value the desired value of the HTTP header
     * @see RequestPredicates#headers(Predicate)
     * @see RequestPredicates#contentType(MediaType...)
     * @see RequestPredicates#accept(MediaType...)
     */
    void header(String name, String value);

    /**
     * Receive notification of a parameter predicate.
     *
     * @param name the name of the parameter
     * @param value the desired value of the parameter
     * @see RequestPredicates#param(String, String)
     */
    void param(String name, String value);

    /**
     * Receive notification of an API version predicate. The version could
     * be fixed ("1.2") or baseline ("1.2+").
     *
     * @param version the configured version
     * @since 5.0
     */
    void version(String version);

    /**
     * Receive first notification of a logical AND predicate.
     * The first subsequent notification will contain the left-hand side of the AND-predicate;
     * followed by {@link #and()}, followed by the right-hand side, followed by {@link #endAnd()}.
     *
     * @see RequestPredicate#and(RequestPredicate)
     */
    void startAnd();

    /**
     * Receive "middle" notification of a logical AND predicate.
     * The following notification contains the right-hand side, followed by {@link #endAnd()}.
     *
     * @see RequestPredicate#and(RequestPredicate)
     */
    void and();

    /**
     * Receive last notification of a logical AND predicate.
     *
     * @see RequestPredicate#and(RequestPredicate)
     */
    void endAnd();

    /**
     * Receive first notification of a logical OR predicate.
     * The first subsequent notification will contain the left-hand side of the OR-predicate;
     * the second notification contains the right-hand side, followed by {@link #endOr()}.
     *
     * @see RequestPredicate#or(RequestPredicate)
     */
    void startOr();

    /**
     * Receive "middle" notification of a logical OR predicate.
     * The following notification contains the right-hand side, followed by {@link #endOr()}.
     *
     * @see RequestPredicate#or(RequestPredicate)
     */
    void or();

    /**
     * Receive last notification of a logical OR predicate.
     *
     * @see RequestPredicate#or(RequestPredicate)
     */
    void endOr();

    /**
     * Receive first notification of a negated predicate.
     * The first subsequent notification will contain the negated predicated, followed
     * by {@link #endNegate()}.
     *
     * @see RequestPredicate#negate()
     */
    void startNegate();

    /**
     * Receive last notification of a negated predicate.
     *
     * @see RequestPredicate#negate()
     */
    void endNegate();

    /**
     * Receive first notification of an unknown predicate.
     */
    void unknown(RequestPredicate predicate);
  }

  /**
   * Extension of {@code RequestPredicate} that can modify the {@code ServerRequest}.
   */
  abstract static class RequestModifyingPredicate implements RequestPredicate {

    public static RequestModifyingPredicate of(RequestPredicate requestPredicate) {
      if (requestPredicate instanceof RequestModifyingPredicate modifyingPredicate) {
        return modifyingPredicate;
      }
      else {
        return new RequestModifyingPredicate() {
          @Override
          protected Result testInternal(ServerRequest request) {
            return Result.of(requestPredicate.test(request));
          }
        };
      }
    }

    @Override
    public final boolean test(ServerRequest request) {
      Result result = testInternal(request);
      boolean value = result.value();
      if (value) {
        result.modify(request);
      }
      return value;
    }

    protected abstract Result testInternal(ServerRequest request);

    protected static final class Result {

      private static final Result TRUE = new Result(true, null);

      private static final Result FALSE = new Result(false, null);

      private final boolean value;

      @Nullable
      private final Consumer<ServerRequest> modify;

      private Result(boolean value, @Nullable Consumer<ServerRequest> modify) {
        this.value = value;
        this.modify = modify;
      }

      public static Result of(boolean value) {
        return of(value, null);
      }

      public static Result of(boolean value, @Nullable Consumer<ServerRequest> commit) {
        if (commit == null) {
          return value ? TRUE : FALSE;
        }
        else {
          return new Result(value, commit);
        }
      }

      public boolean value() {
        return this.value;
      }

      public void modify(ServerRequest request) {
        if (this.modify != null) {
          this.modify.accept(request);
        }
      }
    }

  }

  private static class HttpMethodPredicate implements RequestPredicate {

    private final Set<HttpMethod> httpMethods;

    public HttpMethodPredicate(HttpMethod httpMethod) {
      Assert.notNull(httpMethod, "HttpMethod is required");
      this.httpMethods = Set.of(httpMethod);
    }

    public HttpMethodPredicate(HttpMethod... httpMethods) {
      Assert.notEmpty(httpMethods, "HttpMethods must not be empty");
      this.httpMethods = new LinkedHashSet<>(Arrays.asList(httpMethods));
    }

    @Override
    public boolean test(ServerRequest request) {
      HttpMethod method = method(request);
      boolean match = this.httpMethods.contains(method);
      traceMatch("Method", this.httpMethods, method, match);
      return match;
    }

    private static HttpMethod method(ServerRequest request) {
      if (request.exchange().isPreFlightRequest()) {
        String accessControlRequestMethod =
                request.headers().firstHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        if (accessControlRequestMethod != null) {
          return HttpMethod.valueOf(accessControlRequestMethod);
        }
      }
      return request.method();
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.method(Collections.unmodifiableSet(this.httpMethods));
    }

    @Override
    public String toString() {
      if (httpMethods.size() == 1) {
        return httpMethods.iterator().next().toString();
      }
      else {
        return httpMethods.toString();
      }
    }
  }

  private static class PathPatternPredicate extends RequestModifyingPredicate
          implements ChangePathPatternParserVisitor.Target {

    private PathPattern pattern;

    public PathPatternPredicate(PathPattern pattern) {
      Assert.notNull(pattern, "'pattern' is required");
      this.pattern = pattern;
    }

    @Override
    protected Result testInternal(ServerRequest request) {
      PathContainer pathContainer = request.requestPath().pathWithinApplication();
      PathMatchInfo info = pattern.matchAndExtract(pathContainer);
      if (logger.isTraceEnabled()) {
        traceMatch("Pattern", pattern.getPatternString(), request.path(), info != null);
      }
      if (info != null) {
        return Result.of(true, serverRequest -> mergeAttributes(serverRequest, info.getUriVariables()));
      }
      else {
        return Result.of(false);
      }
    }

    private void mergeAttributes(ServerRequest request, Map<String, String> variables) {
      Map<String, Object> attributes = request.attributes();
      Map<String, String> pathVariables = mergePathVariables(request.pathVariables(), variables);
      attributes.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              Collections.unmodifiableMap(pathVariables));

      PathPattern pattern = mergePatterns(
              (PathPattern) attributes.get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE), this.pattern);
      attributes.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
    }

    @Override
    public Optional<ServerRequest> nest(ServerRequest request) {
      return Optional.ofNullable(pattern.matchStartOfPath(request.requestPath().pathWithinApplication()))
              .map(info -> new SubPathServerRequestWrapper(request, info, pattern));
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.path(pattern.getPatternString());
    }

    @Override
    public void changeParser(PathPatternParser parser) {
      String patternString = pattern.getPatternString();
      this.pattern = parser.parse(patternString);
    }

    @Override
    public String toString() {
      return pattern.getPatternString();
    }
  }

  private static class HeadersPredicate implements RequestPredicate {

    private final Predicate<ServerRequest.Headers> headersPredicate;

    public HeadersPredicate(Predicate<ServerRequest.Headers> headersPredicate) {
      Assert.notNull(headersPredicate, "Predicate is required");
      this.headersPredicate = headersPredicate;
    }

    @Override
    public boolean test(ServerRequest request) {
      if (request.exchange().isPreFlightRequest()) {
        return true;
      }
      else {
        return headersPredicate.test(request.headers());
      }
    }

    @Override
    public String toString() {
      return headersPredicate.toString();
    }
  }

  private static class ContentTypePredicate extends HeadersPredicate {

    private final Set<MediaType> mediaTypes;

    public ContentTypePredicate(MediaType... mediaTypes) {
      this(Set.of(mediaTypes));
    }

    private ContentTypePredicate(Set<MediaType> mediaTypes) {
      super(headers -> {
        MediaType contentType = headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
        boolean match = mediaTypes.stream().anyMatch(mediaType -> mediaType.includes(contentType));
        traceMatch("Content-Type", mediaTypes, contentType, match);
        return match;
      });
      this.mediaTypes = mediaTypes;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.header(HttpHeaders.CONTENT_TYPE,
              mediaTypes.size() == 1 ?
                      mediaTypes.iterator().next().toString() :
                      mediaTypes.toString());
    }

    @Override
    public String toString() {
      return String.format("Content-Type: %s",
              (mediaTypes.size() == 1) ?
                      mediaTypes.iterator().next().toString() :
                      mediaTypes.toString());
    }
  }

  private static class AcceptPredicate extends HeadersPredicate {

    private final Set<MediaType> mediaTypes;

    public AcceptPredicate(MediaType... mediaTypes) {
      this(Set.of(mediaTypes));
    }

    private AcceptPredicate(Set<MediaType> mediaTypes) {
      super(headers -> {
        List<MediaType> acceptedMediaTypes = acceptedMediaTypes(headers);
        boolean match = acceptedMediaTypes.stream()
                .anyMatch(acceptedMediaType -> mediaTypes.stream()
                        .anyMatch(acceptedMediaType::isCompatibleWith));
        traceMatch("Accept", mediaTypes, acceptedMediaTypes, match);
        return match;
      });
      this.mediaTypes = mediaTypes;
    }

    @NonNull
    private static List<MediaType> acceptedMediaTypes(ServerRequest.Headers headers) {
      List<MediaType> acceptedMediaTypes = headers.accept();
      if (acceptedMediaTypes.isEmpty()) {
        acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
      }
      else {
        MimeTypeUtils.sortBySpecificity(acceptedMediaTypes);
      }
      return acceptedMediaTypes;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.header(HttpHeaders.ACCEPT, (mediaTypes.size() == 1) ?
              mediaTypes.iterator().next().toString() :
              mediaTypes.toString());
    }

    @Override
    public String toString() {
      return String.format("Accept: %s", (mediaTypes.size() == 1) ?
              mediaTypes.iterator().next().toString() :
              mediaTypes.toString());
    }
  }

  private static class PathExtensionPredicate implements RequestPredicate {

    private final Predicate<String> extensionPredicate;

    @Nullable
    private final String extension;

    public PathExtensionPredicate(Predicate<String> extensionPredicate) {
      Assert.notNull(extensionPredicate, "Predicate is required");
      this.extensionPredicate = extensionPredicate;
      this.extension = null;
    }

    public PathExtensionPredicate(String extension) {
      Assert.notNull(extension, "Extension is required");

      this.extensionPredicate = s -> {
        boolean match = extension.equalsIgnoreCase(s);
        traceMatch("Extension", extension, s, match);
        return match;
      };
      this.extension = extension;
    }

    @Override
    public boolean test(ServerRequest request) {
      String pathExtension = UriUtils.extractFileExtension(request.path());
      return extensionPredicate.test(pathExtension);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.pathExtension(extension != null ? extension : extensionPredicate.toString());
    }

    @Override
    public String toString() {
      return String.format("*.%s", extension != null ? extension : extensionPredicate);
    }
  }

  private static class ParamPredicate implements RequestPredicate {

    private final String name;

    private final Predicate<String> valuePredicate;

    @Nullable
    private final String value;

    public ParamPredicate(String name, Predicate<String> valuePredicate) {
      Assert.notNull(name, "Name is required");
      Assert.notNull(valuePredicate, "Predicate is required");
      this.name = name;
      this.valuePredicate = valuePredicate;
      this.value = null;
    }

    public ParamPredicate(String name, String value) {
      Assert.notNull(name, "Name is required");
      Assert.notNull(value, "Value is required");
      this.name = name;
      this.valuePredicate = value::equals;
      this.value = value;
    }

    @Override
    public boolean test(ServerRequest request) {
      return request.param(name)
              .filter(valuePredicate)
              .isPresent();
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.param(name, value != null ? value : valuePredicate.toString());
    }

    @Override
    public String toString() {
      return String.format("?%s %s", name, value != null ? value : valuePredicate);
    }
  }

  /**
   * {@link RequestPredicate} for where both {@code left} and {@code right} predicates
   * must match.
   */
  static class AndRequestPredicate extends RequestModifyingPredicate
          implements ChangePathPatternParserVisitor.Target {

    private final RequestPredicate left;

    private final RequestModifyingPredicate leftModifying;

    private final RequestPredicate right;

    private final RequestModifyingPredicate rightModifying;

    public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
      Assert.notNull(left, "Left RequestPredicate is required");
      Assert.notNull(right, "Right RequestPredicate is required");
      this.left = left;
      this.right = right;
      this.leftModifying = of(left);
      this.rightModifying = of(right);
    }

    @Override
    protected Result testInternal(ServerRequest request) {
      Result leftResult = this.leftModifying.testInternal(request);
      if (!leftResult.value()) {
        return leftResult;
      }
      Result rightResult = this.rightModifying.testInternal(request);
      if (!rightResult.value()) {
        return rightResult;
      }
      return Result.of(true, serverRequest -> {
        leftResult.modify(serverRequest);
        rightResult.modify(serverRequest);
      });
    }

    @Override
    public Optional<ServerRequest> nest(ServerRequest request) {
      return this.left.nest(request).flatMap(this.right::nest);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.startAnd();
      this.left.accept(visitor);
      visitor.and();
      this.right.accept(visitor);
      visitor.endAnd();
    }

    @Override
    public void changeParser(PathPatternParser parser) {
      if (this.left instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
      if (this.right instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
    }

    @Override
    public String toString() {
      return String.format("(%s && %s)", this.left, this.right);
    }
  }

  /**
   * {@link RequestPredicate} that negates a delegate predicate.
   */
  static class NegateRequestPredicate extends RequestModifyingPredicate
          implements ChangePathPatternParserVisitor.Target {

    private final RequestPredicate delegate;

    private final RequestModifyingPredicate delegateModifying;

    public NegateRequestPredicate(RequestPredicate delegate) {
      Assert.notNull(delegate, "Delegate is required");
      this.delegate = delegate;
      this.delegateModifying = of(delegate);
    }

    @Override
    protected Result testInternal(ServerRequest request) {
      Result result = this.delegateModifying.testInternal(request);
      return Result.of(!result.value(), result::modify);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.startNegate();
      this.delegate.accept(visitor);
      visitor.endNegate();
    }

    @Override
    public void changeParser(PathPatternParser parser) {
      if (this.delegate instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
    }

    @Override
    public String toString() {
      return "!" + this.delegate;
    }
  }

  /**
   * {@link RequestPredicate} where either {@code left} or {@code right} predicates
   * may match.
   */
  static class OrRequestPredicate extends RequestModifyingPredicate
          implements ChangePathPatternParserVisitor.Target {

    private final RequestPredicate left;

    private final RequestModifyingPredicate leftModifying;

    private final RequestPredicate right;

    private final RequestModifyingPredicate rightModifying;

    public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
      Assert.notNull(left, "Left RequestPredicate is required");
      Assert.notNull(right, "Right RequestPredicate is required");
      this.left = left;
      this.leftModifying = of(left);
      this.right = right;
      this.rightModifying = of(right);
    }

    @Override
    protected Result testInternal(ServerRequest request) {
      Result leftResult = this.leftModifying.testInternal(request);
      if (leftResult.value()) {
        return leftResult;
      }
      else {
        return this.rightModifying.testInternal(request);
      }
    }

    @Override
    public Optional<ServerRequest> nest(ServerRequest request) {
      Optional<ServerRequest> leftResult = this.left.nest(request);
      if (leftResult.isPresent()) {
        return leftResult;
      }
      else {
        return this.right.nest(request);
      }
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.startOr();
      this.left.accept(visitor);
      visitor.or();
      this.right.accept(visitor);
      visitor.endOr();
    }

    @Override
    public void changeParser(PathPatternParser parser) {
      if (this.left instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
      if (this.right instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
    }

    @Override
    public String toString() {
      return String.format("(%s || %s)", this.left, this.right);
    }
  }

  private static class ApiVersionPredicate implements RequestPredicate {

    private final String version;

    private final boolean baselineVersion;

    @Nullable
    private Comparable<?> parsedVersion;

    public ApiVersionPredicate(Object version) {
      if (version instanceof String s) {
        this.baselineVersion = s.endsWith("+");
        this.version = initVersion(s, this.baselineVersion);
      }
      else {
        this.baselineVersion = false;
        this.version = version.toString();
        this.parsedVersion = (Comparable<?>) version;
      }
    }

    private static String initVersion(String version, boolean baselineVersion) {
      return (baselineVersion ? version.substring(0, version.length() - 1) : version);
    }

    @Override
    public boolean test(ServerRequest request) {
      if (this.parsedVersion == null) {
        ApiVersionStrategy strategy = request.apiVersionStrategy();
        Assert.state(strategy != null, "No ApiVersionStrategy to parse version with");
        this.parsedVersion = strategy.parseVersion(this.version);
      }

      Comparable<?> requestVersion = (Comparable<?>) request.attribute(HandlerMapping.API_VERSION_ATTRIBUTE);
      if (requestVersion == null) {
        traceMatch("Version", this.version, null, true);
        return true;
      }

      int result = compareVersions(this.parsedVersion, requestVersion);
      boolean match = (this.baselineVersion ? result <= 0 : result == 0);
      traceMatch("Version", this.version, requestVersion, match);
      return match;
    }

    @SuppressWarnings("unchecked")
    private <V extends Comparable<V>> int compareVersions(Object v1, Object v2) {
      return ((V) v1).compareTo((V) v2);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.version(this.version + (this.baselineVersion ? "+" : ""));
    }

    @Override
    public String toString() {
      return this.version;
    }
  }

  private static class SubPathServerRequestWrapper implements ServerRequest {

    private final ServerRequest request;

    private final RequestPath requestPath;

    private final Map<String, Object> attributes;

    public SubPathServerRequestWrapper(ServerRequest request,
            PathPattern.PathRemainingMatchInfo info, PathPattern pattern) {
      this.request = request;
      this.requestPath = requestPath(request.requestPath(), info);
      this.attributes = mergeAttributes(request, info.getUriVariables(), pattern);
    }

    private static RequestPath requestPath(RequestPath original, PathPattern.PathRemainingMatchInfo info) {
      StringBuilder contextPath = new StringBuilder(original.contextPath().value());
      contextPath.append(info.pathMatched.value());
      int length = contextPath.length();
      if (length > 0 && contextPath.charAt(length - 1) == '/') {
        contextPath.setLength(length - 1);
      }
      return original.modifyContextPath(contextPath.toString());
    }

    private static Map<String, Object> mergeAttributes(ServerRequest request,
            Map<String, String> pathVariables, PathPattern pattern) {
      ConcurrentHashMap<String, Object> result = new ConcurrentHashMap<>(request.attributes());

      result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              mergePathVariables(request.pathVariables(), pathVariables));

      pattern = mergePatterns(
              (PathPattern) request.attribute(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE), pattern);
      result.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
      return result;
    }

    @Override
    public HttpMethod method() {
      return this.request.method();
    }

    @Override
    public String methodName() {
      return this.request.methodName();
    }

    @Override
    public URI uri() {
      return this.request.uri();
    }

    @Override
    public UriBuilder uriBuilder() {
      return this.request.uriBuilder();
    }

    @Override
    public RequestPath requestPath() {
      return this.requestPath;
    }

    @Override
    public Headers headers() {
      return this.request.headers();
    }

    @Override
    public MultiValueMap<String, HttpCookie> cookies() {
      return request.cookies();
    }

    @Override
    public InetSocketAddress remoteAddress() {
      return request.remoteAddress();
    }

    @Override
    public List<HttpMessageConverter<?>> messageConverters() {
      return request.messageConverters();
    }

    @Nullable
    @Override
    public ApiVersionStrategy apiVersionStrategy() {
      return request.apiVersionStrategy();
    }

    @Override
    public <T> T body(Class<T> bodyType) throws IOException {
      return request.body(bodyType);
    }

    @Override
    public <T> T body(ParameterizedTypeReference<T> bodyType) throws IOException {
      return request.body(bodyType);
    }

    @Override
    public <T> T bind(Class<T> bindType) throws BindException {
      return request.bind(bindType);
    }

    @Override
    public <T> T bind(Class<T> bindType, Consumer<WebDataBinder> dataBinderCustomizer) throws BindException {
      return request.bind(bindType, dataBinderCustomizer);
    }

    @Nullable
    @Override
    public Object attribute(String name) {
      return request.attribute(name);
    }

    @Override
    public Map<String, Object> attributes() {
      return attributes;
    }

    @Override
    public Optional<String> param(String name) {
      return request.param(name);
    }

    @Override
    public List<String> params(String name) {
      return request.params(name);
    }

    @Override
    public MultiValueMap<String, String> params() {
      return request.params();
    }

    @Override
    public MultiValueMap<String, Multipart> multipartData() throws IOException {
      return request.multipartData();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> pathVariables() {
      return (Map<String, String>) attributes.getOrDefault(
              RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
    }

    @Override
    public RequestContext exchange() {
      return request.exchange();
    }

    @Override
    public Optional<ServerResponse> checkNotModified(Instant lastModified) {
      return request.checkNotModified(lastModified);
    }

    @Override
    public Optional<ServerResponse> checkNotModified(String etag) {
      return request.checkNotModified(etag);
    }

    @Override
    public Optional<ServerResponse> checkNotModified(Instant lastModified, String etag) {
      return request.checkNotModified(lastModified, etag);
    }

    @Override
    public String toString() {
      return method() + " " + path();
    }
  }

}
