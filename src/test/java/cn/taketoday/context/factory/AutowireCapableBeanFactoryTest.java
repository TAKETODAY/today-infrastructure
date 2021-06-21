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

package cn.taketoday.context.factory;

import org.junit.Test;

import java.lang.reflect.AnnotatedElement;

import javax.annotation.PostConstruct;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Condition;
import cn.taketoday.context.Scope;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.aware.BeanNameAware;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY
 * 2020/9/17 16:16
 */
public class AutowireCapableBeanFactoryTest {

    static class CreateTestBean {
        int property;
    }

    @Test
    public void testCreateBean() {
        try (ApplicationContext context = new StandardApplicationContext()) {
            CreateTestBean bean = context.createBean(CreateTestBean.class);
            CreateTestBean bean2 = context.createBean(CreateTestBean.class);
            assertThat(bean.property).isZero();
            assertThat(bean2).isNotEqualTo(bean);

            CreateTestBean cachedBeanDef = context.createBean(CreateTestBean.class, true);
            CreateTestBean cachedBeanDef2 = context.createBean(CreateTestBean.class, true);

            assertThat(cachedBeanDef.property).isZero();
            assertThat(cachedBeanDef2).isNotEqualTo(cachedBeanDef);

            BeanDefinition beanDefinition = context.getBeanDefinition(CreateTestBean.class);

            assertThat(beanDefinition.getBeanClass()).isEqualTo(CreateTestBean.class);
            assertThat(beanDefinition.getScope()).isEqualTo(Scope.PROTOTYPE);
            assertThat(beanDefinition.getPropertySetters()).isEmpty();
        }
    }

    static class AutowireTestBeanCondition implements Condition {

        @Override
        public boolean matches(final ApplicationContext context, final AnnotatedElement annotated) {
            final ApplicationContext.State state = context.getState();
            return state == ApplicationContext.State.NONE;
        }
    }

    @Conditional(AutowireTestBeanCondition.class)
    @Component(initMethods = "init", destroyMethods = "destroy")
    static class AutowireTestBean implements BeanNameAware, InitializingBean {
        @Value("${1+1}")
        int property;

        @Autowired
        CreateTestBean bean;

        boolean initMethod;
        boolean destroyMethods;
        boolean postConstruct;
        boolean afterPropertiesSet;
        String name;

        boolean afterPostProcessor;
        boolean beforePostProcessor;
        boolean postProcessBeforeDestruction;

        void init() {
            initMethod = true;
        }

        void destroy() {
            destroyMethods = true;
        }

        @PostConstruct
        void postConstruct() {
            postConstruct = true;
        }

        @Override
        public void setBeanName(final String name) {
            this.name = name;
        }

        @Override
        public void afterPropertiesSet() {
            afterPropertiesSet = true;
        }
    }

    @Test
    public void testAutowireBean() {
        try (ApplicationContext context = new StandardApplicationContext()) {
            CreateTestBean cachedBeanDef = context.createBean(CreateTestBean.class, true);
            context.addBeanPostProcessor(new PostProcessor());

            AutowireTestBean autowireTestBean = new AutowireTestBean();
            context.autowireBean(autowireTestBean);

            assertThat(autowireTestBean.name).isEqualTo("autowireTestBean");
            assertThat(autowireTestBean.property).isEqualTo(2);
            assertThat(autowireTestBean.initMethod).isTrue();
            assertThat(autowireTestBean.postConstruct).isTrue();
            assertThat(autowireTestBean.afterPropertiesSet).isTrue();
            assertThat(autowireTestBean.afterPostProcessor).isFalse();
            assertThat(autowireTestBean.beforePostProcessor).isFalse();
            assertThat(autowireTestBean.bean).isNotEqualTo(cachedBeanDef);
        }
    }

    @Test
    public void testAutowireBeanProperties() {
        try (ApplicationContext context = new StandardApplicationContext()) {
            CreateTestBean cachedBeanDef = context.createBean(CreateTestBean.class, true);
            context.addBeanPostProcessor(new PostProcessor());

            AutowireTestBean autowireTestBean = new AutowireTestBean();
            context.autowireBeanProperties(autowireTestBean);
            assertThat(autowireTestBean.name).isNull();
            assertThat(autowireTestBean.property).isEqualTo(2);
            assertThat(autowireTestBean.initMethod).isFalse();
            assertThat(autowireTestBean.postConstruct).isFalse();
            assertThat(autowireTestBean.afterPropertiesSet).isFalse();
            assertThat(autowireTestBean.afterPostProcessor).isFalse();
            assertThat(autowireTestBean.beforePostProcessor).isFalse();
            assertThat(autowireTestBean.bean).isNotEqualTo(cachedBeanDef);
        }
    }

