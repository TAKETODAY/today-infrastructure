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

import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.annotation.Annotation;
import java.util.Map;

import infra.beans.PropertyValues;
import infra.beans.factory.BeanNameAware;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.PropertyResourceConfigurer;
import infra.beans.factory.config.TypedStringValue;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import infra.beans.factory.support.BeanNameGenerator;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.ConfigurableApplicationContext;
import infra.core.env.Environment;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.orm.mybatis.SqlSessionTemplate;
import infra.util.StringUtils;

/**
 * BeanDefinitionRegistryPostProcessor that searches recursively starting from a base package for interfaces and
 * registers them as {@code MapperFactoryBean}. Note that only interfaces with at least one method will be registered;
 * concrete classes will be ignored.
 * <p>
 * The {@code basePackage} property can contain more than one package name, separated by either commas or semicolons.
 * <p>
 * This class supports filtering the mappers created by either specifying a marker interface or an annotation. The
 * {@code annotationClass} property specifies an annotation to search for. The {@code markerInterface} property
 * specifies a parent interface to search for. If both properties are specified, mappers are added for interfaces that
 * match <em>either</em> criteria. By default, these two properties are null, so all interfaces in the given
 * {@code basePackage} are added as mappers.
 * <p>
 * This configurer enables autowire for all the beans that it creates so that they are automatically autowired with the
 * proper {@code SqlSessionFactory} or {@code SqlSessionTemplate}. If there is more than one {@code SqlSessionFactory}
 * in the application, however, autowiring cannot be used. In this case you must explicitly specify either an
 * {@code SqlSessionFactory} or an {@code SqlSessionTemplate} to use via the <em>bean name</em> properties. Bean names
 * are used rather than actual objects because Framework does not initialize property placeholders until after this class
 * is processed.
 * <p>
 * Passing in an actual object which may require placeholders (i.e. DB user password) will fail. Using bean names defers
 * actual object creation until later in the startup process, after all placeholder substitution is completed. However,
 * note that this configurer does support property placeholders of its <em>own</em> properties. The
 * <code>basePackage</code> and bean name properties all support <code>${property}</code> style substitution.
 * <p>
 * Configuration sample:
 *
 * <pre>
 * {@code
 *   <bean class="infra.orm.mapper.MapperScannerConfigurer">
 *       <property name="basePackage" value="sample.mapper" />
 *       <!-- optional unless there are multiple session factories defined -->
 *       <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
 *   </bean>
 * }
 * </pre>
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MapperFactoryBean
 * @see ClassPathMapperScanner
 * @since 4.0
 */
