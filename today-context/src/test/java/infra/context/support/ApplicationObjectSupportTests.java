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
  void obtainApplicationContextWhenNotSet() {
    assertThatThrownBy(() -> testSupport.obtainApplicationContext())
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

  @Test
  void loggerIsProperlyInitialized() {
    assertThat(testSupport.logger).isNotNull();
    assertThat(testSupport.logger.getName()).isEqualTo(TestApplicationObjectSupport.class.getName());
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
