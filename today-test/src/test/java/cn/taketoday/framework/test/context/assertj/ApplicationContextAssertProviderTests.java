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

package cn.taketoday.framework.test.context.assertj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link ApplicationContextAssertProvider} and
 * {@link AssertProviderApplicationContextInvocationHandler}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class ApplicationContextAssertProviderTests {

  @Mock
  private ConfigurableApplicationContext mockContext;

  private RuntimeException startupFailure;

  private Supplier<ApplicationContext> mockContextSupplier;

  private Supplier<ApplicationContext> startupFailureSupplier;

  @BeforeEach
  void setup() {
    this.startupFailure = new RuntimeException();
    this.mockContextSupplier = () -> this.mockContext;
    this.startupFailureSupplier = () -> {
      throw this.startupFailure;
    };
  }

  @Test
  void getWhenTypeIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(
                    () -> ApplicationContextAssertProvider.get(null, ApplicationContext.class, this.mockContextSupplier))
            .withMessageContaining("Type is required");
  }

  @Test
  void getWhenTypeIsClassShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(
                    () -> ApplicationContextAssertProvider.get(null, ApplicationContext.class, this.mockContextSupplier))
            .withMessageContaining("Type is required");
  }

  @Test
  void getWhenContextTypeIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ApplicationContextAssertProvider.get(TestAssertProviderApplicationContextClass.class,
                    ApplicationContext.class, this.mockContextSupplier))
            .withMessageContaining("Type must be an interface");
  }

  @Test
  void getWhenContextTypeIsClassShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ApplicationContextAssertProvider.get(TestAssertProviderApplicationContext.class, null,
                    this.mockContextSupplier))
            .withMessageContaining("ContextType is required");
  }

  @Test
  void getWhenSupplierIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ApplicationContextAssertProvider.get(TestAssertProviderApplicationContext.class,
                    StaticApplicationContext.class, this.mockContextSupplier))
            .withMessageContaining("ContextType must be an interface");
  }

  @Test
  void getWhenContextStartsShouldReturnProxyThatCallsRealMethods() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
    assertThat((Object) context).isNotNull();
    context.getBean("foo");
    then(this.mockContext).should().getBean("foo");
  }

  @Test
  void getWhenContextFailsShouldReturnProxyThatThrowsExceptions() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
    assertThat((Object) context).isNotNull();
    assertThatIllegalStateException().isThrownBy(() -> context.getBean("foo"))
            .withCause(this.startupFailure)
            .withMessageContaining("failed to start");
  }

  @Test
  void getSourceContextWhenContextStartsShouldReturnSourceContext() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
    assertThat(context.getSourceApplicationContext()).isSameAs(this.mockContext);
  }

  @Test
  void getSourceContextWhenContextFailsShouldThrowException() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
    assertThatIllegalStateException().isThrownBy(context::getSourceApplicationContext)
            .withCause(this.startupFailure)
            .withMessageContaining("failed to start");
  }

  @Test
  void getSourceContextOfTypeWhenContextStartsShouldReturnSourceContext() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
    assertThat(context.getSourceApplicationContext(ApplicationContext.class)).isSameAs(this.mockContext);
  }

  @Test
  void getSourceContextOfTypeWhenContextFailsToStartShouldThrowException() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
    assertThatIllegalStateException()
            .isThrownBy(() -> context.getSourceApplicationContext(ApplicationContext.class))
            .withCause(this.startupFailure)
            .withMessageContaining("failed to start");
  }

  @Test
  void getStartupFailureWhenContextStartsShouldReturnNull() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
    assertThat(context.getStartupFailure()).isNull();
  }

  @Test
  void getStartupFailureWhenContextFailsToStartShouldReturnException() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
    assertThat(context.getStartupFailure()).isEqualTo(this.startupFailure);
  }

  @Test
  void assertThatWhenContextStartsShouldReturnAssertions() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
    ApplicationContextAssert<ApplicationContext> contextAssert = assertThat(context);
    assertThat(contextAssert.getApplicationContext()).isSameAs(context);
    assertThat(contextAssert.getStartupFailure()).isNull();
  }

  @Test
  void assertThatWhenContextFailsShouldReturnAssertions() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
    ApplicationContextAssert<ApplicationContext> contextAssert = assertThat(context);
    assertThat(contextAssert.getApplicationContext()).isSameAs(context);
    assertThat(contextAssert.getStartupFailure()).isSameAs(this.startupFailure);
  }

  @Test
  void toStringWhenContextStartsShouldReturnSimpleString() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
    assertThat(context.toString()).startsWith("Started application [ConfigurableApplicationContext.MockitoMock")
            .endsWith("id = [null], applicationName = [null], beanDefinitionCount = 0]");
  }

  @Test
  void toStringWhenContextFailsToStartShouldReturnSimpleString() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.startupFailureSupplier);
    assertThat(context).hasToString("Unstarted application context "
            + "cn.taketoday.context.ApplicationContext[startupFailure=java.lang.RuntimeException]");
  }

  @Test
  void closeShouldCloseContext() {
    ApplicationContextAssertProvider<ApplicationContext> context = get(this.mockContextSupplier);
    context.close();
    then(this.mockContext).should().close();
  }

  private ApplicationContextAssertProvider<ApplicationContext> get(Supplier<ApplicationContext> contextSupplier) {
    return ApplicationContextAssertProvider.get(TestAssertProviderApplicationContext.class,
            ApplicationContext.class, contextSupplier);
  }

  interface TestAssertProviderApplicationContext extends ApplicationContextAssertProvider<ApplicationContext> {

  }

  abstract static class TestAssertProviderApplicationContextClass implements TestAssertProviderApplicationContext {

  }

}
