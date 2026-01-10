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

package infra.building;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;

/**
 * Plugin to apply conventions to projects that are part of Infra build.
 * Conventions are applied in response to various plugins being applied.
 *
 * <p>When the {@link JavaBasePlugin} is applied,
 * {@link TestConventions} and {@link JavaConventions} are applied.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConventionsPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    new CheckstyleConventions().apply(project);
    new JavaConventions().apply(project);
    new TestConventions().apply(project);
  }

}
