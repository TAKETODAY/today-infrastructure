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
package cn.taketoday.orm.mybatis.annotation;

import com.mockrunner.mock.jdbc.MockDataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanNamePopulator;
import cn.taketoday.beans.factory.SimpleThreadScope;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.ConstructorArgumentValues;
import cn.taketoday.beans.factory.support.RuntimeBeanReference;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.PropertySource;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.lang.Component;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.annotation.mapper.ds1.AppConfigWithDefaultMapperScanAndRepeat;
import cn.taketoday.orm.mybatis.annotation.mapper.ds1.AppConfigWithDefaultMapperScans;
import cn.taketoday.orm.mybatis.annotation.mapper.ds1.Ds1Mapper;
import cn.taketoday.orm.mybatis.annotation.mapper.ds2.Ds2Mapper;
import cn.taketoday.orm.mybatis.mapper.AnnotatedMapper;
import cn.taketoday.orm.mybatis.mapper.AppConfigWithDefaultPackageScan;
import cn.taketoday.orm.mybatis.mapper.MapperInterface;
import cn.taketoday.orm.mybatis.mapper.MapperScannerConfigurer;
import cn.taketoday.orm.mybatis.mapper.MapperSubinterface;
import cn.taketoday.orm.mybatis.mapper.child.MapperChildInterface;
import cn.taketoday.orm.mybatis.type.DummyMapperFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the MapperScannerRegistrar.
 * <p>
 */
class MapperScanTest {
  private StandardApplicationContext applicationContext;

