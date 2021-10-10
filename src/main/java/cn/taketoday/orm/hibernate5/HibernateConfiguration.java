/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.orm.hibernate5;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Supplier;

import javax.persistence.Entity;
import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.context.aware.AnnotationImportAware;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.loader.BeanDefinitionReader;
import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.NonNull;
import cn.taketoday.core.Nullable;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY 2019-11-05 22:11
 */
public class HibernateConfiguration
        extends Configuration implements BeanDefinitionLoader, AnnotationImportAware<EnableHibernate> {

  // @since 4.0
  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String SESSION_FACTORY_BEAN_NAME = "org.hibernate.SessionFactory";

  // @since 4.0
  @Nullable
  private BeanDefinitionReader beanDefinitionReader;

  private String[] hibernatePropertiesPrefix = new String[] { "hibernate." };

  @Nullable
  private String dataSourceBeanName;

  @Override
  public void setImportBeanDefinition(EnableHibernate target, BeanDefinition importDef) {
    String[] propertiesPrefix = target.propertiesPrefix();
    if (ObjectUtils.isNotEmpty(propertiesPrefix)) {
      this.hibernatePropertiesPrefix = target.propertiesPrefix();
    }
  }

  protected SessionFactory refreshSessionFactory(ApplicationContext context) {
    DataSource dataSource = getDataSource(context);
    Properties hibernateProperties = getHibernateProperties(dataSource, context);
    setProperties(hibernateProperties);

    log.info("Refresh {} bean", b);
    return buildSessionFactory();
  }

  @NonNull
  private DataSource getDataSource(ApplicationContext context) {
    DataSource dataSource;
    if (StringUtils.isNotEmpty(dataSourceBeanName)) {
      dataSource = context.getBean(dataSourceBeanName, DataSource.class);
    }
    else {
      dataSource = context.getBean("dataSourceBeanName", DataSource.class);
    }
    if (dataSource == null) {
      throw new ConfigurationException("You must provide a javax.sql.DataSource bean");
    }
    return dataSource;
  }

  protected Properties getHibernateProperties(DataSource dataSource, ApplicationContext context) {
    PropsReader propsReader = new PropsReader(context);
    Properties properties = propsReader.readMap(
            new DefaultProps().setPrefix(hibernatePropertiesPrefix));

    properties.put(AvailableSettings.DATASOURCE, dataSource);
    properties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, HibernateSessionContext.class.getName());
    properties.put(AvailableSettings.CLASSLOADERS, Collections.singleton(ClassUtils.getDefaultClassLoader()));
    return properties;
  }

  protected void registerSessionFactoryBean(
          Collection<Class<?>> candidates, ApplicationContext context) {
    for (Class<?> entityClass : candidates) {
      if (entityClass.isAnnotationPresent(Entity.class)) {
        addAnnotatedClass(entityClass);
      }
    }

  }

  @Override
  public void loadBeanDefinitions(
          ConfigurableApplicationContext context, BeanDefinitionRegistry registry) {
    // @since 4.0
    if (beanDefinitionReader == null) {
      beanDefinitionReader = new BeanDefinitionReader(registry);
      beanDefinitionReader.setEnableConditionEvaluation(false);

      beanDefinitionReader.setCustomizers((attributes, definition) -> {
        definition.setSynthetic(true);
        definition.setDestroyMethods("close");
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        log.info("Register 'SessionFactory' bean definition {}", definition);

        definition.setSupplier(new Supplier<Object>() {
          @Override
          public Object get() {
            return refreshSessionFactory(context);
          }
        });
      });

    }

    beanDefinitionReader.registerBean(
            SESSION_FACTORY_BEAN_NAME, SessionFactory.class);

  }

  public void setHibernatePropertiesPrefix(String... hibernatePropertiesPrefix) {
    Assert.notNull(hibernatePropertiesPrefix, "hibernate-properties-prefix must not be null");
    this.hibernatePropertiesPrefix = hibernatePropertiesPrefix;
  }

  public String[] getHibernatePropertiesPrefix() {
    return hibernatePropertiesPrefix;
  }

  public void setBeanDefinitionReader(@Nullable BeanDefinitionReader beanDefinitionReader) {
    this.beanDefinitionReader = beanDefinitionReader;
  }

  @Nullable
  public BeanDefinitionReader getBeanDefinitionReader() {
    return beanDefinitionReader;
  }

  public void setDataSourceBeanName(@Nullable String dataSourceBeanName) {
    this.dataSourceBeanName = dataSourceBeanName;
  }

  @Nullable
  public String getDataSourceBeanName() {
    return dataSourceBeanName;
  }
}

