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

package cn.taketoday.gradle.testkit;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.regex.Pattern;

import cn.taketoday.util.ReflectionUtils;

/**
 * An {@link Extension} for managing the lifecycle of a {@link GradleBuild} stored in a
 * field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class GradleBuildExtension implements BeforeEachCallback, AfterEachCallback {

  private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile("\\[Gradle .+\\]");

  private final Dsl dsl = Dsl.GROOVY;

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    GradleBuild gradleBuild = extractGradleBuild(context);
    URL scriptUrl = findDefaultScript(context);
    if (scriptUrl != null) {
      gradleBuild.script(scriptUrl.getFile());
    }
    URL settingsUrl = getSettings(context);
    if (settingsUrl != null) {
      gradleBuild.settings(settingsUrl.getFile());
    }
    gradleBuild.before();
  }

  private GradleBuild extractGradleBuild(ExtensionContext context) throws Exception {
    Object testInstance = context.getRequiredTestInstance();
    Field gradleBuildField = ReflectionUtils.findField(testInstance.getClass(), "gradleBuild");
    gradleBuildField.setAccessible(true);
    return (GradleBuild) gradleBuildField.get(testInstance);
  }

  private URL findDefaultScript(ExtensionContext context) {
    URL scriptUrl = getScriptForTestMethod(context);
    if (scriptUrl != null) {
      return scriptUrl;
    }
    return getScriptForTestClass(context.getRequiredTestClass());
  }

  private URL getScriptForTestMethod(ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    String name = testClass.getSimpleName() + "-" + removeGradleVersion(context.getRequiredTestMethod().getName())
            + this.dsl.getExtension();
    return testClass.getResource(name);
  }

  private String removeGradleVersion(String methodName) {
    return GRADLE_VERSION_PATTERN.matcher(methodName).replaceAll("").trim();
  }

  private URL getScriptForTestClass(Class<?> testClass) {
    return testClass.getResource(testClass.getSimpleName() + this.dsl.getExtension());
  }

  private URL getSettings(ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    return testClass.getResource("settings.gradle");
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    extractGradleBuild(context).after();
  }

}
