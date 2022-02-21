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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.support.AbstractBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.ApplicationPropertySourcesProcessor;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Prototype;
import cn.taketoday.lang.Singleton;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.DataSize;
import lombok.ToString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Today <br>
 *
 * 2019-01-22 18:55
 */
class BeanFactoryTests {
  private static final Logger log = LoggerFactory.getLogger(BeanFactoryTests.class);

  private StandardApplicationContext context;

  private StandardBeanFactory beanFactory;

  public StandardBeanFactory getBeanFactory() {
    return beanFactory;
  }

  public StandardApplicationContext getContext() {
    return context;
  }

  @BeforeEach
  public void beforeEach() throws IOException {
    context = new StandardApplicationContext();
    ApplicationPropertySourcesProcessor processor = new ApplicationPropertySourcesProcessor(context);
    processor.setPropertiesLocation("info.properties");
    processor.postProcessEnvironment();

    context.scan("cn.taketoday.beans.factory", "test.demo.config");
    context.refresh();

    beanFactory = context.getBeanFactory();
    beanFactory.setConversionService(new ApplicationConversionService());
  }

  @AfterEach
  public void end() {
    ConfigurableApplicationContext context = getContext();
    if (context != null) {
      context.close();
    }
  }

  @Test
  void getBeanWithType() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();

    Object implements1 = beanFactory.getBean(Implements1.class);
    Object implements2 = beanFactory.getBean(Implements2.class);
    Object implements3 = beanFactory.getBean(Implements3.class);

    List beans = beanFactory.getBeans(Interface.class);

    assertThat(beans).hasSize(3)
            .contains(implements1, implements2, implements3);

