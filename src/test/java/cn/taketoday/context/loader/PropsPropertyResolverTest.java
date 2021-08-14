/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.loader;

import java.util.Properties;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.Props;
import cn.taketoday.beans.factory.DefaultPropertySetter;

/**
 * @author Today <br>
 * 
 *         2018-08-04 16:01
 */
public class PropsPropertyResolverTest {

    @Props(value = "info", prefix = "site")
    private Properties properties;

    @Props(value = "info", prefix = "site")
    private String name;

    @Test
    public void test_() throws Throwable {

        try (ConfigurableApplicationContext applicationContext = new StandardApplicationContext()) {
            PropsPropertyResolver propertyResolver = new PropsPropertyResolver(applicationContext);

            DefaultPropertySetter resolveProperty = //
                    propertyResolver.resolveProperty(PropsPropertyResolverTest.class.getDeclaredField("properties"));

            assert resolveProperty.getValue() != null;

            System.out.println("====================");
            System.out.println(resolveProperty.getValue());

        }
    }

    @Test
    public void test_Error() throws Throwable {

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {
            PropsPropertyResolver propertyResolver = new PropsPropertyResolver(applicationContext);

            propertyResolver.resolveProperty(PropsPropertyResolverTest.class.getDeclaredField("name"));
        }

    }

}
