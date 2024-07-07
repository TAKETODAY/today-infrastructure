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

package cn.taketoday.context.testfixture.context.annotation;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class QualifierConfiguration {

  @SuppressWarnings("unused")
  private String bean;

  @Autowired
  @Qualifier("1")
  public void setBean(String bean) {
    this.bean = bean;
  }

  public static class BeansConfiguration {

    @Bean
    @Qualifier("1")
    public String one() {
      return "one";
    }

    @Bean
    @Qualifier("2")
    public String two() {
      return "two";
    }

  }

}
