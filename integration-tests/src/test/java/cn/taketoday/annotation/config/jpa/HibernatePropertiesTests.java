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

package cn.taketoday.annotation.config.jpa;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ContextConsumer;
import cn.taketoday.orm.hibernate5.support.HibernateImplicitNamingStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link HibernateProperties}.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @author Chris Bono
 */
@ExtendWith(MockitoExtension.class)
class HibernatePropertiesTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(TestConfiguration.class);

  @Mock
  private Supplier<String> ddlAutoSupplier;

  @Test
  void noCustomNamingStrategy() {
    this.contextRunner.run(assertHibernateProperties((hibernateProperties) -> {
      assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
      assertThat(hibernateProperties).containsEntry(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
              CamelCaseToUnderscoresNamingStrategy.class.getName());
      assertThat(hibernateProperties).containsEntry(AvailableSettings.IMPLICIT_NAMING_STRATEGY,
              HibernateImplicitNamingStrategy.class.getName());
    }));
  }

  @Test
  void hibernate5CustomNamingStrategies() {
    this.contextRunner
            .withPropertyValues("jpa.hibernate.naming.implicit-strategy:com.example.Implicit",
                    "jpa.hibernate.naming.physical-strategy:com.example.Physical")
            .run(assertHibernateProperties((hibernateProperties) -> {
              assertThat(hibernateProperties).contains(
                      entry(AvailableSettings.IMPLICIT_NAMING_STRATEGY, "com.example.Implicit"),
                      entry(AvailableSettings.PHYSICAL_NAMING_STRATEGY, "com.example.Physical"));
              assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
            }));
  }

  @Test
  void hibernate5CustomNamingStrategiesViaJpaProperties() {
    this.contextRunner
            .withPropertyValues("jpa.properties.hibernate.implicit_naming_strategy:com.example.Implicit",
                    "jpa.properties.hibernate.physical_naming_strategy:com.example.Physical")
            .run(assertHibernateProperties((hibernateProperties) -> {
              // You can override them as we don't provide any default
              assertThat(hibernateProperties).contains(
                      entry(AvailableSettings.IMPLICIT_NAMING_STRATEGY, "com.example.Implicit"),
                      entry(AvailableSettings.PHYSICAL_NAMING_STRATEGY, "com.example.Physical"));
              assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
            }));
  }

  @Test
  void scannerUsesDisabledScannerByDefault() {
    this.contextRunner.run(assertHibernateProperties((hibernateProperties) -> assertThat(hibernateProperties)
            .containsEntry(AvailableSettings.SCANNER, "org.hibernate.boot.archive.scan.internal.DisabledScanner")));
  }

  @Test
  void scannerCanBeCustomized() {
    this.contextRunner.withPropertyValues(
                    "jpa.properties.hibernate.archive.scanner:org.hibernate.boot.archive.scan.internal.StandardScanner")
            .run(assertHibernateProperties((hibernateProperties) -> assertThat(hibernateProperties).containsEntry(
                    AvailableSettings.SCANNER, "org.hibernate.boot.archive.scan.internal.StandardScanner")));
  }

  @Test
  void defaultDdlAutoIsNotInvokedIfPropertyIsSet() {
    this.contextRunner.withPropertyValues("jpa.hibernate.ddl-auto=validate")
            .run(assertDefaultDdlAutoNotInvoked("validate"));
  }

  @Test
  void defaultDdlAutoIsNotInvokedIfHibernateSpecificPropertyIsSet() {
    this.contextRunner.withPropertyValues("jpa.properties.hibernate.hbm2ddl.auto=create")
            .run(assertDefaultDdlAutoNotInvoked("create"));
  }

  @Test
  void defaultDdlAutoIsNotInvokedAndDdlAutoIsNotSetIfJpaDbActionPropertyIsSet() {
    this.contextRunner
            .withPropertyValues(
                    "jpa.properties.jakarta.persistence.schema-generation.database.action=drop-and-create")
            .run(assertHibernateProperties((hibernateProperties) -> {
              assertThat(hibernateProperties).doesNotContainKey(AvailableSettings.HBM2DDL_AUTO);
              assertThat(hibernateProperties).containsEntry(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
                      "drop-and-create");
              then(this.ddlAutoSupplier).should(never()).get();
            }));
  }

  private ContextConsumer<AssertableApplicationContext> assertDefaultDdlAutoNotInvoked(String expectedDdlAuto) {
    return assertHibernateProperties((hibernateProperties) -> {
      assertThat(hibernateProperties).containsEntry(AvailableSettings.HBM2DDL_AUTO, expectedDdlAuto);
      then(this.ddlAutoSupplier).should(never()).get();
    });
  }

  private ContextConsumer<AssertableApplicationContext> assertHibernateProperties(
          Consumer<Map<String, Object>> consumer) {
    return (context) -> {
      assertThat(context).hasSingleBean(JpaProperties.class);
      assertThat(context).hasSingleBean(HibernateProperties.class);
      Map<String, Object> hibernateProperties = context.getBean(HibernateProperties.class)
              .determineHibernateProperties(context.getBean(JpaProperties.class).getProperties(),
                      new HibernateSettings().ddlAuto(this.ddlAutoSupplier));
      consumer.accept(hibernateProperties);
    };
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({ JpaProperties.class, HibernateProperties.class })
  static class TestConfiguration {

  }

}
