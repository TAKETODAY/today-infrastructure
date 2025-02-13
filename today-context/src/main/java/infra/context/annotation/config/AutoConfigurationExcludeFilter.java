/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation.config;

import java.io.IOException;
import java.util.List;

import infra.beans.factory.BeanClassLoaderAware;
import infra.context.annotation.Configuration;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.filter.TypeFilter;
import infra.lang.TodayStrategies;

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
    return metadataReader.getAnnotationMetadata().isAnnotated(Configuration.class);
  }

  private boolean isAutoConfiguration(MetadataReader metadataReader) {
    return getAutoConfigurations().contains(metadataReader.getClassMetadata().getClassName());
  }

  protected List<String> getAutoConfigurations() {
    if (autoConfigurations == null) {
      autoConfigurations = ImportCandidates.load(AutoConfiguration.class, beanClassLoader).getCandidates();
      autoConfigurations.addAll(TodayStrategies.findNames(EnableAutoConfiguration.class, beanClassLoader));
    }
    return this.autoConfigurations;
  }

}