  @BeforeEach
  void setupContext() {
    applicationContext = new StandardApplicationContext();
    applicationContext.getBeanFactory().registerScope("thread", new SimpleThreadScope());

    setupSqlSessionFactory();

    // assume support for autowiring fields is added by MapperScannerConfigurer
    // via
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
//      assertBeanNotLoaded("annotatedMapperZeroMethods"); // as of 1.1.0 mappers
      // with no methods are loaded
    }
    finally {
      applicationContext.close();
    }
  }

  @Test
  void testDefaultMapperScan() {
    applicationContext.register(AppConfigWithDefaultPackageScan.class);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");

    assertThat(applicationContext
            .getBeanDefinition(applicationContext.getBeanNamesForType(MapperScannerConfigurer.class)
                    .iterator().next()).getRole())
            .isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
  }

  @Test
  void testInterfaceScan() {
    applicationContext.register(AppConfigWithPackageScan.class);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testInterfaceScanWithPackageClasses() {
    applicationContext.register(AppConfigWithPackageClasses.class);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testNameGenerator() {
    applicationContext.register(AppConfigWithNameGenerator.class);

    startContext();

    // only child inferfaces should be loaded and named with its class name
    applicationContext.getBean(MapperInterface.class.getName());
    applicationContext.getBean(MapperSubinterface.class.getName());
    applicationContext.getBean(MapperChildInterface.class.getName());
    applicationContext.getBean(AnnotatedMapper.class.getName());
  }

  @Test
  void testMarkerInterfaceScan() {
    applicationContext.register(AppConfigWithMarkerInterface.class);

    startContext();

    // only child inferfaces should be loaded
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("annotatedMapper");
  }

  @Test
  void testAnnotationScan() {
    applicationContext.register(AppConfigWithAnnotation.class);

    startContext();

    // only annotated mappers should be loaded
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
    assertBeanNotLoaded("mapperSubinterface");
  }

  @Test
  void testMarkerInterfaceAndAnnotationScan() {
    applicationContext.register(AppConfigWithMarkerInterfaceAndAnnotation.class);

    startContext();

    // everything should be loaded but the marker interface
    applicationContext.getBean("annotatedMapper");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");

    assertBeanNotLoaded("mapperInterface");
  }

  @Test
  void testCustomMapperFactoryBean() {
    DummyMapperFactoryBean.clear();
    applicationContext.register(AppConfigWithCustomMapperFactoryBean.class);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");

    assertTrue(DummyMapperFactoryBean.getMapperCount() > 0);

  }

  @Test
  void testScanWithNameConflict() {
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClass(Object.class);
    applicationContext.registerBeanDefinition("mapperInterface", definition);

    applicationContext.register(AppConfigWithPackageScan.class);

    startContext();

    assertThat(applicationContext.getBean("mapperInterface").getClass())
            .as("scanner should not overwrite existing bean definition").isSameAs(Object.class);
  }

  private void setupSqlSessionFactory() {
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClass(SqlSessionFactoryBean.class);
    definition.propertyValues().add("dataSource", new MockDataSource());
    applicationContext.registerBeanDefinition("sqlSessionFactory", definition);
  }

  private void assertBeanNotLoaded(String name) {
    Object bean = applicationContext.getBean(name);
    assertThat(bean).as("bean should not be defined for class " + name).isNull();
  }

  @Test
  void testScanWithExplicitSqlSessionFactory() {
    applicationContext.register(AppConfigWithSqlSessionFactory.class);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testScanWithExplicitSqlSessionTemplate() {
    BeanDefinition definition = new BeanDefinition();
    definition.setBeanClass(SqlSessionTemplate.class);
    ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
    constructorArgs.addGenericArgumentValue(new RuntimeBeanReference("sqlSessionFactory"));
    definition.setConstructorArgumentValues(constructorArgs);
    applicationContext.registerBeanDefinition("sqlSessionTemplate", definition);

    applicationContext.register(AppConfigWithSqlSessionTemplate.class);

    startContext();

    // all interfaces with methods should be loaded
    applicationContext.getBean("mapperInterface");
    applicationContext.getBean("mapperSubinterface");
    applicationContext.getBean("mapperChildInterface");
    applicationContext.getBean("annotatedMapper");
  }

  @Test
  void testScanWithMapperScanIsRepeat() {
    applicationContext.register(AppConfigWithMapperScanIsRepeat.class);

    startContext();

    applicationContext.getBean("ds1Mapper");
    applicationContext.getBean("ds2Mapper");
  }

  @Test
  void testScanWithMapperScans() {
    applicationContext.register(AppConfigWithMapperScans.class);

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(2, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    applicationContext.getBean("ds1Mapper");
    applicationContext.getBean("ds2Mapper");
  }

  @Test
  void testScanWithDefaultMapperScanAndRepeat() {
    applicationContext.register(AppConfigWithDefaultMapperScanAndRepeat.class);

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(2, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    applicationContext.getBean("ds1Mapper");
    applicationContext.getBean("ds2Mapper");
  }

  @Test
  void testScanWithDefaultMapperScans() {
    applicationContext.register(AppConfigWithDefaultMapperScans.class);

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(2, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

    applicationContext.getBean("ds1Mapper");
    applicationContext.getBean("ds2Mapper");
  }

  @Test
  void testLazyScanWithPropertySourcesPlaceholderConfigurer() {
    applicationContext.register(LazyConfigWithPropertySourcesPlaceholderConfigurer.class);

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(0, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
    applicationContext.getBean(Ds1Mapper.class);
    assertEquals(1, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

  }

  @Test
  void testLazyConfigWithPropertySource() {
    applicationContext.register(LazyConfigWithPropertySource.class);

    startContext();

    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(0, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
    applicationContext.getBean(Ds1Mapper.class);
    assertEquals(1, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());

  }

  @Disabled
  @Test
  void testScopedProxyMapperScanByDefaultScope() {
    applicationContext.register(ScopedProxy.class);

    startContext();

    List<String> scopedProxyTargetBeans = Stream.of(applicationContext.getBeanDefinitionNames())
            .filter(x -> x.startsWith("scopedTarget")).collect(Collectors.toList());
    assertThat(scopedProxyTargetBeans).hasSize(1).contains("scopedTarget.ds1Mapper");

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
      Ds1Mapper mapper = applicationContext.getBean(Ds1Mapper.class);
      assertThat(mapper.test()).isEqualTo("ds1");
    }
    {
      Ds2Mapper mapper = applicationContext.getBean(Ds2Mapper.class);
      assertThat(mapper.test()).isEqualTo("ds2");
    }
    SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
    assertEquals(2, sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers().size());
  }

  @Configuration
  @MapperScan("cn.taketoday.orm.mybatis.mapper")
  public static class AppConfigWithPackageScan {
  }

  @Configuration
  @MapperScan(basePackageClasses = MapperInterface.class)
  public static class AppConfigWithPackageClasses {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.mapper",
              markerInterface = MapperInterface.class)
  public static class AppConfigWithMarkerInterface {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.mapper",
              annotationClass = Component.class)
  public static class AppConfigWithAnnotation {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.mapper",
              annotationClass = Component.class,
              markerInterface = MapperInterface.class)
  public static class AppConfigWithMarkerInterfaceAndAnnotation {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.mapper",
              sqlSessionTemplateRef = "sqlSessionTemplate")
  public static class AppConfigWithSqlSessionTemplate {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.mapper",
              sqlSessionFactoryRef = "sqlSessionFactory")
  public static class AppConfigWithSqlSessionFactory {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.mapper",
              namePopulator = MapperScanTest.BeanNameGenerator.class)
  public static class AppConfigWithNameGenerator {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.mapper", factoryBean = DummyMapperFactoryBean.class)
  public static class AppConfigWithCustomMapperFactoryBean {
  }

  @Configuration
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.annotation.mapper.ds1")
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.annotation.mapper.ds2")
  public static class AppConfigWithMapperScanIsRepeat {
  }

  @Configuration
  @MapperScans({
          @MapperScan(basePackages = "cn.taketoday.orm.mybatis.annotation.mapper.ds1"),
          @MapperScan(basePackages = "cn.taketoday.orm.mybatis.annotation.mapper.ds2")
  })
  public static class AppConfigWithMapperScans {
  }

  @ComponentScan("cn.taketoday.orm.mybatis.annotation.factory")
  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.annotation.mapper.ds1",
              lazyInitialization = "${mybatis.lazy-initialization:false}")
  public static class LazyConfigWithPropertySourcesPlaceholderConfigurer {

    @Bean
    static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
      PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
      configurer.setLocation(new ClassPathResource("cn/taketoday/orm/mybatis/annotation/scan.properties"));
      return configurer;
    }

  }

  @MapperScan(basePackages = "cn.taketoday.orm.mybatis.annotation.mapper.ds1",
              lazyInitialization = "${mybatis.lazy-initialization:false}")
  @PropertySource("classpath:cn/taketoday/orm/mybatis/annotation/scan.properties")
  public static class LazyConfigWithPropertySource {

  }

  @MapperScan(basePackages = {
          "cn.taketoday.orm.mybatis.annotation.mapper.ds1",
          "cn.taketoday.orm.mybatis.annotation.mapper.ds2"
  }, defaultScope = "${mybatis.default-scope:thread}")
  public static class ScopedProxy {

  }

  public static class BeanNameGenerator implements BeanNamePopulator {

    @Override
    public String populateName(BeanDefinition definition, BeanDefinitionRegistry registry) {
      definition.setBeanName(definition.getBeanClassName());
      return definition.getBeanClassName();
    }
  }

}
