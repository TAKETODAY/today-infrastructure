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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import infra.beans.TypeConverter;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.beans.factory.config.DependencyDescriptor;
import infra.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/29 16:42
 */
class DependencyInjectorTests {

  public void test(ParameterHandlerBean bean) {
    System.out.println(bean);
  }

  public ParameterHandlerBean inject(ParameterHandlerBean bean) {
    return bean;
  }

  @Test
  void resolveArguments() throws NoSuchMethodException {

    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("parameterHandlerBean",
            BeanDefinitionBuilder.rootBeanDefinition(ParameterHandlerBean.class).getBeanDefinition());

    DependencyInjector injector = new DependencyInjector(beanFactory);
    Method test = DependencyInjectorTests.class.getDeclaredMethod("test", ParameterHandlerBean.class);
    Method inject = DependencyInjectorTests.class.getDeclaredMethod("inject", ParameterHandlerBean.class);

    Object[] args = injector.resolveArguments(test);
    assertThat(args).hasSize(1);
    assertThat(args[0]).isInstanceOf(ParameterHandlerBean.class);

    Object inject1 = injector.inject(inject, new DependencyInjectorTests());
    assertThat(inject1).isSameAs(args[0]);
  }

  @Test
  void resolveValue() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("testBean", BeanDefinitionBuilder.rootBeanDefinition(TestBean.class).getBeanDefinition());
    AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
    processor.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(processor);

