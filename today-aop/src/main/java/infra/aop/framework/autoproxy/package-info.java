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

/**
 * Bean post-processors for use in ApplicationContexts to simplify AOP usage
 * by automatically creating AOP proxies without the need to use a ProxyFactoryBean.
 *
 * <p>The various post-processors in this package need only be added to an ApplicationContext
 * (typically in an XML bean definition document) to automatically proxy selected beans.
 *
 * <p><b>NB</b>: Automatic auto-proxying is not supported for BeanFactory implementations,
 * as post-processors beans are only automatically detected in application contexts.
 * Post-processors can be explicitly registered on a ConfigurableBeanFactory instead.
 */
@NonNullApi
@NonNullFields
package infra.aop.framework.autoproxy;

import infra.lang.NonNullApi;
import infra.lang.NonNullFields;
