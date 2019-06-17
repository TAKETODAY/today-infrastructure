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
package test.context.bean;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.ContextException;

/**
 * @author TODAY <br>
 *         2019-06-12 21:19
 */
public class BeanReferenceTest {

    @Autowired
    TEST test;

    @Test
    public void testBeanReference() {

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {
            BeanReference beanReference = new BeanReference("tEST", true, TEST.class);

            applicationContext.registerBean("beanReferenceTest", getClass());

            final BeanDefinition beanDefinition = applicationContext.getBeanDefinition(getClass());

            final PropertyValue propertyValue = beanDefinition.getPropertyValue("test");

            final Object value = propertyValue.getValue();
            System.err.println(value);
            assert value.equals(beanReference);

            try {
                new BeanReference(null, true, TEST.class);
            }
            catch (ContextException e) {
                e.getMessage().equals("Bean name can't be empty");
            }

        }

    }

    public static class TEST {

    }
}
