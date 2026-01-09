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

package infra.jdbc.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.config.AbstractFactoryBean;
import infra.context.ResourceLoaderAware;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

/**
 * {@link FactoryBean} implementation that takes a list of location Strings
 * and creates a sorted array of {@link Resource} instances.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Christian Dupuis
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:12
 */
public class SortedResourcesFactoryBean extends AbstractFactoryBean<Resource[]> implements ResourceLoaderAware {

  private final List<String> locations;

  private PatternResourceLoader patternResourceLoader;

  public SortedResourcesFactoryBean(List<String> locations) {
    this.locations = locations;
    this.patternResourceLoader = new PathMatchingPatternResourceLoader();
  }

  public SortedResourcesFactoryBean(ResourceLoader resourceLoader, List<String> locations) {
    this.locations = locations;
    this.patternResourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.patternResourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
  }

  @Override
  public Class<? extends Resource[]> getObjectType() {
    return Resource[].class;
  }

  @Override
  protected Resource[] createBeanInstance() throws Exception {
    ArrayList<Resource> scripts = new ArrayList<>();
    for (String location : locations) {
      var resources = patternResourceLoader.getResources(location);

      // Cache URLs to avoid repeated I/O during sorting
      Map<Resource, String> urlCache = new LinkedHashMap<>(resources.size());
      for (Resource resource : resources) {
        try {
          urlCache.put(resource, resource.getURL().toString());
        }
        catch (IOException ex) {
          throw new IllegalStateException(
                  "Failed to resolve URL for resource [%s] from location pattern [%s]".formatted(resource, location), ex);
        }
      }

      // Sort using cached URLs
      ArrayList<Resource> sortedResources = new ArrayList<>(urlCache.keySet());
      sortedResources.sort(Comparator.comparing(urlCache::get));

      scripts.addAll(sortedResources);
    }
    return scripts.toArray(Resource.EMPTY_ARRAY);
  }

}
