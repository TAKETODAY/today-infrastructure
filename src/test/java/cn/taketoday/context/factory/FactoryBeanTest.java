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
package cn.taketoday.context.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.aware.ApplicationContextSupport;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ContextUtils;

/**
 * @author Today <br>
 * 
 *         2018-12-25 19:09
 */
public class FactoryBeanTest {

    // bean
    // --------------------------------------
    private static class TEST {

    }

    private static class TESTFactoryBean extends AbstractFactoryBean<TEST> {

        @Override
        protected TEST createBeanInstance() {
            return new TEST();
        }

        @Override
        public Class<TEST> getBeanClass() {
            return TEST.class;
        }
    }

    // @Configuration bean
    // ---------------------------

    static class FactoryBeanConfiguration extends ApplicationContextSupport {

        @Singleton
        public TESTFactoryBean testFactoryBean() {
            return new TESTFactoryBean();
        }
    }

    @Import(FactoryBeanConfiguration.class)
    static class FactoryBeanConfigurationImporter {

    }

    // test
    // --------------------------------------------

    @Test
    public void testFactoryBean() throws NoSuchBeanDefinitionException {

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {

            applicationContext.registerBean("testFactoryBean", TESTFactoryBean.class);

            Map<String, BeanDefinition> beanDefinitions = applicationContext.getBeanDefinitions();

            assertFalse(beanDefinitions.isEmpty());

            Object testFactoryBean = applicationContext.getBean("testFactoryBean");

            TEST bean = applicationContext.getBean(TEST.class);

            System.err.println(applicationContext.getSingletons());

            assertEquals(bean, testFactoryBean);

            assertTrue(testFactoryBean == bean);
            assertFalse(applicationContext.getBean("$testFactoryBean") == null);
        }
    }

    @Test
    public void testPrototypeFactoryBean() throws NoSuchBeanDefinitionException {

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {

            List<BeanDefinition> definitions = //
                    ContextUtils.createBeanDefinitions("testFactoryBean-prototype", TESTFactoryBean.class);

            assertFalse(definitions.isEmpty());

            BeanDefinition beanDefinition = definitions.get(0);
            beanDefinition.setScope(Scope.PROTOTYPE);

            applicationContext.registerBean(beanDefinition);

            Map<String, BeanDefinition> beanDefinitions = applicationContext.getBeanDefinitions();

            assertFalse(beanDefinitions.isEmpty());

            Object testFactoryBean = applicationContext.getBean("testFactoryBean-prototype");

            TEST bean = applicationContext.getBean(TEST.class);

            assertNotEquals(bean, testFactoryBean);

            final Object $testFactoryBean = applicationContext.getBean("$testFactoryBean-prototype");
            assertNotNull($testFactoryBean);
        }
    }

    @Test
    public void testConfigurationFactoryBean() throws NoSuchBeanDefinitionException {

        HashSet<Class<?>> classes = new HashSet<Class<?>>(Arrays.asList(TESTFactoryBean.class));

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {

            applicationContext.load(classes);

            applicationContext.registerBean("factoryBeanConfigurationImporter", FactoryBeanConfigurationImporter.class);

            FactoryBeanConfiguration bean = applicationContext.getBean(FactoryBeanConfiguration.class);
            Object testFactoryBean = applicationContext.getBean("testFactoryBean");

            assertNotNull(bean);
            assertNotNull(testFactoryBean);
            assertTrue(testFactoryBean instanceof TEST);

            assertNotNull(applicationContext.getBean("$testFactoryBean"));
            assertTrue(applicationContext.getBean("$testFactoryBean") instanceof TESTFactoryBean);
        }
    }

}
