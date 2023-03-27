/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.handler.function;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import cn.taketoday.core.TypeReference;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.util.UriBuilder;
import cn.taketoday.web.util.UriUtils;
import cn.taketoday.web.util.pattern.PathMatchInfo;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Implementations of {@link RequestPredicate} that implement various useful
 * request matching operations, such as matching based on path, HTTP method, etc.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
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
    if (!pattern.isEmpty() && !pattern.startsWith("/")) {
      pattern = "/" + pattern;
    }
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

  private static void restoreAttributes(ServerRequest request, Map<String, Object> attributes) {
    request.attributes().clear();
    request.attributes().putAll(attributes);
  }

  private static Map<String, String> mergePathVariables(Map<String, String> oldVariables,
          Map<String, String> newVariables) {

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

  private static class HttpMethodPredicate implements RequestPredicate {

    private final Set<HttpMethod> httpMethods;

    public HttpMethodPredicate(HttpMethod httpMethod) {
      Assert.notNull(httpMethod, "HttpMethod is required");
      this.httpMethods = Set.of(httpMethod);
    }

    public HttpMethodPredicate(HttpMethod... httpMethods) {
      Assert.notEmpty(httpMethods, "HttpMethods must not be empty");
      this.httpMethods = Set.of(httpMethods);
    }

    @Override
    public boolean test(ServerRequest request) {
      HttpMethod method = method(request);
      boolean match = httpMethods.contains(method);
      traceMatch("Method", httpMethods, method, match);
      return match;
    }

    private static HttpMethod method(ServerRequest request) {
      if (request.requestContext().isPreFlightRequest()) {
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
      visitor.method(httpMethods);
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

  private static class PathPatternPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

    private PathPattern pattern;

    public PathPatternPredicate(PathPattern pattern) {
      Assert.notNull(pattern, "'pattern' is required");
      this.pattern = pattern;
    }

    @Override
    public boolean test(ServerRequest request) {
      PathContainer lookupPath = request.requestPath().pathWithinApplication();
      PathMatchInfo info = pattern.matchAndExtract(lookupPath);
      traceMatch("Pattern", pattern.getPatternString(), lookupPath.value(), info != null);
      if (info != null) {
        mergeAttributes(request, info.getUriVariables(), pattern);
        return true;
      }
      else {
        return false;
      }
    }

    private static void mergeAttributes(ServerRequest request,
            Map<String, String> variables, PathPattern pattern) {
      Map<String, String> pathVariables = mergePathVariables(request.pathVariables(), variables);
      request.attributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              Collections.unmodifiableMap(pathVariables));

      pattern = mergePatterns(
              (PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
              pattern);
      request.attributes().put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
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
      if (request.requestContext().isPreFlightRequest()) {
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
        MediaType contentType =
                headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
        boolean match = mediaTypes.stream()
                .anyMatch(mediaType -> mediaType.includes(contentType));
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
      visitor.header(HttpHeaders.ACCEPT,
              (mediaTypes.size() == 1) ?
              mediaTypes.iterator().next().toString() :
              mediaTypes.toString());
    }

    @Override
    public String toString() {
      return String.format("Accept: %s",
              (mediaTypes.size() == 1) ?
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
      visitor.pathExtension(
              (extension != null) ?
              extension :
              extensionPredicate.toString());
    }

    @Override
    public String toString() {
      return String.format("*.%s",
              (extension != null) ?
              extension :
              extensionPredicate);
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
      visitor.param(name,
              (value != null) ?
              value :
              valuePredicate.toString());
    }

    @Override
    public String toString() {
      return String.format("?%s %s", name,
              (value != null) ?
              value :
              valuePredicate);
    }
  }

  /**
   * {@link RequestPredicate} for where both {@code left} and {@code right} predicates
   * must match.
   */
  static class AndRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

    private final RequestPredicate left;

    private final RequestPredicate right;

    public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
      Assert.notNull(left, "Left RequestPredicate is required");
      Assert.notNull(right, "Right RequestPredicate is required");
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean test(ServerRequest request) {
      Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

      if (left.test(request) && right.test(request)) {
        return true;
      }
      restoreAttributes(request, oldAttributes);
      return false;
    }

    @Override
    public Optional<ServerRequest> nest(ServerRequest request) {
      return left.nest(request).flatMap(right::nest);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.startAnd();
      left.accept(visitor);
      visitor.and();
      right.accept(visitor);
      visitor.endAnd();
    }

    @Override
    public void changeParser(PathPatternParser parser) {
      if (left instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
      if (right instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
    }

    @Override
    public String toString() {
      return String.format("(%s && %s)", left, right);
    }
  }

  /**
   * {@link RequestPredicate} that negates a delegate predicate.
   */
  static class NegateRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

    private final RequestPredicate delegate;

    public NegateRequestPredicate(RequestPredicate delegate) {
      Assert.notNull(delegate, "Delegate is required");
      this.delegate = delegate;
    }

    @Override
    public boolean test(ServerRequest request) {
      Map<String, Object> oldAttributes = new HashMap<>(request.attributes());
      boolean result = !delegate.test(request);
      if (!result) {
        restoreAttributes(request, oldAttributes);
      }
      return result;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.startNegate();
      delegate.accept(visitor);
      visitor.endNegate();
    }

    @Override
    public void changeParser(PathPatternParser parser) {
      if (delegate instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
    }

    @Override
    public String toString() {
      return "!" + delegate;
    }
  }

  /**
   * {@link RequestPredicate} where either {@code left} or {@code right} predicates
   * may match.
   */
  static class OrRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

    private final RequestPredicate left;

    private final RequestPredicate right;

    public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
      Assert.notNull(left, "Left RequestPredicate is required");
      Assert.notNull(right, "Right RequestPredicate is required");
      this.left = left;
      this.right = right;
    }

    @Override
    public boolean test(ServerRequest request) {
      Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

      if (left.test(request)) {
        return true;
      }
      else {
        restoreAttributes(request, oldAttributes);
        if (right.test(request)) {
          return true;
        }
      }
      restoreAttributes(request, oldAttributes);
      return false;
    }

    @Override
    public Optional<ServerRequest> nest(ServerRequest request) {
      Optional<ServerRequest> leftResult = left.nest(request);
      if (leftResult.isPresent()) {
        return leftResult;
      }
      else {
        return right.nest(request);
      }
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.startOr();
      left.accept(visitor);
      visitor.or();
      right.accept(visitor);
      visitor.endOr();
    }

    @Override
    public void changeParser(PathPatternParser parser) {
      if (left instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
      if (right instanceof ChangePathPatternParserVisitor.Target target) {
        target.changeParser(parser);
      }
    }

    @Override
    public String toString() {
      return String.format("(%s || %s)", left, right);
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
      contextPath.append(info.getPathMatched().value());
      int length = contextPath.length();
      if (length > 0 && contextPath.charAt(length - 1) == '/') {
        contextPath.setLength(length - 1);
      }
      return original.modifyContextPath(contextPath.toString());
    }

    private static Map<String, Object> mergeAttributes(
            ServerRequest request, Map<String, String> pathVariables, PathPattern pattern) {

      var result = new ConcurrentHashMap<>(request.attributes());
      result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
              mergePathVariables(request.pathVariables(), pathVariables));

      pattern = mergePatterns(
              (PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
              pattern);
      result.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
      return result;
    }

    @Override
    public HttpMethod method() {
      return request.method();
    }

    @Override
    public String methodName() {
      return request.methodName();
    }

    @Override
    public URI uri() {
      return request.uri();
    }

    @Override
    public UriBuilder uriBuilder() {
      return request.uriBuilder();
    }

    @Override
    public RequestPath requestPath() {
      return requestPath;
    }

    @Override
    public Headers headers() {
      return request.headers();
    }

    @Override
    public MultiValueMap<String, HttpCookie> cookies() {
      return request.cookies();
    }

    @Override
    public Optional<InetSocketAddress> remoteAddress() {
      return request.remoteAddress();
    }

    @Override
    public List<HttpMessageConverter<?>> messageConverters() {
      return request.messageConverters();
    }

    @Override
    public <T> T body(Class<T> bodyType) throws IOException {
      return request.body(bodyType);
    }

    @Override
    public <T> T body(TypeReference<T> bodyType) throws IOException {
      return request.body(bodyType);
    }

    @Override
    public Optional<Object> attribute(String name) {
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
    public RequestContext requestContext() {
      return request.requestContext();
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