public class MapperScannerConfigurer
        implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {

  private String basePackage;

  private boolean addToConfig = true;

  private String lazyInitialization;

  private SqlSessionFactory sqlSessionFactory;

  private SqlSessionTemplate sqlSessionTemplate;

  private String sqlSessionFactoryBeanName;

  private String sqlSessionTemplateBeanName;

  @Nullable
  private Class<? extends Annotation> annotationClass;

  @Nullable
  private Class<?> markerInterface;

  @SuppressWarnings("rawtypes")
  private Class<? extends MapperFactoryBean> mapperFactoryBeanClass;

  private ApplicationContext applicationContext;

  private String beanName;

  private boolean processPropertyPlaceHolders;

  private BeanNameGenerator nameGenerator;

  private String defaultScope;

  /**
   * This property lets you set the base package for your mapper interface files.
   * <p>
   * You can set more than one package by using a semicolon or comma as a separator.
   * <p>
   * Mappers will be searched for recursively starting in the specified package(s).
   *
   * @param basePackage base package name
   */
  public void setBasePackage(String basePackage) {
    this.basePackage = basePackage;
  }

  /**
   * Same as {@code MapperFactoryBean#setAddToConfig(boolean)}.
   *
   * @param addToConfig a flag that whether add mapper to MyBatis or not
   * @see MapperFactoryBean#setAddToConfig(boolean)
   */
  public void setAddToConfig(boolean addToConfig) {
    this.addToConfig = addToConfig;
  }

  /**
   * Set whether enable lazy initialization for mapper bean.
   * <p>
   * Default is {@code false}.
   * </p>
   *
   * @param lazyInitialization Set the @{code true} to enable
   */
  public void setLazyInitialization(String lazyInitialization) {
    this.lazyInitialization = lazyInitialization;
  }

  /**
   * This property specifies the annotation that the scanner will search for.
   * <p>
   * The scanner will register all interfaces in the base package that also have the specified annotation.
   * <p>
   * Note this can be combined with markerInterface.
   *
   * @param annotationClass annotation class
   */
  public void setAnnotationClass(@Nullable Class<? extends Annotation> annotationClass) {
    this.annotationClass = annotationClass;
  }

  /**
   * This property specifies the parent that the scanner will search for.
   * <p>
   * The scanner will register all interfaces in the base package that also have the specified interface class as a
   * parent.
   * <p>
   * Note this can be combined with annotationClass.
   *
   * @param superClass parent class
   */
  public void setMarkerInterface(@Nullable Class<?> superClass) {
    this.markerInterface = superClass;
  }

  /**
   * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the Framework context.
   * Usually this is only needed when you have more than one datasource.
   * <p>
   *
   * @param sqlSessionTemplate a template of SqlSession
   */
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  /**
   * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the Framework context.
   * Usually this is only needed when you have more than one datasource.
   * <p>
   * Note bean names are used, not bean references. This is because the scanner loads early during the start process and
   * it is too early to build mybatis object instances.
   *
   * @param sqlSessionTemplateName Bean name of the {@code SqlSessionTemplate}
   */
  public void setSqlSessionTemplateBeanName(String sqlSessionTemplateName) {
    this.sqlSessionTemplateBeanName = sqlSessionTemplateName;
  }

  /**
   * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the Framework context.
   * Usually this is only needed when you have more than one datasource.
   * <p>
   *
   * @param sqlSessionFactory a factory of SqlSession
   */
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
  }

  /**
   * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the Framework context.
   * Usually this is only needed when you have more than one datasource.
   * <p>
   * Note bean names are used, not bean references. This is because the scanner loads early during the start process and
   * it is too early to build mybatis object instances.
   *
   * @param sqlSessionFactoryName Bean name of the {@code SqlSessionFactory}
   */
  public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
    this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
  }

  /**
   * Specifies a flag that whether execute a property placeholder processing or not.
   * <p>
   * The default is {@literal false}. This means that a property placeholder processing does not execute.
   *
   * @param processPropertyPlaceHolders a flag that whether execute a property placeholder processing or not
   */
  public void setProcessPropertyPlaceHolders(boolean processPropertyPlaceHolders) {
    this.processPropertyPlaceHolders = processPropertyPlaceHolders;
  }

  /**
   * The class of the {@link MapperFactoryBean} to return a mybatis proxy as Framework bean.
   *
   * @param mapperFactoryBeanClass The class of the MapperFactoryBean
   */
  @SuppressWarnings("rawtypes")
  public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
    this.mapperFactoryBeanClass = mapperFactoryBeanClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  /**
   * Gets BeanNameGenerator to be used while running the scanner.
   *
   * @return the BeanNameGenerator that has been configured
   */
  public BeanNameGenerator getNameGenerator() {
    return nameGenerator;
  }

  /**
   * Sets BeanNameGenerator to be used while running the scanner.
   *
   * @param nameGenerator the BeanNameGenerator to set
   */
  public void setNameGenerator(BeanNameGenerator nameGenerator) {
    this.nameGenerator = nameGenerator;
  }

  /**
   * Sets the default scope of scanned mappers.
   * <p>
   * Default is {@code null} (equiv to singleton).
   * </p>
   *
   * @param defaultScope the default scope
   */
  public void setDefaultScope(String defaultScope) {
    this.defaultScope = defaultScope;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(this.basePackage, "Property 'basePackage' is required");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    // left intentionally blank
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    if (this.processPropertyPlaceHolders) {
      processPropertyPlaceHolders();
    }

    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
    scanner.setAddToConfig(this.addToConfig);
    scanner.setAnnotationClass(this.annotationClass);
    scanner.setMarkerInterface(this.markerInterface);
    scanner.setSqlSessionFactory(this.sqlSessionFactory);
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
    scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
    scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
    scanner.getScanner().setResourceLoader(this.applicationContext);
    scanner.getScanner().setBeanNameGenerator(this.nameGenerator);
    scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);

    if (StringUtils.hasText(lazyInitialization)) {
      scanner.setLazyInitialization(Boolean.parseBoolean(lazyInitialization));
    }
    if (StringUtils.hasText(defaultScope)) {
      scanner.setDefaultScope(defaultScope);
    }
    scanner.registerFilters();
    scanner.scan(StringUtils.tokenizeToStringArray(
            this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  }

  /*
   * BeanDefinitionRegistries are called early in application startup, before BeanFactoryPostProcessors. This means that
   * PropertyResourceConfigurers will not have been loaded and any property substitution of this class' properties will
   * fail. To avoid this, find any PropertyResourceConfigurers defined in the context and run them on this class' bean
   * definition. Then update the values.
   */
  private void processPropertyPlaceHolders() {
    Map<String, PropertyResourceConfigurer> prcs = applicationContext.getBeansOfType(
            PropertyResourceConfigurer.class, false, false);

    if (!prcs.isEmpty()) {
      BeanDefinition mapperScannerBean = applicationContext.getBeanFactory().getBeanDefinition(beanName);

      // PropertyResourceConfigurer does not expose any methods to explicitly perform
      // property placeholder substitution. Instead, create a BeanFactory that just
      // contains this mapper scanner and post process the factory.
      StandardBeanFactory factory = new StandardBeanFactory();
      factory.registerBeanDefinition(beanName, mapperScannerBean);

      for (PropertyResourceConfigurer prc : prcs.values()) {
        prc.postProcessBeanFactory(factory);
      }

      PropertyValues values = mapperScannerBean.getPropertyValues();

      this.basePackage = getPropertyValue("basePackage", values);
      this.defaultScope = getPropertyValue("defaultScope", values);
      this.lazyInitialization = getPropertyValue("lazyInitialization", values);
      this.sqlSessionFactoryBeanName = getPropertyValue("sqlSessionFactoryBeanName", values);
      this.sqlSessionTemplateBeanName = getPropertyValue("sqlSessionTemplateBeanName", values);
    }

    if (this.basePackage != null) {
      this.basePackage = getEnvironment().resolvePlaceholders(basePackage);
    }
    if (this.sqlSessionFactoryBeanName != null) {
      this.sqlSessionFactoryBeanName = getEnvironment().resolvePlaceholders(sqlSessionFactoryBeanName);
    }
    if (this.sqlSessionTemplateBeanName != null) {
      this.sqlSessionTemplateBeanName = getEnvironment().resolvePlaceholders(sqlSessionTemplateBeanName);
    }
    if (this.lazyInitialization != null) {
      this.lazyInitialization = getEnvironment().resolvePlaceholders(lazyInitialization);
    }
    if (this.defaultScope != null) {
      this.defaultScope = getEnvironment().resolvePlaceholders(defaultScope);
    }
  }

  private Environment getEnvironment() {
    return this.applicationContext.getEnvironment();
  }

  private String getPropertyValue(String propertyName, PropertyValues values) {
    Object value = values.getPropertyValue(propertyName);

    if (value == null) {
      return null;
    }
    else if (value instanceof String) {
      return value.toString();
    }
    else if (value instanceof TypedStringValue) {
      return ((TypedStringValue) value).getValue();
    }
    else {
      return null;
    }
  }

}
