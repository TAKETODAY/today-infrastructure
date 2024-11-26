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

package infra.jdbc.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
      ArrayList<Resource> resources = new ArrayList<>(patternResourceLoader.getResources(location));
      resources.sort((r1, r2) -> {
        try {
          return r1.getURL().toString().compareTo(r2.getURL().toString());
        }
        catch (IOException ex) {
          return 0;
        }
      });
      scripts.addAll(resources);
    }
    return scripts.toArray(Resource.EMPTY_ARRAY);
  }

}
