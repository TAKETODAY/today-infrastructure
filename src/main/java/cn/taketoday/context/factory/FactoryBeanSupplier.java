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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.factory;

import java.util.function.Supplier;

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.ClassUtils;

/**
 * {@link FactoryBean} {@link Supplier}
 * 
 * @author TODAY <br>
 *         2019-12-11 22:32
 */
public class FactoryBeanSupplier<T> implements Supplier<FactoryBean<T>> {

    private FactoryBean<T> factoryBean;
    private final String factoryBeanName;
    private final Class<FactoryBean<T>> beanClass;
    private final AbstractBeanFactory beanFactory;

    @SuppressWarnings("unchecked")
    public FactoryBeanSupplier(BeanDefinition factoryDef, AbstractBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.beanClass = (Class<FactoryBean<T>>) factoryDef.getBeanClass();
        this.factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX.concat(factoryDef.getName());
    }

    @Override
    public FactoryBean<T> get() {
        FactoryBean<T> factoryBean = this.factoryBean;
        if (factoryBean != null) {
            return factoryBean;
        }
        return createIfNecessary(factoryBeanName, beanClass, beanFactory);
    }

    protected FactoryBean<T> createIfNecessary(final String factoryBeanName,
                                               final Class<FactoryBean<T>> beanClass,
                                               final AbstractBeanFactory beanFactory) {

        final Object obj = beanFactory.getSingleton(factoryBeanName);
        if (obj instanceof FactoryBean) {
            return beanClass.cast(obj);
        }
        try {
            // Not exist declaring instance(FactoryBean), create it
            FactoryBean<T> factoryBean = ClassUtils.newInstance(beanClass, beanFactory);
            beanFactory.registerSingleton(factoryBeanName, factoryBean);
            return this.factoryBean = factoryBean;
        }
        catch (ReflectiveOperationException e) {
            throw new ConfigurationException("Cannot create a bean: [" + beanClass + "]");
        }
    }
}
