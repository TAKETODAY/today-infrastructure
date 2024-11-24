/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.orm.mybatis.auto;

import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import infra.annotation.config.context.PropertyPlaceholderAutoConfiguration;
import infra.annotation.config.jdbc.EmbeddedDataSourceConfiguration;
import infra.aop.scope.ScopedProxyFactoryBean;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.PropertiesFactoryBean;
import infra.beans.factory.config.RuntimeBeanReference;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Primary;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.support.SimpleThreadScope;
import infra.core.annotation.Order;
import infra.orm.mybatis.SqlSessionFactoryBean;
import infra.orm.mybatis.SqlSessionTemplate;
import infra.orm.mybatis.annotation.ConfigurationCustomizer;
import infra.orm.mybatis.annotation.MapperScan;
import infra.orm.mybatis.annotation.MybatisAutoConfiguration;
import infra.orm.mybatis.annotation.MybatisProperties;
import infra.orm.mybatis.annotation.SqlSessionFactoryBeanCustomizer;
import infra.orm.mybatis.auto.handler.AtomicNumberTypeHandler;
import infra.orm.mybatis.auto.handler.DummyTypeHandler;
import infra.orm.mybatis.auto.mapper.DateTimeMapper;
import infra.orm.mybatis.mapper.MapperFactoryBean;
import infra.orm.mybatis.mapper.MapperScannerConfigurer;
import infra.orm.mybatis.transaction.ManagedTransactionFactory;
import infra.test.util.TestPropertyValues;
import infra.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/5 22:37
 */
class MybatisAutoConfigurationTests {

  private AnnotationConfigApplicationContext context;

  @BeforeEach
  void init() {
    this.context = new AnnotationConfigApplicationContext();
  }