    TestBean testBean = beanFactory.getBean(TestBean.class);
    assertThat(testBean.intProperty.value).isEqualTo(1);
  }

  @Test
  void constructorInjectionUsesFirstMatchingProvidedArgumentWithoutConsultingFactory() throws Exception {
    var factory = mock(AutowireCapableBeanFactory.class);
    var injector = new DependencyInjector(factory);
    var constructor = IntProperty.class.getDeclaredConstructor(int.class);
    // Primitive parameters are resolved by the factory or a strategy, not by Class.isInstance.
    var referenceConstructor = InjectionTarget.class.getDeclaredConstructor(CharSequence.class);
    var first = new StringBuilder("first");

    assertThat(injector.inject(referenceConstructor, null, 42, first, "second").value()).isSameAs(first);
    assertThat(DependencyInjector.findProvided(constructor.getParameters()[0], new Object[] { 42 })).isNull();
    verifyNoInteractions(factory);
  }

  @Test
  void methodInjectionCombinesProvidedArgumentsWithFactoryDependencies() throws Exception {
    var factory = new StandardBeanFactory();
    var registered = new ParameterHandlerBean();
    var provided = new ParameterHandlerBean();
    var target = new InjectionTarget("prefix");
    factory.registerSingleton("registered", registered);
    factory.registerSingleton("number", 7);
    var injector = new DependencyInjector(factory);
    var method = InjectionTarget.class.getDeclaredMethod("combine", ParameterHandlerBean.class, Integer.class);

    assertThat(injector.inject(method, target, "unrelated", provided))
            .isEqualTo(List.of("prefix", provided, 7));
    assertThat(injector.inject(method, target, "unrelated"))
            .isEqualTo(List.of("prefix", registered, 7));
  }

  @Test
  void methodInjectionWithoutParametersNeedsNoResolutionAndPropagatesTargetFailure() throws Exception {
    var factory = mock(AutowireCapableBeanFactory.class);
    var injector = new DependencyInjector(factory);
    var target = new InjectionTarget("provided");
    var failure = new IllegalArgumentException("target failure");
    var value = InjectionTarget.class.getDeclaredMethod("value");
    var fail = InjectionTarget.class.getDeclaredMethod("fail", RuntimeException.class);
    var inaccessible = InjectionTarget.class.getDeclaredMethod("inaccessible");

    assertThat(injector.resolveArguments(value)).isNull();
    assertThat(injector.inject(value, target)).isSameAs(target.value());
    assertThatThrownBy(() -> injector.inject(fail, target, failure)).isSameAs(failure);
    assertThatIllegalStateException().isThrownBy(() -> injector.inject(inaccessible, target))
            .withMessageContaining("Could not access method", "inaccessible");
    verifyNoInteractions(factory);
  }

  @Test
  void resolveValueDelegatesToFactoryWithDefaultOrExplicitContext() throws Exception {
    var factory = mock(AutowireCapableBeanFactory.class);
    var injector = new DependencyInjector(factory);
    var descriptor = new DependencyDescriptor(MethodParameter.forExecutable(
            InjectionTarget.class.getDeclaredConstructor(CharSequence.class), 0), true);
    var names = new HashSet<String>();
    var converter = mock(TypeConverter.class);
    given(factory.resolveDependency(descriptor, null, null, null)).willReturn("default");
    given(factory.resolveDependency(descriptor, "consumer", names, converter)).willAnswer(invocation -> {
      Set<String> resolvedNames = invocation.getArgument(2);
      resolvedNames.add("dependency");
      return "explicit";
    });

    assertThat(injector.resolveValue(descriptor)).isEqualTo("default");
    assertThat(injector.resolveValue(descriptor, "consumer", names, converter)).isEqualTo("explicit");
    assertThat(names).containsExactly("dependency");
  }

  @Test
  void resolutionStrategiesReceiveContextAndCanBeReset() throws Exception {
    var injector = new DependencyInjector(null);
    var defaultStrategies = injector.getResolvingStrategies();
    var descriptor = new DependencyDescriptor(MethodParameter.forExecutable(
            InjectionTarget.class.getDeclaredConstructor(CharSequence.class), 0), true);
    var names = new HashSet<String>();
    var converter = mock(TypeConverter.class);
    var strategies = new DependencyResolvingStrategies(List.of((dependency, context) -> {
      assertThat(dependency).isSameAs(descriptor);
      assertThat(context.requestingBeanName).isEqualTo("consumer");
      assertThat(context.dependentBeans).isSameAs(names);
      assertThat(context.typeConverter).isSameAs(converter);
      context.addDependentBean("strategyDependency");
      return "resolved";
    }));
    injector.setResolvingStrategies(strategies);

    assertThat(injector.resolveValue(descriptor, "consumer", names, converter)).isEqualTo("resolved");
    assertThat(names).containsExactly("strategyDependency");
    assertThat(injector.getResolvingStrategies()).isSameAs(strategies);

    injector.setResolvingStrategies(null);
    var restored = injector.getResolvingStrategies();
    assertThat(restored).isNotSameAs(strategies).isNotSameAs(defaultStrategies);
    assertThat(injector.getResolvingStrategies()).isSameAs(restored);
  }

  @Test
  void missingDependenciesWithoutAFactoryAreAllowedOnlyForOptionalParameters() throws Exception {
    var injector = new DependencyInjector(null);
    injector.setResolvingStrategies(new DependencyResolvingStrategies());
    var required = InjectionTarget.class.getDeclaredConstructor(CharSequence.class);
    var optional = InjectionTarget.class.getDeclaredMethod("optional", CharSequence.class);

    assertThatThrownBy(() -> injector.resolveArguments(required, (Object[]) null))
            .isInstanceOf(NoSuchBeanDefinitionException.class).hasMessageContaining(CharSequence.class.getName());
    assertThat(injector.resolveArguments(optional, 42)).containsExactly((Object) null);
  }

  @Test
  void unresolvedFactoryDependenciesRespectTheCandidateResolver() throws Exception {
    var factory = new StandardBeanFactory() {
      @Override
      public @Nullable Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
              @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
        return null;
      }
    };
    var injector = new DependencyInjector(factory);
    var required = InjectionTarget.class.getDeclaredConstructor(CharSequence.class);

    assertThatThrownBy(() -> injector.resolveArguments(required)).isInstanceOf(NoSuchBeanDefinitionException.class)
            .hasMessageContaining(CharSequence.class.getName());

    factory.setAutowireCandidateResolver(new AutowireCandidateResolver() {
      @Override
      public boolean isRequired(DependencyDescriptor descriptor) {
        return false;
      }
    });
    assertThat(injector.resolveArguments(required)).containsExactly((Object) null);
    assertThat(injector.getResolvingStrategies()).isSameAs(injector.getResolvingStrategies());
  }

  record InjectionTarget(CharSequence value) {

    List<Object> combine(ParameterHandlerBean dependency, Integer number) {
      return List.of(this.value, dependency, number);
    }

    void fail(RuntimeException failure) {
      throw failure;
    }

    private void inaccessible() {
    }

    void optional(@Nullable CharSequence dependency) {
    }

  }

  static class ParameterHandlerBean {

    public int age;
  }

  static class TestBean {

    @Autowired
    IntProperty intProperty;

  }

  static class Property<T> {
    public T value;

    void update(T value) {
      this.value = value;
    }

  }

  static class IntProperty extends Property<Integer> {
    public IntProperty(int value) {
      this.value = value;
    }
  }

  static class TestDependencyResolvingStrategy implements DependencyResolvingStrategy {

    @Nullable
    @Override
    public Object resolveDependency(DependencyDescriptor descriptor, Context context) {
      if (descriptor.getDependencyType() == IntProperty.class) {
        return new IntProperty(1);
      }
      return null;
    }
  }

}
