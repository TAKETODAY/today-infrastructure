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

package cn.taketoday.web.resource;

import org.webjars.WebJarAssetLocator;

import java.util.List;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * A {@code ResourceResolver} that delegates to the chain to locate a resource and then
 * attempts to find a matching versioned resource contained in a WebJar JAR file.
 *
 * <p>This allows WebJars.org users to write version agnostic paths in their templates,
 * like {@code <script src="/jquery/jquery.min.js"/>}.
 * This path will be resolved to the unique version {@code <script src="/jquery/1.2.0/jquery.min.js"/>},
 * which is a better fit for HTTP caching and version management in applications.
 *
 * <p>This also resolves resources for version agnostic HTTP requests {@code "GET /jquery/jquery.min.js"}.
 *
 * <p>This resolver requires the {@code org.webjars:webjars-locator-core} library
 * on the classpath and is automatically registered if that library is present.
 *
 * @author Brian Clozel
 * @see <a href="https://www.webjars.org">webjars.org</a>
 * @since 4.0
 */
public class WebJarsResourceResolver extends AbstractResourceResolver {

  private static final String WEBJARS_LOCATION = "META-INF/resources/webjars/";

  private static final int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();

  private final WebJarAssetLocator webJarAssetLocator;

  /**
   * Create a {@code WebJarsResourceResolver} with a default {@code WebJarAssetLocator} instance.
   */
  public WebJarsResourceResolver() {
    this(new WebJarAssetLocator());
  }

  /**
   * Create a {@code WebJarsResourceResolver} with a custom {@code WebJarAssetLocator} instance,
   * e.g. with a custom index.
   */
  public WebJarsResourceResolver(WebJarAssetLocator webJarAssetLocator) {
    this.webJarAssetLocator = webJarAssetLocator;
  }

  @Override
  protected Resource resolveResourceInternal(@Nullable RequestContext request,
          String requestPath, List<? extends Resource> locations, ResourceResolvingChain chain) {

    Resource resolved = chain.resolveResource(request, requestPath, locations);
    if (resolved == null) {
      String webJarResourcePath = findWebJarResourcePath(requestPath);
      if (webJarResourcePath != null) {
        return chain.resolveResource(request, webJarResourcePath, locations);
      }
    }
    return resolved;
  }

  @Override
  protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolvingChain chain) {
    String path = chain.resolveUrlPath(resourceUrlPath, locations);
    if (path == null) {
      String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
      if (webJarResourcePath != null) {
        return chain.resolveUrlPath(webJarResourcePath, locations);
      }
    }
    return path;
  }

  @Nullable
  protected String findWebJarResourcePath(String path) {
    int startOffset = path.startsWith("/") ? 1 : 0;
    int endOffset = path.indexOf('/', 1);
    if (endOffset != -1) {
      String webjar = path.substring(startOffset, endOffset);
      String partialPath = path.substring(endOffset + 1);
      String webJarPath = this.webJarAssetLocator.getFullPathExact(webjar, partialPath);
      if (webJarPath != null) {
        return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
      }
    }
    return null;
  }

}
