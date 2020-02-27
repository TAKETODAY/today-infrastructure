/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.factory;

/**
 * Factory hook that allows for custom modification of new bean instances
 * &mdash; for example, checking for marker interfaces or wrapping beans with
 * proxies.
 *
 * <p>
 * Typically, post-processors that populate beans via marker interfaces or the
 * like will implement {@link #postProcessBeforeInitialization}, while
 * post-processors that wrap beans with proxies will normally implement
 * {@link #postProcessAfterInitialization}.
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
 * {@link cn.taketoday.context.Ordered Ordered} semantics. In contrast,
 * {@code BeanPostProcessor} beans that are registered programmatically with a
 * {@code BeanFactory} will be applied in the order of registration; any
 * ordering semantics expressed through implementing the {@code PriorityOrdered}
 * or {@code Ordered} interface will be ignored for programmatically registered
 * post-processors. Furthermore, the
 * {@link cn.taketoday.context.annotation.Order @Order} annotation is not taken
 * into account for {@code BeanPostProcessor} beans.
 * 
 * @author TODAY <br>
 *         2018-07-18 1:01:19
 */
public interface BeanPostProcessor {

    /**
     * Apply this {@code BeanPostProcessor} to the given new bean instance
     * <i>before</i> any bean initialization callbacks (like InitializingBean's
     * {@code afterPropertiesSet} or a custom init-method). The bean will already be
     * populated with property values. The returned bean instance may be a wrapper
     * around the original.
     * <p>
     * The default implementation returns the given {@code bean} as-is.
     * 
     * @param bean
     *            The new bean instance
     * @param def
     *            The definition of the bean
     * @return the bean instance to use, either the original or a wrapped one; if
     *         {@code null}, no subsequent BeanPostProcessors will be invoked
     * @throws Exception
     *             in case of errors
     * @see cn.taketoday.context.factory.InitializingBean#afterPropertiesSet
     */
    default Object postProcessBeforeInitialization(Object bean, BeanDefinition def) throws Exception {
        return bean;
    }

    /**
     * Apply this {@code BeanPostProcessor} to the given new bean instance
     * <i>after</i> any bean initialization callbacks (like InitializingBean's
     * {@code afterPropertiesSet} or a custom init-method). The bean will already be
     * populated with property values. The returned bean instance may be a wrapper
     * around the original.
     * 
     * <p>
     * The default implementation returns the given {@code bean} as-is.
     * 
     * @param bean
     *            the new bean instance
     * @param beanName
     *            the name of the bean
     * @return the bean instance to use, either the original or a wrapped one; if
     *         {@code null}, no subsequent BeanPostProcessors will be invoked
     * @throws Exception
     *             in case of errors
     * @see cn.taketoday.context.factory.InitializingBean#afterPropertiesSet
     * @see cn.taketoday.context.factory.FactoryBean
     */
    default Object postProcessAfterInitialization(Object bean, BeanDefinition def) throws Exception {
        return bean;
    }
}
