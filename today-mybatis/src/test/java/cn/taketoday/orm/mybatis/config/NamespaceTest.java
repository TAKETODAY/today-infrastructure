/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.orm.mybatis.config;

import com.mockrunner.mock.jdbc.MockDataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.mapper.AnnotatedMapper;
import cn.taketoday.orm.mybatis.mapper.MapperInterface;
import cn.taketoday.orm.mybatis.mapper.MapperScannerConfigurer;
import cn.taketoday.orm.mybatis.mapper.MapperSubinterface;
import cn.taketoday.orm.mybatis.mapper.ScopedProxyMapper;
import cn.taketoday.orm.mybatis.mapper.child.MapperChildInterface;
import cn.taketoday.orm.mybatis.type.DummyMapperFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for the MapperScannerRegistrar.
 * <p>
 * This test works fine with  and 3.2 but with 3.1 the registrar is called twice.
 */
class NamespaceTest {
  private ClassPathXmlApplicationContext applicationContext;

  private void startContext() {
    applicationContext.refresh();
    applicationContext.start();

    // this will throw an exception if the beans cannot be found
    applicationContext.getBean("sqlSessionFactory");
  }

  @AfterEach
  void assertNoMapperClass() {
    try {
      // concrete classes should always be ignored by MapperScannerPostProcessor
      assertBeanNotLoaded("mapperClass");

      // no method interfaces should be ignored too
      assertBeanNotLoaded("package-info");
      // assertBeanNotLoaded("annotatedMapperZeroMethods"); // as of 1.1.0 mappers
      // with no methods are loaded
    }
    finally {
      if (applicationContext != null)
        applicationContext.close();
    }
  }

  @Test
  void testInterfaceScan() {

    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/base-package.xml" }, setupSqlSessionFactory());

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(5, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");

    assertThat(applicationContext.getBeanFactory()
            .getBeanDefinition(applicationContext.getBeanNamesForType(MapperScannerConfigurer.class).iterator().next()).getRole())
            .isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);

  }

  @Test
  void testNameGenerator() {

    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/name-generator.xml" }, setupSqlSessionFactory());

    startContext();

    // only child inferfaces should be loaded and named with its class name
    applicationContext.getBean(MapperInterface.class.getName());
    applicationContext.getBean(MapperSubinterface.class.getName());
    applicationContext.getBean(MapperChildInterface.class.getName());
    applicationContext.getBean(AnnotatedMapper.class.getName());
  }

  @Test
  void testMarkerInterfaceScan() {

    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/marker-interface.xml" }, setupSqlSessionFactory());

    startContext();

    // only child inferfaces should be loaded
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("annotatedMapper");
  }

  @Test
  void testAnnotationScan() {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "cn/taketoday/orm/mybatis/config/annotation.xml" },
            setupSqlSessionFactory());

    startContext();

    // only annotated mappers should be loaded
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("mapperSubinterface");
  }

  @Test
  void testMarkerInterfaceAndAnnotationScan() {

    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/marker-and-annotation.xml" }, setupSqlSessionFactory());

    startContext();

    // everything should be loaded but the marker interface
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
  }

  @Test
  void testScanWithExplicitSqlSessionFactory() {

    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/factory-ref.xml" }, setupSqlSessionFactory());

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testScanWithExplicitSqlSessionTemplate() {

    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/factory-ref.xml" }, setupSqlSessionTemplate());

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testScanWithMapperFactoryBeanClass() {
    DummyMapperFactoryBean.clear();
    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/mapper-factory-bean-class.xml" }, setupSqlSessionTemplate());

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");

    assertTrue(DummyMapperFactoryBean.getMapperCount() > 0);
  }

  @Test
  void testLazy() {

    applicationContext = new ClassPathXmlApplicationContext(new String[] { "cn/taketoday/orm/mybatis/config/lazy.xml" },
            setupSqlSessionTemplate());

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(0, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");

    assertEquals(4, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
  }

  @Test
  void testDefaultScope() {
    applicationContext = new ClassPathXmlApplicationContext(
            new String[] { "cn/taketoday/orm/mybatis/config/default-scope.xml" }, false, setupSqlSessionTemplate());

    startContext();

    List<String> scopedProxyTargetBeans = Stream.of(applicationContext.getBeanDefinitionNames())
            .filter(x -> x.startsWith("scopedTarget")).collect(Collectors.toList());
    assertThat(scopedProxyTargetBeans).hasSize(6).contains("scopedTarget.scopedProxyMapper",
            "scopedTarget.annotatedMapper", "scopedTarget.annotatedMapperZeroMethods", "scopedTarget.mapperInterface",
            "scopedTarget.mapperSubinterface", "scopedTarget.mapperChildInterface");

    for (String scopedProxyTargetBean : scopedProxyTargetBeans) {
      {
        BeanDefinition definition = applicationContext.getBeanFactory().getBeanDefinition(scopedProxyTargetBean);
        assertThat(definition.getBeanClassName()).isEqualTo("cn.taketoday.orm.mybatis.mapper.MapperFactoryBean");
        assertThat(definition.getScope()).isEqualTo("thread");
      }
      {
        BeanDefinition definition = applicationContext.getBeanFactory()
                .getBeanDefinition(scopedProxyTargetBean.substring(13));
        assertThat(definition.getBeanClassName()).isEqualTo("cn.taketoday.aop.scope.ScopedProxyFactoryBean");
        assertThat(definition.getScope()).isEqualTo("");
      }
    }
    {
      ScopedProxyMapper mapper = applicationContext.getBean(ScopedProxyMapper.class);
      assertThat(mapper.test()).isEqualTo("test");
    }
    {
      AnnotatedMapper mapper = applicationContext.getBean(AnnotatedMapper.class);
      assertThat(mapper.test()).isEqualTo("main");
    }

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(2, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
  }

  private GenericApplicationContext setupSqlSessionTemplate() {

    GenericApplicationContext genericApplicationContext = setupSqlSessionFactory();
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionTemplate.class);
    ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
    constructorArgs.addGenericArgumentValue(new RuntimeBeanReference("sqlSessionFactory"));
    definition.setConstructorArgumentValues(constructorArgs);
    genericApplicationContext.registerBeanDefinition("sqlSessionTemplate", definition);
    return genericApplicationContext;
  }

  private GenericApplicationContext setupSqlSessionFactory() {

    GenericApplicationContext genericApplicationContext = new GenericApplicationContext();

    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.getPropertyValues().add("dataSource", new MockDataSource());

    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerBeanDefinition("sqlSessionFactory", definition);

    genericApplicationContext.registerBeanDefinition("sqlSessionFactory", definition);

    genericApplicationContext.refresh();

    return genericApplicationContext;
  }

  private void assertBeanNotLoaded(String name) {
    try {
      applicationContext.getBean(name);
      fail("Spring bean should not be defined for class " + name);
    }
    catch (NoSuchBeanDefinitionException nsbde) {
      // success
    }
  }

  public static class BeanNameGenerator implements cn.taketoday.beans.factory.support.BeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry definitionRegistry) {
      return beanDefinition.getBeanClassName();
    }

  }

}
