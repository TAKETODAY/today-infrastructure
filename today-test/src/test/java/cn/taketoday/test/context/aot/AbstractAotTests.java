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

package cn.taketoday.test.context.aot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstract base class for AOT tests.
 *
 * @author Sam Brannen
 * @since 4.0
 */
abstract class AbstractAotTests {

  static final String[] expectedSourceFilesForBasicInfraTests = {
          // Global
          "cn/taketoday/test/context/aot/AotTestContextInitializers__Generated.java",
          "cn/taketoday/test/context/aot/AotTestAttributes__Generated.java",
          // BasicInfraJupiterImportedConfigTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext001_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext001_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterImportedConfigTests__TestContext001_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterImportedConfigTests__TestContext001_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterImportedConfigTests__TestContext001_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicTestConfiguration__TestContext001_BeanDefinitions.java",
          // BasicInfraJupiterSharedConfigTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext002_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext002_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_ManagementApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_ManagementBeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicTestConfiguration__TestContext002_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/management/ManagementConfiguration__TestContext002_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/management/ManagementMessageService__TestContext002_ManagementBeanDefinitions.java",
          
          // BasicInfraJupiterTests -- not generated b/c already generated for BasicInfraJupiterSharedConfigTests.
          // BasicInfraJupiterTests.NestedTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext003_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext003_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_BeanFactoryRegistrations.java",

          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_ManagementApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_ManagementBeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicTestConfiguration__TestContext003_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/management/ManagementConfiguration__TestContext003_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/management/ManagementMessageService__TestContext003_ManagementBeanDefinitions.java",
          
          // BasicInfraVintageTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext004_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext004_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext004_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext004_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicTestConfiguration__TestContext004_BeanDefinitions.java",
          "cn/taketoday/framework/test/context/ImportsContextCustomizer__TestContext001_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext001_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext002_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext003_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext004_BeanDefinitions.java"
  };

  Stream<Class<?>> scan() {
    return new TestClassScanner(classpathRoots()).scan();
  }

  Stream<Class<?>> scan(String... packageNames) {
    return new TestClassScanner(classpathRoots()).scan(packageNames);
  }

  Set<Path> classpathRoots() {
    try {
      return Set.of(classpathRoot());
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  Path classpathRoot() {
    try {
      return Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  Path classpathRoot(Class<?> clazz) {
    try {
      return Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