    static class PostProcessor implements BeanPostProcessor, DestructionBeanPostProcessor {

        @Override
        public Object postProcessBeforeInitialization(Object bean, BeanDefinition def) {
            if (bean instanceof AutowireTestBean) {
                ((AutowireTestBean) bean).beforePostProcessor = true;
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, BeanDefinition def) {
            if (bean instanceof AutowireTestBean) {
                ((AutowireTestBean) bean).afterPostProcessor = true;
            }
            return bean;
        }

        @Override
        public void postProcessBeforeDestruction(Object bean, BeanDefinition def) {
            if (bean instanceof AutowireTestBean) {
                ((AutowireTestBean) bean).postProcessBeforeDestruction = true;
            }
        }

    }

    @Test
    public void testInitializeBean() {
        String beanName = "autowireCapableBeanFactoryTest.AutowireTestBean";

        try (ApplicationContext context = new StandardApplicationContext()) {
            CreateTestBean cachedBeanDef = context.createBean(CreateTestBean.class, true);

            context.addBeanPostProcessor(new PostProcessor());

            AutowireTestBean autowireTestBean = new AutowireTestBean();
            context.initializeBean(autowireTestBean, beanName);

            assertThat(autowireTestBean.name).isEqualTo(beanName);

            assertThat(autowireTestBean.property).isEqualTo(2);
            assertThat(autowireTestBean.initMethod).isTrue();
            assertThat(autowireTestBean.postConstruct).isTrue();
            assertThat(autowireTestBean.afterPropertiesSet).isTrue();
            assertThat(autowireTestBean.afterPostProcessor).isTrue();
            assertThat(autowireTestBean.beforePostProcessor).isTrue();
            assertThat(autowireTestBean.bean).isNotEqualTo(cachedBeanDef);
        }
    }

    @Test
    public void testApplyBeanPostProcessorsBeforeInitialization() {
        String beanName = "autowireCapableBeanFactoryTest.AutowireTestBean";

        try (ApplicationContext context = new StandardApplicationContext()) {

            context.addBeanPostProcessor(new PostProcessor());

            AutowireTestBean autowireTestBean = new AutowireTestBean();

            Object before = context.applyBeanPostProcessorsBeforeInitialization(autowireTestBean, beanName);
            assertThat(autowireTestBean).isEqualTo(before);

            assertThat(autowireTestBean.bean).isNull();
            assertThat(autowireTestBean.name).isNull();
            assertThat(autowireTestBean.property).isZero();
            assertThat(autowireTestBean.initMethod).isFalse();
            assertThat(autowireTestBean.postConstruct).isFalse();
            assertThat(autowireTestBean.afterPropertiesSet).isFalse();

            assertThat(autowireTestBean.afterPostProcessor).isFalse();
            assertThat(autowireTestBean.beforePostProcessor).isTrue();
        }
    }

    @Test
    public void testApplyBeanPostProcessorsAfterInitialization() {
        String beanName = "autowireCapableBeanFactoryTest.AutowireTestBean";

        try (ApplicationContext context = new StandardApplicationContext()) {

            context.addBeanPostProcessor(new PostProcessor());

            AutowireTestBean autowireTestBean = new AutowireTestBean();

            Object after = context.applyBeanPostProcessorsAfterInitialization(autowireTestBean, beanName);
            assertThat(autowireTestBean).isEqualTo(after);

            assertThat(autowireTestBean.bean).isNull();
            assertThat(autowireTestBean.name).isNull();
            assertThat(autowireTestBean.property).isZero();
            assertThat(autowireTestBean.initMethod).isFalse();
            assertThat(autowireTestBean.postConstruct).isFalse();
            assertThat(autowireTestBean.afterPropertiesSet).isFalse();

            assertThat(autowireTestBean.afterPostProcessor).isTrue();
            assertThat(autowireTestBean.beforePostProcessor).isFalse();

        }
    }

    @Test
    public void testDestroyBean() {

        try (ApplicationContext context = new StandardApplicationContext()) {
            context.addBeanPostProcessor(new PostProcessor());

            AutowireTestBean autowireTestBean = new AutowireTestBean();

            assertThat(autowireTestBean.bean).isNull();
            assertThat(autowireTestBean.name).isNull();
            assertThat(autowireTestBean.property).isZero();
            assertThat(autowireTestBean.initMethod).isFalse();
            assertThat(autowireTestBean.postConstruct).isFalse();
            assertThat(autowireTestBean.afterPropertiesSet).isFalse();

            assertThat(autowireTestBean.afterPostProcessor).isFalse();
            assertThat(autowireTestBean.beforePostProcessor).isFalse();

            context.destroyBean(autowireTestBean);
            assertThat(autowireTestBean.postProcessBeforeDestruction).isTrue();

        }
    }
}
