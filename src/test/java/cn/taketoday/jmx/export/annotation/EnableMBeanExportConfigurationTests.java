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

package cn.taketoday.jmx.export.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableMBeanExport;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.MBeanExportConfiguration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.context.annotation.ScopedProxyMode;
import cn.taketoday.context.support.MockEnvironment;
import cn.taketoday.jmx.export.MBeanExporterTests;
import cn.taketoday.jmx.export.TestDynamicMBean;
import cn.taketoday.jmx.export.metadata.InvalidMetadataException;
import cn.taketoday.jmx.support.MBeanServerFactoryBean;
import cn.taketoday.jmx.support.ObjectNameManager;
import cn.taketoday.jmx.support.RegistrationPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link EnableMBeanExport} and {@link MBeanExportConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see AnnotationLazyInitMBeanTests
 */
public class EnableMBeanExportConfigurationTests {

  private AnnotationConfigApplicationContext ctx;

  @AfterEach
  public void closeContext() {
    if (this.ctx != null) {
      this.ctx.close();
    }
  }

  @Test
  public void testLazyNaming() throws Exception {
    load(LazyNamingConfiguration.class);
    validateAnnotationTestBean();
  }

  private void load(Class<?>... config) {
    this.ctx = new AnnotationConfigApplicationContext(config);
  }

  @Test
  public void testOnlyTargetClassIsExposed() throws Exception {
    load(ProxyConfiguration.class);
    validateAnnotationTestBean();
  }

  @Test
  @SuppressWarnings("resource")
  public void testPackagePrivateExtensionCantBeExposed() {
    assertThatExceptionOfType(InvalidMetadataException.class).isThrownBy(() ->
                    new AnnotationConfigApplicationContext(PackagePrivateConfiguration.class))
            .withMessageContaining(PackagePrivateTestBean.class.getName())
            .withMessageContaining("must be public");
  }

  @Test
  @SuppressWarnings("resource")
  public void testPackagePrivateImplementationCantBeExposed() {
    assertThatExceptionOfType(InvalidMetadataException.class).isThrownBy(() ->
                    new AnnotationConfigApplicationContext(PackagePrivateInterfaceImplementationConfiguration.class))
            .withMessageContaining(PackagePrivateAnnotationTestBean.class.getName())
            .withMessageContaining("must be public");
  }

  @Test
  public void testPackagePrivateClassExtensionCanBeExposed() throws Exception {
    load(PackagePrivateExtensionConfiguration.class);
    validateAnnotationTestBean();
  }

  @Test
  public void testPlaceholderBased() throws Exception {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("serverName", "server");
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.setEnvironment(env);
    context.register(PlaceholderBasedConfiguration.class);
    context.refresh();
    this.ctx = context;
    validateAnnotationTestBean();
  }

  @Test
  public void testLazyAssembling() throws Exception {
    System.setProperty("domain", "bean");
    load(LazyAssemblingConfiguration.class);
    try {
      MBeanServer server = (MBeanServer) this.ctx.getBean("server");

      validateMBeanAttribute(server, "bean:name=testBean4", "TEST");
      validateMBeanAttribute(server, "bean:name=testBean5", "FACTORY");
      validateMBeanAttribute(server, "spring:mbean=true", "Rob Harrop");
      validateMBeanAttribute(server, "spring:mbean=another", "Juergen Hoeller");
    }
    finally {
      System.clearProperty("domain");
    }
  }

  @Test
  public void testComponentScan() throws Exception {
    load(ComponentScanConfiguration.class);
    MBeanServer server = (MBeanServer) this.ctx.getBean("server");
    validateMBeanAttribute(server, "bean:name=testBean4", null);
  }

  private void validateAnnotationTestBean() throws Exception {
    MBeanServer server = (MBeanServer) this.ctx.getBean("server");
    validateMBeanAttribute(server, "bean:name=testBean4", "TEST");
  }

  private void validateMBeanAttribute(MBeanServer server, String objectName, String expected) throws Exception {
    ObjectName oname = ObjectNameManager.getInstance(objectName);
    assertThat(server.getObjectInstance(oname)).isNotNull();
    String name = (String) server.getAttribute(oname, "Name");
    assertThat(name).as("Invalid name returned").isEqualTo(expected);
  }

  @Configuration
  @EnableMBeanExport(server = "server")
  static class LazyNamingConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }

    @Bean
    @Lazy
    public AnnotationTestBean testBean() {
      AnnotationTestBean bean = new AnnotationTestBean();
      bean.setName("TEST");
      bean.setAge(100);
      return bean;
    }
  }

  @Configuration
  @EnableMBeanExport(server = "server")
  static class ProxyConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }

    @Bean
    @Lazy
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AnnotationTestBean testBean() {
      AnnotationTestBean bean = new AnnotationTestBean();
      bean.setName("TEST");
      bean.setAge(100);
      return bean;
    }
  }

  @Configuration
  @EnableMBeanExport(server = "${serverName}")
  static class PlaceholderBasedConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }

    @Bean
    @Lazy
    public AnnotationTestBean testBean() {
      AnnotationTestBean bean = new AnnotationTestBean();
      bean.setName("TEST");
      bean.setAge(100);
      return bean;
    }
  }

  @Configuration
  @EnableMBeanExport(server = "server", registration = RegistrationPolicy.REPLACE_EXISTING)
  static class LazyAssemblingConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }

    @Bean("bean:name=testBean4")
    @Lazy
    public AnnotationTestBean testBean4() {
      AnnotationTestBean bean = new AnnotationTestBean();
      bean.setName("TEST");
      bean.setAge(100);
      return bean;
    }

    @Bean("bean:name=testBean5")
    public AnnotationTestBeanFactory testBean5() {
      return new AnnotationTestBeanFactory();
    }

    @Bean(name = "spring:mbean=true")
    @Lazy
    public TestDynamicMBean dynamic() {
      return new TestDynamicMBean();
    }

    @Bean(name = "spring:mbean=another")
    @Lazy
    public MBeanExporterTests.Person person() {
      MBeanExporterTests.Person person = new MBeanExporterTests.Person();
      person.setName("Juergen Hoeller");
      return person;
    }

    @Bean
    @Lazy
    public Object notLoadable() throws Exception {
      return Class.forName("does.not.exist").getDeclaredConstructor().newInstance();
    }
  }

  @Configuration
  @ComponentScan(excludeFilters = @ComponentScan.Filter(Configuration.class))
  @EnableMBeanExport(server = "server")
  static class ComponentScanConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }
  }

  @Configuration
  @EnableMBeanExport(server = "server")
  static class PackagePrivateConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }

    @Bean
    public PackagePrivateTestBean testBean() {
      return new PackagePrivateTestBean();
    }
  }

  @ManagedResource(objectName = "bean:name=packagePrivate")
  private static class PackagePrivateTestBean {

    private String name;

    @ManagedAttribute
    public String getName() {
      return this.name;
    }

    @ManagedAttribute
    public void setName(String name) {
      this.name = name;
    }
  }

  @Configuration
  @EnableMBeanExport(server = "server")
  static class PackagePrivateExtensionConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }

    @Bean
    public PackagePrivateTestBeanExtension testBean() {
      PackagePrivateTestBeanExtension bean = new PackagePrivateTestBeanExtension();
      bean.setName("TEST");
      return bean;
    }
  }

  private static class PackagePrivateTestBeanExtension extends AnnotationTestBean {

  }

  @Configuration
  @EnableMBeanExport(server = "server")
  static class PackagePrivateInterfaceImplementationConfiguration {

    @Bean
    public MBeanServerFactoryBean server() {
      return new MBeanServerFactoryBean();
    }

    @Bean
    public PackagePrivateAnnotationTestBean testBean() {
      return new PackagePrivateAnnotationTestBean();
    }
  }

  private static class PackagePrivateAnnotationTestBean implements AnotherAnnotationTestBean {

    private String bar;

    @Override
    public void foo() {
    }

    @Override
    public String getBar() {
      return this.bar;
    }

    @Override
    public void setBar(String bar) {
      this.bar = bar;
    }

    @Override
    public int getCacheEntries() {
      return 0;
    }
  }

}
