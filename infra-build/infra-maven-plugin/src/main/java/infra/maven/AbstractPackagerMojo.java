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

package infra.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import infra.app.loader.tools.Layout;
import infra.app.loader.tools.LayoutFactory;
import infra.app.loader.tools.Layouts.Expanded;
import infra.app.loader.tools.Layouts.Jar;
import infra.app.loader.tools.Layouts.None;
import infra.app.loader.tools.Layouts.War;
import infra.app.loader.tools.Libraries;
import infra.app.loader.tools.Packager;
import infra.app.loader.tools.layer.CustomLayers;

/**
 * Abstract base class for classes that work with an {@link Packager}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractPackagerMojo extends AbstractDependencyFilterMojo {

  private static final infra.app.loader.tools.Layers IMPLICIT_LAYERS = infra.app.loader.tools.Layers.IMPLICIT;

  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  /**
   * The Maven session.
   */
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  protected MavenSession session;

  /**
   * Maven project helper utils.
   */
  @Component
  protected MavenProjectHelper projectHelper;

  /**
   * The name of the main class. If not specified the first compiled class found that
   * contains a {@code main} method will be used.
   */
  @Parameter
  private String mainClass;

  /**
   * Include system scoped dependencies.
   */
  @Parameter(defaultValue = "false")
  public boolean includeSystemScope;

  /**
   * Layer configuration with options to disable layer creation, exclude layer tools
   * jar, and provide a custom layers configuration file.
   */
  @Parameter
  private Layers layers;

  /**
   * Return the type of archive that should be packaged by this MOJO.
   *
   * @return {@code null}, indicating a layout type will be chosen based on the original
   * archive type
   */
  protected LayoutType getLayout() {
    return null;
  }

  /**
   * Return the layout factory that will be used to determine the {@link LayoutType} if
   * no explicit layout is set.
   *
   * @return {@code null}, indicating a default layout factory will be chosen
   */
  protected LayoutFactory getLayoutFactory() {
    return null;
  }

  /**
   * Return a {@link Packager} configured for this MOJO.
   *
   * @param <P> the packager type
   * @param packager a packager
   * @return a configured packager
   */
  protected <P extends Packager> P configurePackager(P packager) {
    packager.setLayoutFactory(getLayoutFactory());
    packager.addMainClassTimeoutWarningListener(new LoggingMainClassTimeoutWarningListener(this::getLog));
    packager.setMainClass(this.mainClass);
    LayoutType layout = getLayout();
    if (layout != null) {
      getLog().info("Layout: " + layout);
      packager.setLayout(layout.layout());
    }
    if (this.layers == null) {
      packager.setLayers(IMPLICIT_LAYERS);
    }
    else if (this.layers.isEnabled()) {
      packager.setLayers((this.layers.getConfiguration() != null)
                         ? getCustomLayers(this.layers.getConfiguration()) : IMPLICIT_LAYERS);
      packager.setIncludeRelevantJarModeJars(this.layers.isIncludeLayerTools());
    }
    return packager;
  }

  private CustomLayers getCustomLayers(File configuration) {
    try {
      Document document = getDocumentIfAvailable(configuration);
      return new CustomLayersProvider().getLayers(document);
    }
    catch (Exception ex) {
      throw new IllegalStateException(
              "Failed to process custom layers configuration " + configuration.getAbsolutePath(), ex);
    }
  }

  private Document getDocumentIfAvailable(File xmlFile) throws Exception {
    InputSource inputSource = new InputSource(new FileInputStream(xmlFile));
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(inputSource);
  }

  /**
   * Return {@link Libraries} that the packager can use.
   *
   * @param unpacks any libraries that require unpack
   * @return the libraries to use
   * @throws MojoExecutionException on execution error
   */
  protected final Libraries getLibraries(Collection<Dependency> unpacks) throws MojoExecutionException {
    Set<Artifact> artifacts = this.project.getArtifacts();
    Set<Artifact> includedArtifacts = filterDependencies(artifacts, getAdditionalFilters());
    return new ArtifactsLibraries(artifacts, includedArtifacts, this.session.getProjects(), unpacks, getLog());
  }

  private ArtifactsFilter[] getAdditionalFilters() {
    List<ArtifactsFilter> filters = new ArrayList<>();
    if (!this.includeSystemScope) {
      filters.add(new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
    }
    return filters.toArray(new ArtifactsFilter[0]);
  }

  /**
   * Return the source {@link Artifact} to repackage. If a classifier is specified and
   * an artifact with that classifier exists, it is used. Otherwise, the main artifact
   * is used.
   *
   * @param classifier the artifact classifier
   * @return the source artifact to repackage
   */
  protected Artifact getSourceArtifact(String classifier) {
    Artifact sourceArtifact = getArtifact(classifier);
    return (sourceArtifact != null) ? sourceArtifact : this.project.getArtifact();
  }

  private Artifact getArtifact(String classifier) {
    if (classifier != null) {
      for (Artifact attachedArtifact : this.project.getAttachedArtifacts()) {
        if (classifier.equals(attachedArtifact.getClassifier()) && attachedArtifact.getFile() != null
                && attachedArtifact.getFile().isFile()) {
          return attachedArtifact;
        }
      }
    }
    return null;
  }

  protected File getTargetFile(String finalName, String classifier, File targetDirectory) {
    String classifierSuffix = (classifier != null) ? classifier.trim() : "";
    if (!classifierSuffix.isEmpty() && !classifierSuffix.startsWith("-")) {
      classifierSuffix = "-" + classifierSuffix;
    }
    if (!targetDirectory.exists()) {
      targetDirectory.mkdirs();
    }
    return new File(targetDirectory,
            finalName + classifierSuffix + "." + this.project.getArtifact().getArtifactHandler().getExtension());
  }

  /**
   * Archive layout types.
   */
  public enum LayoutType {

    /**
     * Jar Layout.
     */
    JAR(new Jar()),

    /**
     * War Layout.
     */
    WAR(new War()),

    /**
     * Zip Layout.
     */
    ZIP(new Expanded()),

    /**
     * Directory Layout.
     */
    DIR(new Expanded()),

    /**
     * No Layout.
     */
    NONE(new None());

    private final Layout layout;

    LayoutType(Layout layout) {
      this.layout = layout;
    }

    public Layout layout() {
      return this.layout;
    }

  }

}
