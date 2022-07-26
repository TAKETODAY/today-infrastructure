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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.context.support.ServletContextResource;
import cn.taketoday.web.util.UriUtils;

/**
 * A simple {@code ResourceResolver} that tries to find a resource under the given
 * locations matching to the request path.
 *
 * <p>This resolver does not delegate to the {@code ResourceResolverChain} and is
 * expected to be configured at the end in a chain of resolvers.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class PathResourceResolver extends AbstractResourceResolver {

  @Nullable
  private Resource[] allowedLocations;

  private boolean urlDecode = false;

  private final HashMap<Resource, Charset> locationCharsets = new HashMap<>(4);

  /**
   * By default when a Resource is found, the path of the resolved resource is
   * compared to ensure it's under the input location where it was found.
   * However sometimes that may not be the case, e.g. when
   * {@link cn.taketoday.web.resource.CssLinkResourceTransformer}
   * resolves public URLs of links it contains, the CSS file is the location
   * and the resources being resolved are css files, images, fonts and others
   * located in adjacent or parent directories.
   * <p>This property allows configuring a complete list of locations under
   * which resources must be so that if a resource is not under the location
   * relative to which it was found, this list may be checked as well.
   * <p>By default {@link ResourceHttpRequestHandler} initializes this property
   * to match its list of locations.
   *
   * @param locations the list of allowed locations
   * @see ResourceHttpRequestHandler#initAllowedLocations()
   */
  public void setAllowedLocations(@Nullable Resource... locations) {
    this.allowedLocations = locations;
  }

  @Nullable
  public Resource[] getAllowedLocations() {
    return this.allowedLocations;
  }

  /**
   * Configure charsets associated with locations. If a static resource is found
   * under a {@link UrlResource URL resource}
   * location the charset is used to encode the relative path
   */
  public void setLocationCharsets(Map<Resource, Charset> locationCharsets) {
    this.locationCharsets.clear();
    this.locationCharsets.putAll(locationCharsets);
  }

  /**
   * Whether the resource-path should be decoded
   *
   * @see jakarta.servlet.ServletRequest#getCharacterEncoding()
   * @see java.net.URLDecoder#decode(String, String)
   */
  public void setUrlDecode(boolean urlDecode) {
    this.urlDecode = urlDecode;
  }

  /**
   * Whether to decode the request URI when determining the lookup path.
   */
  public boolean isUrlDecode() {
    return urlDecode;
  }

  /**
   * Return charsets associated with static resource locations.
   */
  public Map<Resource, Charset> getLocationCharsets() {
    return Collections.unmodifiableMap(this.locationCharsets);
  }

  @Override
  protected Resource resolveResourceInternal(
          @Nullable RequestContext request, String requestPath,
          List<? extends Resource> locations, ResourceResolvingChain chain) {

    return getResource(requestPath, request, locations);
  }

  @Override
  protected String resolveUrlPathInternal(
          String resourcePath, List<? extends Resource> locations, ResourceResolvingChain chain) {
    return StringUtils.hasText(resourcePath)
                   && getResource(resourcePath, null, locations) != null ? resourcePath : null;
  }

  @Nullable
  private Resource getResource(
          String resourcePath, @Nullable RequestContext request, List<? extends Resource> locations) {
    for (Resource location : locations) {
      try {
        String pathToUse = encodeOrDecodeIfNecessary(resourcePath, request, location);
        Resource resource = getResource(pathToUse, location);
        if (resource != null) {
          return resource;
        }
      }
      catch (IOException ex) {
        if (logger.isDebugEnabled()) {
          String error = "Skip location [" + location + "] due to error";
          if (logger.isTraceEnabled()) {
            logger.trace(error, ex);
          }
          else {
            logger.debug("{}: {}", error, ex.getMessage());
          }
        }
      }
    }
    return null;
  }

  /**
   * Find the resource under the given location.
   * <p>The default implementation checks if there is a readable
   * {@code Resource} for the given path relative to the location.
   *
   * @param resourcePath the path to the resource
   * @param location the location to check
   * @return the resource, or {@code null} if none found
   */
  @Nullable
  protected Resource getResource(String resourcePath, Resource location) throws IOException {
    Resource resource = location.createRelative(resourcePath);
    if (resource.isReadable()) {
      if (checkResource(resource, location)) {
        return resource;
      }
      else if (logger.isWarnEnabled()) {
        Resource[] allowed = getAllowedLocations();
        logger.warn(LogFormatUtils.formatValue(
                "Resource path \"" + resourcePath + "\" was successfully resolved " +
                        "but resource \"" + resource.getURL() + "\" is neither under " +
                        "the current location \"" + location.getURL() + "\" nor under any of " +
                        "the allowed locations " + (allowed != null ? Arrays.asList(allowed) : "[]"), -1, true));
      }
    }
    return null;
  }

  /**
   * Perform additional checks on a resolved resource beyond checking whether the
   * resources exists and is readable. The default implementation also verifies
   * the resource is either under the location relative to which it was found or
   * is under one of the {@link #setAllowedLocations allowed locations}.
   *
   * @param resource the resource to check
   * @param location the location relative to which the resource was found
   * @return "true" if resource is in a valid location, "false" otherwise.
   */
  protected boolean checkResource(Resource resource, Resource location) throws IOException {
    if (isResourceUnderLocation(resource, location)) {
      return true;
    }
    Resource[] allowedLocations = getAllowedLocations();
    if (allowedLocations != null) {
      for (Resource current : allowedLocations) {
        if (isResourceUnderLocation(resource, current)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isResourceUnderLocation(Resource resource, Resource location) throws IOException {
    if (resource.getClass() != location.getClass()) {
      return false;
    }

    String resourcePath;
    String locationPath;

    if (resource instanceof UrlResource) {
      resourcePath = resource.getURL().toExternalForm();
      locationPath = StringUtils.cleanPath(location.getURL().toString());
    }
    else if (resource instanceof ClassPathResource classPathResource) {
      resourcePath = classPathResource.getPath();
      locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
    }
    else if (resource instanceof ServletContextResource servletContextResource) {
      resourcePath = servletContextResource.getPath();
      locationPath = StringUtils.cleanPath(((ServletContextResource) location).getPath());
    }
    else {
      resourcePath = resource.getURL().getPath();
      locationPath = StringUtils.cleanPath(location.getURL().getPath());
    }

    if (locationPath.equals(resourcePath)) {
      return true;
    }
    locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
    return (resourcePath.startsWith(locationPath) && !isInvalidEncodedPath(resourcePath));
  }

  private String encodeOrDecodeIfNecessary(String path, @Nullable RequestContext request, Resource location) {
    if (request != null) {
      if (shouldDecodeRelativePath(location)) {
        try {
          return UriUtils.decode(path, StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e) {
          return path;
        }
      }
      else if (shouldEncodeRelativePath(location)) {
        Charset charset = this.locationCharsets.getOrDefault(location, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        while (tokenizer.hasMoreTokens()) {
          String value = UriUtils.encode(tokenizer.nextToken(), charset);
          sb.append(value);
          sb.append('/');
        }
        if (!path.endsWith("/")) {
          sb.setLength(sb.length() - 1);
        }
        return sb.toString();
      }
    }
    return path;
  }

  private boolean shouldDecodeRelativePath(Resource location) {
    return !(location instanceof UrlResource);
  }

  private boolean shouldEncodeRelativePath(Resource location) {
    return location instanceof UrlResource && urlDecode;
  }

  private boolean isInvalidEncodedPath(String resourcePath) {
    if (resourcePath.contains("%")) {
      // Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars...
      try {
        String decodedPath = URLDecoder.decode(resourcePath, StandardCharsets.UTF_8);
        if (decodedPath.contains("../") || decodedPath.contains("..\\")) {
          logger.warn(LogFormatUtils.formatValue(
                  "Resolved resource path contains encoded \"../\" or \"..\\\": " + resourcePath, -1, true));
          return true;
        }
      }
      catch (IllegalArgumentException ex) {
        // May not be possible to decode...
      }
    }
    return false;
  }

}
