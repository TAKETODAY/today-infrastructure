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

package cn.taketoday.web.handler.function;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    if (path.contains("%")) {
      path = StringUtils.uriDecode(path, StandardCharsets.UTF_8);
    }
    if (StringUtils.isEmpty(path) || isInvalidPath(path)) {
      return Optional.empty();
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

  private String processPath(String path) {
    boolean slash = false;
    for (int i = 0; i < path.length(); i++) {
      if (path.charAt(i) == '/') {
        slash = true;
      }
      else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
        if (i == 0 || (i == 1 && slash)) {
          return path;
        }
        path = slash ? "/" + path.substring(i) : path.substring(i);
        return path;
      }
    }
    return (slash ? "/" : "");
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
    else if (resource instanceof ClassPathResource) {
      resourcePath = ((ClassPathResource) resource).getPath();
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
    if (!resourcePath.startsWith(locationPath)) {
      return false;
    }
    return !resourcePath.contains("%") ||
            !StringUtils.uriDecode(resourcePath, StandardCharsets.UTF_8).contains("../");
  }

  @Override
  public String toString() {
    return this.pattern + " -> " + this.location;
  }

}
