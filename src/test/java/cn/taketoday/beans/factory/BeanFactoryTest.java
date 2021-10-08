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
package cn.taketoday.beans.factory;

import cn.taketoday.beans.FactoryBean;
import cn.taketoday.beans.InitializingBean;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.Value;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.DataSize;
import lombok.ToString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Today <br>
 *
 * 2019-01-22 18:55
 */
public class BeanFactoryTest {

  private static final Logger log = LoggerFactory.getLogger(BeanFactoryTest.class);

  private final ConfigurableApplicationContext context = //
          new StandardApplicationContext("", "cn.taketoday.beans.factory", "test.demo.config");

  private final ConfigurableBeanFactory beanFactory = context.getBeanFactory();

  public ConfigurableBeanFactory getBeanFactory() {
    return beanFactory;
  }

  public ConfigurableApplicationContext getContext() {
    return context;
  }

  @AfterEach
  public void end() {
    ConfigurableApplicationContext context = getContext();
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void test_GetBeanWithType() throws NoSuchBeanDefinitionException {
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

  public String createBeanName(Class<?> c){
    return ClassUtils.getShortName(c);
  }

  @Test
  public void test_GetBeanWithName() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();

    Object bean = beanFactory.getBean(createBeanName(Interface.class));

    Object implements1 = beanFactory.getBean(createBeanName(Implements1.class));
    Object implements2 = beanFactory.getBean(createBeanName(Implements2.class));
    Object implements3 = beanFactory.getBean(createBeanName(Implements3.class));

    assert bean == null; // there isn't a bean named Interface

    assert implements1 != null;
    assert implements2 != null;
    assert implements3 != null;
  }

  @Test
  public void test_GetBeans() throws NoSuchBeanDefinitionException {

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

    ConfigurableBeanFactory beanFactory = getBeanFactory();

    List<Object> annotatedBeans = beanFactory.getAnnotatedBeans(Singleton.class);
    log.debug("beans: {}", annotatedBeans);
    assert annotatedBeans.size() > 0;
  }

  @Test
  public void test_GetType() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    Class<?> type = beanFactory.getType("implements1");
    log.debug("type: {}", type);
    assert Implements1.class == type;
  }

  @Test
  public void test_GetAliases() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    Set<String> aliases = beanFactory.getAliases(Interface.class);

    log.debug("Aliases: {}", aliases);
    assert aliases.size() == 3;
  }

  @Test
  public void test_GetBeanName() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();

    String name = beanFactory.getBeanName(Implements1.class);
    try {
      beanFactory.getBeanName(Interface.class);
      assert false;
    }
    catch (Exception e) {
      assert true;
    }

    assert "implements1".equals(name);
  }

  @Test
  public void test_IsPrototype() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();

    assert beanFactory.isPrototype("FactoryBean-Config");

    try {
      beanFactory.isPrototype("today");
    }
    catch (NoSuchBeanDefinitionException e) {
    }
  }

  @Test
  public void test_IsSingleton() throws NoSuchBeanDefinitionException {
    ConfigurableBeanFactory beanFactory = getBeanFactory();
    assert beanFactory.isSingleton("implements1");
  }

  // ------------------------------------2.1.6

  @Test
  public void testAddBeanPostProcessor() {

    AbstractBeanFactory beanFactory = (AbstractBeanFactory) getBeanFactory();

    final BeanPostProcessor beanPostProcessor = new BeanPostProcessor() { };

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
    //    public int test;
    private DataSize test;

  }

  @ToString
  @Prototype("testBean")
  // @Singleton("test.beans.factory.BeanFactoryTest.FactoryBeanTestBean")
  public static class FactoryBeanTestBean implements FactoryBean<TEST>, InitializingBean {

    @Value("${env['upload.maxFileSize']}")
//    private int testInt;
    private DataSize testInt;

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
    final ConfigurableBeanFactory beanFactory = getBeanFactory();
    final TEST bean = beanFactory.getBean("testBean", TEST.class);

    System.err.println(bean);

    final BeanDefinition beanDefinition = beanFactory.getBeanDefinition("testBean");
    System.err.println(beanDefinition);
    System.err.println(beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + "testBean"));
  }

  @Test
  public void testGetBeansOfType() {
    final ConfigurableBeanFactory beanFactory = getBeanFactory();
    final Map<String, Interface> beansOfType = beanFactory.getBeansOfType(Interface.class);
    assert beansOfType.size() == 3;
  }

  //

  static class RegisterBean implements Interface {

    @Override
    public void test() {

    }
  }

  @Test
  public void registerBean() {
    final ConfigurableBeanFactory beanFactory = getBeanFactory();
    // System.err.println(beanFactory);

    final RegisterBean obj = new RegisterBean();
    beanFactory.registerBean("registerBean", obj);

    final Interface singleton = beanFactory.getBean("registerBean", Interface.class);

    assertThat(singleton)
            .isEqualTo(obj)
            .isNotNull();

    beanFactory.removeBean("registerBean");

    // @since 4.0

    // name
    final RegisterBeanSupplier registerBeanSupplier = new RegisterBeanSupplier();

    beanFactory.registerBean("registerBeanSupplier-singleton", () -> registerBeanSupplier);
    beanFactory.registerBean("registerBeanSupplier-prototype", RegisterBeanSupplier::new);

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

    beanFactory.registerBean(RegisterBeanSupplier.class, RegisterBeanSupplier::new, true);
    final RegisterBeanSupplier prototypeBean = beanFactory.getBean(RegisterBeanSupplier.class);

    assertThat(prototypeBean)
            .isNotNull()
            .isNotEqualTo(beanFactory.getBean(RegisterBeanSupplier.class));

    beanFactory.registerBean(RegisterBeanSupplier.class, RegisterBeanSupplier::new);
    final RegisterBeanSupplier bean = beanFactory.getBean(RegisterBeanSupplier.class);
    assertThat(bean)
            .isNotNull()
            .isEqualTo(beanFactory.getBean(RegisterBeanSupplier.class));

    // Annotation
    beanFactory.registerBean(AnnotationRegisterBeanSupplier.class, AnnotationRegisterBeanSupplier::new, false, true);

    assertThat(beanFactory.getBean(AnnotationRegisterBeanSupplier.class))
            .isNotNull()
            .isEqualTo(beanFactory.getBean("annotationRegisterBeanSupplier"));

    beanFactory.removeBean(AnnotationRegisterBeanSupplier.class);

    beanFactory.registerBean(AnnotationRegisterBeanSupplier.class, AnnotationRegisterBeanSupplier::new, false, false);

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
  public void getSingleton() {
    final ConfigurableBeanFactory beanFactory = getBeanFactory();
    final Interface singleton = beanFactory.getSingleton(Interface.class);
    assertThat(singleton)
            .isNotNull();

  }

}
