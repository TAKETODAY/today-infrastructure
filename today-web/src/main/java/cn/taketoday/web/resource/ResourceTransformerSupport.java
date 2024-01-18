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

import java.util.Collections;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * A base class for a {@code ResourceTransformer} with an optional helper method
 * for resolving public links within a transformed resource.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {

  @Nullable
  private ResourceUrlProvider resourceUrlProvider;

  /**
   * Configure a {@link ResourceUrlProvider} to use when resolving the public
   * URL of links in a transformed resource (e.g. import links in a CSS file).
   * This is required only for links expressed as full paths and not for
   * relative links.
   */
  public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
    this.resourceUrlProvider = resourceUrlProvider;
  }

  /**
   * Return the configured {@code ResourceUrlProvider}.
   */
  @Nullable
  public ResourceUrlProvider getResourceUrlProvider() {
    return this.resourceUrlProvider;
  }

  /**
   * A transformer can use this method when a resource being transformed
   * contains links to other resources. Such links need to be replaced with the
   * public facing link as determined by the resource resolver chain (e.g. the
   * public URL may have a version inserted).
   *
   * @param resourcePath the path to a resource that needs to be re-written
   * @param request the current request
   * @param resource the resource being transformed
   * @param transformerChain the transformer chain
   * @return the resolved URL, or {@code} if not resolvable
   */
  @Nullable
  protected String resolveUrlPath(String resourcePath, RequestContext request,
          Resource resource, ResourceTransformerChain transformerChain) {

    if (resourcePath.startsWith("/")) {
      // full resource path
      ResourceUrlProvider urlProvider = getResourceUrlProvider();
      return urlProvider != null ? urlProvider.getForRequestUrl(request, resourcePath) : null;
    }
    else {
      // try resolving as relative path
      return transformerChain.getResolvingChain()
              .resolveUrlPath(resourcePath, Collections.singletonList(resource));
    }
  }

  /**
   * Transform the given relative request path to an absolute path,
   * taking the path of the given request as a point of reference.
   * The resulting path is also cleaned from sequences like "path/..".
   *
   * @param path the relative path to transform
   * @param request the referer request
   * @return the absolute request path for the given resource path
   */
  protected String toAbsolutePath(String path, RequestContext request) {
    String absolutePath = path;
    if (!path.startsWith("/")) {
      ResourceUrlProvider urlProvider = getResourceUrlProvider();
      Assert.state(urlProvider != null, "No ResourceUrlProvider");
      String requestPath = request.getRequestURI();
      absolutePath = StringUtils.applyRelativePath(requestPath, path);
    }
    return StringUtils.cleanPath(absolutePath);
  }

}