  @AfterEach
  void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void testNoDataSource() {
    this.context.register(MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).isEmpty();
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).isEmpty();
    assertThat(this.context.getBeanNamesForType(MybatisProperties.class)).isEmpty();
  }

  @Test
  void testMultipleDataSource() {
    this.context.register(MultipleDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).isEmpty();
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).isEmpty();
    assertThat(this.context.getBeanNamesForType(MybatisProperties.class)).isEmpty();
  }

  @Test
  void testSingleCandidateDataSource() {
    this.context.register(SingleCandidateDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(MybatisProperties.class)).hasSize(1);
  }

  @Test
  void testDefaultConfiguration() {
    this.context.register(
            EmbeddedDataSourceConfiguration.class,
            MybatisScanMapperConfiguration.class,
            MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class
    );
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(DateTimeMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();

    //    Map<String, LanguageDriver> languageDriverBeans = this.context.getBeansOfType(LanguageDriver.class);
//    assertThat(languageDriverBeans).hasSize(3).containsKeys("freeMarkerLanguageDriver", "velocityLanguageDriver",
//            "thymeleafLanguageDriver");
//
//    assertThat(languageDriverBeans.get("freeMarkerLanguageDriver")).isInstanceOf(FreeMarkerLanguageDriver.class);
//    assertThat(languageDriverBeans.get("velocityLanguageDriver")).isInstanceOf(VelocityLanguageDriver.class);
//    assertThat(languageDriverBeans.get("thymeleafLanguageDriver")).isInstanceOf(ThymeleafLanguageDriver.class);
    LanguageDriverRegistry languageDriverRegistry = sqlSessionFactory.getConfiguration().getLanguageRegistry();
    assertThat(languageDriverRegistry.getDefaultDriverClass()).isEqualTo(XMLLanguageDriver.class);
    assertThat(languageDriverRegistry.getDefaultDriver()).isInstanceOf(XMLLanguageDriver.class);
    assertThat(languageDriverRegistry.getDriver(XMLLanguageDriver.class)).isNotNull();
    assertThat(languageDriverRegistry.getDriver(RawLanguageDriver.class)).isNotNull();
//    assertThat(languageDriverRegistry.getDriver(FreeMarkerLanguageDriver.class)).isNotNull();
//    assertThat(languageDriverRegistry.getDriver(VelocityLanguageDriver.class)).isNotNull();
//    assertThat(languageDriverRegistry.getDriver(ThymeleafLanguageDriver.class)).isNotNull();
  }

  @Test
  void testScanWithLazy() {
    TestPropertyValues.of("mybatis.lazy-initialization:true").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisScanMapperConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(0);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(DateTimeMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();
    this.context.getBean(DateTimeMapper.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
  }

  @Test
  void testAutoScanWithDefault() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();
    this.context.getBean(CityMapper.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
    assertThat(((RuntimeBeanReference) this.context.getBeanDefinition("cityMapper").getPropertyValues()
            .getPropertyValue("sqlSessionTemplate")).getBeanName()).isEqualTo("sqlSessionTemplate");
    assertThat(
            context.getBeanDefinition(
                    CollectionUtils.firstElement(context.getBeanNamesForType(MapperScannerConfigurer.class))).getRole())
            .isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
  }

  @Test
  void testAutoScanWithInjectSqlSessionOnMapperScanIsFalse() {
    TestPropertyValues.of("mybatis.inject-sql-session-on-mapper-scan:false").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();
    this.context.getBean(CityMapper.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
    assertThat(this.context.getBeanDefinition("cityMapper").getPropertyValues().getPropertyValue("sqlSessionTemplate"))
            .isNull();
    assertThat(this.context.getBeanDefinition("cityMapper").getPropertyValues().getPropertyValue("sqlSessionFactory"))
            .isNull();
  }

  @Test
  void testAutoScanWithLazy() {
    TestPropertyValues.of("mybatis.lazy-initialization:true").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(0);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();
    this.context.getBean(CityMapper.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
  }

  @Test
  void testAutoScanWithDefaultScope() {
    TestPropertyValues.of("mybatis.mapper-default-scope:thread").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    {
      this.context.getBean(CityMapper.class);
      BeanDefinition bd = this.context.getBeanDefinition("cityMapper");
      assertThat(bd.getBeanClassName()).isEqualTo(ScopedProxyFactoryBean.class.getName());
      BeanDefinition spbd = this.context.getBeanDefinition("scopedTarget.cityMapper");
      assertThat(spbd.getBeanClassName()).isEqualTo(MapperFactoryBean.class.getName());
      assertThat(spbd.getScope()).isEqualTo("thread");
    }
  }

  @Test
  void testAutoScanWithoutDefaultScope() {
    this.context.register(
            EmbeddedDataSourceConfiguration.class,
            MybatisBootMapperScanAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class,
            CityMapperRepositoryConfiguration.class
    );
    this.context.refresh();

    CityMapper bean = this.context.getBean(CityMapper.class);
    assertThat(bean).isNotNull();
    BeanDefinition df = this.context.getBeanDefinition("cityMapper");
    assertThat(df.getBeanClassName()).isEqualTo(MapperFactoryBean.class.getName());
    assertThat(df.getScope()).isEqualTo("singleton");
  }

  @Test
  void testWithConfigLocation() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config.xml").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            MybatisMapperConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapperImpl.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.BATCH);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isTrue();
  }

  @Test
  void testWithCheckConfigLocationFileExists() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config.xml", "mybatis.check-config-location=true")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
  }

  @Test
  void testWithCheckConfigLocationFileNotSpecify() {
    TestPropertyValues.of("mybatis.check-config-location=true").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
  }

  @Test
  void testWithCheckConfigLocationFileDoesNotExists() {

    TestPropertyValues.of("mybatis.config-location:foo.xml", "mybatis.check-config-location=true")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(context::refresh)
            .withMessageContaining("Error creating bean with name 'mybatisAutoConfiguration': Cannot find config location: ")
            .havingRootCause()
            .withMessageContaining("Cannot find config location: class path resource [foo.xml] (please add config file or check your Mybatis configuration)");
  }

  @Test
  void testWithTypeHandlersPackage() {
    TestPropertyValues.of("mybatis.type-handlers-package:infra.orm.mybatis.auto.handler")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();

    TypeHandlerRegistry typeHandlerRegistry = this.context.getBean(SqlSessionFactory.class).getConfiguration()
            .getTypeHandlerRegistry();
    assertThat(typeHandlerRegistry.hasTypeHandler(BigInteger.class)).isTrue();
    assertThat(typeHandlerRegistry.hasTypeHandler(AtomicInteger.class)).isTrue();
    assertThat(typeHandlerRegistry.hasTypeHandler(AtomicLong.class)).isTrue();
  }

  @Test
  void testWithMapperLocation() {
    TestPropertyValues
            .of("mybatis.type-aliases-package:infra.orm.mybatis.auto.domain",
                    "mybatis.mapper-locations:classpath:infra/orm/mybatis/CityMapper.xml")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getMappedStatementNames()).hasSize(2);
  }

  @Test
  void testWithExecutorType() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config.xml", "mybatis.executor-type:REUSE")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            MybatisMapperConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.REUSE);
  }

  @Test
  void testDefaultBootConfiguration() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
  }

  @Test
  void testWithInterceptorsOrder1() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisInterceptorConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getInterceptors()).hasSize(2);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getInterceptors().get(0))
            .isInstanceOf(MyInterceptor2.class);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getInterceptors().get(1))
            .isInstanceOf(MyInterceptor.class);

    this.context.close();
  }

  @Test
  void testWithInterceptorsOrder2() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisInterceptorConfiguration2.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getInterceptors()).hasSize(2);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getInterceptors().get(0))
            .isInstanceOf(MyInterceptor.class);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getInterceptors().get(1))
            .isInstanceOf(MyInterceptor2.class);

    this.context.close();
  }

  @Test
  void testWithTypeHandlers() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisTypeHandlerConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getTypeHandlerRegistry()
            .getTypeHandler(UUID.class)).isInstanceOf(MyTypeHandler.class);
    this.context.close();
  }

  @Test
  void testWithDatabaseIdProvider() {
    this.context.register(EmbeddedDataSourceConfiguration.class, DatabaseProvidersConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getDatabaseId()).isEqualTo("h2");
  }

  @Test
  void testMixedWithConfigurationFileAndInterceptor() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config-settings-only.xml").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisInterceptorConfiguration.class);
    this.context.refresh();

    org.apache.ibatis.session.Configuration configuration = this.context.getBean(SqlSessionFactory.class)
            .getConfiguration();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(configuration.getDefaultFetchSize()).isEqualTo(1000);
    assertThat(configuration.getInterceptors()).hasSize(2);
    assertThat(configuration.getInterceptors().get(0)).isInstanceOf(MyInterceptor2.class);
    assertThat(configuration.getInterceptors().get(1)).isInstanceOf(MyInterceptor.class);
  }

  @Test
  void testMixedWithConfigurationFileAndDatabaseIdProvider() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config-settings-only.xml").applyTo(this.context);
    this.context.register(
            EmbeddedDataSourceConfiguration.class,
            MybatisBootMapperScanAutoConfiguration.class,
            DatabaseProvidersConfiguration.class
    );
    this.context.refresh();

    org.apache.ibatis.session.Configuration configuration = this.context.getBean(SqlSessionFactory.class)
            .getConfiguration();

    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(configuration.getDefaultFetchSize()).isEqualTo(1000);
    assertThat(configuration.getDatabaseId()).isEqualTo("h2");
  }

  @Test
  void testMixedWithConfigurationFileAndTypeHandlersPackage() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config-settings-only.xml",
            "mybatis.type-handlers-package:infra.orm.mybatis.auto.handler.").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class);
    this.context.refresh();

    org.apache.ibatis.session.Configuration configuration = this.context.getBean(SqlSessionFactory.class)
            .getConfiguration();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(configuration.getDefaultFetchSize()).isEqualTo(1000);
    assertThat(configuration.getTypeHandlerRegistry().getTypeHandler(BigInteger.class))
            .isInstanceOf(DummyTypeHandler.class);
    assertThat(configuration.getTypeHandlerRegistry().getTypeHandler(AtomicInteger.class))
            .isInstanceOf(AtomicNumberTypeHandler.class);
    assertThat(configuration.getTypeHandlerRegistry().getTypeHandler(AtomicLong.class))
            .isInstanceOf(AtomicNumberTypeHandler.class);
    assertThat(configuration.getTypeHandlerRegistry().getTypeHandler(AtomicInteger.class).toString())
            .isEqualTo("type=" + AtomicInteger.class);
  }

  @Test
  void testMixedWithConfigurationFileAndTypeAliasesPackageAndMapperLocations() {
    TestPropertyValues
            .of("mybatis.config-location:mybatis-config-settings-only.xml",
                    "mybatis.type-aliases-package: infra.orm.mybatis.auto.domain",
                    "mybatis.mapper-locations:classpath:infra/orm/mybatis/CityMapper.xml")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class);
    this.context.refresh();

    org.apache.ibatis.session.Configuration configuration = this.context.getBean(SqlSessionFactory.class)
            .getConfiguration();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(configuration.getDefaultFetchSize()).isEqualTo(1000);
    assertThat(configuration.getMappedStatementNames()).contains("selectCityById");
    assertThat(configuration.getMappedStatementNames())
            .contains("infra.orm.mybatis.auto.CityMapperImpl.selectCityById");
    assertThat(configuration.getTypeAliasRegistry().getTypeAliases()).containsKey("city");
    assertThat(configuration.getTypeAliasRegistry().getTypeAliases()).containsKey("name");
  }

  @Test
  void testMixedWithFullConfigurations() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config-settings-only.xml",
            "mybatis.type-handlers-package:infra.orm.mybatis.**.handler",
            "mybatis.type-aliases-package:infra.orm.mybatis.auto.domain",
            "mybatis.mapper-locations:classpath:infra/orm/mybatis/CityMapper.xml",
            "mybatis.executor-type=REUSE").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            MybatisInterceptorConfiguration.class, DatabaseProvidersConfiguration.class);
    this.context.refresh();

    org.apache.ibatis.session.Configuration configuration = this.context.getBean(SqlSessionFactory.class)
            .getConfiguration();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(configuration.getDefaultFetchSize()).isEqualTo(1000);
    assertThat(configuration.getTypeHandlerRegistry().getTypeHandler(BigInteger.class))
            .isInstanceOf(DummyTypeHandler.class);
    assertThat(configuration.getTypeHandlerRegistry().getTypeHandler(AtomicInteger.class))
            .isInstanceOf(AtomicNumberTypeHandler.class);
    assertThat(configuration.getTypeHandlerRegistry().getTypeHandler(AtomicLong.class))
            .isInstanceOf(AtomicNumberTypeHandler.class);
    assertThat(configuration.getMappedStatementNames()).hasSize(4);
    assertThat(configuration.getMappedStatementNames()).contains("selectCityById");
    assertThat(configuration.getMappedStatementNames())
            .contains("infra.orm.mybatis.auto.CityMapperImpl.selectCityById");
    assertThat(configuration.getMappedStatementNames()).contains("findById");
    assertThat(configuration.getMappedStatementNames())
            .contains("infra.orm.mybatis.auto.CityMapper.findById");
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.REUSE);
    assertThat(configuration.getInterceptors()).hasSize(2);
    assertThat(configuration.getInterceptors().get(0)).isInstanceOf(MyInterceptor2.class);
    assertThat(configuration.getInterceptors().get(1)).isInstanceOf(MyInterceptor.class);
    assertThat(configuration.getDatabaseId()).isEqualTo("h2");
  }

  @Test
  void testWithMyBatisConfiguration() {
    TestPropertyValues.of("mybatis.configuration.map-underscore-to-camel-case:true").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isTrue();
  }

  @Test
  void testWithMyBatisConfigurationCustomizeByJavaConfig() {
    TestPropertyValues.of("mybatis.configuration.default-fetch-size:100").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            MybatisPropertiesConfigurationCustomizer.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getDefaultFetchSize()).isEqualTo(100);
    assertThat(sqlSessionFactory.getConfiguration().getTypeHandlerRegistry().getTypeHandler(BigInteger.class))
            .isInstanceOf(DummyTypeHandler.class);
  }

  @Test
  void testWithMyBatisConfigurationCustomizer() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            MyBatisConfigurationCustomizerConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getTypeHandlerRegistry().getTypeHandler(BigInteger.class))
            .isInstanceOf(DummyTypeHandler.class);
    assertThat(sqlSessionFactory.getConfiguration().getCache("test")).isNotNull();
  }

  @Test
  void testWithSqlSessionFactoryBeanCustomizer() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            SqlSessionFactoryBeanCustomizerConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getTypeHandlerRegistry().getTypeHandler(BigInteger.class))
            .isInstanceOf(DummyTypeHandler.class);
    assertThat(sqlSessionFactory.getConfiguration().getCache("test")).isNotNull();
  }

  @Test
  void testConfigFileAndConfigurationWithTogether() {
    TestPropertyValues.of("mybatis.config-location:mybatis-config.xml",
                    "mybatis.configuration.default-statement-timeout:30")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class);

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(context::refresh)
            .havingRootCause()
            .isInstanceOf(IllegalStateException.class)
            .withMessageContaining("Property 'configuration' and 'configLocation' can not specified with together");
  }

  @Test
  void testWithoutConfigurationVariablesAndProperties() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();

    Properties variables = this.context.getBean(SqlSessionFactory.class).getConfiguration().getVariables();
    assertThat(variables).isEmpty();
  }

  @Test
  void testWithConfigurationVariablesOnly() {
    TestPropertyValues.of("mybatis.configuration.variables.key1:value1").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();

    Properties variables = this.context.getBean(SqlSessionFactory.class).getConfiguration().getVariables();
    assertThat(variables).hasSize(1);
    assertThat(variables.getProperty("key1")).isEqualTo("value1");
  }

  @Test
  void testWithConfigurationPropertiesOnly() {
    TestPropertyValues.of("mybatis.configuration-properties.key2:value2").applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();

    Properties variables = this.context.getBean(SqlSessionFactory.class).getConfiguration().getVariables();
    assertThat(variables).hasSize(1);
    assertThat(variables.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  void testWithConfigurationVariablesAndPropertiesOtherKey() {
    TestPropertyValues.of("mybatis.configuration.variables.key1:value1", "mybatis.configuration-properties.key2:value2")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();

    Properties variables = this.context.getBean(SqlSessionFactory.class).getConfiguration().getVariables();
    assertThat(variables).hasSize(2);
    assertThat(variables.getProperty("key1")).isEqualTo("value1");
    assertThat(variables.getProperty("key2")).isEqualTo("value2");
  }

  @Test
  void testWithConfigurationVariablesAndPropertiesSameKey() {
    TestPropertyValues.of("mybatis.configuration.variables.key:value1", "mybatis.configuration-properties.key:value2")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();

    Properties variables = this.context.getBean(SqlSessionFactory.class).getConfiguration().getVariables();
    assertThat(variables).hasSize(1);
    assertThat(variables.getProperty("key")).isEqualTo("value2");
  }

  @Test
  void testCustomSqlSessionFactory() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            CustomSqlSessionFactoryConfiguration.class, MybatisAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().getVariables().getProperty("key"))
            .isEqualTo("value");
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(((RuntimeBeanReference) this.context.getBeanDefinition("cityMapper").getPropertyValues()
            .getPropertyValue("sqlSessionFactory")).getBeanName()).isEqualTo("customSqlSessionFactory");
  }

  @Test
  void testMySqlSessionFactory() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            MySqlSessionFactoryConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionFactory.class)).isInstanceOf(MySqlSessionFactory.class);
  }

  @Test
  void testCustomSqlSessionTemplate() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            CustomSqlSessionTemplateConfiguration.class, MybatisAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.BATCH);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(((RuntimeBeanReference) this.context.getBeanDefinition("cityMapper").getPropertyValues()
            .getPropertyValue("sqlSessionTemplate")).getBeanName()).isEqualTo("customSqlSessionTemplate");
  }

  @Test
  void testMySqlSessionTemplate() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisAutoConfiguration.class,
            MySqlSessionTemplateConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class)).isInstanceOf(MySqlSessionTemplate.class);
  }

  @Test
  void testCustomSqlSessionTemplateAndSqlSessionFactory() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class,
            CustomSqlSessionFactoryConfiguration.class, CustomSqlSessionTemplateConfiguration.class,
            MybatisAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.BATCH);
    assertThat(this.context.getBeanNamesForType(CityMapper.class)).hasSize(1);
    assertThat(((RuntimeBeanReference) this.context.getBeanDefinition("cityMapper").getPropertyValues()
            .getPropertyValue("sqlSessionTemplate")).getBeanName()).isEqualTo("customSqlSessionTemplate");
  }

  @Test
  void testTypeAliasesSuperTypeIsSpecify() {
    TestPropertyValues
            .of("mybatis.type-aliases-package:infra.orm.mybatis.auto.domain",
                    "mybatis.type-aliases-super-type:infra.orm.mybatis.auto.domain.Domain")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisBootMapperScanAutoConfiguration.class);
    this.context.refresh();

    org.apache.ibatis.session.Configuration configuration = this.context.getBean(SqlSessionFactory.class)
            .getConfiguration();
    assertThat(configuration.getTypeAliasRegistry().getTypeAliases()).containsKey("city");
    assertThat(configuration.getTypeAliasRegistry().getTypeAliases()).doesNotContainKey("name");
  }

  @Test
  void testMapperFactoryBean() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MapperFactoryBeanConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(DateTimeMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();
  }

  @Test
  void testMapperScannerConfigurer() {
    this.context.register(EmbeddedDataSourceConfiguration.class, MapperScannerConfigurerConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(DateTimeMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();
  }

//  @Test
//  void testDefaultScriptingLanguageIsSpecify() {
//    TestPropertyValues
//            .of("mybatis.default-scripting-language-driver:org.mybatis.scripting.thymeleaf.ThymeleafLanguageDriver")
//            .applyTo(this.context);
//    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisScanMapperConfiguration.class,
//            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
//    this.context.refresh();
//    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
//    LanguageDriverRegistry languageDriverRegistry = sqlSessionFactory.getConfiguration().getLanguageRegistry();
//    assertThat(languageDriverRegistry.getDefaultDriverClass()).isEqualTo(ThymeleafLanguageDriver.class);
//    assertThat(languageDriverRegistry.getDefaultDriver()).isInstanceOf(ThymeleafLanguageDriver.class);
//    assertThat(languageDriverRegistry.getDriver(ThymeleafLanguageDriver.class)).isNotNull();
//  }

  @Test
  void testExcludeMybatisLanguageDriverAutoConfiguration() {
    TestPropertyValues
            .of("context.autoconfigure.exclude:infra.orm.mybatis.auto.MybatisLanguageDriverAutoConfiguration")
            .applyTo(this.context);
    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisScanMapperConfiguration.class,
            MybatisAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
    this.context.refresh();
    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
    assertThat(sqlSessionFactory.getConfiguration().getMapperRegistry().getMappers()).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionFactory.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(SqlSessionTemplate.class)).hasSize(1);
    assertThat(this.context.getBeanNamesForType(DateTimeMapper.class)).hasSize(1);
    assertThat(this.context.getBean(SqlSessionTemplate.class).getExecutorType()).isEqualTo(ExecutorType.SIMPLE);
    assertThat(this.context.getBean(SqlSessionFactory.class).getConfiguration().isMapUnderscoreToCamelCase()).isFalse();
    assertThat(this.context.getBeanNamesForType(LanguageDriver.class)).hasSize(0);
  }

//  @Test
//  void testMybatisLanguageDriverAutoConfigurationWithSingleCandidate() {
//    TestPropertyValues
//            .of("context.autoconfigure.exclude:infra.orm.mybatis.auto.MybatisLanguageDriverAutoConfiguration")
//            .applyTo(this.context);
//    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisScanMapperConfiguration.class,
//            SingleLanguageDriverConfiguration.class, MybatisAutoConfiguration.class,
//            PropertyPlaceholderAutoConfiguration.class);
//    this.context.refresh();
//    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
//    LanguageDriverRegistry languageDriverRegistry = sqlSessionFactory.getConfiguration().getLanguageRegistry();
//    assertThat(this.context.getBeanNamesForType(LanguageDriver.class)).hasSize(1);
//    assertThat(languageDriverRegistry.getDefaultDriverClass()).isEqualTo(ThymeleafLanguageDriver.class);
//    assertThat(languageDriverRegistry.getDefaultDriver()).isInstanceOf(ThymeleafLanguageDriver.class);
//    assertThat(languageDriverRegistry.getDriver(ThymeleafLanguageDriver.class)).isNotNull();
//  }

//  @Test
//  void testMybatisLanguageDriverAutoConfigurationWithSingleCandidateWhenDefaultLanguageDriverIsSpecify() {
//    TestPropertyValues
//            .of("mybatis.default-scripting-language-driver:org.apache.ibatis.scripting.xmltags.XMLLanguageDriver",
//                    "context.autoconfigure.exclude:org.mybatis.autoconfigure.MybatisLanguageDriverAutoConfiguration")
//            .applyTo(this.context);
//    this.context.register(EmbeddedDataSourceConfiguration.class, MybatisScanMapperConfiguration.class,
//            SingleLanguageDriverConfiguration.class, MybatisAutoConfiguration.class,
//            PropertyPlaceholderAutoConfiguration.class);
//    this.context.refresh();
//    SqlSessionFactory sqlSessionFactory = this.context.getBean(SqlSessionFactory.class);
//    LanguageDriverRegistry languageDriverRegistry = sqlSessionFactory.getConfiguration().getLanguageRegistry();
//    assertThat(this.context.getBeanNamesForType(LanguageDriver.class)).hasSize(1);
//    assertThat(languageDriverRegistry.getDefaultDriverClass()).isEqualTo(XMLLanguageDriver.class);
//    assertThat(languageDriverRegistry.getDefaultDriver()).isInstanceOf(XMLLanguageDriver.class);
//    assertThat(languageDriverRegistry.getDriver(ThymeleafLanguageDriver.class)).isNotNull();
//  }

  @Configuration
  static class MultipleDataSourceConfiguration {
    @Bean
    DataSource dataSourcePrimary() {
      return mock(DataSource.class);
    }

    @Bean
    DataSource dataSourceReplica() {
      return mock(DataSource.class);
    }
  }

  @Configuration
  static class SingleCandidateDataSourceConfiguration {
    @Bean
    @Primary
    DataSource dataSourcePrimary() {
      return mock(DataSource.class);
    }

    @Bean
    DataSource dataSourceReplica() {
      return mock(DataSource.class);
    }
  }

  @Configuration
  @EnableAutoConfiguration
  @MapperScan(basePackages = "infra.orm.mybatis.auto.mapper",
              lazyInitialization = "${mybatis.lazy-initialization:false}")
  static class MybatisScanMapperConfiguration {
  }

  @Configuration
  @EnableAutoConfiguration
  static class MapperFactoryBeanConfiguration {
    @Bean
    MapperFactoryBean<DateTimeMapper> dateTimeMapper(SqlSessionFactory sqlSessionFactory) {
      MapperFactoryBean<DateTimeMapper> factoryBean = new MapperFactoryBean<>(DateTimeMapper.class);
      factoryBean.setSqlSessionFactory(sqlSessionFactory);
      return factoryBean;
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class MapperScannerConfigurerConfiguration {
    @Bean
    static MapperScannerConfigurer mapperScannerConfigurer() {
      MapperScannerConfigurer configurer = new MapperScannerConfigurer();
      configurer.setBasePackage("infra.orm.mybatis.auto.mapper");
      return configurer;
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class MybatisBootMapperScanAutoConfiguration implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
      beanFactory.registerScope("thread", new SimpleThreadScope());
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class MybatisMapperConfiguration {

    @Bean
    public CityMapperImpl cityMapper() {
      return new CityMapperImpl();
    }

  }

  @Configuration
  @EnableAutoConfiguration
  static class MybatisInterceptorConfiguration {

    @Bean
    @Order(2)
    public MyInterceptor myInterceptor() {
      return new MyInterceptor();
    }

    @Bean
    @Order(1)
    public MyInterceptor2 myInterceptor2() {
      return new MyInterceptor2();
    }

  }

  @Configuration
  @EnableAutoConfiguration
  static class MybatisInterceptorConfiguration2 {

    @Bean
    @Order(1)
    public MyInterceptor myInterceptor() {
      return new MyInterceptor();
    }

    @Bean
    @Order(2)
    public MyInterceptor2 myInterceptor2() {
      return new MyInterceptor2();
    }

  }

  @Configuration
  @EnableAutoConfiguration
  static class MybatisTypeHandlerConfiguration {

    @Bean
    public MyTypeHandler myTypeHandler() {
      return new MyTypeHandler();
    }

  }

  @Configuration
  @EnableAutoConfiguration
  static class MybatisPropertiesConfigurationCustomizer {
    @Autowired
    void customize(MybatisProperties properties) {
      properties.getConfiguration().getTypeHandlerRegistry().register(new DummyTypeHandler());
    }
  }

  @Configuration
  @EnableAutoConfiguration
  static class MyBatisConfigurationCustomizerConfiguration {
    @Bean
    ConfigurationCustomizer typeHandlerConfigurationCustomizer() {
      return configuration -> configuration.getTypeHandlerRegistry().register(new DummyTypeHandler());
    }

    @Bean
    ConfigurationCustomizer cacheConfigurationCustomizer() {
      return configuration -> configuration.addCache(new PerpetualCache("test"));
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  static class SqlSessionFactoryBeanCustomizerConfiguration {

    @Bean
    SqlSessionFactoryBeanCustomizer typeHandlerSqlSessionFactoryBeanCustomizer() {
      return factoryBean -> factoryBean.setTypeHandlers(new DummyTypeHandler());
    }

    @Bean
    SqlSessionFactoryBeanCustomizer cacheSqlSessionFactoryBeanCustomizer() {
      return factoryBean -> factoryBean.setCache(new PerpetualCache("test"));
    }
  }

//  @Configuration
//  static class SingleLanguageDriverConfiguration {
//    @Bean
//    ThymeleafLanguageDriver myThymeleafLanguageDriver() {
//      return new ThymeleafLanguageDriver();
//    }
//  }

  @Intercepts(@Signature(type = Map.class, method = "get", args = { Object.class }))
  static class MyInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) {
      return "Test";
    }

    @Override
    public Object plugin(Object target) {
      return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
  }

  @Intercepts(@Signature(type = Map.class, method = "get", args = { Object.class }))
  static class MyInterceptor2 implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) {
      return "Test2";
    }

    @Override
    public Object plugin(Object target) {
      return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
  }

  @Configuration
  static class DatabaseProvidersConfiguration {

    @Bean
    public PropertiesFactoryBean vendorProperties() {
      Properties properties = new Properties();
      properties.put("SQL Server", "sqlserver");
      properties.put("DB2", "db2");
      properties.put("H2", "h2");

      PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
      propertiesFactoryBean.setProperties(properties);
      return propertiesFactoryBean;
    }

    @Bean
    public VendorDatabaseIdProvider vendorDatabaseIdProvider(Properties vendorProperties) {
      VendorDatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
      databaseIdProvider.setProperties(vendorProperties);
      return databaseIdProvider;
    }

  }

  @Configuration
  static class CustomSqlSessionFactoryConfiguration {
    @Bean
    public SqlSessionFactory customSqlSessionFactory(DataSource dataSource) throws Exception {
      SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
      sqlSessionFactoryBean.setDataSource(dataSource);
      Properties props = new Properties();
      props.setProperty("key", "value");
      sqlSessionFactoryBean.setConfigurationProperties(props);
      return sqlSessionFactoryBean.getObject();
    }
  }

  @Configuration
  static class MySqlSessionFactoryConfiguration {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
      MySqlSessionFactory sqlSessionFactory = new MySqlSessionFactory(new org.apache.ibatis.session.Configuration());
      sqlSessionFactory.getConfiguration()
              .setEnvironment(new Environment("", new ManagedTransactionFactory(), dataSource));
      return sqlSessionFactory;
    }
  }

  @Configuration
  static class CustomSqlSessionTemplateConfiguration {
    @Bean
    public SqlSessionTemplate customSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
      return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
    }
  }

  @Configuration
  static class MySqlSessionTemplateConfiguration {
    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
      return new MySqlSessionTemplate(sqlSessionFactory);
    }
  }

  static class MySqlSessionFactory extends DefaultSqlSessionFactory {
    MySqlSessionFactory(org.apache.ibatis.session.Configuration configuration) {
      super(configuration);
    }
  }

  static class MySqlSessionTemplate extends SqlSessionTemplate {
    MySqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
      super(sqlSessionFactory);
    }
  }

  static class MyTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) {

    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) {
      return null;
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) {
      return null;
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) {
      return null;
    }

  }

  @Configuration
  @TestAutoConfigurationPackage(CityMapper.class)
  static class CityMapperRepositoryConfiguration {

  }
}
