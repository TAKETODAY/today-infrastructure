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

package infra.web.handler.method;

import org.jspecify.annotations.Nullable;

import java.util.Set;

import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.ContentNegotiationManager;
import infra.web.handler.condition.ConsumesRequestCondition;
import infra.web.handler.condition.HeadersRequestCondition;
import infra.web.handler.condition.ParamsRequestCondition;
import infra.web.handler.condition.PathPatternsRequestCondition;
import infra.web.handler.condition.ProducesRequestCondition;
import infra.web.handler.condition.RequestCondition;
import infra.web.handler.condition.RequestConditionHolder;
import infra.web.handler.condition.RequestMethodsRequestCondition;
import infra.web.handler.condition.VersionRequestCondition;
import infra.web.util.pattern.PathPatternParser;

/**
 * Request mapping information. A composite for the following conditions:
 * <ol>
 * <li>{@link PathPatternsRequestCondition} with parsed {@code PathPatterns}
 * <li>{@link RequestMethodsRequestCondition}
 * <li>{@link ParamsRequestCondition}
 * <li>{@link HeadersRequestCondition}
 * <li>{@link ConsumesRequestCondition}
 * <li>{@link ProducesRequestCondition}
 * <li>{@code RequestCondition} (optional, custom request condition)
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/1 21:41
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

  private static final ParamsRequestCondition EMPTY_PARAMS = new ParamsRequestCondition();
  private static final HeadersRequestCondition EMPTY_HEADERS = new HeadersRequestCondition();
  private static final RequestConditionHolder EMPTY_CUSTOM = new RequestConditionHolder(null);
  private static final ConsumesRequestCondition EMPTY_CONSUMES = new ConsumesRequestCondition();
  private static final ProducesRequestCondition EMPTY_PRODUCES = new ProducesRequestCondition();
  private static final PathPatternsRequestCondition EMPTY_PATH_PATTERNS = new PathPatternsRequestCondition();
  private static final RequestMethodsRequestCondition EMPTY_REQUEST_METHODS = new RequestMethodsRequestCondition();

  private static final VersionRequestCondition EMPTY_VERSION = new VersionRequestCondition(null, null);

  private final @Nullable String name;

  private final RequestMethodsRequestCondition methodsCondition;

  private final PathPatternsRequestCondition pathPatternsCondition;

  private final ParamsRequestCondition paramsCondition;

  private final HeadersRequestCondition headersCondition;

  private final ConsumesRequestCondition consumesCondition;

  private final ProducesRequestCondition producesCondition;

  private final VersionRequestCondition versionCondition;

  private final RequestConditionHolder customConditionHolder;

  private @Nullable Integer hashCode;

  private final BuilderConfiguration options;

  private final boolean combine;

  RequestMappingInfo(@Nullable String name,
          PathPatternsRequestCondition pathPatterns,
          RequestMethodsRequestCondition methodsCondition,
          ParamsRequestCondition paramsCondition,
          HeadersRequestCondition headersCondition,
          ConsumesRequestCondition consumesCondition,
          ProducesRequestCondition producesCondition,
          VersionRequestCondition versionCondition,
          RequestConditionHolder customCondition,
          BuilderConfiguration options, boolean combine) {

    this.name = StringUtils.hasText(name) ? name : null;
    this.options = options;
    this.paramsCondition = paramsCondition;
    this.methodsCondition = methodsCondition;
    this.headersCondition = headersCondition;
    this.consumesCondition = consumesCondition;
    this.producesCondition = producesCondition;
    this.versionCondition = versionCondition;
    this.customConditionHolder = customCondition;
    this.pathPatternsCondition = pathPatterns;
    this.combine = combine;
  }

  /**
   * Return the name for this mapping, or {@code null}.
   */
  public @Nullable String getName() {
    return this.name;
  }

  /**
   * Return the patterns condition
   *
   * @see #getPathPatternsCondition()
   */
  public PathPatternsRequestCondition getPathPatternsCondition() {
    return this.pathPatternsCondition;
  }

  /**
   * Return the mapping paths that are not patterns.
   */
  public Set<String> getDirectPaths() {
    return pathPatternsCondition.getDirectPaths();
  }

  /**
   * Return the patterns for the {@link #getPathPatternsCondition() active}
   * patterns condition as Strings.
   */
  public Set<String> getPatternValues() {
    return pathPatternsCondition.getPatternValues();
  }

  /**
   * Return the HTTP request methods of this {@link RequestMappingInfo};
   * or instance with 0 request methods (never {@code null}).
   */
  public RequestMethodsRequestCondition getMethodsCondition() {
    return this.methodsCondition;
  }

  /**
   * Return the "parameters" condition of this {@link RequestMappingInfo};
   * or instance with 0 parameter expressions (never {@code null}).
   */
  public ParamsRequestCondition getParamsCondition() {
    return this.paramsCondition;
  }

  /**
   * Return the "headers" condition of this {@link RequestMappingInfo};
   * or instance with 0 header expressions (never {@code null}).
   */
  public HeadersRequestCondition getHeadersCondition() {
    return this.headersCondition;
  }

  /**
   * Return the "consumes" condition of this {@link RequestMappingInfo};
   * or instance with 0 consumes expressions (never {@code null}).
   */
  public ConsumesRequestCondition getConsumesCondition() {
    return this.consumesCondition;
  }

  /**
   * Return the "produces" condition of this {@link RequestMappingInfo};
   * or instance with 0 produces expressions (never {@code null}).
   */
  public ProducesRequestCondition getProducesCondition() {
    return this.producesCondition;
  }

  /**
   * Return the version condition of this {@link RequestMappingInfo},
   * or an instance without a version.
   *
   * @since 5.0
   */
  public VersionRequestCondition getVersionCondition() {
    return this.versionCondition;
  }

  /**
   * Return the "custom" condition of this {@link RequestMappingInfo}, or {@code null}.
   */
  @Nullable
  public RequestCondition<?> getCustomCondition() {
    return this.customConditionHolder.getCondition();
  }

  public boolean isCombine() {
    return combine;
  }

  /**
   * Create a new instance based on the current one, also adding the given
   * custom condition.
   *
   * @param customCondition the custom condition to add
   */
  public RequestMappingInfo addCustomCondition(RequestCondition<?> customCondition) {
    return new RequestMappingInfo(this.name,
            this.pathPatternsCondition,
            this.methodsCondition, this.paramsCondition, this.headersCondition,
            this.consumesCondition, this.producesCondition, versionCondition,
            new RequestConditionHolder(customCondition), this.options, combine);
  }

  /**
   * Combine "this" request mapping info (i.e. the current instance) with
   * another request mapping info instance.
   * <p>Example: combine type- and method-level request mappings.
   *
   * @return a new request mapping info instance; never {@code null}
   */
  @Override
  public RequestMappingInfo combine(RequestMappingInfo other) {
    String name = combineNames(other);

    ParamsRequestCondition params = paramsCondition.combine(other.paramsCondition);
    RequestConditionHolder custom = customConditionHolder.combine(other.customConditionHolder);
    VersionRequestCondition version = versionCondition.combine(other.versionCondition);
    HeadersRequestCondition headers = headersCondition.combine(other.headersCondition);
    ConsumesRequestCondition consumes = consumesCondition.combine(other.consumesCondition);
    ProducesRequestCondition produces = producesCondition.combine(other.producesCondition);
    RequestMethodsRequestCondition methods = methodsCondition.combine(other.methodsCondition);
    PathPatternsRequestCondition pathPatterns = pathPatternsCondition.combine(other.pathPatternsCondition);

    return new RequestMappingInfo(name, pathPatterns,
            methods, params, headers, consumes, produces, version, custom, options, combine);
  }

  @Nullable
  private String combineNames(RequestMappingInfo other) {
    if (this.name != null && other.name != null) {
      String separator = RequestMappingInfoHandlerMethodMappingNamingStrategy.SEPARATOR;
      return this.name + separator + other.name;
    }
    else if (this.name != null) {
      return this.name;
    }
    else {
      return other.name;
    }
  }

  /**
   * Checks if all conditions in this request mapping info match the provided
   * request and returns a potentially new request mapping info with conditions
   * tailored to the current request.
   * <p>For example the returned instance may contain the subset of URL
   * patterns that match to the current request, sorted with best matching
   * patterns on top.
   *
   * @return a new instance in case of a match; or {@code null} otherwise
   */
  @Override
  public @Nullable RequestMappingInfo getMatchingCondition(RequestContext request) {
    RequestMethodsRequestCondition methods = methodsCondition.getMatchingCondition(request);
    if (methods == null) {
      return null;
    }
    ParamsRequestCondition params = paramsCondition.getMatchingCondition(request);
    if (params == null) {
      return null;
    }
    HeadersRequestCondition headers = headersCondition.getMatchingCondition(request);
    if (headers == null) {
      return null;
    }
    ConsumesRequestCondition consumes = consumesCondition.getMatchingCondition(request);
    if (consumes == null) {
      return null;
    }
    ProducesRequestCondition produces = producesCondition.getMatchingCondition(request);
    if (produces == null) {
      return null;
    }
    VersionRequestCondition version = this.versionCondition.getMatchingCondition(request);
    if (version == null) {
      return null;
    }
    PathPatternsRequestCondition pathPatterns = pathPatternsCondition.getMatchingCondition(request);
    if (pathPatterns == null) {
      return null;
    }

    RequestConditionHolder custom = customConditionHolder.getMatchingCondition(request);
    if (custom == null) {
      return null;
    }
    return new RequestMappingInfo(name, pathPatterns, methods, params,
            headers, consumes, produces, version, custom, options, combine);
  }

  /**
   * Compares "this" info (i.e. the current instance) with another info in the
   * context of a request.
   * <p>Note: It is assumed both instances have been obtained via
   * {@link #getMatchingCondition(RequestContext)} to ensure they have
   * conditions with content relevant to current request.
   */
  @Override
  public int compareTo(RequestMappingInfo other, RequestContext request) {
    int result;
    // Automatic vs explicit HTTP HEAD mapping
    if (HttpMethod.HEAD == request.getMethod()) {
      result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
      if (result != 0) {
        return result;
      }
    }
    result = pathPatternsCondition.compareTo(other.getPathPatternsCondition(), request);
    if (result != 0) {
      return result;
    }
    result = this.paramsCondition.compareTo(other.getParamsCondition(), request);
    if (result != 0) {
      return result;
    }
    result = this.headersCondition.compareTo(other.getHeadersCondition(), request);
    if (result != 0) {
      return result;
    }
    result = this.consumesCondition.compareTo(other.getConsumesCondition(), request);
    if (result != 0) {
      return result;
    }
    result = this.producesCondition.compareTo(other.getProducesCondition(), request);
    if (result != 0) {
      return result;
    }
    result = this.versionCondition.compareTo(other.getVersionCondition(), request);
    if (result != 0) {
      return result;
    }
    // Implicit (no method) vs explicit HTTP method mappings
    result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
    if (result != 0) {
      return result;
    }
    result = this.customConditionHolder.compareTo(other.customConditionHolder, request);
    if (result != 0) {
      return result;
    }
    return 0;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof RequestMappingInfo otherInfo) {
      return combine == otherInfo.combine && (
              pathPatternsCondition.equals(otherInfo.pathPatternsCondition)
                      && methodsCondition.equals(otherInfo.methodsCondition)
                      && paramsCondition.equals(otherInfo.paramsCondition)
                      && headersCondition.equals(otherInfo.headersCondition)
                      && consumesCondition.equals(otherInfo.consumesCondition)
                      && producesCondition.equals(otherInfo.producesCondition)
                      && versionCondition.equals(otherInfo.versionCondition)
                      && customConditionHolder.equals(otherInfo.customConditionHolder)
      );
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (hashCode == null) {
      hashCode = calculateHashCode();
    }
    return hashCode;
  }

  private int calculateHashCode() {
    return pathPatternsCondition.hashCode() * 31 +
            this.methodsCondition.hashCode() +
            this.paramsCondition.hashCode() + this.headersCondition.hashCode() +
            this.consumesCondition.hashCode() + this.producesCondition.hashCode() +
            this.versionCondition.hashCode() + this.customConditionHolder.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("{");
    if (!this.methodsCondition.isEmpty()) {
      Set<HttpMethod> httpMethods = this.methodsCondition.getMethods();
      builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
    }

    // Patterns conditions are never empty and have "" (empty path) at least.
    builder.append(' ').append(getPathPatternsCondition());

    if (!this.paramsCondition.isEmpty()) {
      builder.append(", params ").append(this.paramsCondition);
    }
    if (!this.headersCondition.isEmpty()) {
      builder.append(", headers ").append(this.headersCondition);
    }
    if (!this.consumesCondition.isEmpty()) {
      builder.append(", consumes ").append(this.consumesCondition);
    }
    if (!this.producesCondition.isEmpty()) {
      builder.append(", produces ").append(this.producesCondition);
    }
    if (!this.versionCondition.isEmpty()) {
      builder.append(", version ").append(this.versionCondition);
    }
    if (!this.customConditionHolder.isEmpty()) {
      builder.append(", and ").append(this.customConditionHolder);
    }
    builder.append('}');
    return builder.toString();
  }

  /**
   * Return a builder to create a new RequestMappingInfo by modifying this one.
   *
   * @return a builder to create a new, modified instance
   */
  public Builder mutate() {
    return new MutateBuilder(this);
  }

  /**
   * Create a new {@code RequestMappingInfo.Builder} with the given paths.
   *
   * @param paths the paths to use
   */
  public static Builder paths(String... paths) {
    return new DefaultBuilder(paths);
  }

  /**
   * Defines a builder for creating a RequestMappingInfo.
   */
  public interface Builder {

    /**
     * Set the URL path patterns.
     */
    Builder paths(String... paths);

    /**
     * Set the request method conditions.
     */
    Builder methods(HttpMethod... methods);

    /**
     * Set the request param conditions.
     */
    Builder params(String... params);

    /**
     * Set the header conditions.
     * <p>By default this is not set.
     */
    Builder headers(String... headers);

    /**
     * Set the consumes conditions.
     */
    Builder consumes(String... consumes);

    /**
     * Set the produces conditions.
     */
    Builder produces(String... produces);

    /**
     * Set the API version condition.
     *
     * @since 5.0
     */
    Builder version(String version);

    /**
     * Set the mapping name.
     */
    Builder mappingName(String name);

    /**
     * Set a custom condition to use.
     */
    Builder customCondition(RequestCondition<?> condition);

    /**
     * Provide additional configuration needed for request mapping purposes.
     */
    Builder options(BuilderConfiguration options);

    /**
     * combine class level
     */
    Builder combine(boolean combine);

    /**
     * Build the RequestMappingInfo.
     */
    RequestMappingInfo build();
  }

  private static class DefaultBuilder implements Builder {

    private String[] paths;
    private HttpMethod[] methods = new HttpMethod[0];

    private String[] params = Constant.EMPTY_STRING_ARRAY;
    private String[] headers = Constant.EMPTY_STRING_ARRAY;
    private String[] consumes = Constant.EMPTY_STRING_ARRAY;
    private String[] produces = Constant.EMPTY_STRING_ARRAY;

    @Nullable
    private String version;

    private boolean hasAccept;
    private boolean hasContentType;

    @Nullable
    private String mappingName;

    @Nullable
    private RequestCondition<?> customCondition;

    private BuilderConfiguration options = new BuilderConfiguration();

    private boolean combine = true;

    public DefaultBuilder(String... paths) {
      this.paths = paths;
    }

    @Override
    public Builder paths(String... paths) {
      this.paths = paths;
      return this;
    }

    @Override
    public DefaultBuilder methods(HttpMethod... methods) {
      this.methods = methods;
      return this;
    }

    @Override
    public DefaultBuilder params(String... params) {
      this.params = params;
      return this;
    }

    @Override
    public DefaultBuilder headers(String... headers) {
      for (String header : headers) {
        this.hasAccept = hasAccept || header.contains("Accept") || header.contains("accept");
        this.hasContentType = hasContentType || header.contains("Content-Type") || header.contains("content-type");
      }
      this.headers = headers;
      return this;
    }

    @Override
    public DefaultBuilder consumes(String... consumes) {
      this.consumes = consumes;
      return this;
    }

    @Override
    public DefaultBuilder produces(String... produces) {
      this.produces = produces;
      return this;
    }

    @Override
    public Builder version(String version) {
      this.version = version;
      return this;
    }

    @Override
    public DefaultBuilder mappingName(String name) {
      this.mappingName = name;
      return this;
    }

    @Override
    public DefaultBuilder customCondition(RequestCondition<?> condition) {
      this.customCondition = condition;
      return this;
    }

    @Override
    public Builder options(BuilderConfiguration options) {
      this.options = options;
      return this;
    }

    @Override
    public Builder combine(boolean combine) {
      this.combine = combine;
      return this;
    }

    @Override
    public RequestMappingInfo build() {
      VersionRequestCondition versionCondition;
      ApiVersionStrategy versionStrategy = this.options.getApiVersionStrategy();
      if (StringUtils.hasText(this.version)) {
        Assert.state(versionStrategy != null, "API version specified, but no ApiVersionStrategy configured");
        versionCondition = new VersionRequestCondition(this.version, versionStrategy);
      }
      else {
        versionCondition = EMPTY_VERSION;
      }

      return new RequestMappingInfo(mappingName,
              ObjectUtils.isEmpty(paths) ? EMPTY_PATH_PATTERNS : new PathPatternsRequestCondition(options.getPatternParser(), paths),
              ObjectUtils.isEmpty(methods) ? EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(methods),
              ObjectUtils.isEmpty(params) ? EMPTY_PARAMS : new ParamsRequestCondition(params),
              ObjectUtils.isEmpty(headers) ? EMPTY_HEADERS : new HeadersRequestCondition(headers),
              ObjectUtils.isEmpty(consumes) && !hasContentType ? EMPTY_CONSUMES : new ConsumesRequestCondition(consumes, headers),
              ObjectUtils.isEmpty(produces) && !hasAccept ? EMPTY_PRODUCES : new ProducesRequestCondition(produces, headers, options.getContentNegotiationManager()),
              versionCondition, customCondition == null ? EMPTY_CUSTOM : new RequestConditionHolder(customCondition),
              options, combine);
    }
  }

  private static class MutateBuilder implements Builder {

    @Nullable
    private String name;

    private PathPatternsRequestCondition pathPatternsCondition;

    private RequestMethodsRequestCondition methodsCondition;

    private ParamsRequestCondition paramsCondition;

    private HeadersRequestCondition headersCondition;

    private ConsumesRequestCondition consumesCondition;

    private ProducesRequestCondition producesCondition;

    private VersionRequestCondition versionCondition;

    private RequestConditionHolder customConditionHolder;

    private BuilderConfiguration options;

    private boolean combine;

    public MutateBuilder(RequestMappingInfo other) {
      this.name = other.name;
      this.pathPatternsCondition = other.pathPatternsCondition;
      this.methodsCondition = other.methodsCondition;
      this.paramsCondition = other.paramsCondition;
      this.headersCondition = other.headersCondition;
      this.consumesCondition = other.consumesCondition;
      this.producesCondition = other.producesCondition;
      this.customConditionHolder = other.customConditionHolder;
      this.options = other.options;
      this.versionCondition = other.versionCondition;
    }

    @Override
    public Builder paths(String... paths) {
      this.pathPatternsCondition
              = ObjectUtils.isEmpty(paths)
              ? EMPTY_PATH_PATTERNS
              : new PathPatternsRequestCondition(this.options.getPatternParser(), paths);
      return this;
    }

    @Override
    public Builder methods(HttpMethod... methods) {
      this.methodsCondition = ObjectUtils.isEmpty(methods) ?
              EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(methods);
      return this;
    }

    @Override
    public Builder params(String... params) {
      this.paramsCondition = ObjectUtils.isEmpty(params) ?
              EMPTY_PARAMS : new ParamsRequestCondition(params);
      return this;
    }

    @Override
    public Builder headers(String... headers) {
      this.headersCondition = ObjectUtils.isEmpty(headers) ?
              EMPTY_HEADERS : new HeadersRequestCondition(headers);
      return this;
    }

    @Override
    public Builder consumes(String... consumes) {
      this.consumesCondition = ObjectUtils.isEmpty(consumes) ?
              EMPTY_CONSUMES : new ConsumesRequestCondition(consumes);
      return this;
    }

    @Override
    public Builder produces(String... produces) {
      this.producesCondition = ObjectUtils.isEmpty(produces) ?
              EMPTY_PRODUCES :
              new ProducesRequestCondition(produces, null, this.options.getContentNegotiationManager());
      return this;
    }

    @Override
    public Builder version(@Nullable String version) {
      if (version != null) {
        ApiVersionStrategy strategy = this.options.getApiVersionStrategy();
        Assert.state(strategy != null, "API version specified, but no ApiVersionStrategy configured");
        this.versionCondition = new VersionRequestCondition(version, strategy);
      }
      else {
        this.versionCondition = EMPTY_VERSION;
      }
      return this;
    }

    @Override
    public Builder mappingName(String name) {
      this.name = name;
      return this;
    }

    @Override
    public Builder customCondition(RequestCondition<?> condition) {
      this.customConditionHolder = new RequestConditionHolder(condition);
      return this;
    }

    @Override
    public Builder options(BuilderConfiguration options) {
      this.options = options;
      return this;
    }

    @Override
    public Builder combine(boolean combine) {
      this.combine = combine;
      return this;
    }

    @Override
    public RequestMappingInfo build() {
      return new RequestMappingInfo(this.name,
              this.pathPatternsCondition,
              this.methodsCondition, this.paramsCondition, this.headersCondition,
              this.consumesCondition, this.producesCondition, versionCondition,
              this.customConditionHolder, this.options, combine);
    }
  }

  /**
   * Container for configuration options used for request mapping purposes.
   * Such configuration is required to create RequestMappingInfo instances but
   * is typically used across all RequestMappingInfo instances.
   *
   * @see Builder#combine
   */
  public static class BuilderConfiguration {

    private PathPatternParser patternParser = PathPatternParser.defaultInstance;

    @Nullable
    private ContentNegotiationManager contentNegotiationManager;

    @Nullable
    private ApiVersionStrategy apiVersionStrategy;

    public void setPatternParser(PathPatternParser patternParser) {
      this.patternParser = patternParser;
    }

    /**
     * Return the {@link #setPatternParser(PathPatternParser) configured}
     * {@code PathPatternParser}, or {@code null}.
     */
    public PathPatternParser getPatternParser() {
      return this.patternParser;
    }

    /**
     * Set the ContentNegotiationManager to use for the ProducesRequestCondition.
     * <p>By default this is not set.
     */
    public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
      this.contentNegotiationManager = contentNegotiationManager;
    }

    /**
     * Return the ContentNegotiationManager to use for the ProducesRequestCondition,
     * if any.
     */
    @Nullable
    public ContentNegotiationManager getContentNegotiationManager() {
      return this.contentNegotiationManager;
    }

    /**
     * Set the strategy for API versioning.
     *
     * @param apiVersionStrategy the strategy to use
     * @since 5.0
     */
    public void setApiVersionStrategy(@Nullable ApiVersionStrategy apiVersionStrategy) {
      this.apiVersionStrategy = apiVersionStrategy;
    }

    /**
     * Return the configured strategy for API versioning.
     *
     * @since 5.0
     */
    @Nullable
    public ApiVersionStrategy getApiVersionStrategy() {
      return this.apiVersionStrategy;
    }
  }

}
