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
package cn.taketoday.beans.factory.config;

import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;

/**
 * Factory hook that allows for custom modification of new bean instances
 * &mdash; for example, checking for marker interfaces or wrapping beans with
 * proxies.
 * <p>
 * <b>NOTE</b>: this mainly a marker interface for post processing a bean,
 * this is not like  BeanPostProcessor
 * </p>
 * <p>
 * Typically, post-processors that populate beans via marker interfaces or the
 * like will implement {@link InitializationBeanPostProcessor#postProcessBeforeInitialization(Object, String)},
 * while post-processors that wrap beans with proxies will normally implement
 * {@link InitializationBeanPostProcessor#postProcessAfterInitialization(Object, String)}.
 *
 * <h3>Registration</h3>
 * <p>
 * An {@code ApplicationContext} can autodetect {@code BeanPostProcessor} beans
 * in its bean definitions and apply those post-processors to any beans
 * subsequently created. A plain {@code BeanFactory} allows for programmatic
 * registration of post-processors, applying them to all beans created through
 * the bean factory.
 *
 * <h3>Ordering</h3>
 * <p>
 * {@code BeanPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link Ordered Ordered} semantics. In contrast,
 * {@code BeanPostProcessor} beans that are registered programmatically with a
 * {@code BeanFactory} will be applied in the order of registration; any
 * ordering semantics expressed through implementing the {@code PriorityOrdered}
 * or {@code Ordered} interface will be ignored for programmatically registered
 * post-processors. Furthermore, the
 * {@link Order @Order} annotation is not taken
 * into account for {@code BeanPostProcessor} beans.
 *
 * @author TODAY 2018-07-18 1:01:19
 * @see InitializationBeanPostProcessor
 * @see ConfigurableBeanFactory#addBeanPostProcessor(BeanPostProcessor)
 */
public interface BeanPostProcessor {

}
