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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2021/10/19 21:47
 * @since 4.0
 */
public class MissingBeanRegistry {
  private final DefinitionLoadingContext context;
  final ArrayList<ScannedMissingInfo> missingInfos = new ArrayList<>();

  public MissingBeanRegistry(DefinitionLoadingContext context) {
    this.context = context;
  }

  /**
   * Is a context missed bean?
   *
   * @param missingBean The {@link Annotation} declared on the class or a method
   * @param annotated Missed bean class or method
   * @return If the bean is missed in context
   * @since 3.0
   */
  public boolean isMissingBeanInContext(AnnotationAttributes missingBean, AnnotatedElement annotated) {
    if (missingBean != null && context.passCondition(annotated)) {
      // find by bean name
      String beanName = missingBean.getString(Constant.VALUE);
      if (StringUtils.isNotEmpty(beanName) && context.containsBeanDefinition(beanName)) {
        return false;
      }
      // find by type
      Class<?> type = missingBean.getClass("type");
      if (type != void.class) {
        return !context.containsBeanDefinition(type, missingBean.getBoolean("equals"));
      }
      else {
        return !context.containsBeanDefinition(PropsReader.getBeanClass(annotated));
      }
    }
    return false;
  }

  LinkedHashSet<MissingInfo> mayBeMissingInfos = new LinkedHashSet<>();

  public void detectMissingBean(MetadataReader metadataReader) {

  }

  static class MissingInfo {
    final MethodMetadata metadata;
    final Map<String, Object> missingBean;

    MissingInfo(MethodMetadata metadata, Map<String, Object> missingBean) {
      this.metadata = metadata;
      this.missingBean = missingBean;
    }
  }

  public void detectMissingBean(MethodMetadata metadata) {
    Map<String, Object> missingBean = metadata.getAnnotationAttributes(MissingBean.class.getName());
    // TODO passCondition 是否已经通过
    if (missingBean != null && context.passCondition(metadata)) {
      MissingInfo missingInfo = new MissingInfo(metadata, missingBean);
      mayBeMissingInfos.add(missingInfo);
    }
  }

  public void detectMissingBean(Method method) {

    // TODO 记录下来然后 统一注册
    AnnotationAttributes missingBean = AnnotatedElementUtils.getMergedAnnotationAttributes(
            method, MissingBean.class);
//    if (missingBean != null && context.passCondition(annotated)) {
//      missingInfos.add(new ScannedMissingInfo(classNode, missingBean));
//    }
  }

  public void registerMissing(AnnotationAttributes missingBean, BeanDefinition def) {
    // Missing BeanMetadata a flag to determine its a missed bean @since 3.0
    def.setAttribute(MissingBean.MissingBeanMetadata, missingBean);
    // register missed bean
    register(def);
  }

  public void registerMissing(MetadataReader metadataReader) {
    missingInfos.add(new ScannedMissingInfo(metadataReader));
  }

  static class ScannedMissingInfo {
    final MetadataReader metadata;

    ScannedMissingInfo(MetadataReader metadata) {
      this.metadata = metadata;
    }
  }

  /**
   * default is use {@link ClassUtils#getShortName(Class)}
   *
   * <p>
   * sub-classes can overriding this method to provide a strategy to create bean name
   * </p>
   *
   * @param type type
   * @return bean name
   * @see ClassUtils#getShortName(Class)
   */
  protected String createBeanName(Class<?> type) {
    return context.createBeanName(type);
  }

  public void register(BeanDefinition def) {
    context.registerBeanDefinition(def);
  }

}
