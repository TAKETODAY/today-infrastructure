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

package infra.web.handler.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

import infra.core.io.Resource;
import infra.core.io.UrlResource;
import infra.http.server.PathContainer;
import infra.lang.Assert;
import infra.web.resource.ResourceHandlerUtils;
import infra.web.util.UriUtils;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

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
    ResourceHandlerUtils.assertResourceLocation(location);
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
    String path = ResourceHandlerUtils.normalizeInputPath(pathContainer.value());
    if (ResourceHandlerUtils.shouldIgnoreInputPath(path)) {
      return Optional.empty();
    }

    if (!(this.location instanceof UrlResource)) {
      path = UriUtils.decode(path, StandardCharsets.UTF_8);
    }

    try {
      Resource resource = this.location.createRelative(path);
      if (resource.isReadable() && ResourceHandlerUtils.isResourceUnderLocation(this.location, resource)) {
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

  @Override
  public String toString() {
    return this.pattern + " -> " + this.location;
  }

}
