/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.taketoday.context.cglib.proxy.Enhancer;
import cn.taketoday.context.cglib.proxy.MethodInterceptor;

/**
 * The helper class achieve the effect of the prototype
 * 
 * @author TODAY <br>
 *         2019-09-03 21:20
 */
public final class Prototypes {

    private final BeanDefinition def;
    private final AbstractBeanFactory f;

    private Prototypes(AbstractBeanFactory f, BeanDefinition def) {
        this.f = f;
        this.def = def;
    }

    private final Object handle(final Method m, final Object[] a) throws Throwable {
        final Object b = f.getBean(def);
        try {
            return m.invoke(b, a);
        }
        catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
        finally {
            if (f.isFullLifecycle()) {
                f.destroyBean(b, def); // destroyBean after every call
            }
        }
    }

    public static Object newProxyInstance(Class<?> refType, BeanDefinition def, AbstractBeanFactory f) {
        return newProxyInstance(refType, def, f, false);
    }

    /**
     * @param refType
     *            Reference bean class
     * @param def
     *            Target {@link BeanDefinition}
     * @param f
     *            {@link AbstractBeanFactory}
     * @param proxyTargetClass
     *            If true use cglib
     * @return Target prototype object
     */
    public static Object newProxyInstance(Class<?> refType,
                                          BeanDefinition def,
                                          AbstractBeanFactory f,
                                          boolean proxyTargetClass) //
    {
        final Prototypes handler = new Prototypes(f, def);
        if (!proxyTargetClass && refType.isInterface()) { // Use Jdk Proxy @off
            return Proxy.newProxyInstance(refType.getClassLoader(),  def.getBeanClass().getInterfaces(), 
                (final Object p, final Method m, final Object[] a) -> {
                    return handler.handle(m, a);
                }
            ); //@on
        }
        return new Enhancer()
                .setUseCache(true)
                .setSuperclass(refType)
                .setInterfaces(refType.getInterfaces())
                .setClassLoader(refType.getClassLoader())
                .setCallback((MethodInterceptor) (obj, m, a, proxy) -> handler.handle(m, a))
                .create();
    }

}