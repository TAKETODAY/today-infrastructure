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

package infra.gradle.junit;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;

import infra.gradle.testkit.GradleBuild;
import infra.util.ReflectionUtils;

/**
 * {@link BeforeEachCallback} to set a test class's {@code gradleBuild} field prior to
 * test execution.
 *
 * @author Andy Wilkinson
 */
final class GradleBuildFieldSetter implements BeforeEachCallback {

  private final GradleBuild gradleBuild;

  GradleBuildFieldSetter(GradleBuild gradleBuild) {
    this.gradleBuild = gradleBuild;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    Field field = ReflectionUtils.findField(context.getRequiredTestClass(), "gradleBuild");
    field.setAccessible(true);
    field.set(context.getRequiredTestInstance(), this.gradleBuild);

  }

}
