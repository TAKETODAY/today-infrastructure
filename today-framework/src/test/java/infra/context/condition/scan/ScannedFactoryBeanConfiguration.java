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

package infra.context.condition.scan;

import infra.beans.factory.FactoryBean;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.condition.scan.ScanBean;
import infra.context.condition.scan.ScanFactoryBean;

/**
 * Configuration for a factory bean produced by a bean method on a configuration class
 * found via component scanning.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
public class ScannedFactoryBeanConfiguration {

  @Bean
  public FactoryBean<ScanBean> exampleBeanFactoryBean() {
    return new ScanFactoryBean("foo");
  }

}
