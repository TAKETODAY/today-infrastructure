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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlatformExtension;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPublication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.build.bom.Library.Group;
import cn.taketoday.build.bom.Library.Module;
import cn.taketoday.build.maven.MavenRepositoryPlugin;
import cn.taketoday.lang.Nullable;
import groovy.namespace.QName;
import groovy.util.Node;

/**
 * {@link Plugin} for defining a bom. Dependencies are added as constraints in the
 * {@code api} configuration. Imported boms are added as enforced platforms in the
 * {@code api} configuration.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BomPlugin implements Plugin<Project> {

  static final String API_ENFORCED_CONFIGURATION_NAME = "apiEnforced";

  @Override
  public void apply(Project project) {
    PluginContainer plugins = project.getPlugins();
    plugins.apply(MavenRepositoryPlugin.class);
    plugins.apply(JavaPlatformPlugin.class);
    JavaPlatformExtension javaPlatform = project.getExtensions().getByType(JavaPlatformExtension.class);
    javaPlatform.allowDependencies();
    createApiEnforcedConfiguration(project);
    BomExtension bom = project.getExtensions()
            .create("bom", BomExtension.class, project.getDependencies(), project);
    project.getTasks().create("bomrCheck", CheckBom.class, bom);
    new PublishingCustomizer(project, bom).customize();
  }

  private void createApiEnforcedConfiguration(Project project) {
    Configuration apiEnforced = project.getConfigurations()
            .create(API_ENFORCED_CONFIGURATION_NAME, (configuration) -> {
              configuration.setCanBeConsumed(false);
              configuration.setCanBeResolved(false);
              configuration.setVisible(false);
            });
    project.getConfigurations()
            .getByName(JavaPlatformPlugin.ENFORCED_API_ELEMENTS_CONFIGURATION_NAME)
            .extendsFrom(apiEnforced);
    project.getConfigurations()
            .getByName(JavaPlatformPlugin.ENFORCED_RUNTIME_ELEMENTS_CONFIGURATION_NAME)
            .extendsFrom(apiEnforced);
  }

  private static final class PublishingCustomizer {

    private final Project project;

    private final BomExtension bom;

    private PublishingCustomizer(Project project, BomExtension bom) {
      this.project = project;
      this.bom = bom;
    }

    private void customize() {
      PublishingExtension publishing = this.project.getExtensions().getByType(PublishingExtension.class);
      publishing.getPublications().withType(MavenPublication.class).all(this::configurePublication);
    }

    private void configurePublication(MavenPublication publication) {
      publication.pom(this::customizePom);
    }

    @SuppressWarnings("unchecked")
    private void customizePom(MavenPom pom) {
      pom.withXml(xml -> {
        Node projectNode = xml.asNode();
        Node properties = new Node(null, "properties");
        bom.getProperties().forEach(properties::appendNode);
        Node dependencyManagement = findChild(projectNode, "dependencyManagement");
        if (dependencyManagement != null) {
          addPropertiesBeforeDependencyManagement(projectNode, properties);
          addClassifiedManagedDependencies(dependencyManagement);
          replaceVersionsWithVersionPropertyReferences(dependencyManagement);
          addExclusionsToManagedDependencies(dependencyManagement);
          addTypesToManagedDependencies(dependencyManagement);
        }
        else {
          projectNode.children().add(properties);
        }
        addPluginManagement(projectNode);
      });
    }

    @SuppressWarnings("unchecked")
    private void addPropertiesBeforeDependencyManagement(Node projectNode, Node properties) {
      int i = 0;
      List<Object> children = projectNode.children();
      for (Object child : children) {
        if (isNodeWithName(child, "dependencyManagement")) {
          children.add(i, properties);
          break;
        }
        i++;
      }
    }

    private void replaceVersionsWithVersionPropertyReferences(Node dependencyManagement) {
      Node dependencies = findChild(dependencyManagement, "dependencies");
      if (dependencies != null) {
        for (Node dependency : findChildren(dependencies, "dependency")) {
          String groupId = child(dependency, "groupId").text();
          String artifactId = child(dependency, "artifactId").text();
          Node classifierNode = findChild(dependency, "classifier");
          String classifier = (classifierNode != null) ? classifierNode.text() : "";
          String versionProperty = this.bom.getArtifactVersionProperty(groupId, artifactId, classifier);
          if (versionProperty != null) {
            child(dependency, "version").setValue("${" + versionProperty + "}");
          }
        }
      }
    }

    private void addExclusionsToManagedDependencies(Node dependencyManagement) {
      Node dependencies = findChild(dependencyManagement, "dependencies");
      if (dependencies != null) {
        for (Node dependency : findChildren(dependencies, "dependency")) {
          String groupId = child(dependency, "groupId").text();
          String artifactId = child(dependency, "artifactId").text();
          bom.getLibraries()
                  .stream()
                  .flatMap(library -> library.getGroups().stream())
                  .filter(group -> group.getId().equals(groupId))
                  .flatMap(group -> group.getModules().stream())
                  .filter(module -> module.getName().equals(artifactId))
                  .flatMap(module -> module.getExclusions().stream())
                  .forEach(exclusion -> {
                    Node exclusions = findOrCreateNode(dependency, "exclusions");
                    Node node = new Node(exclusions, "exclusion");
                    node.appendNode("groupId", exclusion.getGroupId());
                    node.appendNode("artifactId", exclusion.getArtifactId());
                  });
        }
      }
    }

    private void addTypesToManagedDependencies(Node dependencyManagement) {
      Node dependencies = findChild(dependencyManagement, "dependencies");
      if (dependencies != null) {
        for (Node dependency : findChildren(dependencies, "dependency")) {
          String groupId = child(dependency, "groupId").text();
          String artifactId = child(dependency, "artifactId").text();
          Set<String> types = this.bom.getLibraries()
                  .stream()
                  .flatMap(library -> library.getGroups().stream())
                  .filter(group -> group.getId().equals(groupId))
                  .flatMap(group -> group.getModules().stream())
                  .filter(module -> module.getName().equals(artifactId))
                  .map(Library.Module::getType)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet());
          if (types.size() > 1) {
            throw new IllegalStateException(
                    "Multiple types for " + groupId + ":" + artifactId + ": " + types);
          }
          if (types.size() == 1) {
            String type = types.iterator().next();
            dependency.appendNode("type", type);
          }
        }
      }
    }

    @SuppressWarnings("unchecked")
    private void addClassifiedManagedDependencies(Node dependencyManagement) {
      Node dependencies = findChild(dependencyManagement, "dependencies");
      if (dependencies != null) {
        for (Node dependency : findChildren(dependencies, "dependency")) {
          String groupId = child(dependency, "groupId").text();
          String version = child(dependency, "version").text();
          String artifactId = child(dependency, "artifactId").text();
          Set<String> classifiers = bom.getLibraries()
                  .stream()
                  .flatMap(library -> library.getGroups().stream())
                  .filter(group -> group.getId().equals(groupId))
                  .flatMap(group -> group.getModules().stream())
                  .filter(module -> module.getName().equals(artifactId))
                  .map(Module::getClassifier)
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet());
          Node target = dependency;
          for (String classifier : classifiers) {
            if (!classifier.isEmpty()) {
              if (target == null) {
                target = new Node(null, "dependency");
                target.appendNode("groupId", groupId);
                target.appendNode("artifactId", artifactId);
                target.appendNode("version", version);
                int index = dependency.parent().children().indexOf(dependency);
                dependency.parent().children().add(index + 1, target);
              }
              target.appendNode("classifier", classifier);
            }
            target = null;
          }
        }
      }
    }

    private void addPluginManagement(Node projectNode) {
      for (Library library : bom.getLibraries()) {
        for (Group group : library.getGroups()) {
          Node plugins = findOrCreateNode(projectNode, "build", "pluginManagement", "plugins");
          for (String pluginName : group.getPlugins()) {
            Node plugin = new Node(plugins, "plugin");
            plugin.appendNode("groupId", group.getId());
            plugin.appendNode("artifactId", pluginName);
            String versionProperty = library.getVersionProperty();
            String value = (versionProperty != null)
                           ? "${" + versionProperty + "}"
                           : library.getVersion().getVersion().toString();
            plugin.appendNode("version", value);
          }
        }
      }
    }

    private Node findOrCreateNode(Node parent, String... path) {
      Node current = parent;
      for (String nodeName : path) {
        Node child = findChild(current, nodeName);
        if (child == null) {
          child = new Node(current, nodeName);
        }
        current = child;
      }
      return current;
    }

    private Node child(Node parent, String name) {
      Node child = findChild(parent, name);
      if (child == null) {
        throw new IllegalStateException(name + " Not Found");
      }
      return child;
    }

    @Nullable
    private Node findChild(Node parent, String name) {
      for (Object child : parent.children()) {
        if (child instanceof Node node) {
          if ((node.name() instanceof QName qname) && name.equals(qname.getLocalPart())) {
            return node;
          }
          if (name.equals(node.name())) {
            return node;
          }
        }
      }
      return null;
    }

    private List<Node> findChildren(Node parent, String name) {
      ArrayList<Node> children = new ArrayList<>();
      for (Object child : parent.children()) {
        if (isNodeWithName(child, name)) {
          children.add((Node) child);
        }
      }
      return children;
    }

    private boolean isNodeWithName(Object candidate, String name) {
      if (candidate instanceof Node node) {
        if ((node.name() instanceof QName qname) && name.equals(qname.getLocalPart())) {
          return true;
        }
        return name.equals(node.name());
      }
      return false;
    }

  }

}
