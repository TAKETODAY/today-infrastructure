/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.orm.mybatis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.annotation.Annotation;
import java.util.Optional;

import infra.aop.scope.ScopedProxyFactoryBean;
import infra.aop.scope.ScopedProxyUtils;
import infra.beans.PropertyValues;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.RuntimeBeanReference;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.annotation.ClassPathBeanDefinitionScanner;
import infra.core.type.AnnotationMetadata;
import infra.core.type.filter.AnnotationTypeFilter;
import infra.core.type.filter.AssignableTypeFilter;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.orm.mybatis.SqlSessionTemplate;
import infra.util.StringUtils;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers Mappers by
 * {@code basePackage}, {@code annotationClass}, or {@code markerInterface}.
 * If an {@code annotationClass} and/or {@code markerInterface} is specified, only the
 * specified types will be searched (searching for all interfaces will be disabled).
 * <p>
 * This functionality was previously a private class of {@link MapperScannerConfigurer}
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MapperFactoryBean
 * @since 4.0
 */
public class ClassPathMapperScanner {
  private static final Logger log = LoggerFactory.getLogger(ClassPathMapperScanner.class);

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
    delegate.scan(this::postProcessBeanDefinition, basePackages);
  }

  protected BeanDefinitionHolder postProcessBeanDefinition(BeanDefinitionHolder holder) {
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
    propertyValues.add("mapperInterface", beanClassName);

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
        return ScopedProxyUtils.createScopedProxy(holder, registry, true);
      }
    }
    return holder;
  }

  protected boolean isCandidateComponent(AnnotationMetadata metadata) {
    return metadata.isInterface() && metadata.isIndependent();
  }

  public ClassPathBeanDefinitionScanner getScanner() {
    return delegate;
  }

}
