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

package infra.context.aot;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectArrayAssert;
import org.junit.jupiter.api.Test;

import java.util.List;

import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.context.testfixture.context.aot.scan.noreflective.ReflectiveNotUsed;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnConstructor;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnField;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnInnerField;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnInterface;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnMethod;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnNestedType;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnRecord;
import infra.context.testfixture.context.aot.scan.reflective.ReflectiveOnType;
import infra.context.testfixture.context.aot.scan.reflective2.Reflective2OnType;
import infra.context.testfixture.context.aot.scan.reflective2.reflective21.Reflective21OnType;
import infra.context.testfixture.context.aot.scan.reflective2.reflective22.Reflective22OnType;
import infra.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/10/29 15:32
 */
class ReflectiveProcessorAotContributionBuilderTests {

  @Test
  void classesWithMatchingCandidates() {
    BeanFactoryInitializationAotContribution contribution = new ReflectiveProcessorAotContributionBuilder()
            .withClasses(List.of(String.class, ReflectiveOnInterface.class, Integer.class)).build();
    assertDetectedClasses(contribution).containsOnly(ReflectiveOnInterface.class).hasSize(1);
  }

  @Test
  void classesWithMatchingCandidatesFiltersDuplicates() {
    BeanFactoryInitializationAotContribution contribution = new ReflectiveProcessorAotContributionBuilder()
            .withClasses(List.of(ReflectiveOnField.class, ReflectiveOnInterface.class, Integer.class))
            .withClasses(new Class<?>[] { ReflectiveOnInterface.class, ReflectiveOnMethod.class, String.class })
            .build();
    assertDetectedClasses(contribution)
            .containsOnly(ReflectiveOnInterface.class, ReflectiveOnField.class, ReflectiveOnMethod.class)
            .hasSize(3);
  }

  @Test
  void scanWithMatchingCandidates() {
    String packageName = ReflectiveOnType.class.getPackageName();
    BeanFactoryInitializationAotContribution contribution = new ReflectiveProcessorAotContributionBuilder()
            .scan(getClass().getClassLoader(), packageName).build();
    assertDetectedClasses(contribution).containsOnly(ReflectiveOnType.class, ReflectiveOnInterface.class,
            ReflectiveOnRecord.class, ReflectiveOnField.class, ReflectiveOnConstructor.class,
            ReflectiveOnMethod.class, ReflectiveOnNestedType.Nested.class, ReflectiveOnInnerField.Inner.class);
  }

  @Test
  void scanWithMatchingCandidatesInSubPackages() {
    String packageName = Reflective2OnType.class.getPackageName();
    BeanFactoryInitializationAotContribution contribution = new ReflectiveProcessorAotContributionBuilder()
            .scan(getClass().getClassLoader(), packageName).build();
    assertDetectedClasses(contribution).containsOnly(Reflective2OnType.class,
            Reflective21OnType.class, Reflective22OnType.class);
  }

  @Test
  void scanWithNoCandidate() {
    String packageName = ReflectiveNotUsed.class.getPackageName();
    BeanFactoryInitializationAotContribution contribution = new ReflectiveProcessorAotContributionBuilder()
            .scan(getClass().getClassLoader(), packageName).build();
    assertThat(contribution).isNull();
  }

  @Test
  void classesAndScanWithDuplicatesFiltersThem() {
    BeanFactoryInitializationAotContribution contribution = new ReflectiveProcessorAotContributionBuilder()
            .withClasses(List.of(ReflectiveOnField.class, ReflectiveOnInterface.class, Integer.class))
            .withClasses(new Class<?>[] { ReflectiveOnInterface.class, ReflectiveOnMethod.class, String.class })
            .scan(null, ReflectiveOnType.class.getPackageName())
            .build();
    assertDetectedClasses(contribution)
            .containsOnly(ReflectiveOnType.class, ReflectiveOnInterface.class, ReflectiveOnRecord.class,
                    ReflectiveOnField.class, ReflectiveOnConstructor.class, ReflectiveOnMethod.class,
                    ReflectiveOnNestedType.Nested.class, ReflectiveOnInnerField.Inner.class)
            .hasSize(8);
  }

  @SuppressWarnings("rawtypes")
  private ObjectArrayAssert<Class> assertDetectedClasses(@Nullable BeanFactoryInitializationAotContribution contribution) {
    assertThat(contribution).isNotNull();
    return assertThat(contribution).extracting("classes", InstanceOfAssertFactories.array(Class[].class));
  }

}