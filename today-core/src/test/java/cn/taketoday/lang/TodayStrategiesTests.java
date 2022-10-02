/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.lang;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.lang.TodayStrategies.ArgumentResolver;
import cn.taketoday.lang.TodayStrategies.FailureHandler;
import cn.taketoday.lang.TodayStrategies.DefaultInstantiator;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/2 19:27
 */
class TodayStrategiesTests {

  @BeforeAll
  static void clearCache() {
    TodayStrategies.strategiesCache.clear();
    assertThat(TodayStrategies.strategiesCache).isEmpty();
  }

  @AfterAll
  static void checkCache() {
    assertThat(TodayStrategies.strategiesCache).hasSize(3);
    TodayStrategies.strategiesCache.clear();
  }

  @Test
  @Deprecated
  void loadFactoryNames() {
    List<String> strategyNames = TodayStrategies.findNames(DummyFactory.class, null);
    assertThat(strategyNames).containsExactlyInAnyOrder(MyDummyFactory1.class.getName(), MyDummyFactory2.class.getName());
  }

  @Test
  void loadWhenNoRegisteredImplementationsReturnsEmptyList() {
    List<Integer> factories = TodayStrategies.forDefaultResourceLocation().load(Integer.class);
    assertThat(factories).isEmpty();
  }

  @Test
  void loadWhenDuplicateRegistrationsPresentReturnsListInCorrectOrder() {
    List<DummyFactory> factories = TodayStrategies.forDefaultResourceLocation().load(DummyFactory.class);
    assertThat(factories).hasSize(2);
    assertThat(factories.get(0)).isInstanceOf(MyDummyFactory1.class);
    assertThat(factories.get(1)).isInstanceOf(MyDummyFactory2.class);
  }

  @Test
  void loadWhenPackagePrivateFactory() {
    List<DummyPackagePrivateFactory> factories =
            TodayStrategies.forDefaultResourceLocation().load(DummyPackagePrivateFactory.class);
    assertThat(factories).hasSize(1);
    assertThat(Modifier.isPublic(factories.get(0).getClass().getModifiers())).isFalse();
  }

