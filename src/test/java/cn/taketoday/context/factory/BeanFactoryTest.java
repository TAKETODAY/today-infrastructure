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
package cn.taketoday.context.factory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import lombok.ToString;

/**
 * @author Today <br>
 * 
 *         2019-01-22 18:55
 */
public class BeanFactoryTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BeanFactoryTest.class);

    @Test
    public void test_GetBeanWithType() throws NoSuchBeanDefinitionException {

        setProcess("Get bean by given type");

        ConfigurableBeanFactory beanFactory = getBeanFactory();

        Object bean = beanFactory.getBean(Interface.class);

        Object implements1 = beanFactory.getBean(Implements1.class);
        Object implements2 = beanFactory.getBean(Implements2.class);
        Object implements3 = beanFactory.getBean(Implements3.class);

        assert bean != null;
        assert implements1 != null;
        assert implements2 != null;
        assert implements3 != null;
    }

    @Test
    public void test_GetBeanWithName() throws NoSuchBeanDefinitionException {

        setProcess("Get bean by given bean name");

        ConfigurableBeanFactory beanFactory = getBeanFactory();

        BeanNameCreator beanNameCreator = getApplicationContext().getEnvironment().getBeanNameCreator();
        Object bean = beanFactory.getBean(beanNameCreator.create(Interface.class));

        Object implements1 = beanFactory.getBean(beanNameCreator.create(Implements1.class));
        Object implements2 = beanFactory.getBean(beanNameCreator.create(Implements2.class));
        Object implements3 = beanFactory.getBean(beanNameCreator.create(Implements3.class));

        assert bean == null; // there isn't a bean named Interface

        assert implements1 != null;
        assert implements2 != null;
        assert implements3 != null;
    }

    @Test
    public void test_GetBeans() throws NoSuchBeanDefinitionException {

        setProcess("Get beans by given type");

        ConfigurableBeanFactory beanFactory = getBeanFactory();

        List<Interface> beans = beanFactory.getBeans(Interface.class);

        log.debug("beans: {}", beans);

        assert beans.size() == 3;
        assert beans.contains(beanFactory.getBean(Interface.class));
        assert beans.contains(beanFactory.getBean(Implements1.class));
        assert beans.contains(beanFactory.getBean(Implements2.class));
        assert beans.contains(beanFactory.getBean(Implements3.class));
    }

    @Test
    public void test_GetAnnotatedBeans() throws NoSuchBeanDefinitionException {

        setProcess("Get Annotated Beans");

        ConfigurableBeanFactory beanFactory = getBeanFactory();

        List<Object> annotatedBeans = beanFactory.getAnnotatedBeans(Singleton.class);
        log.debug("beans: {}", annotatedBeans);
        assert annotatedBeans.size() > 0;
    }

    @Test
    public void test_GetType() throws NoSuchBeanDefinitionException {
        setProcess("Get bean's type");
        ConfigurableBeanFactory beanFactory = getBeanFactory();
        BeanNameCreator beanNameCreator = getApplicationContext().getEnvironment().getBeanNameCreator();
        Class<?> type = beanFactory.getType(beanNameCreator.create(Implements1.class));
        log.debug("type: {}", type);
        assert Implements1.class == type;
    }

    @Test
    public void test_GetAliases() throws NoSuchBeanDefinitionException {

        setProcess("Get bean's aliases by given type");

        ConfigurableBeanFactory beanFactory = getBeanFactory();
        Set<String> aliases = beanFactory.getAliases(Interface.class);

        log.debug("Aliases: {}", aliases);
        assert aliases.size() == 3;
    }

    @Test
    public void test_GetBeanName() throws NoSuchBeanDefinitionException {

        setProcess("Get bean name by given type");

        ConfigurableBeanFactory beanFactory = getBeanFactory();

        BeanNameCreator beanNameCreator = getApplicationContext().getEnvironment().getBeanNameCreator();

        String name = beanFactory.getBeanName(Implements1.class);
        try {
            beanFactory.getBeanName(Interface.class);
            assert false;
        }
        catch (Exception e) {
            assert true;
        }

        assert beanNameCreator.create(Implements1.class).equals(name);
    }

    @Test
    public void test_IsPrototype() throws NoSuchBeanDefinitionException {

        setProcess("Whether bean is a prototype");

        ConfigurableBeanFactory beanFactory = getBeanFactory();

        assert beanFactory.isPrototype("FactoryBean-Config");

        try {
            beanFactory.isPrototype("today");
        }
        catch (NoSuchBeanDefinitionException e) {}
    }

    @Test
    public void test_IsSingleton() throws NoSuchBeanDefinitionException {

        setProcess("Whether bean is a singleton");

        ConfigurableBeanFactory beanFactory = getBeanFactory();

        BeanNameCreator beanNameCreator = //
                getApplicationContext()//
                        .getEnvironment()//
                        .getBeanNameCreator();

        assert beanFactory.isSingleton(beanNameCreator.create(Implements1.class));

    }

    // ------------------------------------2.1.6

    @Test
    public void testAddBeanPostProcessor() {
        setProcess("Add Bean Post Processor");

        AbstractBeanFactory beanFactory = (AbstractBeanFactory) getBeanFactory();

        final BeanPostProcessor beanPostProcessor = new BeanPostProcessor() {};

        final List<BeanPostProcessor> postProcessors = beanFactory.getPostProcessors();

        final int size = postProcessors.size();
        System.err.println(size);

        beanFactory.addBeanPostProcessor(beanPostProcessor);

        System.err.println(postProcessors);

        assert postProcessors.size() == size + 1;

        beanFactory.removeBeanPostProcessor(beanPostProcessor);

    }

    @ToString
    public static class TEST {
        public int test;

    }

    @ToString
    @Prototype("testBean")
    // @Singleton("test.context.factory.BeanFactoryTest.FactoryBeanTestBean")
    public static class FactoryBeanTestBean implements FactoryBean<TEST>, InitializingBean {

        @Value("${env['upload.maxFileSize']}")
        private int testInt;

        @Override
        public TEST getBean() {
            final TEST test = new TEST();
            test.test = testInt;

            return test;
        }

        @Override
        public Class<TEST> getBeanClass() {
            return TEST.class;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            System.err.println(testInt);// 10240000
        }
    }

    @Test
    public void testFactoryBean() {
        setProcess("Factory Bean");
        final ConfigurableBeanFactory beanFactory = getBeanFactory();
        final TEST bean = beanFactory.getBean("testBean", TEST.class);

        System.err.println(bean);

        final BeanDefinition beanDefinition = beanFactory.getBeanDefinition("testBean");
        System.err.println(beanDefinition);
        System.err.println(beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + "testBean"));
    }

    @Test
    public void testGetBeansOfType() {

        setProcess("Get Beans Of Type");

        final ConfigurableBeanFactory beanFactory = getBeanFactory();

        final Map<String, Interface> beansOfType = beanFactory.getBeansOfType(Interface.class);

        System.err.println(beansOfType);

        assert beansOfType.size() == 3;

    }

}
