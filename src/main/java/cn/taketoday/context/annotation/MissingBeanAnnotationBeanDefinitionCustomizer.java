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

package cn.taketoday.context.annotation;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.support.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionCustomizer;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/24 22:03</a>
 * @since 4.0
 */
public class MissingBeanAnnotationBeanDefinitionCustomizer implements BeanDefinitionCustomizer {
  private BeanDefinitionRegistry registry;

  private BeanFactory beanFactory;

  @Override
  public void customize(BeanDefinition definition) {
    if (definition instanceof AnnotatedBeanDefinition annotated) {
      AnnotationMetadata metadata = annotated.getMetadata();
      MergedAnnotation<ConditionalOnMissingBean> missingBean = metadata.getAnnotation(ConditionalOnMissingBean.class);
      if (missingBean.isPresent()) {
        // Missing BeanMetadata a flag to determine its missed bean @since 3.0
        definition.setAttribute(MissingBean.MissingBeanMetadata, missingBean);
        Class<?> type = missingBean.getClass("type");
        if (type != void.class) {
          Set<String> beanNames = beanFactory.getBeanNamesForType(type, true, false);
          if (beanNames.isEmpty()) {
            // not found
            // register

          }
          else {
            if (missingBean.getBoolean("equals")) {
              Set<Class<?>> candidateTypes = new LinkedHashSet<>();
              for (String name : beanNames) {
                Class<?> candidateType = beanFactory.getType(name);
                if (type == candidateType) {
                  candidateTypes.add(candidateType);
                }
              }

              if (candidateTypes.isEmpty()) {
                // register
              }
              else {
                //丢弃

              }
            }
          }
        }
      }
    }
  }

}
