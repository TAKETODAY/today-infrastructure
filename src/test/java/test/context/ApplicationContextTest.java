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
package test.context;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.env.Environment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import test.demo.config.Config;
import test.demo.config.ConfigFactoryBean;
import test.demo.config.ConfigurationBean;
import test.demo.config.User;
import test.demo.repository.impl.DefaultUserRepository;

/**
 * @author Today
 * @date 2018年7月3日 下午10:05:21
 */
@Slf4j
public class ApplicationContextTest {

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
            Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getEnvironment().getBeanDefinitionRegistry().getBeanDefinitions();

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

            Map<String, Object> singletonsMap = applicationContext.getSingletons();

            for (Entry<String, Object> entry : singletonsMap.entrySet()) {
                System.err.println(entry.getKey() + "==" + entry.getValue());
            }

            assert config != config_;

            log.debug("{}", config.hashCode());
            log.debug("{}", config_.hashCode());

//			assert config != config_ : "FactoryBean error.";
            log.debug("{}", bean);
            log.debug("{}", propertyValue);
            log.debug("{}", bean.getPro());
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

        try (ApplicationContext applicationContext = new StandardApplicationContext("")) {

            applicationContext.registerBean(User.class);
            applicationContext.registerBean("user", User.class);
            applicationContext.registerBean("user_", User.class);
//			applicationContext.onRefresh(); // init bean

            Map<String, BeanDefinition> beanDefinitionsMap = applicationContext.getEnvironment().getBeanDefinitionRegistry().getBeanDefinitions();

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

    @Test
    public void test_Required() throws NoSuchBeanDefinitionException, BeanDefinitionStoreException {

        try (ApplicationContext applicationContext = new StandardApplicationContext()) {
            applicationContext.loadContext("");
            Environment environment = applicationContext.getEnvironment();
            BeanDefinitionRegistry beanDefinitionRegistry = environment.getBeanDefinitionRegistry();
            System.err.println(beanDefinitionRegistry.getBeanDefinitions());

            Config bean = applicationContext.getBean(Config.class);
            System.out.println(bean);
            assert bean != null;
            assert bean.getUser() != null;
        }
    }

}
