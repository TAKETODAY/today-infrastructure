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
package infra.context.aware;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanNameAware;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.context.EnvironmentAware;
import infra.core.env.Environment;
import infra.stereotype.Singleton;
import lombok.Getter;

/**
 * @author Today <br>
 *
 * 2018-08-08 16:32
 */
@Singleton
@Getter
public class AwareBean implements ApplicationContextAware, BeanFactoryAware, BeanNameAware, EnvironmentAware {

  private String beanName;

  private BeanFactory beanFactory;

  private ApplicationContext applicationContext;

  private Environment environment;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public String toString() {
    return new StringBuilder()//
            .append("{\n\t\"beanName\":\"")//
            .append(beanName)//
            .append("\",\n\t\"beanFactory\":\"")//
            .append(beanFactory)//
            .append("\",\n\t\"applicationContext\":\"")//
            .append(applicationContext)//
            .append("\",\n\t\"environment\":\"")//
            .append(environment)//
            .append("\"\n}")//
            .toString();
  }

}