  @Test
  void loadWhenIncompatibleTypeThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> TodayStrategies.forDefaultResourceLocation().load(String.class))
            .withMessageContaining("Unable to instantiate strategy class "
                    + "[cn.taketoday.lang.MyDummyFactory1] for strategy type [java.lang.String]");
  }

  @Test
  void loadWithLoggingFailureHandlerWhenIncompatibleTypeReturnsEmptyList() {
    Logger logger = mock(Logger.class);
    FailureHandler failureHandler = FailureHandler.logging(logger);
    List<String> factories = TodayStrategies.forDefaultResourceLocation().load(String.class, failureHandler);
    assertThat(factories).isEmpty();
  }

  @Test
  void loadWithArgumentResolverWhenNoDefaultConstructor() {
    ArgumentResolver resolver = ArgumentResolver.of(String.class, "injected");
    List<DummyFactory> list = TodayStrategies.forDefaultResourceLocation(LimitedClassLoader.constructorArgumentFactories)
            .load(DummyFactory.class, resolver);
    assertThat(list).hasSize(3);
    assertThat(list.get(0)).isInstanceOf(MyDummyFactory1.class);
    assertThat(list.get(1)).isInstanceOf(MyDummyFactory2.class);
    assertThat(list.get(2)).isInstanceOf(ConstructorArgsDummyFactory.class);
    assertThat(list).extracting(DummyFactory::getString).containsExactly("Foo", "Bar", "injected");
  }

  @Test
  void loadWhenMultipleConstructorsThrowsException() {
    ArgumentResolver resolver = ArgumentResolver.of(String.class, "injected");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> TodayStrategies.forDefaultResourceLocation(LimitedClassLoader.multipleArgumentFactories)
                    .load(DummyFactory.class, resolver))
            .withMessageContaining("Unable to instantiate strategy class "
                    + "[cn.taketoday.lang.MultipleConstructorArgsDummyFactory] for strategy type [cn.taketoday.lang.DummyFactory]")
            .havingRootCause().withMessageContaining("Class [cn.taketoday.lang.MultipleConstructorArgsDummyFactory] has no suitable constructor");
  }

  @Test
  void loadWithLoggingFailureHandlerWhenMissingArgumentDropsItem() {
    Logger logger = mock(Logger.class);
    FailureHandler failureHandler = FailureHandler.logging(logger);
    List<DummyFactory> factories = TodayStrategies.forDefaultResourceLocation(LimitedClassLoader.multipleArgumentFactories)
            .load(DummyFactory.class, failureHandler);
    assertThat(factories).hasSize(2);
    assertThat(factories.get(0)).isInstanceOf(MyDummyFactory1.class);
    assertThat(factories.get(1)).isInstanceOf(MyDummyFactory2.class);
  }

  @Test
  void loadFactoriesLoadsFromDefaultLocation() {
    List<DummyFactory> factories = TodayStrategies.find(DummyFactory.class, (ClassLoader) null);
    assertThat(factories).hasSize(2);
    assertThat(factories.get(0)).isInstanceOf(MyDummyFactory1.class);
    assertThat(factories.get(1)).isInstanceOf(MyDummyFactory2.class);
  }

  @Test
  void loadForResourceLocationWhenLocationDoesNotExistReturnsEmptyList() {
    List<DummyFactory> factories = TodayStrategies.forLocation(
            "META-INF/missing/missing-today-strategies.properties").load(DummyFactory.class);
    assertThat(factories).isEmpty();
  }

  @Test
  void loadForResourceLocationLoadsFactories() {
    List<DummyFactory> factories = TodayStrategies.forLocation(
            "META-INF/custom/custom.strategies").load(DummyFactory.class);
    assertThat(factories).hasSize(1);
    assertThat(factories.get(0)).isInstanceOf(MyDummyFactory1.class);
  }

  @Test
  void sameCachedResultIsUsedForDefaultClassLoaderAndNullClassLoader() {
    TodayStrategies forNull = TodayStrategies.forDefaultResourceLocation(null);
    TodayStrategies forDefault = TodayStrategies.forDefaultResourceLocation(ClassUtils.getDefaultClassLoader());
    assertThat(forNull).isSameAs(forDefault);
  }

  @Nested
  class FailureHandlerTests {

    @Test
    void throwingReturnsHandlerThatThrowsIllegalArgumentException() {
      FailureHandler handler = FailureHandler.throwing();
      RuntimeException cause = new RuntimeException();
      assertThatIllegalArgumentException().isThrownBy(() -> handler.handleFailure(
              DummyFactory.class, MyDummyFactory1.class.getName(),
              cause)).withMessageStartingWith("Unable to instantiate strategy class").withCause(cause);
    }

    @Test
    void throwingWithFactoryReturnsHandlerThatThrows() {
      FailureHandler handler = FailureHandler.throwing(IllegalStateException::new);
      RuntimeException cause = new RuntimeException();
      assertThatIllegalStateException().isThrownBy(() -> handler.handleFailure(
              DummyFactory.class, MyDummyFactory1.class.getName(),
              cause)).withMessageStartingWith("Unable to instantiate strategy class").withCause(cause);
    }

    @Test
    void loggingReturnsHandlerThatLogs() {
      Logger logger = mock(Logger.class);
      FailureHandler handler = FailureHandler.logging(logger);
      RuntimeException cause = new RuntimeException();
      handler.handleFailure(DummyFactory.class, MyDummyFactory1.class.getName(), cause);
      verify(logger).trace(isA(LogMessage.class), eq(cause));
    }

    @Test
    void handleMessageReturnsHandlerThatAcceptsMessage() {
      List<Throwable> failures = new ArrayList<>();
      List<String> messages = new ArrayList<>();
      FailureHandler handler = FailureHandler.handleMessage((message, failure) -> {
        failures.add(failure);
        messages.add(message.get());
      });
      RuntimeException cause = new RuntimeException();
      handler.handleFailure(DummyFactory.class, MyDummyFactory1.class.getName(), cause);
      assertThat(failures).containsExactly(cause);
      assertThat(messages).hasSize(1);
      assertThat(messages.get(0)).startsWith("Unable to instantiate strategy class");
    }

  }

  @Nested
  class ArgumentResolverTests {

    @Test
    void ofValueResolvesValue() {
      ArgumentResolver resolver = ArgumentResolver.of(CharSequence.class, "test");
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
      assertThat(resolver.resolve(String.class)).isNull();
      assertThat(resolver.resolve(Integer.class)).isNull();
    }

    @Test
    void ofValueSupplierResolvesValue() {
      ArgumentResolver resolver = ArgumentResolver.ofSupplied(CharSequence.class, () -> "test");
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
      assertThat(resolver.resolve(String.class)).isNull();
      assertThat(resolver.resolve(Integer.class)).isNull();
    }

    @Test
    void fromAdaptsFunction() {
      ArgumentResolver resolver = ArgumentResolver.from(
              type -> CharSequence.class.equals(type) ? "test" : null);
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
      assertThat(resolver.resolve(String.class)).isNull();
      assertThat(resolver.resolve(Integer.class)).isNull();
    }

    @Test
    void andValueReturnsComposite() {
      ArgumentResolver resolver = ArgumentResolver.of(CharSequence.class, "test").and(Integer.class, 123);
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
      assertThat(resolver.resolve(String.class)).isNull();
      assertThat(resolver.resolve(Integer.class)).isEqualTo(123);
    }

    @Test
    void andValueWhenSameTypeReturnsCompositeResolvingFirst() {
      ArgumentResolver resolver = ArgumentResolver.of(CharSequence.class, "test").and(CharSequence.class, "ignore");
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
    }

    @Test
    void andValueSupplierReturnsComposite() {
      ArgumentResolver resolver = ArgumentResolver.of(CharSequence.class, "test").andSupplied(Integer.class, () -> 123);
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
      assertThat(resolver.resolve(String.class)).isNull();
      assertThat(resolver.resolve(Integer.class)).isEqualTo(123);
    }

    @Test
    void andValueSupplierWhenSameTypeReturnsCompositeResolvingFirst() {
      ArgumentResolver resolver = ArgumentResolver.of(CharSequence.class, "test").andSupplied(CharSequence.class, () -> "ignore");
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
    }

    @Test
    void andResolverReturnsComposite() {
      ArgumentResolver resolver = ArgumentResolver.of(CharSequence.class, "test").and(Integer.class, 123);
      resolver = resolver.and(ArgumentResolver.of(CharSequence.class, "ignore").and(Long.class, 234L));
      assertThat(resolver.resolve(CharSequence.class)).isEqualTo("test");
      assertThat(resolver.resolve(String.class)).isNull();
      assertThat(resolver.resolve(Integer.class)).isEqualTo(123);
      assertThat(resolver.resolve(Long.class)).isEqualTo(234L);
    }

  }

  @Nested
  class FactoryInstantiatorTests {

    private final ArgumentResolver resolver = ArgumentResolver.of(String.class, "test");
    DefaultInstantiator instantiator = new DefaultInstantiator(resolver);

    @Test
    void defaultConstructorCreatesInstance() throws Exception {
      Object instance = instantiator.instantiate(DefaultConstructor.class);
      assertThat(instance).isNotNull();
    }

    @Test
    void singleConstructorWithArgumentsCreatesInstance() throws Exception {
      Object instance = instantiator.instantiate(SingleConstructor.class);
      assertThat(instance).isNotNull();
    }

    @Test
    void multiplePrivateAndSinglePublicConstructorCreatesInstance() throws Exception {
      Object instance = instantiator.instantiate(MultiplePrivateAndSinglePublicConstructor.class);
      assertThat(instance).isNotNull();
    }

    @Test
    void multiplePackagePrivateAndSinglePublicConstructorCreatesInstance() throws Exception {
      Object instance = instantiator.instantiate(
              MultiplePackagePrivateAndSinglePublicConstructor.class);
      assertThat(instance).isNotNull();
    }

    @Test
    void singlePackagePrivateConstructorCreatesInstance() throws Exception {
      Object instance = instantiator.instantiate(
              SinglePackagePrivateConstructor.class);
      assertThat(instance).isNotNull();
    }

    @Test
    void singlePrivateConstructorCreatesInstance() throws Exception {
      Object instance = instantiator.instantiate(SinglePrivateConstructor.class);
      assertThat(instance).isNotNull();
    }

    @Test
    void multiplePackagePrivateConstructorsThrowsException() {
      assertThatIllegalStateException()
              .isThrownBy(() -> instantiator.instantiate(MultiplePackagePrivateConstructors.class))
              .withMessageContaining("has no suitable constructor");
    }

    static class DefaultConstructor {

    }

    static class SingleConstructor {

      SingleConstructor(String arg) {
      }

    }

    static class MultiplePrivateAndSinglePublicConstructor {

      public MultiplePrivateAndSinglePublicConstructor(String arg) {
        this(arg, false);
      }

      private MultiplePrivateAndSinglePublicConstructor(String arg, boolean extra) {
      }

    }

    static class MultiplePackagePrivateAndSinglePublicConstructor {

      public MultiplePackagePrivateAndSinglePublicConstructor(String arg) {
        this(arg, false);
      }

      MultiplePackagePrivateAndSinglePublicConstructor(String arg, boolean extra) {
      }

    }

    static class SinglePackagePrivateConstructor {

      SinglePackagePrivateConstructor(String arg) {
      }

    }

    static class SinglePrivateConstructor {

      private SinglePrivateConstructor(String arg) {
      }

    }

    static class MultiplePackagePrivateConstructors {

      MultiplePackagePrivateConstructors(String arg) {
        this(arg, false);
      }

      MultiplePackagePrivateConstructors(String arg, boolean extra) {
      }

    }

  }

  private static class LimitedClassLoader extends URLClassLoader {

    private static final ClassLoader constructorArgumentFactories = new LimitedClassLoader("constructor-argument-factories");

    private static final ClassLoader multipleArgumentFactories = new LimitedClassLoader("multiple-arguments-factories");

    LimitedClassLoader(String location) {
      super(new URL[] { toUrl(location) });
    }

    private static URL toUrl(String location) {
      try {
        return new File("src/test/resources/cn/taketoday/lang/" + location + "/").toURI().toURL();
      }
      catch (MalformedURLException ex) {
        throw new IllegalStateException(ex);
      }
    }

  }

}