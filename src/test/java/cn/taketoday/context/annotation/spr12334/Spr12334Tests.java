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

package cn.taketoday.context.annotation.spr12334;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.type.AnnotationMetadata;

/**
 * @author Juergen Hoeller
 * @author Alex Pogrebnyak
 */
public class Spr12334Tests {

  @Test
  public void shouldNotScanTwice() {
    TestImport.scanned = false;

    StandardApplicationContext context = new StandardApplicationContext();
    context.scan(TestImport.class.getPackage().getName());
    context.refresh();
    context.getBean(TestConfiguration.class);
  }

  @Import(TestImport.class)
  public @interface AnotherImport {
  }

  @Configuration
  @AnotherImport
  public static class TestConfiguration {
  }

  public static class TestImport implements ImportBeanDefinitionRegistrar {

    private static boolean scanned = false;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      if (scanned) {
        throw new IllegalStateException("Already scanned");
      }
      scanned = true;
    }
  }

}
