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

package infra.test.context.aot;

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
          "infra/test/context/aot/AotTestContextInitializers__Generated.java",
          "infra/test/context/aot/AotTestAttributes__Generated.java",
          // BasicInfraJupiterImportedConfigTests
          "infra/context/event/DefaultEventListenerFactory__TestContext001_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext001_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterImportedConfigTests__TestContext001_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterImportedConfigTests__TestContext001_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterImportedConfigTests__TestContext001_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicTestConfiguration__TestContext001_BeanDefinitions.java",
          // BasicInfraJupiterSharedConfigTests
          "infra/context/event/DefaultEventListenerFactory__TestContext002_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext002_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_ManagementApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext002_ManagementBeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicTestConfiguration__TestContext002_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementConfiguration__TestContext002_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementMessageService__TestContext002_ManagementBeanDefinitions.java",

          // BasicInfraJupiterTests -- not generated b/c already generated for BasicInfraJupiterSharedConfigTests.
          // BasicInfraJupiterTests.NestedTests
          "infra/context/event/DefaultEventListenerFactory__TestContext003_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext003_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_BeanFactoryRegistrations.java",

          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_ManagementApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_ManagementBeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicTestConfiguration__TestContext003_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementConfiguration__TestContext003_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementMessageService__TestContext003_ManagementBeanDefinitions.java",

          // BasicInfraVintageTests
          "infra/context/event/DefaultEventListenerFactory__TestContext004_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext004_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext004_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext004_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicTestConfiguration__TestContext004_BeanDefinitions.java",

          // DisabledInAotRuntimeMethodLevelTests
          "infra/context/event/DefaultEventListenerFactory__TestContext005_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext005_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/DisabledInAotRuntimeMethodLevelTests__TestContext005_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/DisabledInAotRuntimeMethodLevelTests__TestContext005_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/DisabledInAotRuntimeMethodLevelTests__TestContext005_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext003_EnvironmentPostProcessor.java"

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
