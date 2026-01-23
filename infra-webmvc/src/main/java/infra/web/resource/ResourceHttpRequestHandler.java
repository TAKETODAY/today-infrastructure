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

package infra.web.resource;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import infra.beans.factory.InitializingBean;
import infra.context.ApplicationContext;
import infra.context.expression.EmbeddedValueResolverAware;
import infra.core.StringValueResolver;
import infra.core.io.Resource;
import infra.core.io.UrlResource;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRange;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.MediaTypeFactory;
import infra.http.converter.ResourceHttpMessageConverter;
import infra.http.converter.ResourceRegionHttpMessageConverter;
import infra.http.server.ServerHttpResponse;
import infra.lang.Assert;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.HttpRequestHandler;
import infra.web.NotFoundHandler;
import infra.web.RequestContext;
import infra.web.WebContentGenerator;
import infra.web.accept.ContentNegotiationManager;
import infra.web.cors.CorsConfiguration;
import infra.web.cors.CorsConfigurationSource;
import infra.web.handler.SimpleNotFoundHandler;

/**
 * {@code HttpRequestHandler} that serves static resources in an optimized way
 * according to the guidelines of Page Speed, YSlow, etc.
 *
 * <p>The properties {@linkplain #setLocations "locations"} and
 * {@linkplain #setLocationValues "locationValues"} accept locations from which
 * static resources can be served by this handler. This can be relative to the
 * root of the web application, or from the classpath, e.g.
 * "classpath:/META-INF/public-web-resources/", allowing convenient packaging
 * and serving of resources such as .js, .css, and others in jar files.
 *
 * <p>This request handler may also be configured with a
 * {@link #setResourceResolvers(List) resourcesResolver} and
 * {@link #setResourceTransformers(List) resourceTransformer} chains to support
 * arbitrary resolution and transformation of resources being served. By default
 * a {@link PathResourceResolver} simply finds resources based on the configured
 * "locations". An application can configure additional resolvers and transformers
 * such as the {@link VersionResourceResolver} which can resolve and prepare URLs
 * for resources with a version in the URL.
 *
 * <p>This handler also properly evaluates the {@code Last-Modified} header
 * (if present) so that a {@code 304} status code will be returned as appropriate,
 * avoiding unnecessary overhead for resources that are already cached by the client.
 *
 * @author Keith Donald
 * @author Jeremy Grelle
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ResourceHttpRequestHandler extends WebContentGenerator
        implements HttpRequestHandler, EmbeddedValueResolverAware, InitializingBean, CorsConfigurationSource {

  private static final String URL_RESOURCE_CHARSET_PREFIX = "[charset=";

  private final ArrayList<String> locationValues = new ArrayList<>(4);

  private final ArrayList<Resource> locationResources = new ArrayList<>(4);

  private final ArrayList<Resource> locationsToUse = new ArrayList<>(4);

  private final HashMap<Resource, Charset> locationCharsets = new HashMap<>(4);

  private final ArrayList<ResourceResolver> resourceResolvers = new ArrayList<>(4);

  private final ArrayList<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

  private final Map<String, MediaType> mediaTypes = new HashMap<>(4);

  private @Nullable ResourceResolvingChain resolvingChain;

  private @Nullable ResourceTransformerChain transformerChain;

  private @Nullable ResourceHttpMessageConverter resourceHttpMessageConverter;

  private @Nullable ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter;

  private @Nullable ContentNegotiationManager contentNegotiationManager;

  private @Nullable CorsConfiguration corsConfiguration;

  private @Nullable Function<Resource, String> etagGenerator;

  private @Nullable StringValueResolver embeddedValueResolver;

  private boolean useLastModified = true;

  private boolean optimizeLocations = false;

  private NotFoundHandler notFoundHandler = NotFoundHandler.sharedInstance;

  public ResourceHttpRequestHandler() {
    super(HttpMethod.GET.name(), HttpMethod.HEAD.name());
  }

  /**
   * Configure String-based locations to serve resources from.
   * <p>For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}}
   * allows resources to be served both from the web application root and
   * from any JAR on the classpath that contains a
   * {@code /META-INF/public-web-resources/} directory, with resources in the
   * web application root taking precedence.
   * <p>For {@link UrlResource URL-based resources}
   * (e.g. files, HTTP URLs, etc) this method supports a special prefix to
   * indicate the charset associated with the URL so that relative paths
   * appended to it can be encoded correctly, for example
   * {@code "[charset=Windows-31J]https://example.org/path"}.
   *
   * @see #setLocations(List)
   */
  public void setLocationValues(List<String> locations) {
    Assert.notNull(locations, "Locations list is required");
    this.locationValues.clear();
    this.locationValues.addAll(locations);
  }

  /**
   * Configure locations to serve resources from as pre-resourced Resource's.
   *
   * @see #setLocationValues(List)
   */
  public void setLocations(List<Resource> locations) {
    Assert.notNull(locations, "Locations list is required");
    this.locationResources.clear();
    for (Resource location : locations) {
      ResourceHandlerUtils.assertResourceLocation(location);
      this.locationResources.add(location);
    }
  }

  /**
   * Return the configured {@code List} of {@code Resource} locations including
   * both String-based locations provided via
   * {@link #setLocationValues(List) setLocationValues} and pre-resolved
   * {@code Resource} locations provided via {@link #setLocations(List) setLocations}.
   * <p>Note that the returned list is fully initialized only after
   * initialization via {@link #afterPropertiesSet()}.
   * <p><strong>Note:</strong> the list of locations may be filtered to
   * exclude those that don't actually exist and therefore the list returned from this
   * method may be a subset of all given locations. See {@link #setOptimizeLocations}.
   *
   * @see #setLocationValues
   * @see #setLocations
   */
  public List<Resource> getLocations() {
    if (this.locationsToUse.isEmpty()) {
      // Possibly not yet initialized, return only what we have so far
      return this.locationResources;
    }
    return this.locationsToUse;
  }

  /**
   * Configure the list of {@link ResourceResolver ResourceResolvers} to use.
   * <p>By default {@link PathResourceResolver} is configured. If using this property,
   * it is recommended to add {@link PathResourceResolver} as the last resolver.
   */
  public void setResourceResolvers(@Nullable List<ResourceResolver> resourceResolvers) {
    this.resourceResolvers.clear();
    if (resourceResolvers != null) {
      this.resourceResolvers.addAll(resourceResolvers);
    }
  }

  /**
   * Return the list of configured resource resolvers.
   */
  public List<ResourceResolver> getResourceResolvers() {
    return this.resourceResolvers;
  }

  /**
   * Configure the list of {@link ResourceTransformer ResourceTransformers} to use.
   * <p>By default no transformers are configured for use.
   */
  public void setResourceTransformers(@Nullable List<ResourceTransformer> resourceTransformers) {
    this.resourceTransformers.clear();
    if (resourceTransformers != null) {
      this.resourceTransformers.addAll(resourceTransformers);
    }
  }

  /**
   * Return the list of configured resource transformers.
   */
  public List<ResourceTransformer> getResourceTransformers() {
    return this.resourceTransformers;
  }

  /**
   * Configure the {@link ResourceHttpMessageConverter} to use.
   * <p>By default a {@link ResourceHttpMessageConverter} will be configured.
   */
  public void setResourceHttpMessageConverter(@Nullable ResourceHttpMessageConverter messageConverter) {
    this.resourceHttpMessageConverter = messageConverter;
  }

  /**
   * Return the configured resource converter.
   */
  public @Nullable ResourceHttpMessageConverter getResourceHttpMessageConverter() {
    return this.resourceHttpMessageConverter;
  }

  /**
   * Configure the {@link ResourceRegionHttpMessageConverter} to use.
   * <p>By default a {@link ResourceRegionHttpMessageConverter} will be configured.
   */
  public void setResourceRegionHttpMessageConverter(@Nullable ResourceRegionHttpMessageConverter messageConverter) {
    this.resourceRegionHttpMessageConverter = messageConverter;
  }

  /**
   * Return the configured resource region converter.
   */
  public @Nullable ResourceRegionHttpMessageConverter getResourceRegionHttpMessageConverter() {
    return this.resourceRegionHttpMessageConverter;
  }

  /**
   * Configure a {@code ContentNegotiationManager} to help determine the
   * media types for resources being served. If the manager contains a path
   * extension strategy it will be checked for registered file extension.
   */
  public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
    this.contentNegotiationManager = contentNegotiationManager;
  }

  /**
   * Return the configured content negotiation manager.
   */
  public @Nullable ContentNegotiationManager getContentNegotiationManager() {
    return this.contentNegotiationManager;
  }

  /**
   * Add mappings between file extensions, extracted from the filename of a
   * static {@link Resource}, and corresponding media type to set on the
   * response.
   * <p>Use of this method is typically not necessary since mappings are
   * otherwise determined via
   * {@link MediaTypeFactory#getMediaType(Resource)}.
   *
   * @param mediaTypes media type mappings
   */
  public void setMediaTypes(Map<String, MediaType> mediaTypes) {
    for (Map.Entry<String, MediaType> entry : mediaTypes.entrySet()) {
      this.mediaTypes.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
    }
  }

  /**
   * Return the {@link #setMediaTypes(Map) configured} media types.
   */
  public Map<String, MediaType> getMediaTypes() {
    return this.mediaTypes;
  }

  /**
   * Specify the CORS configuration for resources served by this handler.
   * <p>By default this is not set in which allows cross-origin requests.
   */
  public void setCorsConfiguration(@Nullable CorsConfiguration corsConfiguration) {
    this.corsConfiguration = corsConfiguration;
  }

  /**
   * Return the specified CORS configuration.
   */
  @Override
  public @Nullable CorsConfiguration getCorsConfiguration(RequestContext request) {
    return this.corsConfiguration;
  }

  /**
   * Set whether we should look at the {@link Resource#lastModified()} when
   * serving resources and use this information to drive {@code "Last-Modified"}
   * HTTP response headers.
   * <p>This option is enabled by default and should be turned off if the metadata
   * of the static files should be ignored.
   */
  public void setUseLastModified(boolean useLastModified) {
    this.useLastModified = useLastModified;
  }

  /**
   * Return whether the {@link Resource#lastModified()} information is used
   * to drive HTTP responses when serving static resources.
   */
  public boolean isUseLastModified() {
    return this.useLastModified;
  }

  /**
   * Configure a generator function that will be used to create the ETag information,
   * given a {@link Resource} that is about to be written to the response.
   * <p>This function should return a String that will be used as an argument in
   * {@link RequestContext#checkNotModified(String)}, or {@code null} if no value
   * can be generated for the given resource.
   *
   * @param etagGenerator the HTTP ETag generator function to use.
   */
  public void setEtagGenerator(@Nullable Function<Resource, String> etagGenerator) {
    this.etagGenerator = etagGenerator;
  }

  /**
   * Return the HTTP ETag generator function to be used when serving resources.
   *
   * @return the HTTP ETag generator function
   */
  public @Nullable Function<Resource, String> getEtagGenerator() {
    return this.etagGenerator;
  }

  /**
   * Set whether to optimize the specified locations through an existence
   * check on startup, filtering non-existing directories upfront so that
   * they do not have to be checked on every resource access.
   * <p>The default is {@code false}, for defensiveness against zip files
   * without directory entries which are unable to expose the existence of
   * a directory upfront. Switch this flag to {@code true} for optimized
   * access in case of a consistent jar layout with directory entries.
   */
  public void setOptimizeLocations(boolean optimizeLocations) {
    this.optimizeLocations = optimizeLocations;
  }

  /**
   * Return whether to optimize the specified locations through an existence
   * check on startup, filtering non-existing directories upfront so that
   * they do not have to be checked on every resource access.
   */
  public boolean isOptimizeLocations() {
    return this.optimizeLocations;
  }

  @Override
  public void setEmbeddedValueResolver(@Nullable StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  /**
   * Set the NotFoundHandler to use when a resource is not found.
   * <p>If not set, the default behavior will be to use {@link SimpleNotFoundHandler#sharedInstance}.
   *
   * @param notFoundHandler the handler to use when a resource is not found, can be null in which case
   * the default {@link SimpleNotFoundHandler#sharedInstance} will be used
   * @see SimpleNotFoundHandler#sharedInstance
   * @since 4.0
   */
  public void setNotFoundHandler(@Nullable NotFoundHandler notFoundHandler) {
    this.notFoundHandler = notFoundHandler == null ? NotFoundHandler.sharedInstance : notFoundHandler;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    resolveResourceLocations();

    if (this.resourceResolvers.isEmpty()) {
      this.resourceResolvers.add(new PathResourceResolver());
    }

    initAllowedLocations();

    // Initialize immutable resolver and transformer chains
    this.resolvingChain = new DefaultResourceResolvingChain(this.resourceResolvers);
    this.transformerChain = new DefaultResourceTransformerChain(this.resolvingChain, this.resourceTransformers);

    if (this.resourceHttpMessageConverter == null) {
      this.resourceHttpMessageConverter = new ResourceHttpMessageConverter();
    }
    if (this.resourceRegionHttpMessageConverter == null) {
      this.resourceRegionHttpMessageConverter = new ResourceRegionHttpMessageConverter();
    }

    ContentNegotiationManager manager = getContentNegotiationManager();
    if (manager != null) {
      setMediaTypes(manager.getMediaTypeMappings());
    }

  }

  private void resolveResourceLocations() {
    ArrayList<Resource> result = new ArrayList<>();
    if (!this.locationValues.isEmpty()) {
      ApplicationContext applicationContext = obtainApplicationContext();
      for (String location : this.locationValues) {
        if (this.embeddedValueResolver != null) {
          String resolvedLocation = this.embeddedValueResolver.resolveStringValue(location);
          if (resolvedLocation == null) {
            throw new IllegalArgumentException("Location resolved to null: " + location);
          }
          location = resolvedLocation;
        }
        Charset charset = null;
        location = location.trim();
        if (location.startsWith(URL_RESOURCE_CHARSET_PREFIX)) {
          int endIndex = location.indexOf(']', URL_RESOURCE_CHARSET_PREFIX.length());
          if (endIndex == -1) {
            throw new IllegalArgumentException("Invalid charset syntax in location: " + location);
          }
          String value = location.substring(URL_RESOURCE_CHARSET_PREFIX.length(), endIndex);
          charset = Charset.forName(value);
          location = location.substring(endIndex + 1);
        }
        location = ResourceHandlerUtils.initLocationPath(location);

        Resource resource = applicationContext.getResource(location);
        if (location.equals("/")) {
          throw new IllegalStateException("""
                  The String-based location "/" should be relative to the web application root but \
                  resolved to a Resource of type: %s. If this is intentional, please pass it as a pre-configured \
                  Resource via setLocations.""".formatted(resource.getClass()));
        }
        result.add(resource);
        if (charset != null) {
          if (!(resource instanceof UrlResource)) {
            throw new IllegalArgumentException("Unexpected charset for non-UrlResource: " + resource);
          }
          this.locationCharsets.put(resource, charset);
        }
      }
    }

    result.addAll(this.locationResources);
    if (isOptimizeLocations()) {
      result = result.stream()
              .filter(Resource::exists)
              .collect(Collectors.toCollection(ArrayList::new));
    }

    this.locationsToUse.clear();
    this.locationsToUse.addAll(result);
  }

  /**
   * Look for a {@code PathResourceResolver} among the configured resource
   * resolvers and set its {@code allowedLocations} property (if empty) to
   * match the {@link #setLocations locations} configured on this class.
   */
  protected void initAllowedLocations() {
    if (CollectionUtils.isEmpty(getLocations())) {
      return;
    }
    List<ResourceResolver> resourceResolvers = getResourceResolvers();
    for (int i = resourceResolvers.size() - 1; i >= 0; i--) {
      if (resourceResolvers.get(i) instanceof PathResourceResolver pathResolver) {
        if (ObjectUtils.isEmpty(pathResolver.getAllowedLocations())) {
          pathResolver.setAllowedLocations(getLocations().toArray(Resource.EMPTY_ARRAY));
        }

        pathResolver.setLocationCharsets(this.locationCharsets);
        break;
      }
    }
  }

  /**
   * Processes a resource request.
   * <p>Checks for the existence of the requested resource in the configured list of locations.
   * If the resource does not exist, a {@code 404} response will be returned to the client.
   * If the resource exists, the request will be checked for the presence of the
   * {@code Last-Modified} header, and its value will be compared against the last-modified
   * timestamp of the given resource, returning a {@code 304} status code if the
   * {@code Last-Modified} value  is greater. If the resource is newer than the
   * {@code Last-Modified} value, or the header is not present, the content resource
   * of the resource will be written to the response with caching headers
   * set to expire one year in the future.
   */
  @Override
  public @Nullable Object handleRequest(RequestContext request) throws Throwable {
    // For very general mappings (e.g. "/") we need to check 404 first
    Resource resource = getResource(request);
    if (resource == null) {
      return notFoundHandler.handleNotFound(request);
    }

    if (HttpMethod.OPTIONS == request.getMethod()) {
      request.setHeader(HttpHeaders.ALLOW, getAllowHeader());
      return NONE_RETURN_VALUE;
    }

    // Supported methods and required session
    checkRequest(request);

    // Apply cache settings, if any
    prepareResponse(request);

    // Header phase
    String eTag = getETag(resource);
    if (request.checkNotModified(eTag, isUseLastModified() ? resource.lastModified() : -1)) {
      logger.trace("Resource not modified");
      return NONE_RETURN_VALUE;
    }

    // Check the media type for the resource
    MediaType mediaType = getMediaType(request, resource);
    setHeaders(request, resource, mediaType);

    // Content phase
    ServerHttpResponse outputMessage = request.asHttpOutputMessage();
    if (request.requestHeaders().get(HttpHeaders.RANGE) == null) {
      ResourceHttpMessageConverter converter = this.resourceHttpMessageConverter;
      Assert.state(converter != null, "Not initialized");

      if (HttpMethod.HEAD == request.getMethod()) {
        converter.addDefaultHeaders(outputMessage, resource, mediaType);
      }
      else {
        converter.write(resource, mediaType, outputMessage);
      }
    }
    else {
      ResourceRegionHttpMessageConverter converter = this.resourceRegionHttpMessageConverter;
      Assert.state(converter != null, "Not initialized");
      try {
        List<HttpRange> httpRanges = request.getHeaders().getRange();
        request.setStatus(HttpStatus.PARTIAL_CONTENT);
        converter.write(HttpRange.toResourceRegions(httpRanges, resource), mediaType, outputMessage);
      }
      catch (IllegalArgumentException ex) {
        request.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
        request.sendError(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
      }
    }

    return NONE_RETURN_VALUE;
  }

  private @Nullable String getETag(Resource resource) {
    Function<Resource, String> etagGenerator = getEtagGenerator();
    if (etagGenerator != null) {
      return etagGenerator.apply(resource);
    }
    return null;
  }

  protected @Nullable Resource getResource(RequestContext request) throws IOException {
    String path;
    var matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata == null) {
      path = request.getRequestPath().value();
    }
    else {
      path = matchingMetadata.getPathWithinMapping().value();
    }

    path = processPath(path);
    if (ResourceHandlerUtils.shouldIgnoreInputPath(path) || isInvalidPath(path)) {
      return null;
    }

    ResourceResolvingChain resolvingChain = this.resolvingChain;
    ResourceTransformerChain transformerChain = this.transformerChain;
    Assert.state(resolvingChain != null, "ResourceResolvingChain not initialized.");
    Assert.state(transformerChain != null, "ResourceTransformerChain not initialized.");

    Resource resource = resolvingChain.resolveResource(request, path, getLocations());
    if (resource != null) {
      resource = transformerChain.transform(request, resource);
    }
    return resource;
  }

  /**
   * Process the given resource path.
   * <p>By default, this method delegates to {@link ResourceHandlerUtils#normalizeInputPath}.
   */
  protected String processPath(String path) {
    return ResourceHandlerUtils.normalizeInputPath(path);
  }

  /**
   * Invoked after {@link ResourceHandlerUtils#isInvalidPath(String)}
   * to allow subclasses to perform further validation.
   * <p>By default, this method does not perform any validations.
   */
  protected boolean isInvalidPath(String path) {
    return false;
  }

  /**
   * Determine the media type for the given request and the resource matched
   * to it. This implementation tries to determine the MediaType using one of
   * the following lookups based on the resource filename and its path
   * extension:
   * <ol>
   * <li>{@link #getMediaTypes()}
   * <li>{@link MediaTypeFactory#getMediaTypes(String)}
   * </ol>
   *
   * @param request the current request
   * @param resource the resource to check
   * @return the corresponding media type, or {@code null} if none found
   */
  protected @Nullable MediaType getMediaType(RequestContext request, Resource resource) {
    MediaType result = null;
    MediaType mediaType = null;
    String filename = resource.getName();
    String ext = StringUtils.getFilenameExtension(filename);
    if (ext != null) {
      mediaType = mediaTypes.get(ext.toLowerCase(Locale.ROOT));
    }
    if (mediaType == null) {
      List<MediaType> mediaTypes = MediaTypeFactory.getMediaTypes(filename);
      if (CollectionUtils.isNotEmpty(mediaTypes)) {
        mediaType = mediaTypes.get(0);
      }
    }
    if (mediaType != null) {
      result = mediaType;
    }
    return result;
  }

  /**
   * Set headers on the given response.
   * Called for GET requests as well as HEAD requests.
   *
   * @param response current response
   * @param resource the identified resource (never {@code null})
   * @param mediaType the resource's media type (never {@code null})
   * @throws IOException in case of errors while setting the headers
   */
  protected void setHeaders(RequestContext response, Resource resource, @Nullable MediaType mediaType)
          throws IOException {

    if (mediaType != null) {
      response.setContentType(mediaType.toString());
    }

    HttpHeaders responseHeaders = response.responseHeaders();
    if (resource instanceof HttpResource httpResource) {
      HttpHeaders resourceHeaders = httpResource.getResponseHeaders();
      for (Map.Entry<String, List<String>> entry : resourceHeaders.entrySet()) {
        boolean first = true;
        String headerName = entry.getKey();
        List<String> headerValues = entry.getValue();
        for (String headerValue : headerValues) {
          if (first) {
            responseHeaders.setOrRemove(headerName, headerValue);
          }
          else {
            responseHeaders.add(headerName, headerValue);
          }
          first = false;
        }
      }
    }

    responseHeaders.setOrRemove(HttpHeaders.ACCEPT_RANGES, "bytes");
  }

  @Override
  public String toString() {
    return "ResourceHttpRequestHandler " + locationToString(getLocations());
  }

  private String locationToString(List<Resource> locations) {
    return locations.toString()
            .replaceAll("class path resource", "classpath");
  }

}
