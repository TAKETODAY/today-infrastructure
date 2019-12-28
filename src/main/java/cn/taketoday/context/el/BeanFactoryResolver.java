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
package cn.taketoday.context.el;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.factory.AbstractBeanFactory;
import cn.taketoday.expression.BeanNameResolver;

/**
 * @author TODAY <br>
 *         2019-02-21 17:02
 */
/**
 * 
 * @author TODAY <br>
 *         2019-02-23 10:36
 */
public class BeanFactoryResolver extends BeanNameResolver {

    private final AbstractBeanFactory beanFactory;

    public BeanFactoryResolver(AbstractApplicationContext applicationContext) {
        this.beanFactory = applicationContext.getBeanFactory();
    }

    @Override
    public boolean isReadOnly(String beanName) {
        return true;
    }

    @Override
    public boolean isNameResolved(String beanName) {
        return beanFactory.containsBeanDefinition(beanName);
    }

    @Override
    public Object getBean(String beanName) {
        return beanFactory.getBean(beanName);
    }

}
