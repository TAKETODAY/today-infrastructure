/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.orm.jpa.persistenceunit;

import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.sql.DataSource;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aot.ApplicationContextAotGenerator;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.orm.jpa.JpaVendorAdapter;
import cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean;
import cn.taketoday.orm.jpa.domain.DriversLicense;
import cn.taketoday.orm.jpa.domain.Employee;
import cn.taketoday.orm.jpa.domain.EmployeeCategoryConverter;
import cn.taketoday.orm.jpa.domain.EmployeeId;
import cn.taketoday.orm.jpa.domain.EmployeeKindConverter;
import cn.taketoday.orm.jpa.domain.EmployeeLocation;
import cn.taketoday.orm.jpa.domain.EmployeeLocationConverter;
import cn.taketoday.orm.jpa.domain.Person;
import cn.taketoday.orm.jpa.domain.PersonListener;
import cn.taketoday.orm.jpa.vendor.Database;
import cn.taketoday.orm.jpa.vendor.HibernateJpaVendorAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/28 21:58
 */
class PersistenceManagedTypesBeanRegistrationAotProcessorTests {

  @Test
  void processEntityManagerWithPackagesToScan() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(EntityManagerWithPackagesToScanConfiguration.class);
    compile(context, (initializer, compiled) -> {
      GenericApplicationContext freshApplicationContext = toFreshApplicationContext(
              initializer);
      PersistenceManagedTypes persistenceManagedTypes = freshApplicationContext.getBean(
              "persistenceManagedTypes", PersistenceManagedTypes.class);
      assertThat(persistenceManagedTypes.getManagedClassNames()).containsExactlyInAnyOrder(
              DriversLicense.class.getName(), Person.class.getName(), Employee.class.getName(),
              EmployeeLocationConverter.class.getName());
      assertThat(persistenceManagedTypes.getManagedPackages()).isEmpty();
      assertThat(freshApplicationContext.getBean(
              EntityManagerWithPackagesToScanConfiguration.class).scanningInvoked).isFalse();
    });
  }

  @Test
  void contributeHints() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(EntityManagerWithPackagesToScanConfiguration.class);
    contributeHints(context, hints -> {
      assertThat(RuntimeHintsPredicates.reflection().onType(DriversLicense.class)
              .withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(Person.class)
              .withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(PersonListener.class)
              .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS))
              .accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(Employee.class)
              .withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onMethod(Employee.class, "preRemove"))
              .accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeId.class)
              .withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeLocationConverter.class)
              .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeCategoryConverter.class)
              .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeKindConverter.class)
              .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
      assertThat(RuntimeHintsPredicates.reflection().onType(EmployeeLocation.class)
              .withMemberCategories(MemberCategory.DECLARED_FIELDS)).accepts(hints);
    });
  }

  private void compile(GenericApplicationContext applicationContext,
          BiConsumer<ApplicationContextInitializer, Compiled> result) {
    ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
    TestGenerationContext generationContext = new TestGenerationContext();
    generator.processAheadOfTime(applicationContext, generationContext);
    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(generationContext).compile(compiled ->
            result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
  }

  private GenericApplicationContext toFreshApplicationContext(
          ApplicationContextInitializer initializer) {
    GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
    initializer.initialize(freshApplicationContext);
    freshApplicationContext.refresh();
    return freshApplicationContext;
  }

  private void contributeHints(GenericApplicationContext applicationContext, Consumer<RuntimeHints> result) {
    ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
    TestGenerationContext generationContext = new TestGenerationContext();
    generator.processAheadOfTime(applicationContext, generationContext);
    result.accept(generationContext.getRuntimeHints());
  }

  @Configuration(proxyBeanMethods = false)
  public static class EntityManagerWithPackagesToScanConfiguration {

    private boolean scanningInvoked;

    @Bean
    public DataSource mockDataSource() {
      return mock();
    }

    @Bean
    public HibernateJpaVendorAdapter jpaVendorAdapter() {
      HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
      jpaVendorAdapter.setDatabase(Database.HSQL);
      return jpaVendorAdapter;
    }

    @Bean
    public PersistenceManagedTypes persistenceManagedTypes(ResourceLoader resourceLoader) {
      this.scanningInvoked = true;
      return new PersistenceManagedTypesScanner(resourceLoader)
              .scan("cn.taketoday.orm.jpa.domain");
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
            JpaVendorAdapter jpaVendorAdapter, PersistenceManagedTypes persistenceManagedTypes) {
      LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
      entityManagerFactoryBean.setDataSource(dataSource);
      entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
      entityManagerFactoryBean.setManagedTypes(persistenceManagedTypes);
      return entityManagerFactoryBean;
    }

  }

}