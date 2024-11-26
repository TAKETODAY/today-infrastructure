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

package infra.context.annotation;

import java.io.IOException;
import java.util.Map;

import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.core.Ordered;
import infra.core.PriorityOrdered;
import infra.core.type.classreading.CachingMetadataReaderFactory;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.lang.Nullable;
import infra.util.ClassUtils;

/**
 * A {@link BeanPostProcessor} that honours {@link ImportAware} callback using
 * a mapping computed at build time.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ImportAwareAotBeanPostProcessor implements InitializationBeanPostProcessor, PriorityOrdered {

  private final MetadataReaderFactory metadataReaderFactory;

  private final Map<String, String> importsMapping;

  public ImportAwareAotBeanPostProcessor(Map<String, String> importsMapping) {
    this.metadataReaderFactory = new CachingMetadataReaderFactory();
    this.importsMapping = Map.copyOf(importsMapping);
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    if (bean instanceof ImportAware importAware) {
      setAnnotationMetadata(importAware);
    }
    return bean;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;  // match priority of original post processor
  }

  private void setAnnotationMetadata(ImportAware instance) {
    String importingClass = getImportingClassFor(instance);
    if (importingClass == null) {
      return; // import aware configuration class not imported
    }
    try {
      MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(importingClass);
      instance.setImportMetadata(metadataReader.getAnnotationMetadata());
    }
    catch (IOException ex) {
      throw new IllegalStateException(String.format("Failed to read metadata for '%s'", importingClass), ex);
    }
  }

  @Nullable
  private String getImportingClassFor(ImportAware instance) {
    String target = ClassUtils.getUserClass(instance).getName();
    return this.importsMapping.get(target);
  }

}
