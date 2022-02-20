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

package cn.taketoday.framework.web.servlet.support;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.web.servlet.FilterRegistrationBean;
import jakarta.servlet.DispatcherType;

/**
 * Configuration for {@link ErrorPageFilter}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
class ErrorPageFilterConfiguration {

  @Bean
  ErrorPageFilter errorPageFilter() {
    return new ErrorPageFilter();
  }

  @Bean
  FilterRegistrationBean<ErrorPageFilter> errorPageFilterRegistration(ErrorPageFilter filter) {
    FilterRegistrationBean<ErrorPageFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setOrder(filter.getOrder());
    registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
    return registration;
  }

}
