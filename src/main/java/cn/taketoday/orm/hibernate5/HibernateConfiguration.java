/**
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

import javax.persistence.Entity;
import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.event.ApplicationContextEvent;
import cn.taketoday.context.event.ApplicationEventCapable;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ContextRefreshEvent;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ContextUtils;

/**
 * @author TODAY <br>
 * 2019-11-05 22:11
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class HibernateConfiguration extends Configuration
        implements ApplicationListener<ApplicationContextEvent>, ApplicationEventCapable {

  public static final String SESSION_FACTORY_BEAN_NAME = "org.hibernate.SessionFactory";

  public SessionFactory buildSessionFactory(DataSource dataSource, Properties hibernateProperties) {

    hibernateProperties.put(AvailableSettings.DATASOURCE, dataSource);
    hibernateProperties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, HibernateSessionContext.class.getName());
    hibernateProperties.put(AvailableSettings.CLASSLOADERS, Collections.singleton(ClassUtils.getClassLoader()));

    setProperties(hibernateProperties);

    return super.buildSessionFactory();
  }

  @Override
  public void onApplicationEvent(ApplicationContextEvent event) {

    if (event instanceof ContextRefreshEvent) {
      // TODO 修复懒加载模式下错误
      refreshSessionFactory(event.getApplicationContext());
    }
    else if (event instanceof LoadingMissingBeanEvent) {
      registerSessionFactoryBean(((LoadingMissingBeanEvent) event).getCandidates(), event.getApplicationContext());
    }
  }

  protected void refreshSessionFactory(final ApplicationContext applicationContext) {
    if (applicationContext.getSingleton(SESSION_FACTORY_BEAN_NAME) == null) {
      final DataSource dataSource = applicationContext.getBean(DataSource.class);
      if (dataSource == null) {
        throw new ConfigurationException("You must provide a javax.sql.DataSource bean");
      }
      final Properties properties = ContextUtils.loadProps(
              new DefaultProps().setPrefix("hibernate."),
              applicationContext.getEnvironment().getProperties());

      applicationContext.registerSingleton(SESSION_FACTORY_BEAN_NAME, buildSessionFactory(dataSource, properties));
      LoggerFactory.getLogger(getClass()).info("Refresh 'SessionFactory' bean");
    }
  }

  protected void registerSessionFactoryBean(Collection<Class<?>> candidates, ApplicationContext context) {

    for (Class<?> entityClass : candidates) {
      if (entityClass.isAnnotationPresent(Entity.class)) {
        addAnnotatedClass(entityClass);
      }
    }

    final BeanDefinition def = context.getEnvironment()
            .getBeanDefinitionLoader().createBeanDefinition(SESSION_FACTORY_BEAN_NAME, SessionFactory.class);

    def.setDestroyMethods("close");
    context.registerBeanDefinition(def);
    LoggerFactory.getLogger(getClass()).info("Register 'SessionFactory' bean definition");
  }

  @Override
  public Class<?>[] getApplicationEvent() {
    return new Class<?>[] { LoadingMissingBeanEvent.class, ContextRefreshEvent.class };
  }

}
