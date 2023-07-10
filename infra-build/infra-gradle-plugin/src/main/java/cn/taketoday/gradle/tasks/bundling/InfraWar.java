/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.bundling.War;
import org.gradle.work.DisableCachingByDefault;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * A custom {@link War} task that produces a Infra executable war.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public abstract class InfraWar extends War implements InfraArchive {

  private static final String LAUNCHER = "cn.taketoday.app.loader.WarLauncher";

  private static final String CLASSES_DIRECTORY = "WEB-INF/classes/";

  private static final String LIB_PROVIDED_DIRECTORY = "WEB-INF/lib-provided/";

  private static final String LIB_DIRECTORY = "WEB-INF/lib/";

  private static final String LAYERS_INDEX = "WEB-INF/layers.idx";

  private static final String CLASSPATH_INDEX = "WEB-INF/classpath.idx";

  private final InfraArchiveSupport support;

  private final LayeredSpec layered;

  private final Provider<String> projectName;

  private final Provider<Object> projectVersion;

  private final ResolvedDependencies resolvedDependencies;

  private FileCollection providedClasspath;

  /**
   * Creates a new {@code InfraWar} task.
   */
  public InfraWar() {
    this.support = new InfraArchiveSupport(LAUNCHER, new LibrarySpec(), new ZipCompressionResolver());
    Project project = getProject();
    this.layered = project.getObjects().newInstance(LayeredSpec.class);
    getWebInf().into("lib-provided", fromCallTo(this::getProvidedLibFiles));
    this.support.moveModuleInfoToRoot(getRootSpec());
    getRootSpec().eachFile(this.support::excludeNonZipLibraryFiles);
    this.projectName = project.provider(project::getName);
    this.projectVersion = project.provider(project::getVersion);
    this.resolvedDependencies = new ResolvedDependencies(project);
  }

  private Object getProvidedLibFiles() {
    return (this.providedClasspath != null) ? this.providedClasspath : Collections.emptyList();
  }

  @Override
  public void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts) {
    this.resolvedDependencies.resolvedArtifacts(resolvedArtifacts);
  }

  @Nested
  ResolvedDependencies getResolvedDependencies() {
    return this.resolvedDependencies;
  }

  @Override
  public void copy() {
    this.support.configureManifest(getManifest(), getMainClass().get(), CLASSES_DIRECTORY, LIB_DIRECTORY,
            CLASSPATH_INDEX, (isLayeredDisabled()) ? null : LAYERS_INDEX,
            this.getTargetJavaVersion().get().getMajorVersion(), this.projectName.get(), this.projectVersion.get());
    super.copy();
  }

  private boolean isLayeredDisabled() {
    return !this.layered.getEnabled().get();
  }

  @Override
  protected CopyAction createCopyAction() {
    if (!isLayeredDisabled()) {
      LayerResolver layerResolver = new LayerResolver(this.resolvedDependencies, this.layered, this::isLibrary);
      String layerToolsLocation = this.layered.getIncludeLayerTools().get() ? LIB_DIRECTORY : null;
      return this.support.createCopyAction(this, this.resolvedDependencies, layerResolver, layerToolsLocation);
    }
    return this.support.createCopyAction(this, this.resolvedDependencies);
  }

  @Override
  public void requiresUnpack(String... patterns) {
    this.support.requiresUnpack(patterns);
  }

  @Override
  public void requiresUnpack(Spec<FileTreeElement> spec) {
    this.support.requiresUnpack(spec);
  }

  @Override
  public LaunchScriptConfiguration getLaunchScript() {
    return this.support.getLaunchScript();
  }

  @Override
  public void launchScript() {
    enableLaunchScriptIfNecessary();
  }

  @Override
  public void launchScript(Action<LaunchScriptConfiguration> action) {
    action.execute(enableLaunchScriptIfNecessary());
  }

  /**
   * Returns the provided classpath, the contents of which will be included in the
   * {@code WEB-INF/lib-provided} directory of the war.
   *
   * @return the provided classpath
   */
  @Optional
  @Classpath
  public FileCollection getProvidedClasspath() {
    return this.providedClasspath;
  }

  /**
   * Adds files to the provided classpath to include in the {@code WEB-INF/lib-provided}
   * directory of the war. The given {@code classpath} is evaluated as per
   * {@link Project#files(Object...)}.
   *
   * @param classpath the additions to the classpath
   */
  public void providedClasspath(Object... classpath) {
    FileCollection existingClasspath = this.providedClasspath;
    this.providedClasspath = getProject()
            .files((existingClasspath != null) ? existingClasspath : Collections.emptyList(), classpath);
  }

  /**
   * Sets the provided classpath to include in the {@code WEB-INF/lib-provided}
   * directory of the war.
   *
   * @param classpath the classpath
   */
  public void setProvidedClasspath(FileCollection classpath) {
    this.providedClasspath = getProject().files(classpath);
  }

  /**
   * Sets the provided classpath to include in the {@code WEB-INF/lib-provided}
   * directory of the war. The given {@code classpath} is evaluated as per
   * {@link Project#files(Object...)}.
   *
   * @param classpath the classpath
   */
  public void setProvidedClasspath(Object classpath) {
    this.providedClasspath = getProject().files(classpath);
  }

  /**
   * Return the {@link ZipCompression} that should be used when adding the file
   * represented by the given {@code details} to the jar. By default, any
   * {@link #isLibrary(FileCopyDetails) library} is {@link ZipCompression#STORED stored}
   * and all other files are {@link ZipCompression#DEFLATED deflated}.
   *
   * @param details the file copy details
   * @return the compression to use
   */
  protected ZipCompression resolveZipCompression(FileCopyDetails details) {
    return isLibrary(details) ? ZipCompression.STORED : ZipCompression.DEFLATED;
  }

  /**
   * Returns the spec that describes the layers in a layered jar.
   *
   * @return the spec for the layers
   */
  @Nested
  public LayeredSpec getLayered() {
    return this.layered;
  }

  /**
   * Configures the war's layering using the given {@code action}.
   *
   * @param action the action to apply
   */
  public void layered(Action<LayeredSpec> action) {
    action.execute(this.layered);
  }

  /**
   * Return if the {@link FileCopyDetails} are for a library. By default any file in
   * {@code WEB-INF/lib} or {@code WEB-INF/lib-provided} is considered to be a library.
   *
   * @param details the file copy details
   * @return {@code true} if the details are for a library
   */
  protected boolean isLibrary(FileCopyDetails details) {
    String path = details.getRelativePath().getPathString();
    return path.startsWith(LIB_DIRECTORY) || path.startsWith(LIB_PROVIDED_DIRECTORY);
  }

  private LaunchScriptConfiguration enableLaunchScriptIfNecessary() {
    LaunchScriptConfiguration launchScript = this.support.getLaunchScript();
    if (launchScript == null) {
      launchScript = new LaunchScriptConfiguration(this);
      this.support.setLaunchScript(launchScript);
    }
    return launchScript;
  }

  /**
   * Syntactic sugar that makes {@link CopySpec#into} calls a little easier to read.
   *
   * @param <T> the result type
   * @param callable the callable
   * @return an action to add the callable to the spec
   */
  private static <T> Action<CopySpec> fromCallTo(Callable<T> callable) {
    return (spec) -> spec.from(callTo(callable));
  }

  /**
   * Syntactic sugar that makes {@link CopySpec#from} calls a little easier to read.
   *
   * @param <T> the result type
   * @param callable the callable
   * @return the callable
   */
  private static <T> Callable<T> callTo(Callable<T> callable) {
    return callable;
  }

  private final class LibrarySpec implements Spec<FileCopyDetails> {

    @Override
    public boolean isSatisfiedBy(FileCopyDetails details) {
      return isLibrary(details);
    }

  }

  private final class ZipCompressionResolver implements Function<FileCopyDetails, ZipCompression> {

    @Override
    public ZipCompression apply(FileCopyDetails details) {
      return resolveZipCompression(details);
    }

  }

}
