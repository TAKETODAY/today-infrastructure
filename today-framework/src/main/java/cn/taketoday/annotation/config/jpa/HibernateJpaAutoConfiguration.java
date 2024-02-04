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

package cn.taketoday.annotation.config.jpa;

import org.hibernate.engine.spi.SessionImplementor;

import cn.taketoday.annotation.config.jdbc.DataSourceAutoConfiguration;
import cn.taketoday.annotation.config.jdbc.DataSourceTransactionManagerAutoConfiguration;
import cn.taketoday.annotation.config.transaction.TransactionAutoConfiguration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean;
import jakarta.persistence.EntityManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Manuel Doninger
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration(
        after = { DataSourceAutoConfiguration.class },
        before = { TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class })
@ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class, EntityManager.class, SessionImplementor.class })
@EnableConfigurationProperties(JpaProperties.class)
@Import(HibernateJpaConfiguration.class)
public class HibernateJpaAutoConfiguration {

}
