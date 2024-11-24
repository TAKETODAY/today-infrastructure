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

package infra.jdbc.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.function.Predicate;

import javax.sql.DataSource;

import infra.beans.PropertyValue;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.core.io.ClassPathResource;
import infra.jdbc.BadSqlGrammarException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.AbstractDriverBasedDataSource;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import infra.jdbc.datasource.init.DataSourceInitializer;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
@Execution(ExecutionMode.SAME_THREAD)
class JdbcNamespaceIntegrationTests {

  @Test
  void createEmbeddedDatabase() throws Exception {
    assertCorrectSetup("jdbc-config.xml", "dataSource", "h2DataSource", "derbyDataSource");
  }

  @Test
  void createEmbeddedDatabaseAgain() throws Exception {
    // If Derby isn't cleaned up properly this will fail...
    assertCorrectSetup("jdbc-config.xml", "derbyDataSource");
  }

  @Test
  void createWithResourcePattern() throws Exception {
    assertCorrectSetup("jdbc-config-pattern.xml", "dataSource");
  }

  @Test
  void createWithAnonymousDataSourceAndDefaultDatabaseName() throws Exception {
    assertCorrectSetupForSingleDataSource("jdbc-config-db-name-default-and-anonymous-datasource.xml",
            url -> url.endsWith(EmbeddedDatabaseFactory.DEFAULT_DATABASE_NAME));
  }

  @Test
  void createWithImplicitDatabaseName() throws Exception {
    assertCorrectSetupForSingleDataSource("jdbc-config-db-name-implicit.xml", url -> url.endsWith("dataSource"));
  }

  @Test
  void createWithExplicitDatabaseName() throws Exception {
    assertCorrectSetupForSingleDataSource("jdbc-config-db-name-explicit.xml", url -> url.endsWith("customDbName"));
  }

  @Test
  void createWithGeneratedDatabaseName() throws Exception {
    Predicate<String> urlPredicate = url -> url.startsWith("jdbc:hsqldb:mem:");
    urlPredicate.and(url -> !url.endsWith("dataSource"));
    urlPredicate.and(url -> !url.endsWith("shouldBeOverriddenByGeneratedName"));
    assertCorrectSetupForSingleDataSource("jdbc-config-db-name-generated.xml", urlPredicate);
  }

  @Test
  void createWithEndings() throws Exception {
    assertCorrectSetupAndCloseContext("jdbc-initialize-endings-config.xml", 2, "dataSource");
  }

  @Test
  void createWithEndingsNested() throws Exception {
    assertCorrectSetupAndCloseContext("jdbc-initialize-endings-nested-config.xml", 2, "dataSource");
  }

  @Test
  void createAndDestroy() throws Exception {
    ClassPathXmlApplicationContext context = context("jdbc-destroy-config.xml");
    DataSource dataSource = context.getBean(DataSource.class);
    JdbcTemplate template = new JdbcTemplate(dataSource);
    assertNumRowsInTestTable(template, 1);
    context.getBean(DataSourceInitializer.class).destroy();
    // Table has been dropped
    assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
            assertNumRowsInTestTable(template, 1));
  }

  @Test
  void createAndDestroyNestedWithHsql() throws Exception {
    ClassPathXmlApplicationContext context = context("jdbc-destroy-nested-config.xml");
    DataSource dataSource = context.getBean(DataSource.class);
    JdbcTemplate template = new JdbcTemplate(dataSource);
    assertNumRowsInTestTable(template, 1);
    context.getBean(EmbeddedDatabaseFactoryBean.class).destroy();
    // Table has been dropped
    assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
            assertNumRowsInTestTable(template, 1));
  }

  @Test
  void createAndDestroyNestedWithH2() throws Exception {
    ClassPathXmlApplicationContext context = context("jdbc-destroy-nested-config-h2.xml");
    DataSource dataSource = context.getBean(DataSource.class);
    JdbcTemplate template = new JdbcTemplate(dataSource);
    assertNumRowsInTestTable(template, 1);
    context.getBean(EmbeddedDatabaseFactoryBean.class).destroy();
    // Table has been dropped
    assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
            assertNumRowsInTestTable(template, 1));
  }

  @Test
  void multipleDataSourcesHaveDifferentDatabaseNames() throws Exception {
    StandardBeanFactory factory = new StandardBeanFactory();
    new XmlBeanDefinitionReader(factory).loadBeanDefinitions(new ClassPathResource(
            "jdbc-config-multiple-datasources.xml", getClass()));
    assertBeanPropertyValueOf("databaseName", "firstDataSource", factory);
    assertBeanPropertyValueOf("databaseName", "secondDataSource", factory);
  }

  @Test
  void initializeWithCustomSeparator() throws Exception {
    assertCorrectSetupAndCloseContext("jdbc-initialize-custom-separator.xml", 2, "dataSource");
  }

  @Test
  void embeddedWithCustomSeparator() throws Exception {
    assertCorrectSetupAndCloseContext("jdbc-config-custom-separator.xml", 2, "dataSource");
  }

  private ClassPathXmlApplicationContext context(String file) {
    return new ClassPathXmlApplicationContext(file, getClass());
  }

  private void assertBeanPropertyValueOf(String propertyName, String expected, StandardBeanFactory factory) {
    BeanDefinition bean = factory.getBeanDefinition(expected);
    PropertyValue value = bean.getPropertyValues().get(propertyName);
    assertThat(value).isNotNull();
    assertThat(value.getValue().toString()).isEqualTo(expected);
  }

  private void assertNumRowsInTestTable(JdbcTemplate template, int count) {
    assertThat(template.queryForObject("select count(*) from T_TEST", Integer.class).intValue()).isEqualTo(count);
  }

  private void assertCorrectSetup(String file, String... dataSources) {
    assertCorrectSetupAndCloseContext(file, 1, dataSources);
  }

  private void assertCorrectSetupAndCloseContext(String file, int count, String... dataSources) {
    ConfigurableApplicationContext context = context(file);
    for (String dataSourceName : dataSources) {
      DataSource dataSource = context.getBean(dataSourceName, DataSource.class);
      assertNumRowsInTestTable(new JdbcTemplate(dataSource), count);
      assertThat(dataSource instanceof AbstractDriverBasedDataSource).isTrue();
      AbstractDriverBasedDataSource adbDataSource = (AbstractDriverBasedDataSource) dataSource;
      assertThat(adbDataSource.getUrl()).contains(dataSourceName);
    }
    context.close();
  }

  private void assertCorrectSetupForSingleDataSource(String file, Predicate<String> urlPredicate) {
    var context = context(file);
    DataSource dataSource = context.getBean(DataSource.class);
    assertNumRowsInTestTable(new JdbcTemplate(dataSource), 1);
    assertThat(dataSource instanceof AbstractDriverBasedDataSource).isTrue();
    AbstractDriverBasedDataSource adbDataSource = (AbstractDriverBasedDataSource) dataSource;
    assertThat(urlPredicate.test(adbDataSource.getUrl())).isTrue();
    context.close();
  }

}
