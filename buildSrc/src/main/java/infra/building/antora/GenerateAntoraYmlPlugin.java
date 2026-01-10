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

package infra.building.antora;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;

public class GenerateAntoraYmlPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getTasks().register("generateAntoraYml", GenerateAntoraYmlTask.class, generateAntoraYmlTask -> {
      generateAntoraYmlTask.setGroup("Documentation");
      generateAntoraYmlTask.setDescription("Generates an antora.yml file with information from the build");
      String name = project.getName();
      generateAntoraYmlTask.getComponentName().convention(name);
      String projectVersion = project.getVersion().toString();
      if (!Project.DEFAULT_VERSION.equals(projectVersion)) {
        generateAntoraYmlTask.getVersion().convention(projectVersion);
      }
      RegularFile defaultBaseAntoraYmlFile = project.getLayout().getProjectDirectory().file("antora.yml");
      if (defaultBaseAntoraYmlFile.getAsFile().exists()) {
        generateAntoraYmlTask.getBaseAntoraYmlFile().convention(defaultBaseAntoraYmlFile);
      }
      generateAntoraYmlTask.getOutputFile()
              .convention(project.getLayout().getBuildDirectory().file("generated-antora-resources/antora.yml"));
    });

  }
}