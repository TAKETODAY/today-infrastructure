/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.annotation.config;

import org.jspecify.annotations.Nullable;

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

  @Nullable
  private ClassLoader beanClassLoader;

  @Nullable
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
    List<String> autoConfigurations = this.autoConfigurations;
    if (autoConfigurations == null) {
      this.autoConfigurations = autoConfigurations = ImportCandidates.load(AutoConfiguration.class, beanClassLoader).getCandidates();
      autoConfigurations.addAll(TodayStrategies.findNames(EnableAutoConfiguration.class, beanClassLoader));
    }
    return autoConfigurations;
  }

}

