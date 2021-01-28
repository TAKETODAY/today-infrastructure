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
package cn.taketoday.context;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.ApplicationContextTest.RequiredTest.Bean1;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.DisposableBean;
import cn.taketoday.context.factory.PropertyValue;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import test.demo.config.Config;
import test.demo.config.ConfigFactoryBean;
import test.demo.config.ConfigurationBean;
import test.demo.config.User;
import test.demo.repository.impl.DefaultUserRepository;

/**
 * @author Today
 * 2018年7月3日 下午10:05:21
 */
public class ApplicationContextTest {
    private static final Logger log = LoggerFactory.getLogger(ApplicationContextTest.class);

    private long start;

    @Before
    public void start() {
        start = System.currentTimeMillis();
    }

    @After
    public void end() {
        System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
    }

    /**
     * test ApplicationContext
     * 
     * @throws NoSuchBeanDefinitionException
     */
    @Test
    public void test_ApplicationContext() throws NoSuchBeanDefinitionException {
        try (ApplicationContext applicationContext = new StandardApplicationContext("")) {
            applicationContext.loadContext("test.demo.repository");
            Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getEnvironment().getBeanDefinitionRegistry()
                    .getBeanDefinitions();

            System.out.println(beanDefinitionsMap);

            boolean containsBean = applicationContext.containsBeanDefinition(DefaultUserRepository.class);

            System.out.println(applicationContext.getBean(DefaultUserRepository.class));

            assert containsBean : "UserDaoImpl load error.";
        }
    }

    /**
     * test load context and get singleton.
     * 
     * @throws NoSuchBeanDefinitionException
     */
    @Test
    public void test_LoadSingleton() throws NoSuchBeanDefinitionException {
        try (ApplicationContext applicationContext = new StandardApplicationContext()) {
            applicationContext.loadContext("test.demo.config");
            Config config = applicationContext.getBean(Config.class);
            Config config_ = applicationContext.getBean(Config.class);

            assert config == config_ : "singleton error.";

            String copyright = config.getCopyright();

            assert copyright != null : "properties file load error.";
        }
    }

    /**
     * test load FactoryBean.
     * 
     * @throws NoSuchBeanDefinitionException
     */
    @Test
    public void test_LoadFactoryBean() throws NoSuchBeanDefinitionException {

        try (ApplicationContext applicationContext = new StandardApplicationContext("")) {
            applicationContext.loadContext("test.demo.config");
            Config config = applicationContext.getBean("FactoryBean-Config", Config.class);
            Config config_ = applicationContext.getBean("FactoryBean-Config", Config.class);

            BeanDefinition beanDefinition = applicationContext.getBeanDefinition("FactoryBean-Config");
            PropertyValue propertyValue = beanDefinition.getPropertyValue("pro");
            ConfigFactoryBean bean = applicationContext.getBean("$FactoryBean-Config", ConfigFactoryBean.class);

            assertNotNull(bean); // @Prototype 
            assertNotEquals(config, config_);

            log.debug("{}", config.hashCode());
            log.debug("{}", config_.hashCode());
            log.debug("{}", bean);
            log.debug("{}", propertyValue);
        }
    }

    /**
     * Manual Loading.
     * 
     * @throws NoSuchBeanDefinitionException
     * @throws BeanDefinitionStoreException
     */
    @Test
    public void test_ManualLoadContext() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

        try (StandardApplicationContext applicationContext = new StandardApplicationContext("")) {

            applicationContext.registerBean(User.class);
            applicationContext.registerBean("user", User.class);
            applicationContext.registerBean("user_", User.class);
            //			applicationContext.onRefresh(); // init bean

            Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getBeanDefinitions();

            System.out.println(beanDefinitionsMap);

            Object bean = applicationContext.getBean("user");
            assert beanDefinitionsMap.size() == 2;
            assert bean != null : "error";
        }
    }

    @Test
    public void test_loadFromCollection() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

        try (ApplicationContext applicationContext = //
                new StandardApplicationContext(Arrays.asList(ConfigurationBean.class))) {

            long start = System.currentTimeMillis();

            User bean = applicationContext.getBean(User.class);
            System.err.println(System.currentTimeMillis() - start + "ms");
            System.err.println(applicationContext.getEnvironment().getBeanDefinitionRegistry().getBeanDefinitions());

            bean.setAge(12);

            System.err.println(bean);

            User user = applicationContext.getBean(User.class);

            assert bean != user;

            System.err.println(user);
        }
    }

    // RequiredTest
    // -------------------------------------
    public static class RequiredTest {

        @Autowired(required = false, value = "requiredTestBean")
        private Bean bean;

        @Autowired(required = true, value = "requiredTestBean1")
        private Bean1 bean1;

        public static class Bean {

        }

        public static class Bean1 implements DisposableBean {

            @Override
            public void destroy() throws Exception {
                System.err.println("Bean destroy");
            }
        }
    }

    @Test
    public void test_Required() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {

            applicationContext.registerBean("requiredTest", RequiredTest.class);

            //            applicationContext.registerBean("requiredTestBean1", Bean.class);
            applicationContext.registerBean("requiredTestBean1", Bean1.class);

            RequiredTest requiredTest = applicationContext.getBean(RequiredTest.class);

            assertTrue(requiredTest.bean == null);
            assertTrue(requiredTest.bean1 != null);
        }
    }

}
