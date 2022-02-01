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

package cn.taketoday.context.annotation.auto;

import java.io.IOException;
import java.util.List;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.TodayStrategies;

/**
 * A {@link TypeFilter} implementation that matches registered auto-configuration classes.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 23:44
 */
public class AutoConfigurationExcludeFilter implements TypeFilter, BeanClassLoaderAware {

  private ClassLoader beanClassLoader;

  private volatile List<String> autoConfigurations;

  @Override
  public void setBeanClassLoader(ClassLoader beanClassLoader) {
    this.beanClassLoader = beanClassLoader;
  }

  @Override
  public boolean match(MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException {
    return isConfiguration(metadataReader) && isAutoConfiguration(metadataReader);
  }

  private boolean isConfiguration(MetadataReader metadataReader) {
    return metadataReader.getAnnotationMetadata().isAnnotated(Configuration.class.getName());
  }

  private boolean isAutoConfiguration(MetadataReader metadataReader) {
    return getAutoConfigurations().contains(metadataReader.getClassMetadata().getClassName());
  }

  protected List<String> getAutoConfigurations() {
    if (this.autoConfigurations == null) {
      this.autoConfigurations = TodayStrategies.getStrategiesNames(
              EnableAutoConfiguration.class, this.beanClassLoader);
    }
    return this.autoConfigurations;
  }

}

