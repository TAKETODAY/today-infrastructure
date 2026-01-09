/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.config;

import infra.beans.factory.InitializationBeanPostProcessor;
import infra.core.Ordered;
import infra.core.annotation.Order;

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
