/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 * @since 4.0
 */
public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar {

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
    MergedAnnotation<MapperScan> mapperScan = importMetadata.getAnnotation(MapperScan.class);
    if (mapperScan.isPresent()) {
      registerBeanDefinitions(importMetadata, mapperScan,
              context.getRegistry(), generateBaseBeanName(importMetadata, 0));
    }
  }

  protected void registerBeanDefinitions(
          AnnotationMetadata annoMeta, MergedAnnotation<MapperScan> mapperScan,
          BeanDefinitionRegistry registry, String beanName) {

    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
    builder.addPropertyValue("processPropertyPlaceHolders", true);

    Class<? extends Annotation> annotationClass = mapperScan.getClass("annotationClass");
    if (!Annotation.class.equals(annotationClass)) {
      builder.addPropertyValue("annotationClass", annotationClass);
    }

    Class<?> markerInterface = mapperScan.getClass("markerInterface");
    if (!Class.class.equals(markerInterface)) {
      builder.addPropertyValue("markerInterface", markerInterface);
    }

    Class<? extends BeanNameGenerator> generatorClass = mapperScan.getClass("nameGenerator");
    if (!BeanNameGenerator.class.equals(generatorClass)) {
      builder.addPropertyValue("nameGenerator", BeanUtils.newInstance(generatorClass));
    }

    Class<? extends MapperFactoryBean<?>> mapperFactoryBeanClass = mapperScan.getClass("factoryBean");
    if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
      builder.addPropertyValue("mapperFactoryBeanClass", mapperFactoryBeanClass);
    }

    String sqlSessionTemplateRef = mapperScan.getString("sqlSessionTemplateRef");
    if (StringUtils.hasText(sqlSessionTemplateRef)) {
      builder.addPropertyValue("sqlSessionTemplateBeanName", sqlSessionTemplateRef);
    }

    String sqlSessionFactoryRef = mapperScan.getString("sqlSessionFactoryRef");
    if (StringUtils.hasText(sqlSessionFactoryRef)) {
      builder.addPropertyValue("sqlSessionFactoryBeanName", sqlSessionFactoryRef);
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
      builder.addPropertyValue("lazyInitialization", lazyInitialization);
    }

    String defaultScope = mapperScan.getString("defaultScope");
    if (StringUtils.hasText(defaultScope)) {
      builder.addPropertyValue("defaultScope", defaultScope);
    }

    builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));
    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

    registry.registerBeanDefinition(beanName, builder.getRawBeanDefinition());
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
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BootstrapContext context) {
      MergedAnnotation<MapperScans> annotation = importingClassMetadata.getAnnotation(MapperScans.class);
      if (annotation.isPresent()) {

        int i = 0;
        var mapperScans = annotation.getAnnotationArray(MergedAnnotation.VALUE, MapperScan.class);
        for (MergedAnnotation<MapperScan> mapperScan : mapperScans) {
          registerBeanDefinitions(importingClassMetadata, mapperScan, context.getRegistry(),
                  generateBaseBeanName(importingClassMetadata, i++));
        }
      }
    }
  }

}
