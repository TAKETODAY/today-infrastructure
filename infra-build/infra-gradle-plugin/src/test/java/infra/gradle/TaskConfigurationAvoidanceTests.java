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

package infra.gradle;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import org.gradle.api.Action;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tests that verify the plugin's compliance with task configuration avoidance.
 *
 * @author Andy Wilkinson
 */
@AnalyzeClasses(packages = "infra.gradle",
                importOptions = TaskConfigurationAvoidanceTests.DoNotIncludeTests.class)
class TaskConfigurationAvoidanceTests {

  @ArchTest
  void noApisThatCauseEagerTaskConfigurationShouldBeCalled(JavaClasses classes) {
    ProhibitedMethods prohibited = new ProhibitedMethods();
    prohibited.on(TaskContainer.class)
            .methodsNamed("create", "findByPath, getByPath")
            .method("withType", Class.class, Action.class);
    prohibited.on(TaskCollection.class).methodsNamed("findByName", "getByName");
    ArchRuleDefinition.noClasses()
            .should()
            .callMethodWhere(DescribedPredicate.describe("it would cause eager task configuration", prohibited))
            .check(classes);
  }

  static class DoNotIncludeTests implements ImportOption {

    @Override
    public boolean includes(Location location) {
      return !location.matches(Pattern.compile(".*Tests\\.class"));
    }

  }

  private static final class ProhibitedMethods implements Predicate<JavaMethodCall> {

    private final List<Predicate<JavaMethodCall>> prohibited = new ArrayList<>();

    private ProhibitedConfigurer on(Class<?> type) {
      return new ProhibitedConfigurer(type);
    }

    @Override
    public boolean apply(JavaMethodCall methodCall) {
      for (Predicate<JavaMethodCall> spec : this.prohibited) {
        if (spec.apply(methodCall)) {
          return true;
        }
      }
      return false;
    }

    private final class ProhibitedConfigurer {

      private final Class<?> type;

      private ProhibitedConfigurer(Class<?> type) {
        this.type = type;
      }

      private ProhibitedConfigurer methodsNamed(String... names) {
        for (String name : names) {
          ProhibitedMethods.this.prohibited.add(new ProhibitMethodsNamed(this.type, name));
        }
        return this;
      }

      private ProhibitedConfigurer method(String name, Class<?>... parameterTypes) {
        ProhibitedMethods.this.prohibited
                .add(new ProhibitMethod(this.type, name, Arrays.asList(parameterTypes)));
        return this;
      }

    }

    static class ProhibitMethodsNamed implements Predicate<JavaMethodCall> {

      private final Class<?> owner;

      private final String name;

      ProhibitMethodsNamed(Class<?> owner, String name) {
        this.owner = owner;
        this.name = name;
      }

      @Override
      public boolean apply(JavaMethodCall methodCall) {
        return methodCall.getTargetOwner().isEquivalentTo(this.owner) && methodCall.getName().equals(this.name);
      }

    }

    private static final class ProhibitMethod extends ProhibitMethodsNamed {

      private final List<Class<?>> parameterTypes;

      private ProhibitMethod(Class<?> owner, String name, List<Class<?>> parameterTypes) {
        super(owner, name);
        this.parameterTypes = parameterTypes;
      }

      @Override
      public boolean apply(JavaMethodCall methodCall) {
        return super.apply(methodCall) && match(methodCall.getTarget().getParameterTypes());
      }

      private boolean match(List<JavaType> callParameterTypes) {
        if (this.parameterTypes.size() != callParameterTypes.size()) {
          return false;
        }
        for (int i = 0; i < this.parameterTypes.size(); i++) {
          if (!callParameterTypes.get(i).toErasure().isEquivalentTo(this.parameterTypes.get(i))) {
            return false;
          }
        }
        return true;
      }

    }

  }

}
