/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation.spr12334;

import org.junit.jupiter.api.Test;

import infra.context.BootstrapContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.core.type.AnnotationMetadata;

/**
 * @author Juergen Hoeller
 * @author Alex Pogrebnyak
 */
public class Spr12334Tests {

  @Test
  public void shouldNotScanTwice() {
    TestImport.scanned = false;

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
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
