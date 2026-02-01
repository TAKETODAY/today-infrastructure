/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.context.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationObjectSupportTests {

  private TestApplicationObjectSupport testSupport;

  private ApplicationContext mockContext;

  @BeforeEach
  void setUp() {
    testSupport = new TestApplicationObjectSupport();
    mockContext = mock(ApplicationContext.class);
  }

  @Test
  void setApplicationContextWithValidContext() {
    testSupport.setApplicationContext(mockContext);
    assertThat(testSupport.getApplicationContext()).isSameAs(mockContext);
  }

  @Test
  void setApplicationContextWithNull() {
    testSupport.setApplicationContext(null);
    assertThat(testSupport.getApplicationContext()).isNull();
  }

  @Test
  void setApplicationContextWithNullWhenRequired() {
    TestRequiredContextSupport requiredSupport = new TestRequiredContextSupport();
    assertThatThrownBy(() -> requiredSupport.setApplicationContext(null))
            .isInstanceOf(ApplicationContextException.class)
            .hasMessageContaining("Invalid application context: needs to be of type");
  }

  @Test
  void reinitializationWithDifferentContextThrowsException() {
    testSupport.setApplicationContext(mockContext);
    ApplicationContext anotherContext = mock(ApplicationContext.class);

    assertThatThrownBy(() -> testSupport.setApplicationContext(anotherContext))
            .isInstanceOf(ApplicationContextException.class)
            .hasMessageContaining("Cannot reinitialize with different application context");
  }

  @Test
  void applicationContextWhenNotSet() {
    assertThatThrownBy(() -> testSupport.applicationContext())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No ApplicationContext");
  }

  @Test
  void unwrapFactoryDelegation() {
    testSupport.setApplicationContext(mockContext);
    Object factory = new Object();
    when(mockContext.unwrapFactory(Object.class)).thenReturn(factory);

    assertThat(testSupport.unwrapFactory(Object.class)).isSameAs(factory);
    verify(mockContext).unwrapFactory(Object.class);
  }

  @Test
  void unwrapContextDelegation() {
    testSupport.setApplicationContext(mockContext);
    Object contextImpl = new Object();
    when(mockContext.unwrap(Object.class)).thenReturn(contextImpl);

    assertThat(testSupport.unwrapContext(Object.class)).isSameAs(contextImpl);
    verify(mockContext).unwrap(Object.class);
  }

  @Test
  void getMessageSourceAccessorWhenContextNotSet() {
    assertThat(testSupport.getMessageSourceAccessor()).isNull();
  }

  @Test
  void getMessageSourceAccessorWhenContextNotSetButRequired() {
    TestRequiredContextSupport requiredSupport = new TestRequiredContextSupport();
    assertThatThrownBy(requiredSupport::getMessageSourceAccessor)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does not run in an ApplicationContext");
  }

  @Test
  void setApplicationContextWithInvalidContextType() {
    TestContextTypeSupport contextTypeSupport = new TestContextTypeSupport();
    assertThatThrownBy(() -> contextTypeSupport.setApplicationContext(mockContext))
            .isInstanceOf(ApplicationContextException.class)
            .hasMessageContaining("Invalid application context: needs to be of type");
  }

  @Test
  void initApplicationContextIsCalled() {
    TestInitializationSupport initSupport = new TestInitializationSupport();
    initSupport.setApplicationContext(mockContext);
    assertThat(initSupport.isInitCalled()).isTrue();
  }

  @Test
  void messageSourceAccessorIsInitializedAndCached() {
    testSupport.setApplicationContext(mockContext);
    MessageSourceAccessor accessor1 = testSupport.getMessageSourceAccessor();
    MessageSourceAccessor accessor2 = testSupport.getMessageSourceAccessor();

    assertThat(accessor1).isNotNull();
    assertThat(accessor1).isSameAs(accessor2);
  }

  // Test support classes
  private static class TestApplicationObjectSupport extends ApplicationObjectSupport {

  }

  private static class TestRequiredContextSupport extends ApplicationObjectSupport {
    @Override
    protected boolean isContextRequired() {
      return true;
    }
  }

  private static class TestContextTypeSupport extends ApplicationObjectSupport {
    @Override
    protected Class<?> requiredContextClass() {
      return CustomApplicationContext.class;
    }
  }

  private interface CustomApplicationContext extends ApplicationContext { }

  private static class TestInitializationSupport extends ApplicationObjectSupport {
    private boolean initCalled = false;

    @Override
    protected void initApplicationContext(ApplicationContext context) {
      super.initApplicationContext(context);
      this.initCalled = true;
    }

    public boolean isInitCalled() {
      return initCalled;
    }
  }
}
