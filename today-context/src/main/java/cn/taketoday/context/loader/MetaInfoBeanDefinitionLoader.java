/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.loader;

import java.io.IOException;
import java.util.Set;

import cn.taketoday.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import cn.taketoday.context.support.ContextUtils;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;

/**
 * @author TODAY 2021/10/7 22:31
 * @since 4.0
 */
public class MetaInfoBeanDefinitionLoader implements BeanDefinitionLoader {
  public static final String META_INFO_beans = "META-INF/beans";

  private static final Logger log = LoggerFactory.getLogger(MetaInfoBeanDefinitionLoader.class);

  /**
   * Resolve bean from META-INF/beans
   *
   * @see #META_INFO_beans
   * @since 2.1.6
   */
  @Override
  public void loadBeanDefinitions(BootstrapContext context) {
    try {
      loadMetaInfoBeans(context);
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
  }

  /**
   * Resolve bean from META-INF/beans
   *
   * @param context reader context
   * @see #META_INFO_beans
   * @since 2.1.6
   */
  public void loadMetaInfoBeans(BootstrapContext context) throws IOException {
    log.debug("Loading META-INF/beans");
    // Load the META-INF/beans @since 2.1.6
    // ---------------------------------------------------
    Set<String> beans = ContextUtils.loadFromMetaInfoClass(META_INFO_beans);

    MetadataReaderFactory metadataReaderFactory = context.getMetadataReaderFactory();
    for (String beanClassName : beans) {
      MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(beanClassName);
      AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
      // pass the condition evaluation
      if (context.passCondition(annotationMetadata)) {

        AnnotatedGenericBeanDefinition definition = new AnnotatedGenericBeanDefinition(annotationMetadata);
        definition.setResource(metadataReader.getResource());

        context.registerBeanDefinition(context.generateBeanName(definition), definition);
      }
    }

    log.debug("Found {} META-INF/beans", beans.size());
  }

}
