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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.RootBeanDefinition;
import infra.context.annotation.componentscan.importing.ImportingConfig;
import infra.context.annotation.componentscan.simple.SimpleComponent;

/**
 * Tests covering overlapping use of @ComponentScan and @Import annotations.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class ComponentScanAndImportAnnotationInteractionTests {

  @Test
  public void componentScanOverlapsWithImport() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config1.class);
    ctx.register(Config2.class);
    ctx.refresh(); // no conflicts found trying to register SimpleComponent
    ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
  }

  @Test
  public void componentScanOverlapsWithImportUsingAsm() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config1", new RootBeanDefinition(Config1.class.getName()));
    ctx.registerBeanDefinition("config2", new RootBeanDefinition(Config2.class.getName()));
    ctx.refresh(); // no conflicts found trying to register SimpleComponent
    ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
  }

  @Test
  public void componentScanViaImport() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config3.class);
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @Test
  public void componentScanViaImportUsingAsm() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(Config3.class.getName()));
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @Test
  public void componentScanViaImportUsingScan() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.scan("infra.context.annotation.componentscan.importing");
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @Test
  public void circularImportViaComponentScan() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(ImportingConfig.class.getName()));
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @ComponentScan("infra.context.annotation.componentscan.simple")
  static final class Config1 {
  }

  @Import(infra.context.annotation.componentscan.simple.SimpleComponent.class)
  static final class Config2 {
  }

  @Import(ImportedConfig.class)
  static final class Config3 {
  }

  @ComponentScan("infra.context.annotation.componentscan.simple")
  @ComponentScan("infra.context.annotation.componentscan.importing")
  public static final class ImportedConfig {
  }

}
