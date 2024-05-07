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

package cn.taketoday.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaApplication;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import cn.taketoday.gradle.dsl.InfraApplicationExtension;
import cn.taketoday.gradle.tasks.bundling.InfraBuildImage;
import cn.taketoday.gradle.tasks.bundling.InfraJar;
import cn.taketoday.gradle.tasks.run.InfraRun;
import cn.taketoday.util.StringUtils;

/**
 * {@link Action} that is executed in response to the {@link JavaPlugin} being applied.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class JavaPluginAction implements PluginApplicationAction {

  private static final String PARAMETERS_COMPILER_ARG = "-parameters";

  private final SinglePublishedArtifact singlePublishedArtifact;

  JavaPluginAction(SinglePublishedArtifact singlePublishedArtifact) {
    this.singlePublishedArtifact = singlePublishedArtifact;
  }

  @Override
  public Class<? extends Plugin<? extends Project>> getPluginClass() {
    return JavaPlugin.class;
  }

  @Override
  public void execute(Project project) {
    if (project.hasProperty("classifyJar")) {
      classifyJarTask(project);
    }
    configureBuildTask(project);
    configureProductionRuntimeClasspathConfiguration(project);
    configureDevelopmentOnlyConfiguration(project);
    configureTestAndDevelopmentOnlyConfiguration(project);
    TaskProvider<ResolveMainClassName> resolveMainClassName = configureResolveMainClassNameTask(project);
    TaskProvider<InfraJar> infraJar = configureInfraJarTask(project, resolveMainClassName);
    configureInfraBuildImageTask(project, infraJar);
    configureArtifactPublication(infraJar);
    configureInfraRunTask(project, resolveMainClassName);
    TaskProvider<ResolveMainClassName> resolveMainTestClassName = configureResolveMainTestClassNameTask(project);
    configureInfraTestRunTask(project, resolveMainTestClassName);
    project.afterEvaluate(this::configureUtf8Encoding);
    configureParametersCompilerArg(project);
    configureAdditionalMetadataLocations(project);
  }

  private void classifyJarTask(Project project) {
    project.getTasks()
            .named(JavaPlugin.JAR_TASK_NAME, Jar.class)
            .configure(task -> task.getArchiveClassifier().convention("plain"));
  }

  private void configureBuildTask(Project project) {
    project.getTasks()
            .named(BasePlugin.ASSEMBLE_TASK_NAME)
            .configure((task) -> task.dependsOn(this.singlePublishedArtifact));
  }

  private TaskProvider<ResolveMainClassName> configureResolveMainClassNameTask(Project project) {
    return project.getTasks().register(InfraApplicationPlugin.RESOLVE_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class, resolveMainClassName -> {
      ExtensionContainer extensions = project.getExtensions();
      resolveMainClassName.setDescription("Resolves the name of the application's main class.");
      resolveMainClassName.setGroup(BasePlugin.BUILD_GROUP);
      Callable<FileCollection> classpath = () -> project.getExtensions()
              .getByType(SourceSetContainer.class)
              .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
              .getOutput();
      resolveMainClassName.setClasspath(classpath);
      resolveMainClassName.getConfiguredMainClassName().convention(project.provider(() -> {
        String javaApplicationMainClass = getJavaApplicationMainClass(extensions);
        if (javaApplicationMainClass != null) {
          return javaApplicationMainClass;
        }
        InfraApplicationExtension extension = project.getExtensions().getByType(InfraApplicationExtension.class);
        return extension.getMainClass().getOrNull();
      }));
      resolveMainClassName.getOutputFile()
              .set(project.getLayout().getBuildDirectory().file("resolvedMainClassName"));
    });
  }

  private TaskProvider<ResolveMainClassName> configureResolveMainTestClassNameTask(Project project) {
    return project.getTasks().register(InfraApplicationPlugin.RESOLVE_TEST_MAIN_CLASS_NAME_TASK_NAME, ResolveMainClassName.class, resolveMainClassName -> {
      resolveMainClassName.setDescription("Resolves the name of the application's test main class.");
      resolveMainClassName.setGroup(BasePlugin.BUILD_GROUP);
      Callable<FileCollection> classpath = () -> {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        return project.files(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getOutput(),
                sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput());
      };
      resolveMainClassName.setClasspath(classpath);
      resolveMainClassName.getOutputFile()
              .set(project.getLayout().getBuildDirectory().file("resolvedMainTestClassName"));
    });
  }

  private static String getJavaApplicationMainClass(ExtensionContainer extensions) {
    JavaApplication javaApplication = extensions.findByType(JavaApplication.class);
    if (javaApplication == null) {
      return null;
    }
    return javaApplication.getMainClass().getOrNull();
  }

  private TaskProvider<InfraJar> configureInfraJarTask(Project project, TaskProvider<ResolveMainClassName> resolveMainClassName) {
    ConfigurationContainer config = project.getConfigurations();
    SourceSet mainSourceSet = javaPluginExtension(project).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    Configuration developmentOnly = config.getByName(InfraApplicationPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    Configuration testAndDevelopmentOnly = config.getByName(InfraApplicationPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    Configuration productionRuntimeClasspath = config.getByName(InfraApplicationPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    Configuration runtimeClasspath = config.getByName(mainSourceSet.getRuntimeClasspathConfigurationName());
    Callable<FileCollection> classpath = () -> mainSourceSet.getRuntimeClasspath()
            .minus(developmentOnly.minus(productionRuntimeClasspath))
            .minus(testAndDevelopmentOnly.minus(productionRuntimeClasspath))
            .filter(new JarTypeFileSpec());

    return project.getTasks().register(InfraApplicationPlugin.INFRA_JAR_TASK_NAME, InfraJar.class, infraJar -> {
      infraJar.setDescription("Assembles an executable jar archive containing the main classes and their dependencies.");
      infraJar.setGroup(BasePlugin.BUILD_GROUP);
      infraJar.classpath(classpath);
      Provider<String> manifestStartClass = project.provider(
              () -> (String) infraJar.getManifest().getAttributes().get("Start-Class"));
      infraJar.getMainClass().convention(resolveMainClassName.flatMap(
              resolver -> manifestStartClass.isPresent() ? manifestStartClass : resolver.readMainClassName()));
      infraJar.getTargetJavaVersion().set(project.provider(() -> javaPluginExtension(project).getTargetCompatibility()));
      infraJar.resolvedArtifacts(runtimeClasspath.getIncoming().getArtifacts().getResolvedArtifacts());
    });
  }

  private void configureInfraBuildImageTask(Project project, TaskProvider<InfraJar> infraJar) {
    project.getTasks().register(InfraApplicationPlugin.INFRA_BUILD_IMAGE_TASK_NAME, InfraBuildImage.class, (buildImage) -> {
      buildImage.setDescription("Builds an OCI image of the application using the output of the infraJar task");
      buildImage.setGroup(BasePlugin.BUILD_GROUP);
      buildImage.getArchiveFile().set(infraJar.get().getArchiveFile());
    });
  }

  private void configureArtifactPublication(TaskProvider<InfraJar> infraJar) {
    this.singlePublishedArtifact.addJarCandidate(infraJar);
  }

  private void configureInfraRunTask(Project project, TaskProvider<ResolveMainClassName> resolveMainClassName) {
    Callable<FileCollection> classpath = () -> javaPluginExtension(project).getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getRuntimeClasspath()
            .filter(new JarTypeFileSpec());
    project.getTasks().register(InfraApplicationPlugin.INFRA_RUN_TASK_NAME, InfraRun.class, (run) -> {
      run.setDescription("Runs this project as a Infra application.");
      run.setGroup(ApplicationPlugin.APPLICATION_GROUP);
      run.classpath(classpath);
      run.getMainClass().convention(resolveMainClassName.flatMap(ResolveMainClassName::readMainClassName));
      configureToolchainConvention(project, run);
    });
  }

  private void configureInfraTestRunTask(Project project, TaskProvider<ResolveMainClassName> resolveMainClassName) {
    Callable<FileCollection> classpath = () -> javaPluginExtension(project).getSourceSets()
            .getByName(SourceSet.TEST_SOURCE_SET_NAME)
            .getRuntimeClasspath()
            .filter(new JarTypeFileSpec());
    project.getTasks().register("infraTestRun", InfraRun.class, (run) -> {
      run.setDescription("Runs this project as a Infra application using the test runtime classpath.");
      run.setGroup(ApplicationPlugin.APPLICATION_GROUP);
      run.classpath(classpath);
      run.getMainClass().convention(resolveMainClassName.flatMap(ResolveMainClassName::readMainClassName));
      configureToolchainConvention(project, run);
    });
  }

  private void configureToolchainConvention(Project project, InfraRun run) {
    JavaToolchainSpec toolchain = project.getExtensions().getByType(JavaPluginExtension.class).getToolchain();
    JavaToolchainService toolchainService = project.getExtensions().getByType(JavaToolchainService.class);
    run.getJavaLauncher().convention(toolchainService.launcherFor(toolchain));
  }

  static JavaPluginExtension javaPluginExtension(Project project) {
    return project.getExtensions().getByType(JavaPluginExtension.class);
  }

  private void configureUtf8Encoding(Project evaluatedProject) {
    evaluatedProject.getTasks().withType(JavaCompile.class).configureEach(this::configureUtf8Encoding);
  }

  private void configureUtf8Encoding(JavaCompile compile) {
    if (compile.getOptions().getEncoding() == null) {
      compile.getOptions().setEncoding("UTF-8");
    }
  }

  private void configureParametersCompilerArg(Project project) {
    project.getTasks().withType(JavaCompile.class).configureEach((compile) -> {
      List<String> compilerArgs = compile.getOptions().getCompilerArgs();
      if (!compilerArgs.contains(PARAMETERS_COMPILER_ARG)) {
        compilerArgs.add(PARAMETERS_COMPILER_ARG);
      }
    });
  }

  private void configureAdditionalMetadataLocations(Project project) {
    project.afterEvaluate(evaluated -> evaluated.getTasks()
            .withType(JavaCompile.class)
            .configureEach(this::configureAdditionalMetadataLocations));
  }

  private void configureAdditionalMetadataLocations(JavaCompile compile) {
    SourceSetContainer sourceSets = compile.getProject()
            .getExtensions()
            .getByType(JavaPluginExtension.class)
            .getSourceSets();
    sourceSets.stream()
            .filter(candidate -> candidate.getCompileJavaTaskName().equals(compile.getName()))
            .map(match -> match.getResources().getSrcDirs())
            .findFirst()
            .ifPresent(locations -> compile.doFirst(new AdditionalMetadataLocationsConfigurer(locations)));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void configureProductionRuntimeClasspathConfiguration(Project project) {
    Configuration productionRuntimeClasspath = project.getConfigurations().create(
            InfraApplicationPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    productionRuntimeClasspath.setVisible(false);
    Configuration runtimeClasspath = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    productionRuntimeClasspath.attributes(attributes -> {
      ProviderFactory providers = project.getProviders();
      AttributeContainer sourceAttributes = runtimeClasspath.getAttributes();
      for (Attribute attribute : sourceAttributes.keySet()) {
        attributes.attributeProvider(attribute, providers.provider(() -> sourceAttributes.getAttribute(attribute)));
      }
    });
    productionRuntimeClasspath.setExtendsFrom(runtimeClasspath.getExtendsFrom());
    productionRuntimeClasspath.setCanBeResolved(runtimeClasspath.isCanBeResolved());
    productionRuntimeClasspath.setCanBeConsumed(runtimeClasspath.isCanBeConsumed());
  }

  private void configureDevelopmentOnlyConfiguration(Project project) {
    Configuration developmentOnly = project.getConfigurations().create(InfraApplicationPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    developmentOnly.setDescription("Configuration for development-only dependencies such as Infra DevTools.");
    Configuration runtimeClasspath = project.getConfigurations()
            .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    runtimeClasspath.extendsFrom(developmentOnly);
  }

  private void configureTestAndDevelopmentOnlyConfiguration(Project project) {
    ConfigurationContainer container = project.getConfigurations();
    Configuration testAndDevelopmentOnly = container.create(InfraApplicationPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
    testAndDevelopmentOnly.setDescription("Configuration for test and development-only dependencies such as Infra DevTools.");

    Configuration runtimeClasspath = container.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    Configuration testImplementation = container.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME);

    runtimeClasspath.extendsFrom(testAndDevelopmentOnly);
    testImplementation.extendsFrom(testAndDevelopmentOnly);
  }

  /**
   * Task {@link Action} to add additional meta-data locations. We need to use an
   * inner-class rather than a lambda due to
   * https://github.com/gradle/gradle/issues/5510.
   */
  private static final class AdditionalMetadataLocationsConfigurer implements Action<Task> {

    private final Set<File> locations;

    private AdditionalMetadataLocationsConfigurer(Set<File> locations) {
      this.locations = locations;
    }

    @Override
    public void execute(Task task) {
      if (!(task instanceof JavaCompile compile)) {
        return;
      }
      if (hasConfigurationProcessorOnClasspath(compile)) {
        configureAdditionalMetadataLocations(compile);
      }
    }

    private boolean hasConfigurationProcessorOnClasspath(JavaCompile compile) {
      Set<File> files = (compile.getOptions().getAnnotationProcessorPath() != null)
              ? compile.getOptions().getAnnotationProcessorPath().getFiles() : compile.getClasspath().getFiles();
      return files.stream()
              .map(File::getName)
              .anyMatch(name -> name.startsWith("infra-configuration-processor"));
    }

    private void configureAdditionalMetadataLocations(JavaCompile compile) {
      compile.getOptions()
              .getCompilerArgs()
              .add("-Acn.taketoday.context.properties.additionalMetadataLocations="
                      + StringUtils.collectionToCommaDelimitedString(this.locations));
    }

  }

}
