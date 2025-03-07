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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.TypedStringValue;
import infra.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import infra.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import infra.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.lang.Nullable;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConstructorResolver} focused on AOT constructor and factory
 * method resolution.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 20:51
 */
class ConstructorResolverAotTests {

  @Test
  void detectBeanInstanceExecutableWithBeanClassAndFactoryMethodName() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("testBean", "test");
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
            .addConstructorArgReference("testBean").getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            ReflectionUtils.findMethod(SampleFactory.class, "create", String.class));
  }

  @Test
  void detectBeanInstanceExecutableWithBeanClassNameAndFactoryMethodName() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("testBean", "test");
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SampleFactory.class.getName())
            .setFactoryMethod("create").addConstructorArgReference("testBean")
            .getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            ReflectionUtils.findMethod(SampleFactory.class, "create", String.class));
  }

  @Test
  void beanDefinitionWithFactoryMethodNameAndAssignableIndexedConstructorArgs() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("testNumber", 1L);
    beanFactory.registerSingleton("testBean", "test");
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
            .addConstructorArgReference("testNumber")
            .addConstructorArgReference("testBean").getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
            .findMethod(SampleFactory.class, "create", Number.class, String.class));
  }

  @Test
  void beanDefinitionWithFactoryMethodNameAndAssignableGenericConstructorArgs() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("test");
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1L);
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
            .findMethod(SampleFactory.class, "create", Number.class, String.class));
  }

  @Test
  void beanDefinitionWithFactoryMethodNameAndAssignableTypeStringValues() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues()
            .addGenericArgumentValue(new TypedStringValue("test"));
    beanDefinition.getConstructorArgumentValues()
            .addGenericArgumentValue(new TypedStringValue("1", Integer.class));
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
            .findMethod(SampleFactory.class, "create", Number.class, String.class));
  }

  @Test
  void beanDefinitionWithFactoryMethodNameAndMatchingMethodNames() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(DummySampleFactory.class).setFactoryMethod("of")
            .addConstructorArgValue(42).getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
            .findMethod(DummySampleFactory.class, "of", Integer.class));
  }

  @Test
  void beanDefinitionWithFactoryMethodNameAndOverriddenMethod() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", new RootBeanDefinition(ExtendedSampleFactory.class));
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(String.class).setFactoryMethodOnBean("resolve", "config")
            .addConstructorArgValue("test").getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
            .findMethod(ExtendedSampleFactory.class, "resolve", String.class));
  }

  @Test
  void detectBeanInstanceExecutableWithBeanClassAndFactoryMethodNameIgnoreTargetType() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("testBean", "test");
    RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(SampleFactory.class).setFactoryMethod("create")
            .addConstructorArgReference("testBean").getBeanDefinition();
    beanDefinition.setTargetType(String.class);
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            ReflectionUtils.findMethod(SampleFactory.class, "create", String.class));
  }

  @Test
  void beanDefinitionWithIndexedConstructorArgsForMultipleConstructors() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("testNumber", 1L);
    beanFactory.registerSingleton("testBean", "test");
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SampleBeanWithConstructors.class)
            .addConstructorArgReference("testNumber")
            .addConstructorArgReference("testBean").getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(SampleBeanWithConstructors.class
            .getDeclaredConstructor(Number.class, String.class));
  }

  @Test
  void beanDefinitionWithGenericConstructorArgsForMultipleConstructors() throws Exception {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("testNumber", 1L);
    beanFactory.registerSingleton("testBean", "test");
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SampleBeanWithConstructors.class)
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue("test");
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1L);
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(SampleBeanWithConstructors.class
            .getDeclaredConstructor(Number.class, String.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingIndexedValue() throws NoSuchMethodException {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .addConstructorArgValue(42).getBeanDefinition();
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingGenericValue() throws NoSuchMethodException {
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(42);
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingArrayFromIndexedValue() throws NoSuchMethodException {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorArraySample.class)
            .addConstructorArgValue(42).getBeanDefinition();
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(MultiConstructorArraySample.class
            .getDeclaredConstructor(Integer[].class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingArrayFromGenericValue() throws NoSuchMethodException {
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorArraySample.class)
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(42);
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(MultiConstructorArraySample.class
            .getDeclaredConstructor(Integer[].class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingListFromIndexedValue() throws NoSuchMethodException {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorListSample.class)
            .addConstructorArgValue(42).getBeanDefinition();
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorListSample.class.getDeclaredConstructor(List.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingListFromGenericValue() throws NoSuchMethodException {
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorListSample.class)
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(42);
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorListSample.class.getDeclaredConstructor(List.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingIndexedValueAsInnerBean() throws NoSuchMethodException {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .addConstructorArgValue(
                    BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
                            .addConstructorArgValue("42").getBeanDefinition())
            .getBeanDefinition();
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingGenericValueAsInnerBean() throws NoSuchMethodException {
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
            BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
                    .addConstructorArgValue("42").getBeanDefinition());
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingIndexedValueAsInnerBeanFactory() throws NoSuchMethodException {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .addConstructorArgValue(BeanDefinitionBuilder
                    .rootBeanDefinition(IntegerFactoryBean.class).getBeanDefinition())
            .getBeanDefinition();
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndMatchingGenericValueAsInnerBeanFactory() throws NoSuchMethodException {
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .getBeanDefinition();
    beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(
            BeanDefinitionBuilder.rootBeanDefinition(IntegerFactoryBean.class).getBeanDefinition());
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            MultiConstructorSample.class.getDeclaredConstructor(Integer.class));
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndNonMatchingValue() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .addConstructorArgValue(Locale.ENGLISH).getBeanDefinition();
    assertThatIllegalStateException().isThrownBy(() -> resolve(new StandardBeanFactory(), beanDefinition))
            .withMessageContaining(MultiConstructorSample.class.getName())
            .withMessageContaining("and argument types [java.util.Locale]");
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndNonMatchingValueAsInnerBean() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSample.class)
            .addConstructorArgValue(BeanDefinitionBuilder
                    .rootBeanDefinition(Locale.class, "getDefault")
                    .getBeanDefinition())
            .getBeanDefinition();
    assertThatIllegalStateException().isThrownBy(() -> resolve(new StandardBeanFactory(), beanDefinition))
            .withMessageContaining(MultiConstructorSample.class.getName())
            .withMessageContaining("and argument types [java.util.Locale]");
  }

  @Test
  void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClass() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(
            ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
    beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull()
            .isEqualTo(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
  }

  @Test
  void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClassAndNoResolvableType() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull()
            .isEqualTo(NumberHolderFactoryBean.class.getDeclaredConstructors()[0]);
  }

  @Test
  void detectBeanInstanceExecutableWithFactoryBeanSetInBeanClassThatDoesNotMatchTargetType() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(
            ResolvableType.forClassWithGenerics(NumberHolder.class, String.class));
    beanDefinition.setBeanClass(NumberHolderFactoryBean.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> resolve(beanFactory, beanDefinition))
            .withMessageContaining("Incompatible target type")
            .withMessageContaining(NumberHolder.class.getName())
            .withMessageContaining(NumberHolderFactoryBean.class.getName());
  }

  @Test
  void beanDefinitionWithClassArrayConstructorArgAndStringArrayValueType() throws NoSuchMethodException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(ConstructorClassArraySample.class.getName())
            .addConstructorArgValue(new String[] { "test1, test2" })
            .getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(
            ConstructorClassArraySample.class.getDeclaredConstructor(Class[].class));
  }

  @Test
  void beanDefinitionWithClassArrayConstructorArgAndStringValueType() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(ConstructorClassArraySample.class.getName())
            .addConstructorArgValue("test1").getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isIn(
            List.of(ConstructorClassArraySample.class.getDeclaredConstructors()));
  }

  @Test
  void beanDefinitionWithClassArrayConstructorArgAndAnotherMatchingConstructor() throws NoSuchMethodException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorClassArraySample.class.getName())
            .addConstructorArgValue(new String[] { "test1, test2" })
            .getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull()
            .isEqualTo(MultiConstructorClassArraySample.class
                    .getDeclaredConstructor(String[].class));
  }

  @Test
  void beanDefinitionWithClassArrayFactoryMethodArgAndStringArrayValueType() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(ClassArrayFactoryMethodSample.class.getName())
            .setFactoryMethod("of")
            .addConstructorArgValue(new String[] { "test1, test2" })
            .getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull().isEqualTo(ReflectionUtils
            .findMethod(ClassArrayFactoryMethodSample.class, "of", Class[].class));
  }

  @Test
  void beanDefinitionWithClassArrayFactoryMethodArgAndAnotherMatchingConstructor() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(
                    ClassArrayFactoryMethodSampleWithAnotherFactoryMethod.class.getName())
            .setFactoryMethod("of").addConstructorArgValue("test1")
            .getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull()
            .isEqualTo(ReflectionUtils.findMethod(
                    ClassArrayFactoryMethodSampleWithAnotherFactoryMethod.class, "of",
                    String[].class));
  }

  @Test
  void beanDefinitionWithMultiConstructorSimilarArgumentsAndMatchingValues() throws NoSuchMethodException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSimilarArgumentsSample.class)
            .addConstructorArgValue("Test").addConstructorArgValue(1).addConstructorArgValue(2)
            .getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull()
            .isEqualTo(MultiConstructorSimilarArgumentsSample.class
                    .getDeclaredConstructor(String.class, Integer.class, Integer.class));
  }

  @Test
  void beanDefinitionWithMultiConstructorSimilarArgumentsAndNullValueForCommonArgument() throws NoSuchMethodException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSimilarArgumentsSample.class)
            .addConstructorArgValue(null).addConstructorArgValue(null).addConstructorArgValue("Test")
            .getBeanDefinition();
    Executable executable = resolve(beanFactory, beanDefinition);
    assertThat(executable).isNotNull()
            .isEqualTo(MultiConstructorSimilarArgumentsSample.class
                    .getDeclaredConstructor(String.class, Integer.class, String.class));
  }

  @Test
  void beanDefinitionWithMultiConstructorSimilarArgumentsAndNullValueForSpecificArgument() throws NoSuchMethodException {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(MultiConstructorSimilarArgumentsSample.class)
            .addConstructorArgValue(null).addConstructorArgValue(1).addConstructorArgValue(null)
            .getBeanDefinition();
    assertThatIllegalStateException().isThrownBy(() -> resolve(beanFactory, beanDefinition))
            .withMessageContaining(MultiConstructorSimilarArgumentsSample.class.getName());
  }

  @Test
  void beanDefinitionWithMultiArgConstructorAndPrimitiveConversion() throws NoSuchMethodException {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(ConstructorPrimitiveFallback.class)
            .addConstructorArgValue("true").getBeanDefinition();
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isEqualTo(
            ConstructorPrimitiveFallback.class.getDeclaredConstructor(boolean.class));
  }

  @Test
  void beanDefinitionWithFactoryWithOverloadedClassMethodsOnInterface() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(FactoryWithOverloadedClassMethodsOnInterface.class)
            .setFactoryMethod("byAnnotation").addConstructorArgValue(Nullable.class)
            .getBeanDefinition();
    Executable executable = resolve(new StandardBeanFactory(), beanDefinition);
    assertThat(executable).isEqualTo(ReflectionUtils.findMethod(
            FactoryWithOverloadedClassMethodsOnInterface.class, "byAnnotation",
            Class.class));
  }

  private Executable resolve(StandardBeanFactory beanFactory, BeanDefinition beanDefinition) {
    return new ConstructorResolver(beanFactory)
            .resolveConstructorOrFactoryMethod("testBean", (RootBeanDefinition) beanDefinition);
  }

  static class IntegerFactoryBean implements FactoryBean<Integer> {

    @Override
    public Integer getObject() {
      return 42;
    }

    @Override
    public Class<?> getObjectType() {
      return Integer.class;
    }
  }

  @SuppressWarnings("unused")
  static class MultiConstructorSample {

    MultiConstructorSample(String name) {
    }

    MultiConstructorSample(Integer value) {
    }
  }

  @SuppressWarnings("unused")
  static class MultiConstructorArraySample {

    public MultiConstructorArraySample(String... names) {
    }

    public MultiConstructorArraySample(Integer... values) {
    }
  }

  @SuppressWarnings("unused")
  static class MultiConstructorListSample {

    public MultiConstructorListSample(String name) {
    }

    public MultiConstructorListSample(List<Integer> values) {
    }
  }

  interface DummyInterface {

    static String of(Object o) {
      return o.toString();
    }
  }

  @SuppressWarnings("unused")
  static class DummySampleFactory implements DummyInterface {

    static String of(Integer value) {
      return value.toString();
    }

    protected String resolve(String value) {
      return value;
    }
  }

  @SuppressWarnings("unused")
  static class ExtendedSampleFactory extends DummySampleFactory {

    @Override
    protected String resolve(String value) {
      return super.resolve(value);
    }
  }

  @SuppressWarnings("unused")
  static class ConstructorClassArraySample {

    ConstructorClassArraySample(Class<?>... classArrayArg) {
    }

    ConstructorClassArraySample(Executor somethingElse) {
    }
  }

  @SuppressWarnings("unused")
  static class MultiConstructorClassArraySample {

    MultiConstructorClassArraySample(Class<?>... classArrayArg) {
    }

    MultiConstructorClassArraySample(String... stringArrayArg) {
    }
  }

  @SuppressWarnings("unused")
  static class ClassArrayFactoryMethodSample {

    static String of(Class<?>[] classArrayArg) {
      return "test";
    }
  }

  @SuppressWarnings("unused")
  static class ClassArrayFactoryMethodSampleWithAnotherFactoryMethod {

    static String of(Class<?>[] classArrayArg) {
      return "test";
    }

    static String of(String[] classArrayArg) {
      return "test";
    }
  }

  @SuppressWarnings("unnused")
  static class ConstructorPrimitiveFallback {

    public ConstructorPrimitiveFallback(boolean useDefaultExecutor) {
    }

    public ConstructorPrimitiveFallback(Executor executor) {
    }
  }

  static class SampleBeanWithConstructors {

    public SampleBeanWithConstructors() {
    }

    public SampleBeanWithConstructors(String name) {
    }

    public SampleBeanWithConstructors(Number number, String name) {
    }
  }

  interface FactoryWithOverloadedClassMethodsOnInterface {

    static FactoryWithOverloadedClassMethodsOnInterface byAnnotation(
            Class<? extends Annotation> annotationType) {
      return byAnnotation(annotationType, SearchStrategy.INHERITED_ANNOTATIONS);
    }

    static FactoryWithOverloadedClassMethodsOnInterface byAnnotation(
            Class<? extends Annotation> annotationType,
            SearchStrategy searchStrategy) {
      return null;
    }
  }

  static class MultiConstructorSimilarArgumentsSample {

    MultiConstructorSimilarArgumentsSample(String name, Integer counter, String value) {
    }

    MultiConstructorSimilarArgumentsSample(String name, Integer counter, Integer value) {
    }
  }
}
