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
package cn.taketoday.context.el;

import org.junit.Test;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.el.ELFieldTest.User;

/**
 * @author TODAY <br>
 *         2019-06-12 20:39
 */
public class BeanFactoryResolverTest {

    @Test
    public void testIsReadOnly() {

        try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {

            applicationContext.registerBean("user", User.class);

            BeanFactoryResolver beanFactoryResolver = new BeanFactoryResolver(applicationContext);

            assert beanFactoryResolver.isReadOnly("user");
            assert beanFactoryResolver.isNameResolved("user");
            assert beanFactoryResolver.getBean("user") != null;

        }
    }

}
