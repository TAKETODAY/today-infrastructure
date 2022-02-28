/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import javax.sql.DataSource;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.DefaultProps;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.loader.AnnotationBeanDefinitionRegistrar;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import jakarta.persistence.Entity;

/**
 * @author TODAY 2019-11-05 22:11
 */
@cn.taketoday.context.annotation.Configuration(proxyBeanMethods = false)
public class HibernateConfiguration extends Configuration
        implements AnnotationBeanDefinitionRegistrar<EnableHibernate>, ApplicationContextAware {

  // @since 4.0
  private final Logger log = LoggerFactory.getLogger(getClass());

  public static final String SESSION_FACTORY_BEAN_NAME = "org.hibernate.SessionFactory";

  private String[] hibernatePropertiesPrefix = new String[] { "hibernate." };

  @Nullable
  private String dataSourceBeanName;

  private String sessionFactoryBeanName = SESSION_FACTORY_BEAN_NAME;

  private String[] entityPackages; // TODO entityPackages scan

  private ApplicationContext context;

  protected SessionFactory refreshSessionFactory() {
    Properties hibernateProperties = getHibernateProperties();
    setProperties(hibernateProperties);

    log.info("Refresh '{}' org.hibernate.SessionFactory bean", sessionFactoryBeanName);
    return buildSessionFactory();
  }

  @NonNull
  private DataSource getDataSource(ApplicationContext context) {
    DataSource dataSource;
    if (StringUtils.isNotEmpty(dataSourceBeanName)) {
      dataSource = context.getBean(dataSourceBeanName, DataSource.class);
    }
    else {
      dataSource = context.getBean(DataSource.class);
    }
    if (dataSource == null) {
      throw new ConfigurationException("You must provide a javax.sql.DataSource bean in " + context);
    }
    return dataSource;
  }

  protected Properties getHibernateProperties() {
    Assert.state(context != null, "No ApplicationContext");
    DataSource dataSource = getDataSource(context);
    PropsReader propsReader = new PropsReader(context);
    Properties properties = propsReader.readMap(
            new DefaultProps().setPrefix(hibernatePropertiesPrefix));

    properties.put(AvailableSettings.DATASOURCE, dataSource);
    properties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, HibernateSessionContext.class.getName());
    properties.put(AvailableSettings.CLASSLOADERS, Collections.singleton(ClassUtils.getDefaultClassLoader()));
    return properties;
  }

  // FIXME
  protected void registerSessionFactoryBean(Collection<Class<?>> candidates, ApplicationContext context) {
    for (Class<?> entityClass : candidates) {
      if (entityClass.isAnnotationPresent(Entity.class)) {
        addAnnotatedClass(entityClass);
      }
    }
  }

  @Override
  public void registerBeanDefinitions(
          EnableHibernate target, AnnotationMetadata annotatedMetadata, BootstrapContext context) {

    applySettings(target);

    BeanDefinition definition = new BeanDefinition(
            sessionFactoryBeanName, SessionFactory.class);

    definition.setSynthetic(true);
    definition.setDestroyMethod("close");
    definition.setInstanceSupplier(this::refreshSessionFactory);
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

    log.info("Register 'SessionFactory' bean definition {}", definition);

    context.registerBeanDefinition(definition);
  }

  private void applySettings(EnableHibernate target) {
    String[] propertiesPrefix = target.propertiesPrefix();
    if (ObjectUtils.isNotEmpty(propertiesPrefix)) {
      setHibernatePropertiesPrefix(propertiesPrefix);
    }

    String dataSourceName = target.dataSource();
    if (StringUtils.isNotEmpty(dataSourceName)) {
      setDataSourceBeanName(dataSourceName);
    }

    String sessionFactoryName = target.sessionFactory();
    if (StringUtils.isNotEmpty(sessionFactoryName)) {
      setSessionFactoryBeanName(sessionFactoryName);
    }

    String[] entityPackages = target.entityPackages();
    if (ObjectUtils.isNotEmpty(entityPackages)) {
      setEntityPackages(entityPackages);
    }

  }

  public void setHibernatePropertiesPrefix(String... hibernatePropertiesPrefix) {
    Assert.notNull(hibernatePropertiesPrefix, "hibernate-properties-prefix must not be null");
    this.hibernatePropertiesPrefix = hibernatePropertiesPrefix;
  }

  public String[] getHibernatePropertiesPrefix() {
    return hibernatePropertiesPrefix;
  }

  public void setDataSourceBeanName(@Nullable String dataSourceBeanName) {
    this.dataSourceBeanName = dataSourceBeanName;
  }

  @Nullable
  public String getDataSourceBeanName() {
    return dataSourceBeanName;
  }

  public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
    this.sessionFactoryBeanName = sessionFactoryBeanName;
  }

  public String getSessionFactoryBeanName() {
    return sessionFactoryBeanName;
  }

  public void setEntityPackages(String... entityPackages) {
    this.entityPackages = entityPackages;
  }

  public String[] getEntityPackages() {
    return entityPackages;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) {
    this.context = context;
  }

}

