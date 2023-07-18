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

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.TaskAction;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import cn.taketoday.build.bom.Library.Group;
import cn.taketoday.build.bom.version.DependencyVersion;

/**
 * Checks the validity of a bom.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CheckBom extends DefaultTask {

  private final BomExtension bom;

  @Inject
  public CheckBom(BomExtension bom) {
    this.bom = bom;
  }

  @TaskAction
  void checkBom() {
    for (Library library : this.bom.getLibraries()) {
      for (Group group : library.getGroups()) {
        for (Library.Module module : group.getModules()) {
          if (!module.getExclusions().isEmpty()) {
            checkExclusions(group.getId(), module, library.getVersion().getVersion());
          }
        }
      }
    }
  }

  private void checkExclusions(String groupId, Library.Module module, DependencyVersion version) {
    Set<String> resolved = getProject().getConfigurations()
            .detachedConfiguration(getProject().getDependencies().create(groupId + ":" + module.getName() + ":" + version))
            .getResolvedConfiguration()
            .getResolvedArtifacts()
            .stream()
            .map(artifact -> artifact.getModuleVersion().getId())
            .map(id -> id.getGroup() + ":" + id.getModule().getName())
            .collect(Collectors.toSet());
    Set<String> exclusions = module.getExclusions()
            .stream()
            .map(exclusion -> exclusion.getGroupId() + ":" + exclusion.getArtifactId())
            .collect(Collectors.toSet());
    TreeSet<String> unused = new TreeSet<>();
    for (String exclusion : exclusions) {
      if (!resolved.contains(exclusion)) {
        if (exclusion.endsWith(":*")) {
          String group = exclusion.substring(0, exclusion.indexOf(':') + 1);
          if (resolved.stream().noneMatch(candidate -> candidate.startsWith(group))) {
            unused.add(exclusion);
          }
        }
        else {
          unused.add(exclusion);
        }
      }
    }
    exclusions.removeAll(resolved);
    if (!unused.isEmpty()) {
      throw new InvalidUserDataException(
              "Unnecessary exclusions on " + groupId + ":" + module.getName() + ": " + exclusions);
    }
  }

}
