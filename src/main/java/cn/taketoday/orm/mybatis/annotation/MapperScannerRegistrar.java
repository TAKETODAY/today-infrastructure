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
package cn.taketoday.orm.mybatis.annotation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;

import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanNamePopulator;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanUtils;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.orm.mybatis.mapper.ClassPathMapperScanner;
import cn.taketoday.orm.mybatis.mapper.MapperFactoryBean;
import cn.taketoday.orm.mybatis.mapper.MapperScannerConfigurer;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * A {@link ImportBeanDefinitionRegistrar} to allow annotation configuration of MyBatis mapper scanning. Using
 * an @Enable annotation allows beans to be registered via @Component configuration, whereas implementing
 * {@code BeanDefinitionRegistryPostProcessor} will work for XML configuration.
 *
 * @author Michael Lanyon
 * @author Eduardo Macarron
 * @author Putthiphong Boonphong
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 * @since 4.0
 */
public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar {

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, DefinitionLoadingContext context) {
    MergedAnnotation<MapperScan> mapperScan = importMetadata.getAnnotation(MapperScan.class);
    if (mapperScan.isPresent()) {
      registerBeanDefinitions(importMetadata, mapperScan,
              context.getRegistry(), generateBaseBeanName(importMetadata, 0));
    }
  }

  protected void registerBeanDefinitions(
          AnnotationMetadata annoMeta, MergedAnnotation<MapperScan> mapperScan,
          BeanDefinitionRegistry registry, String beanName) {

    BeanDefinition definition = new BeanDefinition(MapperScannerConfigurer.class);
    definition.addPropertyValue("processPropertyPlaceHolders", true);

    Class<? extends Annotation> annotationClass = mapperScan.getClass("annotationClass");
    if (!Annotation.class.equals(annotationClass)) {
      definition.addPropertyValue("annotationClass", annotationClass);
    }

    Class<?> markerInterface = mapperScan.getClass("markerInterface");
    if (!Class.class.equals(markerInterface)) {
      definition.addPropertyValue("markerInterface", markerInterface);
    }

    Class<? extends BeanNamePopulator> generatorClass = mapperScan.getClass("namePopulator");
    if (!BeanNamePopulator.class.equals(generatorClass)) {
      definition.addPropertyValue("namePopulator", BeanUtils.newInstance(generatorClass));
    }

    Class<? extends MapperFactoryBean<?>> mapperFactoryBeanClass = mapperScan.getClass("factoryBean");
    if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
      definition.addPropertyValue("mapperFactoryBeanClass", mapperFactoryBeanClass);
    }

    String sqlSessionTemplateRef = mapperScan.getString("sqlSessionTemplateRef");
    if (StringUtils.hasText(sqlSessionTemplateRef)) {
      definition.addPropertyValue("sqlSessionTemplateBeanName", mapperScan.getString("sqlSessionTemplateRef"));
    }

    String sqlSessionFactoryRef = mapperScan.getString("sqlSessionFactoryRef");
    if (StringUtils.hasText(sqlSessionFactoryRef)) {
      definition.addPropertyValue("sqlSessionFactoryBeanName", mapperScan.getString("sqlSessionFactoryRef"));
    }

    ArrayList<String> basePackages = new ArrayList<>();
    basePackages.addAll(Arrays.stream(mapperScan.getStringArray("value"))
            .filter(StringUtils::hasText).toList()
    );

    basePackages.addAll(Arrays.stream(mapperScan.getStringArray("basePackages"))
            .filter(StringUtils::hasText).toList()
    );

    basePackages.addAll(Arrays.stream(mapperScan.getClassArray("basePackageClasses"))
            .map(ClassUtils::getPackageName).toList()
    );

    if (basePackages.isEmpty()) {
      basePackages.add(getDefaultBasePackage(annoMeta));
    }

    String lazyInitialization = mapperScan.getString("lazyInitialization");
    if (StringUtils.hasText(lazyInitialization)) {
      definition.addPropertyValue("lazyInitialization", lazyInitialization);
    }

    String defaultScope = mapperScan.getString("defaultScope");
    if (StringUtils.hasText(defaultScope)) {
      definition.addPropertyValue("defaultScope", defaultScope);
    }

    definition.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));

    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

    registry.registerBeanDefinition(beanName, definition);
  }

  private static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
    return importingClassMetadata.getClassName() + "#" +
            MapperScannerRegistrar.class.getSimpleName() + "#" + index;
  }

  private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
    return ClassUtils.getPackageName(importingClassMetadata.getClassName());
  }

  /**
   * A {@link MapperScannerRegistrar} for {@link MapperScans}.
   */
  static class RepeatingRegistrar extends MapperScannerRegistrar {

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, DefinitionLoadingContext context) {
      MergedAnnotation<MapperScans> annotation = importingClassMetadata.getAnnotation(MapperScans.class);
      if (annotation.isPresent()) {

        int i = 0;
        MergedAnnotation<MapperScan>[] mapperScans = annotation.getAnnotationArray(
                MergedAnnotation.VALUE, MapperScan.class);
        for (MergedAnnotation<MapperScan> mapperScan : mapperScans) {
          registerBeanDefinitions(importingClassMetadata, mapperScan, context.getRegistry(),
                  generateBaseBeanName(importingClassMetadata, i++));
        }
      }
    }
  }

}
