/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.resource;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlBasedResource;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MediaTypeFactory;
import cn.taketoday.http.converter.ResourceHttpMessageConverter;
import cn.taketoday.http.converter.ResourceRegionHttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHttpOutputMessage;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.WebContentGenerator;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.cors.CorsConfigurationSource;
import cn.taketoday.web.handler.HandlerAdapter;
import cn.taketoday.web.handler.RequestHandler;
import cn.taketoday.web.servlet.ServletUtils;

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
 * @since 4.0
 */
public class ResourceHttpRequestHandler extends WebContentGenerator
        implements RequestHandler, EmbeddedValueResolverAware, InitializingBean, CorsConfigurationSource {

  private static final Logger logger = LoggerFactory.getLogger(ResourceHttpRequestHandler.class);

  private static final String URL_RESOURCE_CHARSET_PREFIX = "[charset=";

  private final ArrayList<String> locationValues = new ArrayList<>(4);

  private final ArrayList<Resource> locationResources = new ArrayList<>(4);

  private final ArrayList<Resource> locationsToUse = new ArrayList<>(4);

  private final HashMap<Resource, Charset> locationCharsets = new HashMap<>(4);

  private final ArrayList<ResourceResolver> resourceResolvers = new ArrayList<>(4);

  private final ArrayList<ResourceTransformer> resourceTransformers = new ArrayList<>(4);

  @Nullable
  private ResourceResolvingChain resolvingChain;

  @Nullable
  private ResourceTransformerChain transformerChain;

  @Nullable
  private ResourceHttpMessageConverter resourceHttpMessageConverter;

  @Nullable
  private ResourceRegionHttpMessageConverter resourceRegionHttpMessageConverter;

  @Nullable
  private ContentNegotiationManager contentNegotiationManager;

  private final Map<String, MediaType> mediaTypes = new HashMap<>(4);

  @Nullable
  private CorsConfiguration corsConfiguration;

  private boolean useLastModified = true;

  private boolean optimizeLocations = false;

  @Nullable
  private StringValueResolver embeddedValueResolver;

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
   * <p>For {@link cn.taketoday.core.io.UrlBasedResource URL-based resources}
   * (e.g. files, HTTP URLs, etc) this method supports a special prefix to
   * indicate the charset associated with the URL so that relative paths
   * appended to it can be encoded correctly, for example
   * {@code "[charset=Windows-31J]https://example.org/path"}.
   *
   * @see #setLocations(List)
   */
  public void setLocationValues(List<String> locations) {
    Assert.notNull(locations, "Locations list must not be null");
    this.locationValues.clear();
    this.locationValues.addAll(locations);
  }

  /**
   * Configure locations to serve resources from as pre-resourced Resource's.
   *
   * @see #setLocationValues(List)
   */
  public void setLocations(List<Resource> locations) {
    Assert.notNull(locations, "Locations list must not be null");
    this.locationResources.clear();
    this.locationResources.addAll(locations);
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
  @Nullable
  public ResourceHttpMessageConverter getResourceHttpMessageConverter() {
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
  @Nullable
  public ResourceRegionHttpMessageConverter getResourceRegionHttpMessageConverter() {
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
  @Nullable
  public ContentNegotiationManager getContentNegotiationManager() {
    return this.contentNegotiationManager;
  }

  /**
   * Add mappings between file extensions, extracted from the filename of a
   * static {@link Resource}, and corresponding media type to set on the
   * response.
   * <p>Use of this method is typically not necessary since mappings are
   * otherwise determined via
   * {@link jakarta.servlet.ServletContext#getMimeType(String)} or via
   * {@link MediaTypeFactory#getMediaType(Resource)}.
   *
   * @param mediaTypes media type mappings
   */
  public void setMediaTypes(Map<String, MediaType> mediaTypes) {
    mediaTypes.forEach((ext, mediaType) ->
            this.mediaTypes.put(ext.toLowerCase(Locale.ENGLISH), mediaType));
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
  @Nullable
  public CorsConfiguration getCorsConfiguration(RequestContext request) {
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
        Resource resource = applicationContext.getResource(location);
        if (location.equals("/")) {
          throw new IllegalStateException(
                  "The String-based location \"/\" should be relative to the web application root " +
                          "but resolved to a Resource of type: " + resource.getClass() + ". " +
                          "If this is intentional, please pass it as a pre-configured Resource via setLocations.");
        }
        result.add(resource);
        if (charset != null) {
          if (!(resource instanceof UrlBasedResource)) {
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
  public Object handleRequest(RequestContext request) throws Exception {
    // For very general mappings (e.g. "/") we need to check 404 first
    Resource resource = getResource(request);
    if (resource == null) {
      logger.debug("Resource not found");
      request.sendError(HttpStatus.NOT_FOUND.value());
      return HandlerAdapter.NONE_RETURN_VALUE;
    }

    if (HttpMethod.OPTIONS.matches(request.getMethodValue())) {
      request.responseHeaders().set(HttpHeaders.ALLOW, getAllowHeader());
      return HandlerAdapter.NONE_RETURN_VALUE;
    }

    // Supported methods and required session
    checkRequest(request);

    // Header phase
    if (isUseLastModified() && request.checkNotModified(resource.lastModified())) {
      logger.trace("Resource not modified");
      return HandlerAdapter.NONE_RETURN_VALUE;
    }

    // Apply cache settings, if any
    prepareResponse(request);

    // Check the media type for the resource
    MediaType mediaType = getMediaType(request, resource);
    setHeaders(request, resource, mediaType);

    // Content phase
    RequestContextHttpOutputMessage outputMessage = new RequestContextHttpOutputMessage(request);
    HttpHeaders requestHeaders = request.requestHeaders();
    if (requestHeaders.get(HttpHeaders.RANGE) == null) {
      ResourceHttpMessageConverter converter = this.resourceHttpMessageConverter;
      Assert.state(converter != null, "Not initialized");
      converter.write(resource, mediaType, outputMessage);
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
        request.responseHeaders().set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
        request.sendError(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
      }
    }

    return HandlerAdapter.NONE_RETURN_VALUE;
  }

  @Nullable
  protected Resource getResource(RequestContext request) throws IOException {
    String path;
    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata == null) {
      path = request.getLookupPath().pathWithinApplication().value();
    }
    else {
      path = request.getMatchingMetadata().getPathWithinMapping().value();
    }

    path = processPath(path);
    if (!StringUtils.hasText(path) || isInvalidPath(path)) {
      return null;
    }
    if (isInvalidEncodedPath(path)) {
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
   * <p>The default implementation replaces:
   * <ul>
   * <li>Backslash with forward slash.
   * <li>Duplicate occurrences of slash with a single slash.
   * <li>Any combination of leading slash and control characters (00-1F and 7F)
   * with a single "/" or "". For example {@code "  / // foo/bar"}
   * becomes {@code "/foo/bar"}.
   * </ul>
   */
  protected String processPath(String path) {
    path = StringUtils.replace(path, "\\", "/");
    path = cleanDuplicateSlashes(path);
    return cleanLeadingSlash(path);
  }

  private String cleanDuplicateSlashes(String path) {
    StringBuilder sb = null;
    char prev = 0;
    for (int i = 0; i < path.length(); i++) {
      char curr = path.charAt(i);
      try {
        if ((curr == '/') && (prev == '/')) {
          if (sb == null) {
            sb = new StringBuilder(path.substring(0, i));
          }
          continue;
        }
        if (sb != null) {
          sb.append(path.charAt(i));
        }
      }
      finally {
        prev = curr;
      }
    }
    return sb != null ? sb.toString() : path;
  }

  private String cleanLeadingSlash(String path) {
    boolean slash = false;
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == '/') {
        slash = true;
      }
      else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
        if (i == 0 || (i == 1 && slash)) {
          return path;
        }
        return (slash ? "/" + path.substring(i) : path.substring(i));
      }
    }
    return (slash ? "/" : "");
  }

  /**
   * Check whether the given path contains invalid escape sequences.
   *
   * @param path the path to validate
   * @return {@code true} if the path is invalid, {@code false} otherwise
   */
  private boolean isInvalidEncodedPath(String path) {
    if (path.contains("%")) {
      try {
        // Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (isInvalidPath(decodedPath)) {
          return true;
        }
        decodedPath = processPath(decodedPath);
        if (isInvalidPath(decodedPath)) {
          return true;
        }
      }
      catch (IllegalArgumentException ex) {
        // May not be possible to decode...
      }
    }
    return false;
  }

  /**
   * Identifies invalid resource paths. By default rejects:
   * <ul>
   * <li>Paths that contain "WEB-INF" or "META-INF"
   * <li>Paths that contain "../" after a call to
   * {@link cn.taketoday.util.StringUtils#cleanPath}.
   * <li>Paths that represent a {@link cn.taketoday.util.ResourceUtils#isUrl
   * valid URL} or would represent one after the leading slash is removed.
   * </ul>
   * <p><strong>Note:</strong> this method assumes that leading, duplicate '/'
   * or control characters (e.g. white space) have been trimmed so that the
   * path starts predictably with a single '/' or does not have one.
   *
   * @param path the path to validate
   * @return {@code true} if the path is invalid, {@code false} otherwise
   */
  protected boolean isInvalidPath(String path) {
    if (path.contains("WEB-INF") || path.contains("META-INF")) {
      if (logger.isWarnEnabled()) {
        logger.warn(LogFormatUtils.formatValue(
                "Path with \"WEB-INF\" or \"META-INF\": [" + path + "]", -1, true));
      }
      return true;
    }
    if (path.contains(":/")) {
      String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
      if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
        if (logger.isWarnEnabled()) {
          logger.warn(LogFormatUtils.formatValue(
                  "Path represents URL or has \"url:\" prefix: [" + path + "]", -1, true));
        }
        return true;
      }
    }
    if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
      if (logger.isWarnEnabled()) {
        logger.warn(LogFormatUtils.formatValue(
                "Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]", -1, true));
      }
      return true;
    }
    return false;
  }

  /**
   * Determine the media type for the given request and the resource matched
   * to it. This implementation tries to determine the MediaType using one of
   * the following lookups based on the resource filename and its path
   * extension:
   * <ol>
   * <li>{@link jakarta.servlet.ServletContext#getMimeType(String)}
   * <li>{@link #getMediaTypes()}
   * <li>{@link MediaTypeFactory#getMediaTypes(String)}
   * </ol>
   *
   * @param request the current request
   * @param resource the resource to check
   * @return the corresponding media type, or {@code null} if none found
   */
  @Nullable
  protected MediaType getMediaType(RequestContext request, Resource resource) {
    MediaType result = null;
    if (ServletDetector.runningInServlet(request)) {
      String mimeType = ServletUtils.getServletContext(request).getMimeType(resource.getName());
      if (StringUtils.hasText(mimeType)) {
        result = MediaType.parseMediaType(mimeType);
      }
    }
    if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
      MediaType mediaType = null;
      String filename = resource.getName();
      String ext = StringUtils.getFilenameExtension(filename);
      if (ext != null) {
        mediaType = this.mediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
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
    }
    return result;
  }

  /**
   * Set headers on the given servlet response.
   * Called for GET requests as well as HEAD requests.
   *
   * @param response current servlet response
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
      resourceHeaders.forEach((headerName, headerValues) -> {
        boolean first = true;
        for (String headerValue : headerValues) {
          if (first) {
            responseHeaders.set(headerName, headerValue);
          }
          else {
            responseHeaders.add(headerName, headerValue);
          }
          first = false;
        }
      });
    }

    responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
  }

  @Override
  public String toString() {
    return "ResourceHttpRequestHandler " + locationToString(getLocations());
  }

  private String locationToString(List<Resource> locations) {
    return locations.toString()
            .replaceAll("class path resource", "classpath")
            .replaceAll("ServletContext resource", "ServletContext");
  }

}
