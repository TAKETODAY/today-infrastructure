/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.orm.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.annotation.Annotation;
import java.util.Optional;

import cn.taketoday.aop.scope.ScopedProxyFactoryBean;
import cn.taketoday.aop.scope.ScopedProxyUtils;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.util.StringUtils;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers Mappers by {@code basePackage}, {@code annotationClass}, or
 * {@code markerInterface}. If an {@code annotationClass} and/or {@code markerInterface} is specified, only the
 * specified types will be searched (searching for all interfaces will be disabled).
 * <p>
 * This functionality was previously a private class of {@link MapperScannerConfigurer}
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @see MapperFactoryBean
 * @since 4.0
 */
public class ClassPathMapperScanner {
  private static final Logger log = LoggerFactory.getLogger(ClassPathMapperScanner.class);

  // Copy of FactoryBean#OBJECT_TYPE_ATTRIBUTE
  static final String FACTORY_BEAN_OBJECT_TYPE = FactoryBean.OBJECT_TYPE_ATTRIBUTE;

  private boolean addToConfig = true;

  private boolean lazyInitialization;

  @Nullable
  private SqlSessionFactory sqlSessionFactory;

  @Nullable
  private SqlSessionTemplate sqlSessionTemplate;

  @Nullable
  private String sqlSessionTemplateBeanName;

  @Nullable
  private String sqlSessionFactoryBeanName;

  @Nullable
  private Class<? extends Annotation> annotationClass = Mapper.class;

  @Nullable
  private Class<?> markerInterface;

  private Class<? extends MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;

  private String defaultScope;

  private final ClassPathBeanDefinitionScanner delegate;

  public ClassPathMapperScanner(BeanDefinitionRegistry registry) {
    this.delegate = new ClassPathBeanDefinitionScanner(registry, false);
    delegate.setCandidateComponentPredicate(this::isCandidateComponent);
  }

  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  public void setAnnotationClass(@Nullable Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  /**
   * Set whether enable lazy initialization for mapper bean.
   * <p>
   * Default is {@code false}.
   * </p>
   *
   * @param lazyInitialization Set the @{code true} to enable
   */
  public void setLazyInitialization(boolean lazyInitialization) {
    this.lazyInitialization = lazyInitialization;
  }

  public void setMarkerInterface(@Nullable Class<?> markerInterface) {
    this.markerInterface = markerInterface;
  }

  public void setSqlSessionFactory(@Nullable SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  public void setSqlSessionTemplate(@Nullable SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  public void setSqlSessionTemplateBeanName(@Nullable String sqlSessionTemplateBeanName) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateBeanName;
  }

  public void setSqlSessionFactoryBeanName(@Nullable String sqlSessionFactoryBeanName) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName;
  }

  /**
   * Set the {@code MapperFactoryBean} class.
   *
   * @param mapperFactoryBeanClass the {@code MapperFactoryBean} class
   */
  public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
    this.mapperFactoryBeanClass = mapperFactoryBeanClass == null ? MapperFactoryBean.class : mapperFactoryBeanClass;
  }

  /**
   * Set the default scope of scanned mappers.
   * <p>
   * Default is {@code null} (equiv to singleton).
   * </p>
   *
   * @param defaultScope the scope
   */
  public void setDefaultScope(String defaultScope) {
    this.defaultScope = defaultScope;
  }

  /**
   * Configures parent scanner to search for the right interfaces. It can search for all interfaces or just for those
   * that extends a markerInterface or/and those annotated with the annotationClass
   */
  public void registerFilters() {
    boolean acceptAllInterfaces = true;

    // if specified, use the given annotation and / or marker interface
    if (this.annotationClass != null) {
      delegate.addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
      acceptAllInterfaces = false;
    }

    // override AssignableTypeFilter to ignore matches on the actual marker interface
    if (this.markerInterface != null) {
      delegate.addIncludeFilter(new AssignableTypeFilter(this.markerInterface) {
        @Override
        protected boolean matchClassName(String className) {
          return false;
        }
      });
      acceptAllInterfaces = false;
    }

    if (acceptAllInterfaces) {
      // default include filter that accepts all classes
      delegate.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
    }

    // exclude package-info.java
    delegate.addExcludeFilter((metadataReader, metadataReaderFactory) -> {
      String className = metadataReader.getClassMetadata().getClassName();
      return className.endsWith("package-info");
    });
  }

