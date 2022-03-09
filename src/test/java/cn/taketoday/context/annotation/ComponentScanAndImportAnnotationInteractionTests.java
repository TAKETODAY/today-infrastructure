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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.annotation.componentscan.importing.ImportingConfig;
import cn.taketoday.context.annotation.componentscan.simple.SimpleComponent;
import cn.taketoday.context.support.StandardApplicationContext;

/**
 * Tests covering overlapping use of @ComponentScan and @Import annotations.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class ComponentScanAndImportAnnotationInteractionTests {

  @Test
  public void componentScanOverlapsWithImport() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config1.class);
    ctx.register(Config2.class);
    ctx.refresh(); // no conflicts found trying to register SimpleComponent
    ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
  }

  @Test
  public void componentScanOverlapsWithImportUsingAsm() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.registerBeanDefinition("config1", new RootBeanDefinition(Config1.class.getName()));
    ctx.registerBeanDefinition("config2", new RootBeanDefinition(Config2.class.getName()));
    ctx.refresh(); // no conflicts found trying to register SimpleComponent
    ctx.getBean(SimpleComponent.class); // succeeds -> there is only one bean of type SimpleComponent
  }

  @Test
  public void componentScanViaImport() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config3.class);
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @Test
  public void componentScanViaImportUsingAsm() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(Config3.class.getName()));
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @Test
  public void componentScanViaImportUsingScan() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.scan("cn.taketoday.context.annotation.componentscan.importing");
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @Test
  public void circularImportViaComponentScan() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.registerBeanDefinition("config", new RootBeanDefinition(ImportingConfig.class.getName()));
    ctx.refresh();
    ctx.getBean(SimpleComponent.class);
  }

  @ComponentScan("cn.taketoday.context.annotation.componentscan.simple")
  static final class Config1 {
  }

  @Import(SimpleComponent.class)
  static final class Config2 {
  }

  @Import(ImportedConfig.class)
  static final class Config3 {
  }

  @ComponentScan("cn.taketoday.context.annotation.componentscan.simple")
  @ComponentScan("cn.taketoday.context.annotation.componentscan.importing")
  public static final class ImportedConfig {
  }

}
