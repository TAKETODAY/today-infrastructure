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

package infra.gradle.tasks.bundling;

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
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * A custom {@link Jar} task that produces a Infra executable jar.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public abstract class InfraJar extends Jar implements InfraArchive {

  private static final String LAUNCHER = "infra.app.loader.JarLauncher";

  private static final String CLASSES_DIRECTORY = "APP-INF/classes/";

  private static final String LIB_DIRECTORY = "APP-INF/lib/";

  private static final String LAYERS_INDEX = "APP-INF/layers.idx";

  private static final String CLASSPATH_INDEX = "APP-INF/classpath.idx";

  private final InfraArchiveSupport support;

  private final CopySpec appInfSpec;

  private final LayeredSpec layered;

  private final Provider<String> projectName;

  private final Provider<Object> projectVersion;

  private final ResolvedDependencies resolvedDependencies;

  private FileCollection classpath;

  /**
   * Creates a new {@code InfraJar} task.
   */
  public InfraJar() {
    this.support = new InfraArchiveSupport(LAUNCHER, new LibrarySpec(), new ZipCompressionResolver());
    Project project = getProject();
    this.appInfSpec = project.copySpec().into("APP-INF");
    this.layered = project.getObjects().newInstance(LayeredSpec.class);
    configureAppInfSpec(this.appInfSpec);
    getMainSpec().with(this.appInfSpec);
    this.projectName = project.provider(project::getName);
    this.projectVersion = project.provider(project::getVersion);
    this.resolvedDependencies = new ResolvedDependencies(project);
  }

  private void configureAppInfSpec(CopySpec appInfSpec) {
    appInfSpec.into("classes", fromCallTo(this::classpathDirectories));
    appInfSpec.into("lib", fromCallTo(this::classpathFiles)).eachFile(this.support::excludeNonZipFiles);
    this.support.moveModuleInfoToRoot(appInfSpec);
    moveMetaInfToApp(appInfSpec);
  }

  private Iterable<File> classpathDirectories() {
    return classpathEntries(File::isDirectory);
  }

  private Iterable<File> classpathFiles() {
    return classpathEntries(File::isFile);
  }

  private Iterable<File> classpathEntries(Spec<File> filter) {
    return (this.classpath != null) ? this.classpath.filter(filter) : Collections.emptyList();
  }

  private void moveMetaInfToApp(CopySpec spec) {
    spec.eachFile((file) -> {
      String path = file.getRelativeSourcePath().getPathString();
      if (path.startsWith("META-INF/") && !path.equals("META-INF/aop.xml") && !path.endsWith(".kotlin_module")
              && !path.startsWith("META-INF/services/")) {
        this.support.moveToRoot(file);
      }
    });
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
    return !getLayered().getEnabled().get();
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
   * Returns the spec that describes the layers in a layered jar.
   *
   * @return the spec for the layers
   */
  @Nested
  public LayeredSpec getLayered() {
    return this.layered;
  }

  /**
   * Configures the jar's layering using the given {@code action}.
   *
   * @param action the action to apply
   */
  public void layered(Action<LayeredSpec> action) {
    action.execute(this.layered);
  }

  @Override
  public FileCollection getClasspath() {
    return this.classpath;
  }

  @Override
  public void classpath(Object... classpath) {
    FileCollection existingClasspath = this.classpath;
    if (existingClasspath != null) {
      this.classpath = getProject().files(existingClasspath, classpath);
    }
    else {
      this.classpath = getProject().files(classpath);
    }
  }

  @Override
  public void setClasspath(Object classpath) {
    this.classpath = getProject().files(classpath);
  }

  @Override
  public void setClasspath(FileCollection classpath) {
    this.classpath = getProject().files(classpath);
  }

  /**
   * Returns a {@code CopySpec} that can be used to add content to the {@code APP-INF}
   * directory of the jar.
   *
   * @return a {@code CopySpec} for {@code APP-INF}
   */
  @Internal
  public CopySpec getAppInf() {
    CopySpec child = getProject().copySpec();
    this.appInfSpec.with(child);
    return child;
  }

  /**
   * Calls the given {@code action} to add content to the {@code APP-INF} directory of
   * the jar.
   *
   * @param action the {@code Action} to call
   * @return the {@code CopySpec} for {@code APP-INF} that was passed to the
   * {@code Action}
   */
  public CopySpec appInf(Action<CopySpec> action) {
    CopySpec appInf = getAppInf();
    action.execute(appInf);
    return appInf;
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
   * Return if the {@link FileCopyDetails} are for a library. By default any file in
   * {@code APP-INF/lib} is considered to be a library.
   *
   * @param details the file copy details
   * @return {@code true} if the details are for a library
   */
  protected boolean isLibrary(FileCopyDetails details) {
    String path = details.getRelativePath().getPathString();
    return path.startsWith(LIB_DIRECTORY);
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
