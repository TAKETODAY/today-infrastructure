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

package cn.taketoday.beans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanPostProcessor;
import cn.taketoday.beans.factory.DestructionBeanPostProcessor;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2021/10/8 12:53
 * @since 4.0
 */
public class DisposableBeanAdapter implements DisposableBean {
  public static final Class<? extends Annotation>
          PreDestroy = ClassUtils.load("javax.annotation.PreDestroy");

  /**
   * Destroy bean instance
   *
   * @param obj Bean instance
   * @throws Exception When destroy a bean
   */
  public static void destroyBean(final Object obj) throws Exception {
    destroyBean(obj, null);
  }

  /**
   * Destroy bean instance
   *
   * @param obj Bean instance
   * @throws Exception When destroy a bean
   */
  public static void destroyBean(final Object obj, final BeanDefinition def) throws Exception {
    destroyBean(obj, def, null);
  }

  /**
   * Destroy bean instance
   *
   * @param obj Bean instance
   * @throws Exception When destroy a bean
   */
  public static void destroyBean(final Object obj,
                                 final BeanDefinition def,
                                 final List<BeanPostProcessor> postProcessors) throws Exception {

    Assert.notNull(obj, "bean instance must not be null");

    if (CollectionUtils.isNotEmpty(postProcessors)) {
      for (final BeanPostProcessor postProcessor : postProcessors) {
        if (postProcessor instanceof DestructionBeanPostProcessor) {
          final DestructionBeanPostProcessor destruction = (DestructionBeanPostProcessor) postProcessor;
          if (destruction.requiresDestruction(obj)) {
            destruction.postProcessBeforeDestruction(obj, def);
          }
        }
      }
    }

    // use real class
    final Class<?> beanClass = ClassUtils.getUserClass(obj);
    final List<String> destroyMethods = def != null ? Arrays.asList(def.getDestroyMethods()) : null;

    for (final Method method : ReflectionUtils.getDeclaredMethods(beanClass)) {
      if (((destroyMethods != null && destroyMethods.contains(method.getName()))
              || AnnotationUtils.isPresent(method, PreDestroy)) // PreDestroy
              && method.getParameterCount() == 0) { // 0参数
        // fix: can not access a member @since 2.1.6
        ReflectionUtils.makeAccessible(method).invoke(obj);
      }
    }

    if (obj instanceof DisposableBean) {
      ((DisposableBean) obj).destroy();
    }
  }

  @Override
  public void destroy() throws Exception {

  }

}
