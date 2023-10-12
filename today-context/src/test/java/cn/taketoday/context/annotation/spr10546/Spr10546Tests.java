/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation.spr10546;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.spr10546.scanpackage.AEnclosingConfig;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Rob Winch
 */
public class Spr10546Tests {
  private ConfigurableApplicationContext context;

  @AfterEach
  public void closeContext() {
    if (context != null) {
      context.close();
    }
  }

  // These fail prior to fixing SPR-10546

  @Test
  public void enclosingConfigFirstParentDefinesBean() {
    assertLoadsMyBean(AEnclosingConfig.class, AEnclosingConfig.ChildConfig.class);
  }

  /**
   * Prior to fixing SPR-10546 this might have succeeded depending on the ordering the
   * classes were picked up. If they are picked up in the same order as
   * {@link #enclosingConfigFirstParentDefinesBean()} then it would fail. This test is
   * mostly for illustration purposes, but doesn't hurt to continue using it.
   *
   * <p>We purposely use the {@link AEnclosingConfig} to make it alphabetically prior to the
   * {@link AEnclosingConfig.ChildConfig} which encourages this to occur with the
   * classpath scanning implementation being used by the author of this test.
   */
  @Test
  public void enclosingConfigFirstParentDefinesBeanWithScanning() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    context = ctx;
    ctx.scan(AEnclosingConfig.class.getPackage().getName());
    ctx.refresh();
    assertThat(context.getBean("myBean", String.class)).isEqualTo("myBean");
  }

//  @Test
//  public void enclosingConfigFirstParentDefinesBeanWithImportResource() {
//    assertLoadsMyBean(AEnclosingWithImportResourceConfig.class, AEnclosingWithImportResourceConfig.ChildConfig.class);
//  }

  @Configuration
  static class AEnclosingWithImportResourceConfig {
    @Configuration
    public static class ChildConfig extends ParentWithImportResourceConfig { }
  }

  @Test
  public void enclosingConfigFirstParentDefinesBeanWithComponentScan() {
    assertLoadsMyBean(AEnclosingWithComponentScanConfig.class, AEnclosingWithComponentScanConfig.ChildConfig.class);
  }

  @Configuration
  static class AEnclosingWithComponentScanConfig {
    @Configuration
    public static class ChildConfig extends ParentWithComponentScanConfig { }
  }

  @Test
  public void enclosingConfigFirstParentWithParentDefinesBean() {
    assertLoadsMyBean(AEnclosingWithGrandparentConfig.class, AEnclosingWithGrandparentConfig.ChildConfig.class);
  }

  @Configuration
  static class AEnclosingWithGrandparentConfig {
    @Configuration
    public static class ChildConfig extends ParentWithParentConfig { }
  }

  @Test
  public void importChildConfigThenChildConfig() {
    assertLoadsMyBean(ImportChildConfig.class, ChildConfig.class);
  }

  @Configuration
  static class ChildConfig extends ParentConfig { }

  @Configuration
  @Import(ChildConfig.class)
  static class ImportChildConfig { }

  // These worked prior, but validating they continue to work

  @Test
  public void enclosingConfigFirstParentDefinesBeanWithImport() {
    assertLoadsMyBean(AEnclosingWithImportConfig.class, AEnclosingWithImportConfig.ChildConfig.class);
  }

  @Configuration
  static class AEnclosingWithImportConfig {
    @Configuration
    public static class ChildConfig extends ParentWithImportConfig { }
  }

  @Test
  public void childConfigFirst() {
    assertLoadsMyBean(AEnclosingConfig.ChildConfig.class, AEnclosingConfig.class);
  }

  @Test
  public void enclosingConfigOnly() {
    assertLoadsMyBean(AEnclosingConfig.class);
  }

  @Test
  public void childConfigOnly() {
    assertLoadsMyBean(AEnclosingConfig.ChildConfig.class);
  }

  private void assertLoadsMyBean(Class<?>... annotatedClasses) {
    context = new AnnotationConfigApplicationContext(annotatedClasses);
    assertThat(context.getBean("myBean", String.class)).isEqualTo("myBean");
  }

}
