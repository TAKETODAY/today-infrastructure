/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * <p>
 * JUnit-3.8-based unit test which verifies expected <em>init</em> and
 * <em>destroy</em> bean lifecycle behavior as requested in <a
 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-3775"
 * target="_blank">SPR-3775</a>.
 * </p>
 * <p>
 * Specifically, combinations of the following are tested:
 * </p>
 * <ul>
 * <li>{@link InitializingBean} &amp; {@link DisposableBean} interfaces</li>
 * <li>Custom {@link BeanDefinition#getInitMethods() init} &amp;
 * {@link BeanDefinition#getDestroyMethod() destroy} methods</li>
 * <li>JSR 250's {@link PostConstruct @PostConstruct} &amp;
 * {@link PreDestroy @PreDestroy} annotations</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class Spr3775InitDestroyLifecycleTests {

  private static final Logger logger = LoggerFactory.getLogger(Spr3775InitDestroyLifecycleTests.class);

  /** LIFECYCLE_TEST_BEAN. */
  private static final String LIFECYCLE_TEST_BEAN = "lifecycleTestBean";

  private void debugMethods(Class<?> clazz, String category, List<String> methodNames) {
    if (logger.isDebugEnabled()) {
      logger.debug(clazz.getSimpleName() + ": " + category + ": " + methodNames);
    }
  }

  private void assertMethodOrdering(
          Class<?> clazz, String category, List<String> expectedMethods, List<String> actualMethods) {
    debugMethods(clazz, category, actualMethods);
    assertThat(ObjectUtils.nullSafeEquals(expectedMethods, actualMethods)).as("Verifying " + category + ": expected<" + expectedMethods + "> but got<" + actualMethods + ">.").isTrue();
  }

  private StandardBeanFactory createBeanFactoryAndRegisterBean(
          final Class<?> beanClass, final String initMethodName, final String destroyMethodName) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = new BeanDefinition(beanClass);
    beanDefinition.setInitMethods(initMethodName);
    beanDefinition.setDestroyMethod(destroyMethodName);

    InitDestroyAnnotationBeanPostProcessor postProcessor = new InitDestroyAnnotationBeanPostProcessor();
    postProcessor.setInitAnnotationType(PostConstruct.class);
    postProcessor.setDestroyAnnotationType(PreDestroy.class);
    beanFactory.addBeanPostProcessor(postProcessor);
    beanFactory.registerBeanDefinition(LIFECYCLE_TEST_BEAN, beanDefinition);
    return beanFactory;
  }

  @Test
  public void testInitDestroyMethods() {
    final Class<?> beanClass = InitDestroyBean.class;
    final StandardBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass,
            "afterPropertiesSet", "destroy");
    final InitDestroyBean bean = (InitDestroyBean) beanFactory.getBean(LIFECYCLE_TEST_BEAN);
    assertMethodOrdering(beanClass, "init-methods", Arrays.asList("afterPropertiesSet"), bean.initMethods);
    beanFactory.destroySingletons();
    assertMethodOrdering(beanClass, "destroy-methods", Arrays.asList("destroy"), bean.destroyMethods);
  }

  @Test
  public void testInitializingDisposableInterfaces() {
    final Class<?> beanClass = CustomInitializingDisposableBean.class;
    final StandardBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "customInit",
            "customDestroy");
    final CustomInitializingDisposableBean bean = (CustomInitializingDisposableBean) beanFactory.getBean(LIFECYCLE_TEST_BEAN);
    assertMethodOrdering(beanClass, "init-methods", Arrays.asList("afterPropertiesSet", "customInit"),
            bean.initMethods);
    beanFactory.destroySingletons();
    assertMethodOrdering(beanClass, "destroy-methods", Arrays.asList("destroy", "customDestroy"),
            bean.destroyMethods);
  }

  @Test
  @Disabled
  public void testInitializingDisposableInterfacesWithShadowedMethods() {
    final Class<?> beanClass = InitializingDisposableWithShadowedMethodsBean.class;
    final StandardBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass,
            "afterPropertiesSet", "destroy");
    final InitializingDisposableWithShadowedMethodsBean bean = (InitializingDisposableWithShadowedMethodsBean) beanFactory.getBean(LIFECYCLE_TEST_BEAN);
    assertMethodOrdering(beanClass, "init-methods", List.of("InitializingBean.afterPropertiesSet"),
            bean.initMethods);
    beanFactory.destroySingletons();
    assertMethodOrdering(beanClass, "destroy-methods", List.of("DisposableBean.destroy"), bean.destroyMethods);
  }

  @Test
  public void testJsr250Annotations() {
    final Class<?> beanClass = CustomAnnotatedInitDestroyBean.class;
    final StandardBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass, "customInit",
            "customDestroy");
    final CustomAnnotatedInitDestroyBean bean = (CustomAnnotatedInitDestroyBean) beanFactory.getBean(LIFECYCLE_TEST_BEAN);
    assertMethodOrdering(beanClass, "init-methods", Arrays.asList("postConstruct", "afterPropertiesSet",
            "customInit"), bean.initMethods);
    beanFactory.destroySingletons();
    assertMethodOrdering(beanClass, "destroy-methods", Arrays.asList("preDestroy", "destroy", "customDestroy"),
            bean.destroyMethods);
  }

  @Disabled
  @Test
  public void testJsr250AnnotationsWithShadowedMethods() {
    final Class<?> beanClass = CustomAnnotatedInitDestroyWithShadowedMethodsBean.class;
    final StandardBeanFactory beanFactory =
            createBeanFactoryAndRegisterBean(beanClass, "customInit", "customDestroy");
    final CustomAnnotatedInitDestroyWithShadowedMethodsBean bean =
            (CustomAnnotatedInitDestroyWithShadowedMethodsBean) beanFactory.getBean(LIFECYCLE_TEST_BEAN);
    assertMethodOrdering(beanClass, "init-methods",
            Arrays.asList("@PostConstruct.afterPropertiesSet", "customInit"), bean.initMethods);
    beanFactory.destroySingletons();
    assertMethodOrdering(beanClass, "destroy-methods", Arrays.asList("@PreDestroy.destroy", "customDestroy"),
            bean.destroyMethods);
  }

  @Test
  @Disabled
  public void testAllLifecycleMechanismsAtOnce() {
    final Class<?> beanClass = AllInOneBean.class;
    final StandardBeanFactory beanFactory = createBeanFactoryAndRegisterBean(beanClass,
            "afterPropertiesSet", "destroy");
    final AllInOneBean bean = (AllInOneBean) beanFactory.getBean(LIFECYCLE_TEST_BEAN);
    assertMethodOrdering(beanClass, "init-methods", List.of("afterPropertiesSet"), bean.initMethods);
    beanFactory.destroySingletons();
    assertMethodOrdering(beanClass, "destroy-methods", List.of("destroy"), bean.destroyMethods);
  }

  public static class InitDestroyBean {

    final List<String> initMethods = new ArrayList<>();
    final List<String> destroyMethods = new ArrayList<>();

    public void afterPropertiesSet() throws Exception {
      this.initMethods.add("afterPropertiesSet");
    }

    public void destroy() throws Exception {
      this.destroyMethods.add("destroy");
    }
  }

  public static class InitializingDisposableWithShadowedMethodsBean
          extends InitDestroyBean implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
      this.initMethods.add("InitializingBean.afterPropertiesSet");
    }

    @Override
    public void destroy() throws Exception {
      this.destroyMethods.add("DisposableBean.destroy");
    }
  }

  public static class CustomInitDestroyBean {

    final List<String> initMethods = new ArrayList<>();
    final List<String> destroyMethods = new ArrayList<>();

    public void customInit() throws Exception {
      this.initMethods.add("customInit");
    }

    public void customDestroy() throws Exception {
      this.destroyMethods.add("customDestroy");
    }
  }

  public static class CustomInitializingDisposableBean extends CustomInitDestroyBean
          implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() throws Exception {
      this.initMethods.add("afterPropertiesSet");
    }

    @Override
    public void destroy() throws Exception {
      this.destroyMethods.add("destroy");
    }
  }

  public static class CustomAnnotatedInitDestroyBean extends CustomInitializingDisposableBean {

    @PostConstruct
    public void postConstruct() throws Exception {
      this.initMethods.add("postConstruct");
    }

    @PreDestroy
    public void preDestroy() throws Exception {
      this.destroyMethods.add("preDestroy");
    }
  }

  public static class CustomAnnotatedInitDestroyWithShadowedMethodsBean extends CustomInitializingDisposableBean {

    @PostConstruct
    @Override
    public void afterPropertiesSet() throws Exception {
      this.initMethods.add("@PostConstruct.afterPropertiesSet");
    }

    @PreDestroy
    @Override
    public void destroy() throws Exception {
      this.destroyMethods.add("@PreDestroy.destroy");
    }
  }

  public static class AllInOneBean implements InitializingBean, DisposableBean {

    final List<String> initMethods = new ArrayList<>();
    final List<String> destroyMethods = new ArrayList<>();

    @Override
//    @PostConstruct
    public void afterPropertiesSet() throws Exception {
      this.initMethods.add("afterPropertiesSet");
    }

    @Override
//    @PreDestroy
    public void destroy() throws Exception {
      this.destroyMethods.add("destroy");
    }
  }

}
