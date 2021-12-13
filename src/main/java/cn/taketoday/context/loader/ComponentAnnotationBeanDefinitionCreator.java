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

import cn.taketoday.context.annotation.ScannedBeanDefinition;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Component;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY 2021/10/10 22:20
 * @since 4.0
 */
public class ComponentAnnotationBeanDefinitionCreator implements BeanDefinitionLoadingStrategy {

  @Override
  public void loadBeanDefinitions(
          MetadataReader metadata, DefinitionLoadingContext loadingContext) {
    // annotation on class
    AnnotationMetadata annotationMetadata = metadata.getAnnotationMetadata();
    annotationMetadata.getAnnotations().stream(Component.class).forEach(component -> {

      String[] nameArray = component.getStringValueArray();
      // first is bean name, after name is aliases
      ScannedBeanDefinition definition = new ScannedBeanDefinition(metadata);
      if (ObjectUtils.isEmpty(nameArray)) {
        String beanName = loadingContext.generateBeanName(definition);
        definition.setName(beanName);
      }
      else {
        // >= 1
        definition.setName(nameArray[0]);
        if (nameArray.length > 1) {
          for (int i = 1; i < nameArray.length; i++) {
            loadingContext.registerAlias(nameArray[0], nameArray[i]);
          }
        }
      }
      loadingContext.registerBeanDefinition(definition);
    });
  }

}
