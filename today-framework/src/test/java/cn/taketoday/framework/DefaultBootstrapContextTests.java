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

package cn.taketoday.framework;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.framework.BootstrapRegistry.InstanceSupplier;
import cn.taketoday.framework.BootstrapRegistry.Scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 12:26
 */
class DefaultBootstrapContextTests {

  private DefaultBootstrapContext context = new DefaultBootstrapContext();

  private AtomicInteger counter = new AtomicInteger();

  private StaticApplicationContext applicationContext = new StaticApplicationContext();

  @Test
  void registerWhenTypeIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.context.register(null, InstanceSupplier.of(1)))
            .withMessage("Type is required");
  }

  @Test
  void registerWhenRegistrationIsNullThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.context.register(Integer.class, null))
            .withMessage("InstanceSupplier is required");
  }

  @Test
  void registerWhenNotAlreadyRegisteredRegistersInstance() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    assertThat(this.context.get(Integer.class)).isEqualTo(0);
    assertThat(this.context.get(Integer.class)).isEqualTo(0);
  }

  @Test
  void registerWhenAlreadyRegisteredRegistersReplacedInstance() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    this.context.register(Integer.class, InstanceSupplier.of(100));
    assertThat(this.context.get(Integer.class)).isEqualTo(100);
    assertThat(this.context.get(Integer.class)).isEqualTo(100);
  }

  @Test
  void registerWhenSingletonAlreadyCreatedThrowsException() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    this.context.get(Integer.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> this.context.register(Integer.class, InstanceSupplier.of(100)))
            .withMessage("java.lang.Integer has already been created");
  }

  @Test
  void registerWhenPrototypeAlreadyCreatedReplacesInstance() {
    this.context.register(Integer.class,
            InstanceSupplier.from(this.counter::getAndIncrement).withScope(Scope.PROTOTYPE));
    this.context.get(Integer.class);
    this.context.register(Integer.class, InstanceSupplier.of(100));
    assertThat(this.context.get(Integer.class)).isEqualTo(100);
  }

  @Test
  void registerWhenAlreadyCreatedThrowsException() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    this.context.get(Integer.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> this.context.register(Integer.class, InstanceSupplier.of(100)))
            .withMessage("java.lang.Integer has already been created");
  }

  @Test
  void registerWithDependencyRegistersInstance() {
    this.context.register(Integer.class, InstanceSupplier.of(100));
    this.context.register(String.class, this::integerAsString);
    assertThat(this.context.get(String.class)).isEqualTo("100");
  }

  private String integerAsString(BootstrapContext context) {
    return String.valueOf(context.get(Integer.class));
  }

  @Test
  void registerIfAbsentWhenAbsentRegisters() {
    this.context.registerIfAbsent(Long.class, InstanceSupplier.of(100L));
    assertThat(this.context.get(Long.class)).isEqualTo(100L);
  }

  @Test
  void registerIfAbsentWhenPresentDoesNotRegister() {
    this.context.registerIfAbsent(Long.class, InstanceSupplier.of(1L));
    this.context.registerIfAbsent(Long.class, InstanceSupplier.of(100L));
    assertThat(this.context.get(Long.class)).isEqualTo(1L);
  }

  @Test
  void isRegisteredWhenNotRegisteredReturnsFalse() {
    this.context.register(Number.class, InstanceSupplier.of(1));
    assertThat(this.context.isRegistered(Long.class)).isFalse();
  }

  @Test
  void isRegisteredWhenRegisteredReturnsTrue() {
    this.context.register(Number.class, InstanceSupplier.of(1));
    assertThat(this.context.isRegistered(Number.class)).isTrue();
  }

  @Test
  void getRegisteredInstanceSupplierWhenNotRegisteredReturnsNull() {
    this.context.register(Number.class, InstanceSupplier.of(1));
    assertThat(this.context.getRegisteredInstanceSupplier(Long.class)).isNull();
  }

  @Test
  void getRegisteredInstanceSupplierWhenRegisteredReturnsRegistration() {
    InstanceSupplier<Number> instanceSupplier = InstanceSupplier.of(1);
    this.context.register(Number.class, instanceSupplier);
    assertThat(this.context.getRegisteredInstanceSupplier(Number.class)).isSameAs(instanceSupplier);
  }

  @Test
  void getWhenNoRegistrationThrowsIllegalStateException() {
    this.context.register(Number.class, InstanceSupplier.of(1));
    assertThatIllegalStateException().isThrownBy(() -> this.context.get(Long.class))
            .withMessageContaining("has not been registered");
  }

  @Test
  void getWhenRegisteredAsNullReturnsNull() {
    this.context.register(Number.class, InstanceSupplier.of(null));
    assertThat(this.context.get(Number.class)).isNull();
  }

  @Test
  void getWhenSingletonCreatesOnlyOneInstance() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    assertThat(this.context.get(Integer.class)).isEqualTo(0);
    assertThat(this.context.get(Integer.class)).isEqualTo(0);
  }

  @Test
  void getWhenPrototypeCreatesOnlyNewInstances() {
    this.context.register(Integer.class,
            InstanceSupplier.from(this.counter::getAndIncrement).withScope(Scope.PROTOTYPE));
    assertThat(this.context.get(Integer.class)).isEqualTo(0);
    assertThat(this.context.get(Integer.class)).isEqualTo(1);
  }

  @Test
  void testName() {

  }

  @Test
  void getOrElseWhenNoRegistrationReturnsOther() {
    this.context.register(Number.class, InstanceSupplier.of(1));
    assertThat(this.context.getOrElse(Long.class, -1L)).isEqualTo(-1);
  }

  @Test
  void getOrElseWhenRegisteredAsNullReturnsNull() {
    this.context.register(Number.class, InstanceSupplier.of(null));
    assertThat(this.context.getOrElse(Number.class, -1)).isNull();
  }

  @Test
  void getOrElseCreatesReturnsOnlyOneInstance() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    assertThat(this.context.getOrElse(Integer.class, -1)).isEqualTo(0);
    assertThat(this.context.getOrElse(Integer.class, -1)).isEqualTo(0);
  }

  @Test
  void getOrElseSupplyWhenNoRegistrationReturnsSupplied() {
    this.context.register(Number.class, InstanceSupplier.of(1));
    assertThat(this.context.getOrElseSupply(Long.class, () -> -1L)).isEqualTo(-1);
  }

  @Test
  void getOrElseSupplyWhenRegisteredAsNullReturnsNull() {
    this.context.register(Number.class, InstanceSupplier.of(null));
    assertThat(this.context.getOrElseSupply(Number.class, () -> -1L)).isNull();
  }

  @Test
  void getOrElseSupplyCreatesOnlyOneInstance() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    assertThat(this.context.getOrElseSupply(Integer.class, () -> -1)).isEqualTo(0);
    assertThat(this.context.getOrElseSupply(Integer.class, () -> -1)).isEqualTo(0);
  }

  @Test
  void getOrElseThrowWhenNoRegistrationThrowsSuppliedException() {
    this.context.register(Number.class, InstanceSupplier.of(1));
    assertThatIOException().isThrownBy(() -> this.context.getOrElseThrow(Long.class, IOException::new));
  }

  @Test
  void getOrElseThrowWhenRegisteredAsNullReturnsNull() {
    this.context.register(Number.class, InstanceSupplier.of(null));
    assertThat(this.context.getOrElseThrow(Number.class, RuntimeException::new)).isNull();
  }

  @Test
  void getOrElseThrowCreatesOnlyOneInstance() {
    this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
    assertThat(this.context.getOrElseThrow(Integer.class, RuntimeException::new)).isEqualTo(0);
    assertThat(this.context.getOrElseThrow(Integer.class, RuntimeException::new)).isEqualTo(0);
  }

  @Test
  void closeMulticastsEventToListeners() {
    TestCloseListener listener = new TestCloseListener();
    this.context.addCloseListener(listener);
    assertThat(listener).wasNotCalled();
    this.context.close(this.applicationContext);
    assertThat(listener).wasCalledOnlyOnce().hasBootstrapContextSameAs(this.context)
            .hasApplicationContextSameAs(this.applicationContext);
  }

  @Test
  void addCloseListenerIgnoresMultipleCallsWithSameListener() {
    TestCloseListener listener = new TestCloseListener();
    this.context.addCloseListener(listener);
    this.context.addCloseListener(listener);
    this.context.close(this.applicationContext);
    assertThat(listener).wasCalledOnlyOnce();
  }

  @Test
  void instanceSupplierGetScopeWhenNotConfiguredReturnsSingleton() {
    InstanceSupplier<String> supplier = InstanceSupplier.of("test");
    assertThat(supplier.getScope()).isEqualTo(Scope.SINGLETON);
    assertThat(supplier.get(null)).isEqualTo("test");
  }

  @Test
  void instanceSupplierWithScopeChangesScope() {
    InstanceSupplier<String> supplier = InstanceSupplier.of("test").withScope(Scope.PROTOTYPE);
    assertThat(supplier.getScope()).isEqualTo(Scope.PROTOTYPE);
    assertThat(supplier.get(null)).isEqualTo("test");
  }

  private static class TestCloseListener
          implements ApplicationListener<BootstrapContextClosedEvent>, AssertProvider<CloseListenerAssert> {

    private int called;

    private BootstrapContext bootstrapContext;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(BootstrapContextClosedEvent event) {
      this.called++;
      this.bootstrapContext = event.getBootstrapContext();
      this.applicationContext = event.getApplicationContext();
    }

    @Override
    public CloseListenerAssert assertThat() {
      return new CloseListenerAssert(this);
    }

  }

  private static class CloseListenerAssert extends AbstractAssert<CloseListenerAssert, TestCloseListener> {

    CloseListenerAssert(TestCloseListener actual) {
      super(actual, CloseListenerAssert.class);
    }

    CloseListenerAssert wasCalledOnlyOnce() {
      assertThat(this.actual.called).as("action calls").isEqualTo(1);
      return this;
    }

    CloseListenerAssert wasNotCalled() {
      assertThat(this.actual.called).as("action calls").isEqualTo(0);
      return this;
    }

    CloseListenerAssert hasBootstrapContextSameAs(BootstrapContext bootstrapContext) {
      assertThat(this.actual.bootstrapContext).isSameAs(bootstrapContext);
      return this;
    }

    CloseListenerAssert hasApplicationContextSameAs(ApplicationContext applicationContext) {
      assertThat(this.actual.applicationContext).isSameAs(applicationContext);
      return this;
    }

  }

}