    assert implements1 != null;
    assert implements2 != null;
    assert implements3 != null;
  }

  public String createBeanName(Class<?> c) {
    return BeanDefinitionBuilder.defaultBeanName(c);
  }

  @Test
  void getBeanWithName() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();

    Object bean = beanFactory.getBean(createBeanName(Interface.class));

    Object implements1 = beanFactory.getBean(createBeanName(Implements1.class));
    Object implements2 = beanFactory.getBean(createBeanName(Implements2.class));
    Object implements3 = beanFactory.getBean(createBeanName(Implements3.class));

    assertThat(implements1).isInstanceOf(Interface.class);
    assertThat(implements2).isInstanceOf(Interface.class);
    assertThat(implements3).isInstanceOf(Interface.class);

    assert bean == null; // there isn't a bean named Interface

    assert implements1 != null;
    assert implements2 != null;
    assert implements3 != null;
  }

  @Test
  void getBeans() throws NoSuchBeanDefinitionException {

    ConfigurableBeanFactory beanFactory = getBeanFactory();

    List<Interface> beans = beanFactory.getBeans(Interface.class);

    log.debug("beans: {}", beans);

    assert beans.size() == 3;
    assert beans.contains(beanFactory.getBean(Implements1.class));
    assert beans.contains(beanFactory.getBean(Implements2.class));
    assert beans.contains(beanFactory.getBean(Implements3.class));
  }

  @Test
  void getAnnotatedBeans() throws NoSuchBeanDefinitionException {

    ConfigurableBeanFactory beanFactory = getBeanFactory();

    List<Object> annotatedBeans = beanFactory.getAnnotatedBeans(Singleton.class);
    log.debug("beans: {}", annotatedBeans);
    assert annotatedBeans.size() > 0;
  }

  @Test
  void getType() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    Class<?> type = beanFactory.getType("implements1");
    log.debug("type: {}", type);
    assert Implements1.class == type;
  }

  @Test
  void isPrototype() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();

    assert beanFactory.isPrototype("FactoryBean-Config");

    try {
      beanFactory.isPrototype("today");
      fail("isPrototype");
    }
    catch (NoSuchBeanDefinitionException e) {
    }
  }

  @Test
  void isSingleton() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    assert beanFactory.isSingleton("implements1");
  }

  // ------------------------------------2.1.6

  @Test
  void testAddBeanPostProcessor() {

    AbstractBeanFactory beanFactory = (AbstractBeanFactory) getBeanFactory();

    BeanPostProcessor beanPostProcessor = new BeanPostProcessor() { };

    List<BeanPostProcessor> postProcessors = beanFactory.getBeanPostProcessors();

    int size = postProcessors.size();
    System.err.println(size);

    beanFactory.addBeanPostProcessor(beanPostProcessor);

    System.err.println(postProcessors);

    assert postProcessors.size() == size + 1;

    beanFactory.removeBeanPostProcessor(beanPostProcessor);

  }

  @ToString
  public static class TEST {
    //    public int test;
    private DataSize test;

  }

  @ToString
  @Prototype("testBean")
  // @Singleton("test.beans.factory.BeanFactoryTest.FactoryBeanTestBean")
  public static class FactoryBeanTestBean implements FactoryBean<TEST>, InitializingBean {

    @Value("${upload.maxFileSize}")
//    private int testInt;
    private DataSize testInt;

    @Override
    public TEST getObject() {
      TEST test = new TEST();
      test.test = testInt;
      return test;
    }

    @Override
    public Class<TEST> getObjectType() {
      return TEST.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      System.err.println(testInt);// 10240000
    }
  }

  @Test
  void testFactoryBean() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    TEST bean = beanFactory.getBean("testBean", TEST.class);

    System.err.println(bean);

    BeanDefinition beanDefinition = beanFactory.getBeanDefinition("testBean");
    System.err.println(beanDefinition);
    System.err.println(beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + "testBean"));
  }

  @Test
  void testGetBeansOfType() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    Map<String, Interface> beansOfType = beanFactory.getBeansOfType(Interface.class);
    assert beansOfType.size() == 3;
  }

  //

  static class RegisterBean implements Interface {

    @Override
    public void test() {

    }
  }

  @Test
  void registerBean() {
    StandardBeanFactory beanFactory = getBeanFactory();
    // System.err.println(beanFactory);

    RegisterBean obj = new RegisterBean();
    context.registerSingleton("registerBean", obj);

    Interface singleton = beanFactory.getBean("registerBean", Interface.class);

    assertThat(singleton)
            .isEqualTo(obj)
            .isNotNull();

    beanFactory.removeBeanDefinition("registerBean");

    // @since 4.0

    // name
    RegisterBeanSupplier registerBeanSupplier = new RegisterBeanSupplier();

    context.registerBean("registerBeanSupplier-singleton", () -> registerBeanSupplier);
    context.registerBean("registerBeanSupplier-prototype", RegisterBeanSupplier::new);

    assertThat(registerBeanSupplier)
            .isEqualTo(beanFactory.getBean("registerBeanSupplier-singleton"))
            .isNotNull()
            .isNotEqualTo(beanFactory.getBean("registerBeanSupplier-prototype"))
            .isNotNull();

    assertThat(beanFactory.getBean("registerBeanSupplier-prototype"))
            .isNotNull()
            .isNotEqualTo(beanFactory.getBean("registerBeanSupplier-prototype"))
            .isNotNull();

    // type

    context.registerBean(RegisterBeanSupplier.class, RegisterBeanSupplier::new, true);
    RegisterBeanSupplier prototypeBean = beanFactory.getBean(RegisterBeanSupplier.class);

    assertThat(prototypeBean)
            .isNotNull()
            .isNotEqualTo(beanFactory.getBean(RegisterBeanSupplier.class));

    context.registerBean(RegisterBeanSupplier.class, RegisterBeanSupplier::new);
    RegisterBeanSupplier bean = beanFactory.getBean(RegisterBeanSupplier.class);
    assertThat(bean)
            .isNotNull()
            .isEqualTo(beanFactory.getBean(RegisterBeanSupplier.class));

    beanFactory.removeBeanDefinition("AnnotationBean");
    // Annotation
    context.registerBean(AnnotationRegisterBeanSupplier.class, AnnotationRegisterBeanSupplier::new, false, true);

    assertThat(beanFactory.getBean(AnnotationRegisterBeanSupplier.class))
            .isNotNull()
            .isEqualTo(beanFactory.getBean("annotationRegisterBeanSupplier"));

    beanFactory.getBeanNamesForType(AnnotationRegisterBeanSupplier.class, true, false)
            .forEach(beanFactory::removeBeanDefinition);

    context.registerBean(AnnotationRegisterBeanSupplier.class, AnnotationRegisterBeanSupplier::new, false, false);

    assertThat(beanFactory.getBean(AnnotationRegisterBeanSupplier.class))
            .isNotNull()
            .isEqualTo(beanFactory.getBean("AnnotationBean"));
  }

  static class RegisterBeanSupplier {

  }

  @Component("AnnotationBean")
  static class AnnotationRegisterBeanSupplier {

  }

  @Test
  void getSingleton() {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    Interface singleton = beanFactory.getSingleton(Interface.class);
    assertThat(singleton)
            .isNotNull();

  }

}
