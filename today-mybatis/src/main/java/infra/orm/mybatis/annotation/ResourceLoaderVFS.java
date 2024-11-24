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

package infra.orm.mybatis.annotation;

import org.apache.ibatis.io.VFS;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;

/**
 * @author Hans Westerbeek
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:20
 */
public class ResourceLoaderVFS extends VFS {

  private final PatternResourceLoader resourceLoader;

  public ResourceLoaderVFS() {
    this.resourceLoader = new PathMatchingPatternResourceLoader(getClass().getClassLoader());
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  protected List<String> list(URL url, String path) throws IOException {
    String urlString = url.toString();
    String baseUrlString = urlString.endsWith("/") ? urlString : urlString.concat("/");
    Set<Resource> resources = resourceLoader.getResources(baseUrlString + "**/*.class");
    return resources.stream()
            .map(resource -> preserveSubpackageName(baseUrlString, resource, path))
            .collect(Collectors.toList());
  }

  private static String preserveSubpackageName(
          String baseUrlString, Resource resource, String rootPath) {
    try {
      return rootPath + (rootPath.endsWith("/") ? "" : "/")
              + resource.getURL().toString().substring(baseUrlString.length());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
