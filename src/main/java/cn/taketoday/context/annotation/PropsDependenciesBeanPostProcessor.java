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

import cn.taketoday.beans.factory.DependenciesBeanPostProcessor;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * class level {@link Props}
 *
 * @author TODAY 2021/11/15 22:55
 * @since 4.0
 */
public class PropsDependenciesBeanPostProcessor implements DependenciesBeanPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(PropsDependenciesBeanPostProcessor.class);

  private final PropsReader propsReader;

  public PropsDependenciesBeanPostProcessor(ApplicationContext context) {
    this.propsReader = new PropsReader(context);
  }

  @Override
  public void processDependencies(Object bean, BeanDefinition definition) {
    Class<?> beanClass = bean.getClass();

    MergedAnnotation<Props> annotation = MergedAnnotations.from(beanClass).get(Props.class);
    if (annotation.isPresent()) {
      if (log.isDebugEnabled()) {
        log.debug("Loading Properties For: [{}]", beanClass.getName());
      }

      DefaultProps defaultProps = new DefaultProps(annotation);
      PropertyResolver propertyResolver = propsReader.getResolver(defaultProps);

      for (BeanProperty property : BeanMetadata.from(beanClass)) {
        if (!property.isReadOnly()) {
          Object converted = propsReader.readProperty(property, defaultProps, propertyResolver);
          if (converted != null) {
            property.setValue(bean, converted);
          }
        }
      }
    }
  }

}
