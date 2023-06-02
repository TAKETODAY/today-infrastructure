/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.domain;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.ClassPathScanningCandidateComponentProvider;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.framework.domain.scan.a.EmbeddableA;
import cn.taketoday.framework.domain.scan.a.EntityA;
import cn.taketoday.framework.domain.scan.b.EmbeddableB;
import cn.taketoday.framework.domain.scan.b.EntityB;
import cn.taketoday.framework.domain.scan.c.EmbeddableC;
import cn.taketoday.framework.domain.scan.c.EntityC;
import cn.taketoday.test.util.TestPropertyValues;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityScanner}.
 *
 * @author Phillip Webb
 */
class EntityScannerTests {

  @Test
  void createWhenContextIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new EntityScanner(null))
            .withMessageContaining("Context must not be null");
  }

  @Test
  void scanShouldScanFromSinglePackage() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
    EntityScanner scanner = new EntityScanner(context);
    Set<Class<?>> scanned = scanner.scan(Entity.class);
    assertThat(scanned).containsOnly(EntityA.class, EntityB.class, EntityC.class);
    context.close();
  }

  @Test
  void scanShouldScanFromResolvedPlaceholderPackage() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    TestPropertyValues.of("com.example.entity-package=cn.taketoday.framework.domain.scan")
            .applyTo(context);
    context.register(ScanPlaceholderConfig.class);
    context.refresh();
    EntityScanner scanner = new EntityScanner(context);
    Set<Class<?>> scanned = scanner.scan(Entity.class);
    assertThat(scanned).containsOnly(EntityA.class, EntityB.class, EntityC.class);
    context.close();
  }

  @Test
  void scanShouldScanFromMultiplePackages() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanAConfig.class,
            ScanBConfig.class);
    EntityScanner scanner = new EntityScanner(context);
    Set<Class<?>> scanned = scanner.scan(Entity.class);
    assertThat(scanned).containsOnly(EntityA.class, EntityB.class);
    context.close();
  }

  @Test
  void scanShouldFilterOnAnnotation() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
    EntityScanner scanner = new EntityScanner(context);
    assertThat(scanner.scan(Entity.class)).containsOnly(EntityA.class, EntityB.class, EntityC.class);
    assertThat(scanner.scan(Embeddable.class)).containsOnly(EmbeddableA.class, EmbeddableB.class,
            EmbeddableC.class);
    assertThat(scanner.scan(Entity.class, Embeddable.class)).containsOnly(EntityA.class, EntityB.class,
            EntityC.class, EmbeddableA.class, EmbeddableB.class, EmbeddableC.class);
    context.close();
  }

  @Test
  void scanShouldUseCustomCandidateComponentProvider() throws ClassNotFoundException {
    ClassPathScanningCandidateComponentProvider candidateComponentProvider = mock(
            ClassPathScanningCandidateComponentProvider.class);
    given(candidateComponentProvider.findCandidateComponents("cn.taketoday.framework.domain.scan"))
            .willReturn(Collections.emptySet());
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
    TestEntityScanner scanner = new TestEntityScanner(context, candidateComponentProvider);
    scanner.scan(Entity.class);
    ArgumentCaptor<AnnotationTypeFilter> annotationTypeFilter = ArgumentCaptor.forClass(AnnotationTypeFilter.class);
    then(candidateComponentProvider).should().addIncludeFilter(annotationTypeFilter.capture());
    then(candidateComponentProvider).should()
            .findCandidateComponents("cn.taketoday.framework.domain.scan");
    then(candidateComponentProvider).shouldHaveNoMoreInteractions();
    assertThat(annotationTypeFilter.getValue().getAnnotationType()).isEqualTo(Entity.class);
  }

  @Test
  void scanShouldScanCommaSeparatedPackagesInPlaceholderPackage() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    TestPropertyValues.of(
                    "com.example.entity-package=cn.taketoday.framework.domain.scan.a,cn.taketoday.framework.domain.scan.b")
            .applyTo(context);
    context.register(ScanPlaceholderConfig.class);
    context.refresh();
    EntityScanner scanner = new EntityScanner(context);
    Set<Class<?>> scanned = scanner.scan(Entity.class);
    assertThat(scanned).containsOnly(EntityA.class, EntityB.class);
    context.close();
  }

  private static class TestEntityScanner extends EntityScanner {

    private final ClassPathScanningCandidateComponentProvider candidateComponentProvider;

    TestEntityScanner(ApplicationContext context,
            ClassPathScanningCandidateComponentProvider candidateComponentProvider) {
      super(context);
      this.candidateComponentProvider = candidateComponentProvider;
    }

    @Override
    protected ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
            ApplicationContext context) {
      return this.candidateComponentProvider;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EntityScan("cn.taketoday.framework.domain.scan")
  static class ScanConfig {

  }

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackageClasses = EntityA.class)
  static class ScanAConfig {

  }

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackageClasses = EntityB.class)
  static class ScanBConfig {

  }

  @Configuration(proxyBeanMethods = false)
  @EntityScan("${com.example.entity-package}")
  static class ScanPlaceholderConfig {

  }

}
