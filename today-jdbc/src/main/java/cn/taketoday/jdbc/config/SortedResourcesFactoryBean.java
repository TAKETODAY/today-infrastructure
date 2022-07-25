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

package cn.taketoday.jdbc.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.AbstractFactoryBean;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;

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
