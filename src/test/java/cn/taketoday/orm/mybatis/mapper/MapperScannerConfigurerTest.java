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
package cn.taketoday.orm.mybatis.mapper;

import com.mockrunner.mock.jdbc.MockDataSource;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.support.BeanNamePopulator;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.SimpleThreadScope;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.PropertyPlaceholderConfigurer;
import cn.taketoday.beans.factory.support.RuntimeBeanReference;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.lang.Component;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.mapper.child.MapperChildInterface;
import cn.taketoday.orm.mybatis.type.DummyMapperFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MapperScannerConfigurerTest {
  private GenericApplicationContext applicationContext;

  @BeforeEach
  void setupContext() {
    applicationContext = new GenericApplicationContext();

    // add the mapper scanner as a bean definition rather than explicitly setting a
    // postProcessor on the context so initialization follows the same code path as reading from
    // an XML config file
    BeanDefinition definition = new BeanDefinition(MapperScannerConfigurer.class);
    definition.propertyValues().add("basePackage", "cn.taketoday.orm.mybatis.mapper");
    applicationContext.registerBeanDefinition("mapperScanner", definition);
    applicationContext.getBeanFactory().registerScope("thread", new SimpleThreadScope());

    setupSqlSessionFactory("sqlSessionFactory");

    // assume support for autowiring fields is added by MapperScannerConfigurer via
    // cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner.includeAnnotationConfig
  }

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
      // assertBeanNotLoaded("annotatedMapperZeroMethods"); // as of 1.1.0 mappers with no methods are loaded
    }
    finally {
      applicationContext.close();
    }
  }

  @Test
  void testInterfaceScan() {
    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);

    assertEquals(5, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("scopedProxyMapper");
    applicationContext.getBean("scopedTarget.scopedProxyMapper");

//    assertThat(Stream.of(applicationContext.getBeanDefinitionNames())
//            .filter(x -> x.startsWith("scopedTarget")))
//            .hasSize(1);
//    assertThat(applicationContext.getBeanDefinition("mapperInterface").propertyValues()
//            .getPropertyValue("mapperInterface"))
//            .isEqualTo(MapperInterface.class);
//    assertThat(applicationContext.getBeanDefinition("mapperSubinterface").propertyValues().getPropertyValue("mapperInterface"))
//            .isEqualTo(MapperSubinterface.class);
//    assertThat(applicationContext.getBeanDefinition("mapperChildInterface").propertyValues().getPropertyValue("mapperInterface"))
//            .isEqualTo(MapperChildInterface.class);
//    assertThat(applicationContext.getBeanDefinition("annotatedMapper").propertyValues().getPropertyValue("mapperInterface"))
//            .isEqualTo(AnnotatedMapper.class);
//    assertThat(applicationContext.getBeanDefinition("scopedTarget.scopedProxyMapper").propertyValues()
//            .getPropertyValue("mapperInterface")).isEqualTo(ScopedProxyMapper.class);
  }

  @Test
  void testNameGenerator() {
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClass(BeanNamePopulator0.class);
    applicationContext.registerBeanDefinition("BeanNamePopulator", definition);

    applicationContext.getBeanDefinition("mapperScanner")
            .propertyValues()
            .add("namePopulator", RuntimeBeanReference.from("BeanNamePopulator"));

    startContext();

    // only child inferfaces should be loaded and named with its class name
    applicationContext.getBean(MapperInterface.class.getName());
    applicationContext.getBean(MapperSubinterface.class.getName());
    applicationContext.getBean(MapperChildInterface.class.getName());
    applicationContext.getBean(AnnotatedMapper.class.getName());
  }

  @Test
  void testMarkerInterfaceScan() {
    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("markerInterface",
            MapperInterface.class);

    startContext();

    // only child inferfaces should be loaded
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("annotatedMapper");
  }

  @Test
  void testAnnotationScan() {
    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("annotationClass", Component.class);

    startContext();

    // only annotated mappers should be loaded
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("mapperSubinterface");
  }

  @Test
  void testMarkerInterfaceAndAnnotationScan() {
    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("markerInterface",
            MapperInterface.class);
    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("annotationClass", Component.class);

    startContext();

    // everything should be loaded but the marker interface
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
  }

  @Test
  @Disabled
  void testScopedProxyMapperScan() {
    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("annotationClass", Mapper.class);

    startContext();
    {
      BeanDefinition definition = applicationContext.getBeanDefinition("scopedProxyMapper");
      assertThat(definition.getBeanClassName()).isEqualTo("cn.taketoday.aop.scope.ScopedProxyFactoryBean");
      assertThat(definition.getScope()).isEqualTo("");
    }
    {
      BeanDefinition definition = applicationContext.getBeanDefinition("scopedTarget.scopedProxyMapper");
      assertThat(definition.getBeanClassName()).isEqualTo("cn.taketoday.orm.mybatis.mapper.MapperFactoryBean");
      assertThat(definition.getScope()).isEqualTo("thread");
    }
    {
      ScopedProxyMapper mapper = applicationContext.getBean(ScopedProxyMapper.class);
      assertThat(mapper.test()).isEqualTo("test");
    }
    {
      ScopedProxyMapper mapper = applicationContext.getBean("scopedTarget.scopedProxyMapper", ScopedProxyMapper.class);
      assertThat(mapper.test()).isEqualTo("test");
    }
    {
      ScopedProxyMapper mapper = applicationContext.getBean("scopedProxyMapper", ScopedProxyMapper.class);
      assertThat(mapper.test()).isEqualTo("test");
    }

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(1, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
  }

  @Test
  @Disabled
  void testScopedProxyMapperScanByDefault() {
    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("defaultScope", "thread");

    startContext();

    List<String> scopedProxyTargetBeans = Stream.of(applicationContext.getBeanDefinitionNames())
            .filter(x -> x.startsWith("scopedTarget")).collect(Collectors.toList());
    assertThat(scopedProxyTargetBeans).hasSize(6).contains("scopedTarget.scopedProxyMapper",
            "scopedTarget.annotatedMapper", "scopedTarget.annotatedMapperZeroMethods", "scopedTarget.mapperInterface",
            "scopedTarget.mapperSubinterface", "scopedTarget.mapperChildInterface");

    for (String scopedProxyTargetBean : scopedProxyTargetBeans) {
      {
        BeanDefinition definition = applicationContext.getBeanDefinition(scopedProxyTargetBean);
        assertThat(definition.getBeanClassName()).isEqualTo("cn.taketoday.orm.mybatis.mapper.MapperFactoryBean");
        assertThat(definition.getScope()).isEqualTo("thread");
      }
      {
        BeanDefinition definition = applicationContext.getBeanDefinition(scopedProxyTargetBean.substring(13));
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

  @Test
  void testScanWithExplicitSqlSessionFactory() {
    setupSqlSessionFactory("sqlSessionFactory2");

    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("sqlSessionFactoryBeanName",
            "sqlSessionFactory2");

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

/*  @Test
  void testScanWithExplicitSqlSessionTemplate() {
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClass(SqlSessionTemplate.class);
    ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
    constructorArgs.addGenericArgumentValue(new RuntimeBeanReference("sqlSessionFactory"));
    definition.setConstructorArgumentValues(constructorArgs);
    applicationContext.registerBeanDefinition("sqlSessionTemplate", definition);

    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("sqlSessionTemplateBeanName",
            "sqlSessionTemplate");

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }*/

  @Test
  void testScanWithExplicitSqlSessionFactoryViaPlaceholder() {
    setupSqlSessionFactory("sqlSessionFactory2");

    // use a property placeholder for the session factory name
    applicationContext.getBeanDefinition("mapperScanner").propertyValues().add("sqlSessionFactoryBeanName",
            "${sqlSessionFactoryBeanNameProperty}");

    Properties props = new java.util.Properties();
    props.put("sqlSessionFactoryBeanNameProperty", "sqlSessionFactory2");

    BeanDefinition propertyDefinition = new BeanDefinition();
    propertyDefinition.setBeanClass(PropertyPlaceholderConfigurer.class);
    propertyDefinition.propertyValues().add("properties", props);

    applicationContext.registerBeanDefinition("propertiesPlaceholder", propertyDefinition);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testScanWithNameConflict() {
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClass(Object.class);
    applicationContext.registerBeanDefinition("mapperInterface", definition);

    startContext();

    assertThat(applicationContext.getBean("mapperInterface").getClass())
            .as("scanner should not overwrite existing bean definition").isSameAs(Object.class);
  }

  @Test
  void testScanWithPropertyPlaceholders() {
    BeanDefinition definition = applicationContext.getBeanDefinition("mapperScanner");

    // use a property placeholder for basePackage
    definition.propertyValues().remove("basePackage");
    definition.propertyValues().add("basePackage", "${basePackageProperty}");
    definition.propertyValues().add("processPropertyPlaceHolders", true);
    // for lazy initialization
    definition.propertyValues().add("lazyInitialization", "${mybatis.lazy-initialization:false}");

    // also use a property placeholder for an SqlSessionFactory property
    // to make sure the configLocation was setup correctly and MapperScanner did not change
    // regular property placeholder substitution
    definition = applicationContext.getBeanDefinition("sqlSessionFactory");
    definition.propertyValues().remove("configLocation");
    definition.propertyValues().add("configLocation", "${configLocationProperty}");

    Properties props = new java.util.Properties();
    props.put("basePackageProperty", "cn.taketoday.orm.mybatis.mapper");
    props.put("configLocationProperty", "classpath:cn/taketoday/orm/mybatis/mybatis-config.xml");
    props.put("mybatis.lazy-initialization", "true");

    BeanDefinition propertyDefinition = new BeanDefinition(PropertySourcesPlaceholderConfigurer.class);
    propertyDefinition.propertyValues().add("properties", props);

    applicationContext.registerBeanDefinition("propertiesPlaceholder", propertyDefinition);

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    System.out.println(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers());
    assertEquals(1, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");

    assertEquals(5, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    // make sure the configLocation was setup correctly
    // mybatis-config.xml changes the executor from the default SIMPLE type
    SqlSessionFactory sessionFactory = (SqlSessionFactory) applicationContext.getBean("sqlSessionFactory");
    assertThat(sessionFactory.getConfiguration().getDefaultExecutorType()).isSameAs(ExecutorType.REUSE);
  }

  @Test
  void testScanWithMapperFactoryBeanClass() {
    DummyMapperFactoryBean.clear();

    applicationContext.getBeanDefinition("mapperScanner")
            .propertyValues().add("mapperFactoryBeanClass", DummyMapperFactoryBean.class);

    startContext();

    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");

    assertTrue(DummyMapperFactoryBean.getMapperCount() > 0);
  }

  @Test
  void testMapperBeanAttribute() {
    startContext();

    assertThat(applicationContext.getBeanDefinition("annotatedMapper")
            .getAttribute(ClassPathMapperScanner.FACTORY_BEAN_OBJECT_TYPE)).isEqualTo(AnnotatedMapper.class.getName());
  }

  private void setupSqlSessionFactory(String name) {
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.propertyValues().add("dataSource", new MockDataSource());
    applicationContext.registerBeanDefinition(name, definition);
  }

  private void assertBeanNotLoaded(String name) {
    try {
      BeanFactoryUtils.requiredBean(applicationContext, name);
      fail("bean should not be defined for class " + name);
    }
    catch (NoSuchBeanDefinitionException nsbde) {
      // success
    }
  }

  public static class BeanNamePopulator0 implements BeanNamePopulator {

    @Override
    public String populateName(BeanDefinition definition, BeanDefinitionRegistry registry) {
      definition.setBeanName(definition.getBeanClassName());
      return definition.getBeanClassName();
    }
  }

}
