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

package infra.test.context;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import infra.app.test.mock.mockito.MockitoTestExecutionListener;
import infra.app.test.mock.mockito.ResetMocksTestExecutionListener;
import infra.core.Ordered;
import infra.core.annotation.AnnotationConfigurationException;
import infra.test.context.event.ApplicationEventsTestExecutionListener;
import infra.test.context.event.EventPublishingTestExecutionListener;
import infra.test.context.jdbc.SqlScriptsTestExecutionListener;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;
import infra.test.context.transaction.TransactionalTestExecutionListener;
import infra.test.context.web.MockTestExecutionListener;

import static infra.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for the {@link TestExecutionListeners @TestExecutionListeners}
 * annotation, which verify:
 * <ul>
 * <li>Proper registering of {@linkplain TestExecutionListener listeners} in
 * conjunction with a {@link TestContextManager}</li>
 * <li><em>Inherited</em> functionality proposed in
 * </li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TestExecutionListenersTests {

  @Test
  void defaultListeners() {
    List<Class<?>> expected = asList(MockTestExecutionListener.class,//
            DirtiesContextBeforeModesTestExecutionListener.class,//
            ApplicationEventsTestExecutionListener.class,//
            MockitoTestExecutionListener.class,
            DependencyInjectionTestExecutionListener.class,//
            DirtiesContextTestExecutionListener.class,//
            TransactionalTestExecutionListener.class,//
            SqlScriptsTestExecutionListener.class,//
            EventPublishingTestExecutionListener.class,
            ResetMocksTestExecutionListener.class
    );
    assertRegisteredListeners(DefaultListenersTestCase.class, expected);
  }

  /**
   * @since 4.0
   */
  @Test
  void defaultListenersMergedWithCustomListenerPrepended() {
    List<Class<?>> expected = asList(QuuxTestExecutionListener.class,//
            MockTestExecutionListener.class,//
            DirtiesContextBeforeModesTestExecutionListener.class,//
            ApplicationEventsTestExecutionListener.class,//
            MockitoTestExecutionListener.class,
            DependencyInjectionTestExecutionListener.class,//
            DirtiesContextTestExecutionListener.class,//
            TransactionalTestExecutionListener.class,//
            SqlScriptsTestExecutionListener.class,//
            EventPublishingTestExecutionListener.class,

            ResetMocksTestExecutionListener.class

    );
    assertRegisteredListeners(MergedDefaultListenersWithCustomListenerPrependedTestCase.class, expected);
  }

  /**
   * @since 4.0
   */
  @Test
  void defaultListenersMergedWithCustomListenerAppended() {
    List<Class<?>> expected = asList(MockTestExecutionListener.class,//
            DirtiesContextBeforeModesTestExecutionListener.class,//
            ApplicationEventsTestExecutionListener.class,//
            MockitoTestExecutionListener.class,
            DependencyInjectionTestExecutionListener.class,//
            DirtiesContextTestExecutionListener.class,//
            TransactionalTestExecutionListener.class,
            SqlScriptsTestExecutionListener.class,//
            EventPublishingTestExecutionListener.class,//
            ResetMocksTestExecutionListener.class,
            BazTestExecutionListener.class
    );
    assertRegisteredListeners(MergedDefaultListenersWithCustomListenerAppendedTestCase.class, expected);
  }

  /**
   * @since 4.0
   */
  @Test
  void defaultListenersMergedWithCustomListenerInserted() {
    List<Class<?>> expected = asList(MockTestExecutionListener.class,//
            DirtiesContextBeforeModesTestExecutionListener.class,//
            ApplicationEventsTestExecutionListener.class,//
            MockitoTestExecutionListener.class,
            DependencyInjectionTestExecutionListener.class,//
            BarTestExecutionListener.class,//
            DirtiesContextTestExecutionListener.class,//
            TransactionalTestExecutionListener.class,//
            SqlScriptsTestExecutionListener.class,//
            EventPublishingTestExecutionListener.class,

            ResetMocksTestExecutionListener.class
    );
    assertRegisteredListeners(MergedDefaultListenersWithCustomListenerInsertedTestCase.class, expected);
  }

  @Test
  void nonInheritedDefaultListeners() {
    assertRegisteredListeners(NonInheritedDefaultListenersTestCase.class, List.of(QuuxTestExecutionListener.class));
  }

  @Test
  void inheritedDefaultListeners() {
    assertRegisteredListeners(InheritedDefaultListenersTestCase.class, List.of(QuuxTestExecutionListener.class));
    assertRegisteredListeners(SubInheritedDefaultListenersTestCase.class, List.of(QuuxTestExecutionListener.class));
    assertRegisteredListeners(SubSubInheritedDefaultListenersTestCase.class,
            asList(QuuxTestExecutionListener.class, EnigmaTestExecutionListener.class));
  }

  @Test
  void customListeners() {
    assertNumRegisteredListeners(ExplicitListenersTestCase.class, 3);
  }

  @Test
  void customListenersDeclaredOnInterface() {
    assertRegisteredListeners(ExplicitListenersOnTestInterfaceTestCase.class,
            asList(FooTestExecutionListener.class, BarTestExecutionListener.class));
  }

  @Test
  void nonInheritedListeners() {
    assertNumRegisteredListeners(NonInheritedListenersTestCase.class, 1);
  }

  @Test
  void inheritedListeners() {
    assertNumRegisteredListeners(InheritedListenersTestCase.class, 4);
  }

  @Test
  void customListenersRegisteredViaMetaAnnotation() {
    assertNumRegisteredListeners(MetaTestCase.class, 3);
  }

  @Test
  void nonInheritedListenersRegisteredViaMetaAnnotation() {
    assertNumRegisteredListeners(MetaNonInheritedListenersTestCase.class, 1);
  }

  @Test
  void inheritedListenersRegisteredViaMetaAnnotation() {
    assertNumRegisteredListeners(MetaInheritedListenersTestCase.class, 4);
  }

  @Test
  void customListenersRegisteredViaMetaAnnotationWithOverrides() {
    assertNumRegisteredListeners(MetaWithOverridesTestCase.class, 3);
  }

  @Test
  void customsListenersRegisteredViaMetaAnnotationWithInheritedListenersWithOverrides() {
    assertNumRegisteredListeners(MetaInheritedListenersWithOverridesTestCase.class, 5);
  }

  @Test
  void customListenersRegisteredViaMetaAnnotationWithNonInheritedListenersWithOverrides() {
    assertNumRegisteredListeners(MetaNonInheritedListenersWithOverridesTestCase.class, 8);
  }

  @Test
  void listenersAndValueAttributesDeclared() {
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
            new TestContextManager(DuplicateListenersConfigTestCase.class));
  }

  private List<Class<?>> classes(TestContextManager testContextManager) {
    return testContextManager.getTestExecutionListeners().stream().map(Object::getClass).collect(toList());
  }

  private List<String> names(List<Class<?>> classes) {
    return classes.stream().map(Class::getSimpleName).collect(toList());
  }

  private void assertRegisteredListeners(Class<?> testClass, List<Class<?>> expected) {
    TestContextManager testContextManager = new TestContextManager(testClass);
    assertThat(names(classes(testContextManager)))
            .as("TELs registered for " + testClass.getSimpleName())
            .isEqualTo(names(expected));
  }

  private void assertNumRegisteredListeners(Class<?> testClass, int expected) {
    TestContextManager testContextManager = new TestContextManager(testClass);
    assertThat(testContextManager.getTestExecutionListeners().size()).as("Num registered TELs for " + testClass).isEqualTo(expected);
  }

  // -------------------------------------------------------------------

  static class DefaultListenersTestCase {
  }

  @TestExecutionListeners(
          listeners = { QuuxTestExecutionListener.class, DependencyInjectionTestExecutionListener.class },
          mergeMode = MERGE_WITH_DEFAULTS)
  static class MergedDefaultListenersWithCustomListenerPrependedTestCase {
  }

  @TestExecutionListeners(listeners = BazTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
  static class MergedDefaultListenersWithCustomListenerAppendedTestCase {
  }

  @TestExecutionListeners(listeners = BarTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
  static class MergedDefaultListenersWithCustomListenerInsertedTestCase {
  }

  @TestExecutionListeners(QuuxTestExecutionListener.class)
  static class InheritedDefaultListenersTestCase extends DefaultListenersTestCase {
  }

  static class SubInheritedDefaultListenersTestCase extends InheritedDefaultListenersTestCase {
  }

  @TestExecutionListeners(EnigmaTestExecutionListener.class)
  static class SubSubInheritedDefaultListenersTestCase extends SubInheritedDefaultListenersTestCase {
  }

  @TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
  static class NonInheritedDefaultListenersTestCase extends InheritedDefaultListenersTestCase {
  }

  @TestExecutionListeners(
          { FooTestExecutionListener.class, BarTestExecutionListener.class, BazTestExecutionListener.class })
  static class ExplicitListenersTestCase {
  }

  @TestExecutionListeners(QuuxTestExecutionListener.class)
  static class InheritedListenersTestCase extends ExplicitListenersTestCase {
  }

  @TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
  static class NonInheritedListenersTestCase extends InheritedListenersTestCase {
  }

  @TestExecutionListeners({ FooTestExecutionListener.class, BarTestExecutionListener.class })
  interface ExplicitListenersTestInterface {
  }

  static class ExplicitListenersOnTestInterfaceTestCase implements ExplicitListenersTestInterface {
  }

  @TestExecutionListeners(listeners = FooTestExecutionListener.class, value = BarTestExecutionListener.class)
  static class DuplicateListenersConfigTestCase {
  }

  @TestExecutionListeners({
          FooTestExecutionListener.class,
          BarTestExecutionListener.class,
          BazTestExecutionListener.class
  })
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaListeners {
  }

  @TestExecutionListeners(QuuxTestExecutionListener.class)
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaInheritedListeners {
  }

  @TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaNonInheritedListeners {
  }

  @TestExecutionListeners
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaListenersWithOverrides {

    Class<? extends TestExecutionListener>[] listeners() default
            { FooTestExecutionListener.class, BarTestExecutionListener.class };
  }

  @TestExecutionListeners
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaInheritedListenersWithOverrides {

    Class<? extends TestExecutionListener>[] listeners() default QuuxTestExecutionListener.class;

    boolean inheritListeners() default true;
  }

  @TestExecutionListeners
  @Retention(RetentionPolicy.RUNTIME)
  @interface MetaNonInheritedListenersWithOverrides {

    Class<? extends TestExecutionListener>[] listeners() default QuuxTestExecutionListener.class;

    boolean inheritListeners() default false;
  }

  @MetaListeners
  static class MetaTestCase {
  }

  @MetaInheritedListeners
  static class MetaInheritedListenersTestCase extends MetaTestCase {
  }

  @MetaNonInheritedListeners
  static class MetaNonInheritedListenersTestCase extends MetaInheritedListenersTestCase {
  }

  @MetaListenersWithOverrides(listeners = {
          FooTestExecutionListener.class,
          BarTestExecutionListener.class,
          BazTestExecutionListener.class
  })
  static class MetaWithOverridesTestCase {
  }

  @MetaInheritedListenersWithOverrides(listeners = { FooTestExecutionListener.class, BarTestExecutionListener.class })
  static class MetaInheritedListenersWithOverridesTestCase extends MetaWithOverridesTestCase {
  }

  @MetaNonInheritedListenersWithOverrides(listeners = {
          FooTestExecutionListener.class,
          BarTestExecutionListener.class,
          BazTestExecutionListener.class
  }, inheritListeners = true)
  static class MetaNonInheritedListenersWithOverridesTestCase extends MetaInheritedListenersWithOverridesTestCase {
  }

  static class FooTestExecutionListener extends AbstractTestExecutionListener {
  }

  static class BarTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
      // 2500 is between DependencyInjectionTestExecutionListener (2000) and
      // DirtiesContextTestExecutionListener (3000)
      return 2500;
    }
  }

  static class BazTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE;
    }
  }

  static class QuuxTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }
  }

  static class EnigmaTestExecutionListener extends AbstractTestExecutionListener {
  }

}
