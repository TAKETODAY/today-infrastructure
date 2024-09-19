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

package cn.taketoday.web.handler.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.util.UriUtils;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Lookup function used by {@link RouterFunctions#resources(String, Resource)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Arjen Poutsma
 * @since 4.0
 */
class PathResourceLookupFunction implements Function<ServerRequest, Optional<Resource>> {

  private final PathPattern pattern;

  private final Resource location;

  public PathResourceLookupFunction(String pattern, Resource location) {
    Assert.hasLength(pattern, "'pattern' must not be empty");
    Assert.notNull(location, "'location' is required");
    this.pattern = PathPatternParser.defaultInstance.parse(pattern);
    this.location = location;
  }

  @Override
  public Optional<Resource> apply(ServerRequest request) {
    PathContainer pathContainer = request.requestPath().pathWithinApplication();
    if (!this.pattern.matches(pathContainer)) {
      return Optional.empty();
    }

    pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
    String path = processPath(pathContainer.value());
    if (StringUtils.isBlank(path) || isInvalidPath(path)) {
      return Optional.empty();
    }
    if (isInvalidEncodedInputPath(path)) {
      return Optional.empty();
    }

    if (!(this.location instanceof UrlResource)) {
      path = UriUtils.decode(path, StandardCharsets.UTF_8);
    }

    try {
      Resource resource = this.location.createRelative(path);
      if (resource.isReadable() && isResourceUnderLocation(resource)) {
        return Optional.of(resource);
      }
      else {
        return Optional.empty();
      }
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
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
        return slash ? "/" + path.substring(i) : path.substring(i);
      }
    }
    return slash ? "/" : "";
  }

  private boolean isInvalidPath(String path) {
    if (path.contains("WEB-INF") || path.contains("META-INF")) {
      return true;
    }
    if (path.contains(":/")) {
      String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
      if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
        return true;
      }
    }
    return path.contains("..") && StringUtils.cleanPath(path).contains("../");
  }

  private boolean isInvalidEncodedInputPath(String path) {
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

  private boolean isResourceUnderLocation(Resource resource) throws IOException {
    if (resource.getClass() != this.location.getClass()) {
      return false;
    }

    String resourcePath;
    String locationPath;

    if (resource instanceof UrlResource) {
      resourcePath = resource.getURL().toExternalForm();
      locationPath = StringUtils.cleanPath(this.location.getURL().toString());
    }
    else if (resource instanceof ClassPathResource cpr) {
      resourcePath = cpr.getPath();
      locationPath = StringUtils.cleanPath(((ClassPathResource) this.location).getPath());
    }
    else {
      resourcePath = resource.getURL().getPath();
      locationPath = StringUtils.cleanPath(this.location.getURL().getPath());
    }

    if (locationPath.equals(resourcePath)) {
      return true;
    }
    locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
    return (resourcePath.startsWith(locationPath) && !isInvalidEncodedResourcePath(resourcePath));
  }

  private boolean isInvalidEncodedResourcePath(String resourcePath) {
    if (resourcePath.contains("%")) {
      // Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars...
      try {
        String decodedPath = URLDecoder.decode(resourcePath, StandardCharsets.UTF_8);
        if (decodedPath.contains("../") || decodedPath.contains("..\\")) {
          return true;
        }
      }
      catch (IllegalArgumentException ex) {
        // May not be possible to decode...
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return this.pattern + " -> " + this.location;
  }

}
