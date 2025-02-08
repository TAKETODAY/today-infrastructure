/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.test.mock.mockito;

import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.util.ReflectionUtils;
import infra.util.ReflectionUtils.FieldCallback;

/**
 * {@link TestExecutionListener} to enable {@link MockBean @MockBean} and
 * {@link SpyBean @SpyBean} support. Also triggers
 * {@link MockitoAnnotations#openMocks(Object)} when any Mockito annotations used,
 * primarily to allow {@link Captor @Captor} annotations.
 * <p>
 * To use the automatic reset support of {@code @MockBean} and {@code @SpyBean}, configure
 * {@link ResetMocksTestExecutionListener} as well.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ResetMocksTestExecutionListener
 * @since 4.0
 */
public class MockitoTestExecutionListener extends AbstractTestExecutionListener {

  private static final String MOCKS_ATTRIBUTE_NAME = MockitoTestExecutionListener.class.getName() + ".mocks";

  @Override
  public final int getOrder() {
    return 1950;
  }

  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    initMocks(testContext);
    injectFields(testContext);
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    if (Boolean.TRUE.equals(
            testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
      initMocks(testContext);
      reinjectFields(testContext);
    }
  }

  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    Object mocks = testContext.getAttribute(MOCKS_ATTRIBUTE_NAME);
    if (mocks instanceof AutoCloseable) {
      ((AutoCloseable) mocks).close();
    }
  }

  private void initMocks(TestContext testContext) {
    if (hasMockitoAnnotations(testContext)) {
      testContext.setAttribute(MOCKS_ATTRIBUTE_NAME, MockitoAnnotations.openMocks(testContext.getTestInstance()));
    }
  }

  private boolean hasMockitoAnnotations(TestContext testContext) {
    MockitoAnnotationCollection collector = new MockitoAnnotationCollection();
    ReflectionUtils.doWithFields(testContext.getTestClass(), collector);
    return collector.hasAnnotations();
  }

  private void injectFields(TestContext testContext) {
    postProcessFields(testContext, (mockitoField, postProcessor) -> postProcessor.inject(mockitoField.field,
            mockitoField.target, mockitoField.definition));
  }

  private void reinjectFields(final TestContext testContext) {
    postProcessFields(testContext, (mockitoField, postProcessor) -> {
      ReflectionUtils.makeAccessible(mockitoField.field);
      ReflectionUtils.setField(mockitoField.field, testContext.getTestInstance(), null);
      postProcessor.inject(mockitoField.field, mockitoField.target, mockitoField.definition);
    });
  }

  private void postProcessFields(TestContext testContext, BiConsumer<MockitoField, MockitoPostProcessor> consumer) {
    DefinitionsParser parser = new DefinitionsParser();
    parser.parse(testContext.getTestClass());
    if (!parser.getDefinitions().isEmpty()) {
      MockitoPostProcessor postProcessor = testContext.getApplicationContext()
              .getBean(MockitoPostProcessor.class);
      for (Definition definition : parser.getDefinitions()) {
        Field field = parser.getField(definition);
        if (field != null) {
          consumer.accept(new MockitoField(field, testContext.getTestInstance(), definition), postProcessor);
        }
      }
    }
  }

  /**
   * {@link FieldCallback} to collect Mockito annotations.
   */
  private static final class MockitoAnnotationCollection implements FieldCallback {

    private final Set<Annotation> annotations = new LinkedHashSet<>();

    @Override
    public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
      for (Annotation annotation : field.getDeclaredAnnotations()) {
        if (annotation.annotationType().getName().startsWith("org.mockito")) {
          this.annotations.add(annotation);
        }
      }
    }

    boolean hasAnnotations() {
      return !this.annotations.isEmpty();
    }

  }

  private static final class MockitoField {

    private final Field field;

    private final Object target;

    private final Definition definition;

    private MockitoField(Field field, Object instance, Definition definition) {
      this.field = field;
      this.target = instance;
      this.definition = definition;
    }

  }

}
