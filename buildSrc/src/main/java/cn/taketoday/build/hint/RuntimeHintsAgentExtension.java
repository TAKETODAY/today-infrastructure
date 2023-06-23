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

package cn.taketoday.build.hint;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;

import java.util.Collections;

/**
 * Entry point to the DSL extension for the {@link RuntimeHintsAgentPlugin} Gradle plugin.
 *
 * @author Brian Clozel
 */
public class RuntimeHintsAgentExtension {

  private final SetProperty<String> includedPackages;

  private final SetProperty<String> excludedPackages;

  public RuntimeHintsAgentExtension(ObjectFactory objectFactory) {
    this.includedPackages = objectFactory.setProperty(String.class).convention(Collections.singleton("cn.taketoday"));
    this.excludedPackages = objectFactory.setProperty(String.class).convention(Collections.emptySet());
  }

  public SetProperty<String> getIncludedPackages() {
    return this.includedPackages;
  }

  public SetProperty<String> getExcludedPackages() {
    return this.excludedPackages;
  }

  String asJavaAgentArgument() {
    StringBuilder builder = new StringBuilder();
    this.includedPackages.get().forEach(packageName -> builder.append('+').append(packageName).append(','));
    this.excludedPackages.get().forEach(packageName -> builder.append('-').append(packageName).append(','));
    return builder.toString();
  }
}
