/*
 * Copyright 2002-present the original author or authors.
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

package infra.building.nullability;

import net.ltgt.gradle.errorprone.CheckSeverity;
import net.ltgt.gradle.errorprone.ErrorProneOptions;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

/**
 * Nullability configuration options for a {@link JavaCompile} task.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public abstract class NullabilityOptions {

  /**
   * Internal use only.
   *
   * @param errorProne the ErrorProne options to which the nullability options are
   * applied
   */
  @Inject
  public NullabilityOptions(ErrorProneOptions errorProne) {
    Provider<Checking> checkingAsEnum = getChecking()
            .map((string) -> Checking.valueOf(string.toUpperCase(Locale.ROOT)));
    errorProne.getEnabled().set(checkingAsEnum.map((checking) -> checking != Checking.DISABLED));
    errorProne.getDisableAllChecks().set(checkingAsEnum.map((checking) -> checking != Checking.DISABLED));
    errorProne.getCheckOptions().putAll(checkingAsEnum.map(this::checkOptions));
    errorProne.getChecks().putAll(checkingAsEnum.map(this::checks));
  }

  private Map<String, String> checkOptions(Checking checking) {
    if (checking == Checking.DISABLED) {
      return Collections.emptyMap();
    }
    Map<String, String> options = new LinkedHashMap<>();
    options.put("NullAway:OnlyNullMarked", "true");
    List<String> customContractAnnotations = new ArrayList<>();
    customContractAnnotations.add("infra.lang.Contract");
    if (checking == Checking.TESTS) {
      customContractAnnotations.add("org.assertj.core.internal.annotation.Contract");
    }
    options.put("NullAway:CustomContractAnnotations", String.join(",", customContractAnnotations));
    options.put("NullAway:JSpecifyMode", "true");
    options.put("NullAway:UnannotatedSubPackages", "infra.bytecode,infra.app.loader,infra.mock.web,infra.test,infra.app.test");
    if (checking == Checking.TESTS) {
      options.put("NullAway:HandleTestAssertionLibraries", "true");
    }
    return options;
  }

  private Map<String, CheckSeverity> checks(Checking checking) {
    if (checking != Checking.DISABLED) {
      return Map.of("NullAway", CheckSeverity.WARN);
    }
    return Collections.emptyMap();
  }

  /**
   * Returns the type of checking to perform.
   *
   * @return the type of checking
   */
  public abstract Property<String> getChecking();

  /**
   * The type of null checking to perform for the {@link JavaCompile} task.
   */
  enum Checking {

    /**
     * Main code nullability checking is performed.
     */
    MAIN,

    /**
     * Test code nullability checking is performed.
     */
    TESTS,

    /**
     * Nullability checking is disabled.
     */
    DISABLED

  }

}