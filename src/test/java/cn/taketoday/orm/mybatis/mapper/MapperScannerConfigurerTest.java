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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.config.PropertyPlaceholderConfigurer;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.config.SimpleThreadScope;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.lang.Component;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
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
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(MapperScannerConfigurer.class);
    definition.getPropertyValues().add("basePackage", "org.mybatis.spring.mapper");
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

    assertThat(Stream.of(applicationContext.getBeanDefinitionNames()).filter(x -> x.startsWith("scopedTarget")))
            .hasSize(1);
    assertThat(applicationContext.getBeanDefinition("mapperInterface").getPropertyValues().get("mapperInterface"))
            .isEqualTo(MapperInterface.class);
    assertThat(applicationContext.getBeanDefinition("mapperSubinterface").getPropertyValues().get("mapperInterface"))
            .isEqualTo(MapperSubinterface.class);
    assertThat(applicationContext.getBeanDefinition("mapperChildInterface").getPropertyValues().get("mapperInterface"))
            .isEqualTo(MapperChildInterface.class);
    assertThat(applicationContext.getBeanDefinition("annotatedMapper").getPropertyValues().get("mapperInterface"))
            .isEqualTo(AnnotatedMapper.class);
    assertThat(applicationContext.getBeanDefinition("scopedTarget.scopedProxyMapper").getPropertyValues()
            .get("mapperInterface")).isEqualTo(ScopedProxyMapper.class);
  }

  @Test
  void testNameGenerator() {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(BeanNameGenerator0.class);
    applicationContext.registerBeanDefinition("beanNameGenerator", definition);

    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("nameGenerator",
            new RuntimeBeanReference("beanNameGenerator"));

    startContext();

    // only child inferfaces should be loaded and named with its class name
    applicationContext.getBean(MapperInterface.class.getName());
    applicationContext.getBean(MapperSubinterface.class.getName());
    applicationContext.getBean(MapperChildInterface.class.getName());
    applicationContext.getBean(AnnotatedMapper.class.getName());
  }

  @Test
  void testMarkerInterfaceScan() {
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("markerInterface",
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
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("annotationClass", Component.class);

    startContext();

    // only annotated mappers should be loaded
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("mapperSubinterface");
  }

  @Test
  void testMarkerInterfaceAndAnnotationScan() {
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("markerInterface",
            MapperInterface.class);
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("annotationClass", Component.class);

    startContext();

    // everything should be loaded but the marker interface
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
  }

  @Test
  void testScopedProxyMapperScan() {
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("annotationClass", Mapper.class);

    startContext();
    {
      BeanDefinition definition = applicationContext.getBeanDefinition("scopedProxyMapper");
      assertThat(definition.getBeanClassName()).isEqualTo("cn.taketoday.aop.scope.ScopedProxyFactoryBean");
      assertThat(definition.getScope()).isEqualTo("");
    }
    {
      BeanDefinition definition = applicationContext.getBeanDefinition("scopedTarget.scopedProxyMapper");
      assertThat(definition.getBeanClassName()).isEqualTo("org.mybatis.spring.mapper.MapperFactoryBean");
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
  void testScopedProxyMapperScanByDefault() {
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("defaultScope", "thread");

    startContext();

    List<String> scopedProxyTargetBeans = Stream.of(applicationContext.getBeanDefinitionNames())
            .filter(x -> x.startsWith("scopedTarget")).collect(Collectors.toList());
    assertThat(scopedProxyTargetBeans).hasSize(6).contains("scopedTarget.scopedProxyMapper",
            "scopedTarget.annotatedMapper", "scopedTarget.annotatedMapperZeroMethods", "scopedTarget.mapperInterface",
            "scopedTarget.mapperSubinterface", "scopedTarget.mapperChildInterface");

    for (String scopedProxyTargetBean : scopedProxyTargetBeans) {
      {
        BeanDefinition definition = applicationContext.getBeanDefinition(scopedProxyTargetBean);
        assertThat(definition.getBeanClassName()).isEqualTo("org.mybatis.spring.mapper.MapperFactoryBean");
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

    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("sqlSessionFactoryBeanName",
            "sqlSessionFactory2");

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testScanWithExplicitSqlSessionTemplate() {
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionTemplate.class);
    ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
    constructorArgs.addGenericArgumentValue(new RuntimeBeanReference("sqlSessionFactory"));
    definition.setConstructorArgumentValues(constructorArgs);
    applicationContext.registerBeanDefinition("sqlSessionTemplate", definition);

    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("sqlSessionTemplateBeanName",
            "sqlSessionTemplate");

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testScanWithExplicitSqlSessionFactoryViaPlaceholder() {
    setupSqlSessionFactory("sqlSessionFactory2");

    // use a property placeholder for the session factory name
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("sqlSessionFactoryBeanName",
            "${sqlSessionFactoryBeanNameProperty}");

    Properties props = new java.util.Properties();
    props.put("sqlSessionFactoryBeanNameProperty", "sqlSessionFactory2");

    GenericBeanDefinition propertyDefinition = new GenericBeanDefinition();
    propertyDefinition.setBeanClass(PropertyPlaceholderConfigurer.class);
    propertyDefinition.getPropertyValues().add("properties", props);

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
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(Object.class);
    applicationContext.registerBeanDefinition("mapperInterface", definition);

    startContext();

    assertThat(applicationContext.getBean("mapperInterface").getClass())
            .as("scanner should not overwrite existing bean definition").isSameAs(Object.class);
  }

  @Test
  void testScanWithPropertyPlaceholders() {
    GenericBeanDefinition definition = (GenericBeanDefinition) applicationContext.getBeanDefinition("mapperScanner");

    // use a property placeholder for basePackage
    definition.getPropertyValues().remove("basePackage");
    definition.getPropertyValues().add("basePackage", "${basePackageProperty}");
    definition.getPropertyValues().add("processPropertyPlaceHolders", true);
    // for lazy initialization
    definition.getPropertyValues().add("lazyInitialization", "${mybatis.lazy-initialization:false}");

    // also use a property placeholder for an SqlSessionFactory property
    // to make sure the configLocation was setup correctly and MapperScanner did not change
    // regular property placeholder substitution
    definition = (GenericBeanDefinition) applicationContext.getBeanDefinition("sqlSessionFactory");
    definition.getPropertyValues().remove("configLocation");
    definition.getPropertyValues().add("configLocation", "${configLocationProperty}");

    Properties props = new java.util.Properties();
    props.put("basePackageProperty", "org.mybatis.spring.mapper");
    props.put("configLocationProperty", "classpath:org/mybatis/spring/mybatis-config.xml");
    props.put("mybatis.lazy-initialization", "true");

    GenericBeanDefinition propertyDefinition = new GenericBeanDefinition();
    propertyDefinition.setBeanClass(PropertyPlaceholderConfigurer.class);
    propertyDefinition.getPropertyValues().add("properties", props);

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
    applicationContext.getBeanDefinition("mapperScanner").getPropertyValues().add("mapperFactoryBeanClass",
            DummyMapperFactoryBean.class);

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
    GenericBeanDefinition definition = new GenericBeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.getPropertyValues().add("dataSource", new MockDataSource());
    applicationContext.registerBeanDefinition(name, definition);
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

  public static class BeanNameGenerator0 implements BeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
      definition.setBeanName(definition.getBeanClassName());
      return definition.getBeanClassName();
    }

  }

}
