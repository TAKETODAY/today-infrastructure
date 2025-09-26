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

package infra.context.annotation.configuration;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportResource;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ImportResource} support.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ImportResourceTests {

  @Test
  public void importXml() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlConfig.class);
    assertThat(ctx.containsBean("javaDeclaredBean")).as("did not contain java-declared bean").isTrue();
    assertThat(ctx.containsBean("xmlDeclaredBean")).as("did not contain xml-declared bean").isTrue();
    TestBean tb = ctx.getBean("javaDeclaredBean", TestBean.class);
    assertThat(tb.getName()).isEqualTo("myName");
    ctx.close();
  }

  @Test
  public void importXmlIsInheritedFromSuperclassDeclarations() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(FirstLevelSubConfig.class);
    assertThat(ctx.containsBean("xmlDeclaredBean")).isTrue();
    ctx.close();
  }

  @Test
  public void importXmlIsMergedFromSuperclassDeclarations() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SecondLevelSubConfig.class);
    assertThat(ctx.containsBean("secondLevelXmlDeclaredBean")).as("failed to pick up second-level-declared XML bean").isTrue();
    assertThat(ctx.containsBean("xmlDeclaredBean")).as("failed to pick up parent-declared XML bean").isTrue();
    ctx.close();
  }

  @Test
  public void importXmlWithNamespaceConfig() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithAopNamespaceConfig.class);
    Object bean = ctx.getBean("proxiedXmlBean");
    assertThat(AopUtils.isAopProxy(bean)).isTrue();
    ctx.close();
  }

  @Test
  public void importXmlWithOtherConfigurationClass() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlWithConfigurationClass.class);
    assertThat(ctx.containsBean("javaDeclaredBean")).as("did not contain java-declared bean").isTrue();
    assertThat(ctx.containsBean("xmlDeclaredBean")).as("did not contain xml-declared bean").isTrue();
    TestBean tb = ctx.getBean("javaDeclaredBean", TestBean.class);
    assertThat(tb.getName()).isEqualTo("myName");
    ctx.close();
  }

  @Test
  public void importWithPlaceholder() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    PropertySource<?> propertySource = new MapPropertySource("test",
            Collections.singletonMap("test", "taketoday"));
    ctx.getEnvironment().getPropertySources().addFirst(propertySource);
    ctx.register(ImportXmlConfig.class);
    ctx.refresh();
    assertThat(ctx.containsBean("xmlDeclaredBean")).as("did not contain xml-declared bean").isTrue();
    ctx.close();
  }

  @Test
  public void importXmlWithAutowiredConfig() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ImportXmlAutowiredConfig.class);
    String name = ctx.getBean("xmlBeanName", String.class);
    assertThat(name).isEqualTo("xml.declared");
    ctx.close();
  }

  @Configuration
  @ImportResource("classpath:infra/context/annotation/configuration/ImportXmlConfig-context.xml")
  static class ImportXmlConfig {
    @Value("${name}")
    private String name;

    public @Bean
    TestBean javaDeclaredBean() {
      return new TestBean(this.name);
    }
  }

  @Configuration
  @ImportResource("classpath:infra/context/annotation/configuration/ImportXmlConfig-context.xml")
  static class BaseConfig {
  }

  @Configuration
  static class FirstLevelSubConfig extends BaseConfig {
  }

  @Configuration
  @ImportResource("classpath:infra/context/annotation/configuration/SecondLevelSubConfig-context.xml")
  static class SecondLevelSubConfig extends BaseConfig {
  }

  @Configuration
  @ImportResource("classpath:infra/context/annotation/configuration/ImportXmlWithAopNamespace-context.xml")
  static class ImportXmlWithAopNamespaceConfig {
  }

  @Aspect
  static class AnAspect {
    @Before("execution(* infra.beans.testfixture.beans.TestBean.*(..))")
    public void advice() { }
  }

  @Configuration
  @ImportResource("classpath:infra/context/annotation/configuration/ImportXmlWithConfigurationClass-context.xml")
  static class ImportXmlWithConfigurationClass {
  }

  @Configuration
  @ImportResource("classpath:infra/context/annotation/configuration/ImportXmlConfig-context.xml")
  static class ImportXmlAutowiredConfig {
    @Autowired
    TestBean xmlDeclaredBean;

    public @Bean
    String xmlBeanName() {
      return xmlDeclaredBean.getName();
    }
  }

}
