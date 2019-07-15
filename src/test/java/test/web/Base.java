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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.web;

import java.util.Map;

import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.web.servlet.StandardWebServletApplicationContext;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Today <br>
 * 
 *         2018-12-24 19:17
 */
@Getter
@Setter
public class Base {

    private ServletContext servletContext;

    private WebServletApplicationContext applicationContext;
    long start = System.currentTimeMillis();

    @Before
    public void before() {

        servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getContextPath()).thenReturn("");
        applicationContext = new StandardWebServletApplicationContext();

        ((StandardWebServletApplicationContext) applicationContext).setServletContext(servletContext);

        applicationContext.getEnvironment().addActiveProfile("test");
        applicationContext.loadContext("test.web");
    }

    @After
    public void after() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @Test
    public void test_() {
        System.err.println(System.currentTimeMillis() - start);
        Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getBeanDefinitions();

        System.err.println(beanDefinitionsMap);
//		for (Class<?> class1 : ClassUtils.getClassCache()) {
//			System.err.println(class1);
//		}

    }

}
