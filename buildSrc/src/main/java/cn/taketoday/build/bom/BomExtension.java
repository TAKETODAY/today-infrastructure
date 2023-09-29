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

package cn.taketoday.build.bom;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserCodeException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import cn.taketoday.build.bom.Library.Exclusion;
import cn.taketoday.build.bom.Library.Group;
import cn.taketoday.build.bom.Library.LibraryVersion;
import cn.taketoday.build.bom.Library.ProhibitedVersion;
import cn.taketoday.build.bom.version.DependencyVersion;
import cn.taketoday.build.maven.MavenExec;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FileCopyUtils;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import static cn.taketoday.build.bom.BomPlugin.API_ENFORCED_CONFIGURATION_NAME;
import static org.gradle.api.plugins.JavaPlatformPlugin.API_CONFIGURATION_NAME;

/**
 * DSL extensions for {@link BomPlugin}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BomExtension {

  /**
   * Name of the task that generates the deployed pom file.
   */
  public static final String GENERATE_POM_TASK_NAME = "generatePomFileForMavenPublication";

  private final LinkedHashMap<String, DependencyVersion> properties = new LinkedHashMap<>();

  private final HashMap<String, String> artifactVersionProperties = new HashMap<>();

  private final ArrayList<Library> libraries = new ArrayList<>();

  private final DependencyHandler dependencyHandler;

  private final Project project;

  private final ObjectFactory objectFactory;

  public BomExtension(DependencyHandler dependencyHandler, Project project) {
    this.project = project;
    this.objectFactory = project.getObjects();
    this.dependencyHandler = dependencyHandler;
  }

  public List<Library> getLibraries() {
    return libraries;
  }

  public void library(String name, Action<LibraryHandler> action) {
    library(name, null, action);
  }

  public void library(String name, @Nullable String version, Action<LibraryHandler> action) {
    LibraryHandler libraryHandler = objectFactory.newInstance(LibraryHandler.class, (version != null) ? version : "");
    action.execute(libraryHandler);
    LibraryVersion libraryVersion = new LibraryVersion(DependencyVersion.parse(libraryHandler.version));
    addLibrary(new Library(name, libraryVersion, libraryHandler.groups, libraryHandler.prohibitedVersions));
  }

  public void effectiveBomArtifact() {
    Configuration effectiveBomConfiguration = project.getConfigurations().create("effectiveBom");
    project.getTasks().matching(task -> task.getName().equals(GENERATE_POM_TASK_NAME)).forEach(task -> {
      Sync syncBom = project.getTasks().create("syncBom", Sync.class);
      syncBom.dependsOn(task);
      File generatedBomDir = new File(project.getBuildDir(), "generated/bom");
      syncBom.setDestinationDir(generatedBomDir);
      syncBom.from(((GenerateMavenPom) task).getDestination(), (pom) -> pom.rename((name) -> "pom.xml"));
      try {
        String settingsXmlContent = FileCopyUtils.copyToString(new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream("effective-bom-settings.xml"),
                        StandardCharsets.UTF_8))
                .replace("localRepositoryPath",
                        new File(project.getBuildDir(), "local-m2-repository").getAbsolutePath());
        syncBom.from(project.getResources().getText().fromString(settingsXmlContent),
                (settingsXml) -> settingsXml.rename((name) -> "settings.xml"));
      }
      catch (IOException ex) {
        throw new GradleException("Failed to prepare settings.xml", ex);
      }
      MavenExec generateEffectiveBom = project.getTasks()
              .create("generateEffectiveBom", MavenExec.class);
      generateEffectiveBom.setProjectDir(generatedBomDir);
      File effectiveBom = new File(project.getBuildDir(),
              "generated/effective-bom/" + project.getName() + "-effective-bom.xml");
      generateEffectiveBom.args("--settings", "settings.xml", "help:effective-pom",
              "-Doutput=" + effectiveBom);
      generateEffectiveBom.dependsOn(syncBom);
      generateEffectiveBom.getOutputs().file(effectiveBom);
      generateEffectiveBom.doLast(new StripUnrepeatableOutputAction(effectiveBom));
      project.getArtifacts()
              .add(effectiveBomConfiguration.getName(), effectiveBom,
                      (artifact) -> artifact.builtBy(generateEffectiveBom));
    });
  }

  private String createDependencyNotation(String groupId, String artifactId, DependencyVersion version) {
    return groupId + ":" + artifactId + ":" + version;
  }

  Map<String, DependencyVersion> getProperties() {
    return properties;
  }

  String getArtifactVersionProperty(String groupId, String artifactId, String classifier) {
    String coordinates = groupId + ":" + artifactId + ":" + classifier;
    return artifactVersionProperties.get(coordinates);
  }

  private void putArtifactVersionProperty(String groupId, String artifactId, String versionProperty) {
    putArtifactVersionProperty(groupId, artifactId, null, versionProperty);
  }

  private void putArtifactVersionProperty(String groupId, String artifactId, String classifier,
          String versionProperty) {
    String coordinates = groupId + ":" + artifactId + ":" + ((classifier != null) ? classifier : "");
    String existing = artifactVersionProperties.putIfAbsent(coordinates, versionProperty);
    if (existing != null) {
      throw new InvalidUserDataException("Cannot put version property for '" + coordinates
              + "'. Version property '" + existing + "' has already been stored.");
    }
  }

  private void addLibrary(Library library) {
    libraries.add(library);
    String versionProperty = library.getVersionProperty();
    if (versionProperty != null) {
      properties.put(versionProperty, library.getVersion().getVersion());
    }
    for (Group group : library.getGroups()) {
      for (Library.Module module : group.getModules()) {
        putArtifactVersionProperty(group.getId(), module.getName(), module.getClassifier(), versionProperty);
        dependencyHandler.getConstraints()
                .add(API_CONFIGURATION_NAME, createDependencyNotation(group.getId(),
                        module.getName(), library.getVersion().getVersion()));
      }
      for (String bomImport : group.getBoms()) {
        putArtifactVersionProperty(group.getId(), bomImport, versionProperty);
        String bomDependency = createDependencyNotation(group.getId(), bomImport, library.getVersion().getVersion());
        dependencyHandler.add(API_CONFIGURATION_NAME, dependencyHandler.platform(bomDependency));
        dependencyHandler.add(API_ENFORCED_CONFIGURATION_NAME, dependencyHandler.enforcedPlatform(bomDependency));
      }
    }
  }

  public static class LibraryHandler {

    private final ArrayList<Group> groups = new ArrayList<>();

    private final ArrayList<ProhibitedVersion> prohibitedVersions = new ArrayList<>();

    private String version;

    @Inject
    public LibraryHandler(String version) {
      this.version = version;
    }

    public void version(String version) {
      this.version = version;
    }

    public void group(String id, Action<GroupHandler> action) {
      GroupHandler groupHandler = new GroupHandler(id);
      action.execute(groupHandler);
      groups.add(new Group(groupHandler.id, groupHandler.modules, groupHandler.plugins, groupHandler.imports));
    }

    public void prohibit(Action<ProhibitedHandler> action) {
      ProhibitedHandler handler = new ProhibitedHandler();
      action.execute(handler);
      this.prohibitedVersions.add(new ProhibitedVersion(handler.versionRange, handler.startsWith,
              handler.endsWith, handler.contains, handler.reason));
    }

    public static class ProhibitedHandler {

      private String reason;

      private final List<String> startsWith = new ArrayList<>();

      private final List<String> endsWith = new ArrayList<>();

      private final List<String> contains = new ArrayList<>();

      private VersionRange versionRange;

      public void versionRange(String versionRange) {
        try {
          this.versionRange = VersionRange.createFromVersionSpec(versionRange);
        }
        catch (InvalidVersionSpecificationException ex) {
          throw new InvalidUserCodeException("Invalid version range", ex);
        }
      }

      public void startsWith(String startsWith) {
        this.startsWith.add(startsWith);
      }

      public void startsWith(Collection<String> startsWith) {
        this.startsWith.addAll(startsWith);
      }

      public void endsWith(String endsWith) {
        this.endsWith.add(endsWith);
      }

      public void endsWith(Collection<String> endsWith) {
        this.endsWith.addAll(endsWith);
      }

      public void contains(String contains) {
        this.contains.add(contains);
      }

      public void contains(List<String> contains) {
        this.contains.addAll(contains);
      }

      public void because(String because) {
        this.reason = because;
      }

    }

    public static class GroupHandler extends GroovyObjectSupport {

      private final String id;
      private List<String> imports = Collections.emptyList();
      private List<String> plugins = Collections.emptyList();
      private List<Library.Module> modules = Collections.emptyList();

      public GroupHandler(String id) {
        this.id = id;
      }

      public void setModules(List<Object> modules) {
        this.modules = new ArrayList<>();
        for (Object input : modules) {
          if (input instanceof Library.Module module) {
            this.modules.add(module);
          }
          else {
            this.modules.add(new Library.Module((String) input));
          }
        }
      }

      public void setImports(List<String> imports) {
        this.imports = imports;
      }

      public void setPlugins(List<String> plugins) {
        this.plugins = plugins;
      }

      public Library.Module methodMissing(String name, Object args) {
        if (args instanceof Object[] && ((Object[]) args).length == 1) {
          Object arg = ((Object[]) args)[0];
          if (arg instanceof Closure<?> closure) {
            ModuleHandler moduleHandler = new ModuleHandler();
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.setDelegate(moduleHandler);
            closure.call(moduleHandler);
            return new Library.Module(name, moduleHandler.type, moduleHandler.classifier, moduleHandler.exclusions);
          }
        }
        throw new InvalidUserDataException("Invalid configuration for module '" + name + "'");
      }

      public static class ModuleHandler {

        private final List<Exclusion> exclusions = new ArrayList<>();

        private String type;

        private String classifier;

        public void exclude(Map<String, String> exclusion) {
          this.exclusions.add(new Exclusion(exclusion.get("group"), exclusion.get("module")));
        }

        public void setType(String type) {
          this.type = type;
        }

        public void setClassifier(String classifier) {
          this.classifier = classifier;
        }

      }

    }

  }

  private static final class StripUnrepeatableOutputAction implements Action<Task> {

    private final File effectiveBom;

    private StripUnrepeatableOutputAction(File xmlFile) {
      this.effectiveBom = xmlFile;
    }

    @Override
    public void execute(Task task) {
      try {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this.effectiveBom);
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList comments = (NodeList) xpath.evaluate("//comment()", document, XPathConstants.NODESET);
        for (int i = 0; i < comments.getLength(); i++) {
          org.w3c.dom.Node comment = comments.item(i);
          comment.getParentNode().removeChild(comment);
        }
        org.w3c.dom.Node build = (org.w3c.dom.Node) xpath.evaluate("/project/build", document,
                XPathConstants.NODE);
        build.getParentNode().removeChild(build);
        org.w3c.dom.Node reporting = (org.w3c.dom.Node) xpath.evaluate("/project/reporting", document,
                XPathConstants.NODE);
        reporting.getParentNode().removeChild(reporting);
        TransformerFactory.newInstance()
                .newTransformer()
                .transform(new DOMSource(document), new StreamResult(this.effectiveBom));
      }
      catch (Exception ex) {
        throw new TaskExecutionException(task, ex);
      }
    }

  }

}