  /**
   * Calls the parent search that will search and register all the candidates. Then the registered objects are post
   * processed to set them as MapperFactoryBeans
   */
  public void scan(String... basePackages) {
//    if (beanDefinitions.isEmpty()) {
//      log.warn("No MyBatis mapper was found in '{}' package. Please check your configuration.", Arrays.toString(basePackages));
//    }
    delegate.scan(this::postProcessBeanDefinition, basePackages);
  }

  protected void postProcessBeanDefinition(BeanDefinitionHolder holder) {
    AbstractBeanDefinition definition = (AbstractBeanDefinition) holder.getBeanDefinition();

    boolean scopedProxy = false;
    if (ScopedProxyFactoryBean.class.getName().equals(definition.getBeanClassName())) {
      definition = (AbstractBeanDefinition) Optional
              .ofNullable(((RootBeanDefinition) definition).getDecoratedDefinition())
              .map(BeanDefinitionHolder::getBeanDefinition).orElseThrow(() -> new IllegalStateException(
                      "The target bean definition of scoped proxy bean not found. Root bean definition[" + holder + "]"));
      scopedProxy = true;
    }

    String beanClassName = definition.getBeanClassName();
    log.debug("Creating MapperFactoryBean with name '{}' and '{}' mapperInterface",
            holder.getBeanName(), beanClassName);

    // the mapper interface is the original class of the bean
    // but, the actual class of the bean is MapperFactoryBean
    definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59

    definition.setBeanClass(mapperFactoryBeanClass);

    PropertyValues propertyValues = definition.getPropertyValues();
    propertyValues.add("addToConfig", addToConfig);

    // Attribute for MockitoPostProcessor
    // https://github.com/mybatis/spring-boot-starter/issues/475
    definition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, beanClassName);

    boolean explicitFactoryUsed = false;
    if (StringUtils.hasText(sqlSessionFactoryBeanName)) {
      propertyValues.add("sqlSessionFactory", RuntimeBeanReference.from(sqlSessionFactoryBeanName));
      explicitFactoryUsed = true;
    }
    else if (sqlSessionFactory != null) {
      propertyValues.add("sqlSessionFactory", sqlSessionFactory);
      explicitFactoryUsed = true;
    }

    if (StringUtils.hasText(sqlSessionTemplateBeanName)) {
      if (explicitFactoryUsed) {
        log.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
      }
      propertyValues.add("sqlSessionTemplate", RuntimeBeanReference.from(sqlSessionTemplateBeanName));
      explicitFactoryUsed = true;
    }
    else if (sqlSessionTemplate != null) {
      if (explicitFactoryUsed) {
        log.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
      }
      propertyValues.add("sqlSessionTemplate", sqlSessionTemplate);
      explicitFactoryUsed = true;
    }

    if (!explicitFactoryUsed) {
      log.debug("Enabling autowire by type for MapperFactoryBean with name '{}'.", holder.getBeanName());
      definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    }

    definition.setLazyInit(lazyInitialization);

    if (!scopedProxy) {
      if (BeanDefinition.SCOPE_SINGLETON.equals(definition.getScope()) && defaultScope != null) {
        definition.setScope(defaultScope);
      }
      if (!definition.isSingleton()) {
        BeanDefinitionRegistry registry = delegate.getRegistry();
        BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
        if (registry.containsBeanDefinition(proxyHolder.getBeanName())) {
          registry.removeBeanDefinition(proxyHolder.getBeanName());
        }
        registry.registerBeanDefinition(proxyHolder.getBeanName(), proxyHolder.getBeanDefinition());
      }
    }
  }

  protected boolean isCandidateComponent(AnnotationMetadata metadata) {
    return metadata.isInterface() && metadata.isIndependent();
  }

//
//  /**
//   * {@inheritDoc}
//   */
//  @Override
//  protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
//    if (super.checkCandidate(beanName, beanDefinition)) {
//      return true;
//    }
//    else {
//      log.warn("Skipping MapperFactoryBean with name '{}' and '{}' mapperInterface. Bean already defined with the same name!",
//              beanName, beanDefinition.getBeanClassName());
//      return false;
//    }
//  }

  public ClassPathBeanDefinitionScanner getScanner() {
    return delegate;
  }

}
