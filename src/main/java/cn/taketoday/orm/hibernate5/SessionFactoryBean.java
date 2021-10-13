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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.hibernate5;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import javax.persistence.Entity;

import cn.taketoday.beans.DisposableBean;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.lang.Autowired;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author TODAY <br>
 * 2018-08-03 16:36
 */
public class SessionFactoryBean extends Configuration //
        implements FactoryBean<SessionFactory>, InitializingBean, DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(SessionFactoryBean.class);

  @Autowired
  private javax.sql.DataSource dataSource;

  @Props(prefix = { "hibernate." })
  private Properties hibernateProperties;

  private SessionFactory sessionFactory;

  private String[] entityLocations;

  @Override
  public void afterPropertiesSet() {

    if (this.hibernateProperties != null) {
      hibernateProperties.put(AvailableSettings.DATASOURCE, dataSource);
      hibernateProperties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, HibernateSessionContext.class.getName());
      hibernateProperties.put(AvailableSettings.CLASSLOADERS, Collections.singleton(ClassUtils.getDefaultClassLoader()));

      addProperties(this.hibernateProperties);
    }

    final String[] entityLocations = getEntityLocations();
    if (ObjectUtils.isEmpty(entityLocations)) {
      final Set<Class<?>> candidates = new CandidateComponentScanner().scan(entityLocations);
      candidates.parallelStream()
              .filter(c -> c.isAnnotationPresent(Entity.class))
              .forEach(this::addClass);
    }
    else {
      CandidateComponentScanner
              .getSharedInstance()
              .getAnnotatedClasses(Entity.class)
              .parallelStream()
              .forEach(this::addClass);
    }

    // Build SessionFactory instance.
    this.sessionFactory = buildSessionFactory();
    log.debug("SessionFactory build success.");
  }

  public final Configuration getConfiguration() {
    return this;
  }

  @Override
  public void destroy() {
    if (this.sessionFactory != null) {
      this.sessionFactory.close();
    }
  }

  @Override
  public SessionFactory getBean() {
    log.debug("Obtian sessionFactory");
    return this.sessionFactory;
  }

  @Override
  public Class<SessionFactory> getBeanClass() {
    return SessionFactory.class;
  }

  public String[] getEntityLocations() {
    return entityLocations;
  }

  public SessionFactoryBean setEntityLocations(String... entityLocations) {
    this.entityLocations = entityLocations;
    return this;
  }

}
