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
package cn.taketoday.aop.proxy;

import java.lang.reflect.Constructor;

import cn.taketoday.aop.ProxyCreator;
import cn.taketoday.aop.intercept.CglibMethodInterceptor;
import cn.taketoday.context.cglib.proxy.Enhancer;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;

/**
 * 
 * @author TODAY <br>
 *         2018-11-10 13:03
 */
public class CglibProxyCreator implements ProxyCreator {

    private static final Logger log = LoggerFactory.getLogger(CglibProxyCreator.class);

    @Override
    public Object createProxy(TargetSource targetSource, BeanFactory beanFactory) {

        log.debug("Creating Cglib Proxy, target source is: [{}]", targetSource);

        final Class<?> targetClass = targetSource.getTargetClass();
        final Enhancer enhancer = new Enhancer()//
                .setSuperclass(targetClass)//
                .setInterfaces(targetSource.getInterfaces())//
                .setInterceptDuringConstruction(false)//
                .setCallback(new CglibMethodInterceptor(targetSource));

        try {
            return doEnhance(beanFactory, enhancer, ClassUtils.obtainConstructor(targetClass));
        }
        catch (NoSuchMethodException e) {
            throw new ConfigurationException("You must provide at least one suitable Constructor", e);
        }
    }

    protected Object doEnhance(final BeanFactory beanFactory,
                               final Enhancer enhancer,
                               final Constructor<?> constructor) {
        if (constructor.getParameterCount() == 0) {// <init>()
            return enhancer.create();
        }
        final Object[] resolveParameter = ContextUtils.resolveParameter(constructor, beanFactory);
        return enhancer.create(constructor.getParameterTypes(), resolveParameter);
    }

}
