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

import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.ContextUtils;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY 2021/10/7 22:31
 * @since 4.0
 */
public class MetaInfoBeanDefinitionLoader implements BeanDefinitionLoader {
  private static final Logger log = LoggerFactory.getLogger(MetaInfoBeanDefinitionLoader.class);

  /**
   * Resolve bean from META-INF/beans
   *
   * @see Constant#META_INFO_beans
   * @since 2.1.6
   */
  @Override
  public void loadBeanDefinitions(DefinitionLoadingContext context) {
    loadMetaInfoBeans(context);
  }

  /**
   * Resolve bean from META-INF/beans
   *
   * @param context reader context
   * @see Constant#META_INFO_beans
   * @since 2.1.6
   */
  public Set<Class<?>> loadMetaInfoBeans(DefinitionLoadingContext context) {
    log.debug("Loading META-INF/beans");

    // Load the META-INF/beans @since 2.1.6
    // ---------------------------------------------------
    Set<Class<?>> beans = ContextUtils.loadFromMetaInfo(Constant.META_INFO_beans);
    // @since 4.0 load from StrategiesLoader strategy file
    beans.addAll(TodayStrategies.getDetector().getTypes(MissingBean.class));

    BeanDefinitionBuilder builder = context.createBuilder();

    for (Class<?> beanClass : beans) {
      AnnotationAttributes missingBean = AnnotationUtils.getAttributes(MissingBean.class, beanClass);
      if (missingBean != null) {
        if (context.isMissingBeanInContext(missingBean, beanClass)) {
          // MissingBean in 'META-INF/beans' @since 3.0
          String name = createBeanName(beanClass);
          builder.build(name, missingBean, context.getMissingBeanRegistry()::registerMissing);
        }
        else {
          log.info("@MissingBean -> '{}' cannot pass the condition " +
                           "or contains its bean definition, dont register to the map", beanClass);
        }
      }
      else {
        if (context.passCondition(beanClass)) {
          // can't be a missed bean. MissingBean load after normal loading beans
          List<BeanDefinition> defs = BeanDefinitionBuilder.from(beanClass);
          for (BeanDefinition def : defs) {
            context.registerBeanDefinition(def);
          }
        }
      }

    }
    return beans;
  }

}
