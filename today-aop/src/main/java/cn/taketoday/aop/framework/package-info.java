/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

/**
 * Package containing Infra basic AOP infrastructure, compliant with the
 * <a href="http://aopalliance.sourceforge.net">AOP Alliance</a> interfaces.
 *
 * <p>Infra AOP supports proxying interfaces or classes, introductions, and offers
 * static and dynamic pointcuts.
 *
 * <p>Any Infra AOP proxy can be cast to the ProxyConfig AOP configuration interface
 * in this package to add or remove interceptors.
 *
 * <p>The ProxyFactoryBean is a convenient way to create AOP proxies in a BeanFactory
 * or ApplicationContext. However, proxies can be created programmatically using the
 * ProxyFactory class.
 */
@NonNullApi
@NonNullFields
package cn.taketoday.aop.framework;

import cn.taketoday.lang.NonNullApi;
import cn.taketoday.lang.NonNullFields;