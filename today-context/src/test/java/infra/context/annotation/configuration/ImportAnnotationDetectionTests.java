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

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests that @Import may be used both as a locally declared and meta-declared
 * annotation, that all declarations are processed, and that any local declaration
 * is processed last.
 *
 * @author Chris Beams
 * @since 4.0
 */
@SuppressWarnings("resource")
public class ImportAnnotationDetectionTests {

  @Test
  public void multipleMetaImportsAreProcessed() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(MultiMetaImportConfig.class);
    ctx.refresh();
    assertThat(ctx.containsBean("testBean1")).isTrue();
    assertThat(ctx.containsBean("testBean2")).isTrue();
  }

  @Test
  public void localAndMetaImportsAreProcessed() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(MultiMetaImportConfigWithLocalImport.class);
    ctx.refresh();
    assertThat(ctx.containsBean("testBean1")).isTrue();
    assertThat(ctx.containsBean("testBean2")).isTrue();
    assertThat(ctx.containsBean("testBean3")).isTrue();
  }

  @Test
  public void localImportIsProcessedLast() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.setAllowBeanDefinitionOverriding(true);
    ctx.register(MultiMetaImportConfigWithLocalImportWithBeanOverride.class);
    ctx.refresh();
    assertThat(ctx.containsBean("testBean1")).isTrue();
    assertThat(ctx.containsBean("testBean2")).isTrue();
    assertThat(ctx.getBean("testBean2", TestBean.class).getName()).isEqualTo("2a");
  }

  @Test
  public void importFromBean() throws Exception {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(ImportFromBean.class);
    ctx.refresh();
    assertThat(ctx.containsBean("importAnnotationDetectionTests.ImportFromBean")).isTrue();
    assertThat(ctx.containsBean("testBean1")).isTrue();
    assertThat(ctx.getBean("testBean1", TestBean.class).getName()).isEqualTo("1");
  }

  @Configuration
  @MetaImport1
  @MetaImport2
  static class MultiMetaImportConfig {
  }

  @Configuration
  @MetaImport1
  @MetaImport2
  @Import(Config3.class)
  static class MultiMetaImportConfigWithLocalImport {
  }

  @Configuration
  @MetaImport1
  @MetaImport2
  @Import(Config2a.class)
  static class MultiMetaImportConfigWithLocalImportWithBeanOverride {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import(Config1.class)
  @interface MetaImport1 {
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Import(Config2.class)
  @interface MetaImport2 {
  }

  @Configuration
  static class Config1 {
    @Bean
    TestBean testBean1() {
      return new TestBean("1");
    }
  }

  @Configuration
  static class Config2 {
    @Bean
    TestBean testBean2() {
      return new TestBean("2");
    }
  }

  @Configuration
  static class Config2a {
    @Bean
    TestBean testBean2() {
      return new TestBean("2a");
    }
  }

  @Configuration
  static class Config3 {
    @Bean
    TestBean testBean3() {
      return new TestBean("3");
    }
  }

  @MetaImport1
  static class ImportFromBean {

  }
}
