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

import cn.taketoday.beans.dependency.DisableDependencyInjection;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.annotation.ConfigBeanDefinition;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * @author TODAY 2021/10/19 21:47
 * @since 4.0
 */
public class MissingBeanRegistry {
  private static final Logger log = LoggerFactory.getLogger(MissingBeanRegistry.class);

  private final DefinitionLoadingContext context;
  final ArrayList<ScannedMissingInfo> missingInfos = new ArrayList<>();

  public MissingBeanRegistry(DefinitionLoadingContext context) {
    this.context = context;
  }

  LinkedHashSet<MissingInfo> mayBeMissingInfos = new LinkedHashSet<>();

  public void process() {
    for (MissingInfo missingInfo : mayBeMissingInfos) {
      if (isMissingBeanInFactory(missingInfo)) {
        registerMissing(missingInfo);
      }
    }
  }

  public void registerMissing(MissingInfo missingInfo) {
    MethodMetadata beanMethod = missingInfo.metadata;
    log.debug("register missing bean: {}", beanMethod);
    AnnotationMetadata annotationMetadata = context.getAnnotationMetadata(
            beanMethod.getReturnTypeName());
    ConfigBeanDefinition definition = new ConfigBeanDefinition(
            missingInfo.config, beanMethod, annotationMetadata);
    MergedAnnotation<MissingBean> missingBean = missingInfo.missingBean;

    String name = missingBean.getString(MergedAnnotation.VALUE);
    if (StringUtils.hasText(name)) {
      definition.setName(name);
    }
    else {
      definition.setName(beanMethod.getMethodName());
    }

    definition.setFactoryBeanName(missingInfo.config.getName());
    definition.setFactoryMethodName(beanMethod.getMethodName());
    definition.setDestroyMethod(missingBean.getString(BeanDefinition.DESTROY_METHOD));
    definition.setInitMethods(missingBean.getStringArray(BeanDefinition.INIT_METHODS));

    // DisableDependencyInjection
    if (beanMethod.getAnnotations().isPresent(DisableDependencyInjection.class)) {
      definition.setEnableDependencyInjection(false);
    }

    registerMissing(missingBean, definition);
  }

  public void registerMissing(MergedAnnotation<MissingBean> missingBean, BeanDefinition def) {
    // Missing BeanMetadata a flag to determine its a missed bean @since 3.0
    def.setAttribute(MissingBean.MissingBeanMetadata, missingBean);
    // register missed bean
    register(def);
  }

  public boolean isMissingBeanInFactory(MissingInfo missingInfo) {
    MergedAnnotation<MissingBean> missingBean = missingInfo.missingBean;
    // find by bean name
    String beanName = missingBean.getString(MergedAnnotation.VALUE);
    if (StringUtils.hasText(beanName)) {
      if (context.containsBeanDefinition(beanName)) {
        return false;
      }
    }
    else {
      // use default name -> method-name
      String methodName = missingInfo.metadata.getMethodName();
      if (context.containsBeanDefinition(methodName)) {
        return false;
      }
    }

    // find by type
    Class<?> type = missingBean.getClass("type");
    if (type != void.class) {
      return !context.containsBeanDefinition(type, missingBean.getBoolean("equals"));
    }
    // check method return-type
    String returnTypeName = missingInfo.metadata.getReturnTypeName();
    Class<?> returnType = ClassUtils.resolveClassName(
            returnTypeName, context.getApplicationContext().getClassLoader());
    return !context.containsBeanDefinition(returnType);
  }

  public void detectMissingBean(MetadataReader metadataReader) {

  }

  record MissingInfo(MethodMetadata metadata, BeanDefinition config, MergedAnnotation<MissingBean> missingBean) {

  }

  public void detectMissingBean(MethodMetadata metadata, BeanDefinition config) {
    MergedAnnotation<MissingBean> missingBean = metadata.getAnnotations().get(MissingBean.class);
    if (missingBean.isPresent() && context.passCondition(metadata)) {
      MissingInfo missingInfo = new MissingInfo(metadata, config, missingBean);
//      mayBeMissingInfos.add(missingInfo);
      if (isMissingBeanInFactory(missingInfo)) {
        registerMissing(missingInfo);
      }
    }
  }

  public void registerMissing(MetadataReader metadataReader) {
    missingInfos.add(new ScannedMissingInfo(metadataReader));
  }

  record ScannedMissingInfo(MetadataReader metadata) {

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
