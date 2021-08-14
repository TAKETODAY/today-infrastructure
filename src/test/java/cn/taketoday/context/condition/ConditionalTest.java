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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.condition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.beans.Singleton;

/**
 * @author TODAY <br>
 *         2019-09-16 22:39
 */
public class ConditionalTest {

    @Singleton
    @ConditionalOnClass("javax.inject.Inject")
    public static class ConditionalClass {

    }

    @Singleton
    @ConditionalOnClass("Inject")
    public static class ConditionalMissingClass {

    }

    //
    @Singleton
    @ConditionalOnMissingClass("Inject")
    public static class ConditionalOnMissing {

    }

    @Singleton
    @ConditionalOnMissingClass("javax.inject.Inject")
    public static class ConditionalMissed {

    }

    // ConditionalOnClass
    // ------------------------------

    @Test
    public void testConditionalOnClass() {

        try (final ApplicationContext applicationContext = //
                new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {

            assertTrue(applicationContext.containsBeanDefinition(ConditionalClass.class));
            assertTrue(applicationContext.containsBeanDefinition(ConditionalOnMissing.class));
            assertTrue(!applicationContext.containsBeanDefinition(ConditionalMissingClass.class));
            assertTrue(!applicationContext.containsBeanDefinition(ConditionalMissed.class));
        }
    }

    // ConditionalOnExpression
    // ------------------------------------------------

    @Singleton
    @ConditionalOnExpression("${1+1==2}")
    public static class ConditionalExpression_ {

    }

    @Singleton
    @ConditionalOnExpression("${1+1!=2}")
    public static class ConditionalExpression__ {

    }

    @Test
    public void testConditionalOnExpression() {

        try (final ApplicationContext applicationContext = //
                new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {

            assertTrue(applicationContext.containsBeanDefinition(ConditionalExpression_.class));
            assertTrue(!applicationContext.containsBeanDefinition(ConditionalExpression__.class));
        }
    }

    // ConditionalOnProperty
    // ---------------------------------

    @Singleton
    @ConditionalOnProperty("test.property")
    public static class ConditionalOnProperty_ {

    }

    @Singleton
    @ConditionalOnProperty("test.none.property")
    public static class ConditionalOnProperty__ {

    }

    @Singleton
    @ConditionalOnProperty(value = "property", prefix = "test.")
    public static class ConditionalOnProperty___ {

    }

    @Singleton
    @ConditionalOnProperty(value = "property", prefix = "test.none")
    public static class ConditionalOnProperty____ {

    }

    @Test
    public void testConditionalOnProperty() throws Exception {

        try (final ApplicationContext applicationContext = //
                new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {

            assertTrue(applicationContext.containsBeanDefinition(ConditionalOnProperty_.class));
            assertTrue(!applicationContext.containsBeanDefinition(ConditionalOnProperty__.class));
            assertTrue(applicationContext.containsBeanDefinition(ConditionalOnProperty___.class));
            assertTrue(!applicationContext.containsBeanDefinition(ConditionalOnProperty____.class));
        }
    }

    // ConditionalOnResource
    // ----------------------------
    @Singleton
    @ConditionalOnResource("none")
    public static class ConditionalOnResource_ {

    }

    @Singleton
    @ConditionalOnResource("info.properties")
    public static class ConditionalOnResource__ {

    }

    @Singleton
    @ConditionalOnResource("classpath:/info.properties")
    public static class ConditionalOnResource___ {

    }

    @Singleton
    @ConditionalOnResource("classpath:info.properties")
    public static class ConditionalOnResource____ {

    }

    @Test
    public void testConditionalOnResource() throws Exception {

        try (final ApplicationContext applicationContext = //
                new StandardApplicationContext("info.properties", "cn.taketoday.context.condition")) {

            assertFalse(applicationContext.containsBeanDefinition(ConditionalOnResource_.class));

            assertTrue(applicationContext.containsBeanDefinition(ConditionalOnResource__.class));
            assertTrue(applicationContext.containsBeanDefinition(ConditionalOnResource___.class));
            assertTrue(applicationContext.containsBeanDefinition(ConditionalOnResource____.class));
        }
    }

}
