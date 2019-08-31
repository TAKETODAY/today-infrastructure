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
package test.context.loader;

import java.util.Properties;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Env;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.loader.ValuePropertyResolver;

/**
 * @author Today <br>
 * 
 *         2018-08-04 15:58
 */
public class ValuePropertyResolveTest {

    @Value("#{site.host}")
    private String host = null;

    @Env("site.name")
    private String name = null;

    @Env
    private String test = null;

    @Test
    public void testResolveProperty() throws Exception {

        ValuePropertyResolver propertyResolver = new ValuePropertyResolver();

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {

            // host
            // ----------------------------
            PropertyValue host = propertyResolver.resolveProperty(ValuePropertyResolveTest.class.getDeclaredField("host"));

            assert host.getValue() != null;

            System.out.println("Site -> " + host.getValue());

            // name
            // ----------------------------
            PropertyValue name = propertyResolver.resolveProperty(ValuePropertyResolveTest.class.getDeclaredField("name"));

            assert name.getValue() != null;

            System.out.println("Name -> " + name.getValue());

            // test
            // ----------------------------
            final Properties properties = applicationContext.getEnvironment().getProperties();
            properties.put("test.context.loader.ValuePropertyResolveTest.test", "TEST");

            PropertyValue test = propertyResolver.resolveProperty(ValuePropertyResolveTest.class.getDeclaredField("test"));

            assert "TEST".equals(test.getValue());

        }

    }

}
