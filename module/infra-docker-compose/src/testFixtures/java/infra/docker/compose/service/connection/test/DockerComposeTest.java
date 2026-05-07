/*
 * Copyright 2012-present the original author or authors.
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

package infra.docker.compose.service.connection.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.app.Application;
import infra.context.service.connection.ConnectionDetails;
import infra.test.process.DisabledIfProcessUnavailable;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.test.testcontainers.TestImage;

/**
 * A {@link Test test} that exercises Infra Docker Compose support.
 * <p>
 * Before the test is executed, a {@link Application} that is configured to use the
 * specified Docker Compose file is started. Any bean that exists in the resulting
 * application context can be injected as a parameter into the test method. Typically,
 * this will be a {@link ConnectionDetails} implementation.
 * <p>
 * Once the test has executed, the {@link Application} is tidied up such that the
 * Docker Compose services are stopped and destroyed and the application context is
 * closed.
 *
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 */
@Test
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DockerComposeTestExtension.class)
@DisabledIfDockerUnavailable
@DisabledIfProcessUnavailable({ "docker", "compose" })
public @interface DockerComposeTest {

  /**
   * The name of the compose file to use. Loaded as a classpath resource relative to the
   * test class. The image name in the compose file can be parameterized using
   * <code>{image}</code> and it will be replaced using the specified {@link #image}
   * reference.
   *
   * @return the compose file
   */
  String composeFile();

  /**
   * Additional resources to copy next to the compose file. Loaded as a classpath
   * resource relative to the test class.
   *
   * @return the additional resources to copy
   */
  String[] additionalResources() default {};

  /**
   * The Docker image reference.
   *
   * @return the Docker image reference
   * @see TestImage
   */
  TestImage image();

}
