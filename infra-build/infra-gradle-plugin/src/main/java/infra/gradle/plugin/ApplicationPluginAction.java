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

package infra.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.distribution.plugins.DistributionPlugin;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.jvm.application.scripts.TemplateBasedScriptGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import infra.gradle.tasks.run.InfraRun;

import static infra.gradle.plugin.InfraApplicationPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME;
import static org.gradle.api.plugins.ApplicationPlugin.TASK_RUN_NAME;

/**
 * Action that is executed in response to the {@link ApplicationPlugin} being applied.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ApplicationPluginAction implements PluginApplicationAction {

  private static final String UNSPECIFIED_VERSION = "unspecified";

  @Override
  public boolean autoApply(Project project) {
    return false;
  }

  @Override
  public void execute(Project project) {
    JavaApplication javaApplication = project.getExtensions().getByType(JavaApplication.class);
    DistributionContainer distributions = project.getExtensions().getByType(DistributionContainer.class);
    Distribution distribution = distributions.create("infra");
    distribution.getDistributionBaseName().convention(project.provider(() -> javaApplication.getApplicationName() + "-infra-app"));

    TaskProvider<CreateStartScripts> infraStartScripts = project.getTasks().register("infraStartScripts", CreateStartScripts.class,
            task -> configureCreateStartScripts(project, javaApplication, distribution, task));

    CopySpec binCopySpec = project.copySpec().into("bin").from(infraStartScripts);
    configureFilePermissions(binCopySpec, 0755);
    distribution.getContents().with(binCopySpec);
    applyApplicationDefaultJvmArgsToRunTasks(project.getTasks(), javaApplication);

    project.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class).configure(task -> {
      Attributes attributes = task.getManifest().getAttributes();
      String versionString = String.valueOf(project.getVersion());
      if (!UNSPECIFIED_VERSION.equals(versionString)) {
        attributes.putIfAbsent("Implementation-Version", versionString);
      }
      attributes.putIfAbsent("Implementation-Title", project.getName());
    });

    // filter classpath
    FileCollection excludeClasspath = excludeClasspath(project);
    Spec<File> excludeSpec = JarTypeFileSpec.exclude();
    distributions.getByName(DistributionPlugin.MAIN_DISTRIBUTION_NAME)
            .contents(copySpec -> copySpec.exclude(element -> {
              if (!element.isDirectory()) {
                File file = element.getFile();
                if (file.getName().endsWith(".jar")) {
                  return excludeClasspath.contains(file) || excludeSpec.isSatisfiedBy(file);
                }
              }
              return false;
            }));

    Provider<String> mainClassNameProvider = project.getTasks().named(RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class)
            .flatMap(ResolveMainClassName::readMainClassName);

    project.getTasks().named(ApplicationPlugin.TASK_START_SCRIPTS_NAME, CreateStartScripts.class, startScripts -> {
      FileCollection classpath = startScripts.getClasspath();
      if (classpath != null) {
        startScripts.setClasspath(classpath.filter(file -> {
          if (file.getName().endsWith(".jar")) {
            return !excludeClasspath.contains(file) && !excludeSpec.isSatisfiedBy(file);
          }
          return true;
        }));
      }

      if (!startScripts.getMainClass().isPresent()) {
        startScripts.dependsOn(RESOLVE_MAIN_CLASS_NAME_TASK_NAME);
        startScripts.getMainClass().set(mainClassNameProvider);
      }
    });

    project.getTasks().named(TASK_RUN_NAME, JavaExec.class, run -> {
      if (!run.getMainClass().isPresent()) {
        run.dependsOn(RESOLVE_MAIN_CLASS_NAME_TASK_NAME);
        run.getMainClass().set(mainClassNameProvider);
      }
    });

  }

  static FileCollection excludeClasspath(Project project) {
    ConfigurationContainer config = project.getConfigurations();
    Configuration developmentOnly = config.getByName(InfraApplicationPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    Configuration testAndDevelopmentOnly = config.getByName(InfraApplicationPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    Configuration productionRuntimeClasspath = config.getByName(InfraApplicationPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);

    return developmentOnly.minus(productionRuntimeClasspath)
            .plus(testAndDevelopmentOnly.minus(productionRuntimeClasspath));
  }

  private void applyApplicationDefaultJvmArgsToRunTasks(TaskContainer tasks, JavaApplication javaApplication) {
    applyApplicationDefaultJvmArgsToRunTask(tasks, javaApplication, InfraApplicationPlugin.INFRA_RUN_TASK_NAME);
    applyApplicationDefaultJvmArgsToRunTask(tasks, javaApplication, InfraApplicationPlugin.INFRA_TEST_RUN_TASK_NAME);
  }

  private void applyApplicationDefaultJvmArgsToRunTask(TaskContainer tasks, JavaApplication javaApplication, String taskName) {
    tasks.named(taskName, InfraRun.class).configure(infraRun ->
            infraRun.conventionMapping("jvmArgs", javaApplication::getApplicationDefaultJvmArgs));
  }

  private void configureCreateStartScripts(Project project, JavaApplication application, Distribution distribution, CreateStartScripts startScripts) {
    startScripts.setDescription("Generates OS-specific start scripts to run the project as a Infra application.");

    ((TemplateBasedScriptGenerator) startScripts.getUnixStartScriptGenerator())
            .setTemplate(project.getResources().getText().fromString(loadResource("/unixStartScript.txt")));

    ((TemplateBasedScriptGenerator) startScripts.getWindowsStartScriptGenerator())
            .setTemplate(project.getResources().getText().fromString(loadResource("/windowsStartScript.txt")));

    project.getConfigurations().all(configuration -> {
      if (InfraApplicationPlugin.INFRA_ARCHIVES_CONFIGURATION_NAME.equals(configuration.getName())) {
        distribution.getContents().with(artifactFilesToLibCopySpec(project, configuration));
        startScripts.setClasspath(configuration.getArtifacts().getFiles());
      }
    });
    startScripts.conventionMapping("outputDir", () -> project.getLayout().getBuildDirectory().dir("infraScripts").get().getAsFile());
    startScripts.conventionMapping("applicationName", application::getApplicationName);
    startScripts.conventionMapping("defaultJvmOpts", application::getApplicationDefaultJvmArgs);
  }

  private CopySpec artifactFilesToLibCopySpec(Project project, Configuration configuration) {
    CopySpec copySpec = project.copySpec().into("lib").from(artifactFiles(configuration));
    configureFilePermissions(copySpec, 0644);
    return copySpec;
  }

  private Callable<FileCollection> artifactFiles(Configuration configuration) {
    return () -> configuration.getArtifacts().getFiles();
  }

  @Override
  public Class<? extends Plugin<Project>> getPluginClass() {
    return ApplicationPlugin.class;
  }

  private String loadResource(String name) {
    try (InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(name))) {
      char[] buffer = new char[4096];
      int read;
      StringWriter writer = new StringWriter();
      while ((read = reader.read(buffer)) > 0) {
        writer.write(buffer, 0, read);
      }
      return writer.toString();
    }
    catch (IOException ex) {
      throw new GradleException("Failed to read '" + name + "'", ex);
    }
  }

  private void configureFilePermissions(CopySpec copySpec, int mode) {
    try {
      Method filePermissions = copySpec.getClass().getMethod("filePermissions", Action.class);
      filePermissions.invoke(copySpec, new Action<>() {

        @Override
        public void execute(Object filePermissions) {
          String unixPermissions = Integer.toString(mode, 8);
          try {
            Method unix = filePermissions.getClass().getMethod("unix", String.class);
            unix.invoke(filePermissions, unixPermissions);
          }
          catch (Exception ex) {
            throw new GradleException("Failed to set file permissions to '" + unixPermissions + "'",
                    ex);
          }
        }

      });
    }
    catch (Exception ex) {
      throw new GradleException("Failed to set file permissions", ex);
    }
  }

}
