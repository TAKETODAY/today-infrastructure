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

package cn.taketoday.jdbc.config;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabase;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import cn.taketoday.stereotype.Component;

/**
 * Configuration for embedded data sources.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DataSourceAutoConfiguration
 * @since 4.0 2022/2/23 18:07
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataSourceProperties.class)
public class EmbeddedDataSourceConfiguration implements BeanClassLoaderAware {

  private ClassLoader classLoader;

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Component(destroyMethod = "shutdown")
  public EmbeddedDatabase dataSource(DataSourceProperties properties) {
    return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseConnection.get(this.classLoader).getType())
            .setName(properties.determineDatabaseName()).build();
  }

}
