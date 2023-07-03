/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.aot.hint.ExecutableHint;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.context.properties.BoundConfigurationProperties;
import cn.taketoday.context.properties.ConfigurationPropertiesBean;
import cn.taketoday.context.properties.NestedConfigurationProperty;
import cn.taketoday.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 17:41
 */
class BindableRuntimeHintsRegistrarTests {

  @Test
  void registerHints() {
    RuntimeHints runtimeHints = new RuntimeHints();
    Class<?>[] types = { BoundConfigurationProperties.class, ConfigurationPropertiesBean.class };
    BindableRuntimeHintsRegistrar registrar = new BindableRuntimeHintsRegistrar(types);
    registrar.registerHints(runtimeHints);
    for (Class<?> type : types) {
      assertThat(RuntimeHintsPredicates.reflection().onType(type)).accepts(runtimeHints);
    }
  }

  @Test
  void registerHintsWithIterable() {
    RuntimeHints runtimeHints = new RuntimeHints();
    List<Class<?>> types = Arrays.asList(BoundConfigurationProperties.class, ConfigurationPropertiesBean.class);
    BindableRuntimeHintsRegistrar registrar = BindableRuntimeHintsRegistrar.forTypes(types);
    registrar.registerHints(runtimeHints);
    for (Class<?> type : types) {
      assertThat(RuntimeHintsPredicates.reflection().onType(type)).accepts(runtimeHints);
    }
  }

  @Test
  void registerHintsWhenNoClasses() {
    RuntimeHints runtimeHints = new RuntimeHints();
    BindableRuntimeHintsRegistrar registrar = new BindableRuntimeHintsRegistrar(new Class<?>[0]);
    registrar.registerHints(runtimeHints);
    assertThat(runtimeHints.reflection().typeHints()).isEmpty();
  }

  @Test
  void registerHintsViaForType() {
    RuntimeHints runtimeHints = new RuntimeHints();
    Class<?>[] types = { BoundConfigurationProperties.class, ConfigurationPropertiesBean.class };
    BindableRuntimeHintsRegistrar registrar = BindableRuntimeHintsRegistrar.forTypes(types);
    registrar.registerHints(runtimeHints);
    for (Class<?> type : types) {
      assertThat(RuntimeHintsPredicates.reflection().onType(type)).accepts(runtimeHints);
    }
  }

  @Test
  void registerHintsWhenJavaBean() {
    RuntimeHints runtimeHints = registerHints(JavaBean.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement().satisfies(javaBeanBinding(JavaBean.class));
  }

  @Test
  void registerHintsWhenJavaBeanWithSeveralConstructors() throws NoSuchMethodException {
    RuntimeHints runtimeHints = registerHints(WithSeveralConstructors.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement()
            .satisfies(javaBeanBinding(WithSeveralConstructors.class,
                    WithSeveralConstructors.class.getDeclaredConstructor()));
  }

  @Test
  void registerHintsWhenJavaBeanWithMapOfPojo() {
    RuntimeHints runtimeHints = registerHints(WithMap.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
            .anySatisfy(javaBeanBinding(WithMap.class, "getAddresses"))
            .anySatisfy(javaBeanBinding(Address.class));
  }

  @Test
  void registerHintsWhenJavaBeanWithListOfPojo() {
    RuntimeHints runtimeHints = registerHints(WithList.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
            .anySatisfy(javaBeanBinding(WithList.class, "getAllAddresses"))
            .anySatisfy(javaBeanBinding(Address.class));
  }

  @Test
  void registerHintsWhenJavaBeanWitArrayOfPojo() {
    RuntimeHints runtimeHints = registerHints(WithArray.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
            .anySatisfy(javaBeanBinding(WithArray.class, "getAllAddresses"))
            .anySatisfy(javaBeanBinding(Address.class));
  }

  @Test
  void registerHintsWhenJavaBeanWithListOfJavaType() {
    RuntimeHints runtimeHints = registerHints(WithSimpleList.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement()
            .satisfies(javaBeanBinding(WithSimpleList.class, "getNames"));
  }

  @Test
  void registerHintsWhenValueObject() {
    RuntimeHints runtimeHints = registerHints(Immutable.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement()
            .satisfies(valueObjectBinding(Immutable.class));
  }

  @Test
  void registerHintsWhenValueObjectWithSpecificConstructor() throws NoSuchMethodException {
    RuntimeHints runtimeHints = registerHints(ImmutableWithSeveralConstructors.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement()
            .satisfies(valueObjectBinding(ImmutableWithSeveralConstructors.class,
                    ImmutableWithSeveralConstructors.class.getDeclaredConstructor(String.class)));
  }

  @Test
  void registerHintsWhenValueObjectWithSeveralLayersOfPojo() {
    RuntimeHints runtimeHints = registerHints(ImmutableWithList.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(3)
            .anySatisfy(valueObjectBinding(ImmutableWithList.class))
            .anySatisfy(valueObjectBinding(Person.class))
            .anySatisfy(valueObjectBinding(Address.class));
  }

  @Test
  void registerHintsWhenHasNestedTypeNotUsedIsIgnored() {
    RuntimeHints runtimeHints = registerHints(WithNested.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement().satisfies(javaBeanBinding(WithNested.class));
  }

  @Test
  void registerHintsWhenWhenHasNestedExternalType() {
    RuntimeHints runtimeHints = registerHints(WithExternalNested.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(3)
            .anySatisfy(
                    javaBeanBinding(WithExternalNested.class, "getName", "setName", "getSampleType", "setSampleType"))
            .anySatisfy(javaBeanBinding(SampleType.class, "getNested"))
            .anySatisfy(javaBeanBinding(SampleType.Nested.class));
  }

  @Test
  void registerHintsWhenHasRecursiveType() {
    RuntimeHints runtimeHints = registerHints(WithRecursive.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
            .anySatisfy(javaBeanBinding(WithRecursive.class, "getRecursive", "setRecursive"))
            .anySatisfy(javaBeanBinding(Recursive.class, "getRecursive", "setRecursive"));
  }

  @Test
  void registerHintsWhenValueObjectWithRecursiveType() {
    RuntimeHints runtimeHints = registerHints(ImmutableWithRecursive.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
            .anySatisfy(valueObjectBinding(ImmutableWithRecursive.class))
            .anySatisfy(valueObjectBinding(ImmutableRecursive.class));
  }

  @Test
  void registerHintsWhenHasWellKnownTypes() {
    RuntimeHints runtimeHints = registerHints(WithWellKnownTypes.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement()
            .satisfies(javaBeanBinding(WithWellKnownTypes.class, "getApplicationContext", "setApplicationContext",
                    "getEnvironment", "setEnvironment"));
  }

  @Test
  void registerHintsWhenHasCrossReference() {
    RuntimeHints runtimeHints = registerHints(WithCrossReference.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(3)
            .anySatisfy(javaBeanBinding(WithCrossReference.class, "getCrossReferenceA", "setCrossReferenceA"))
            .anySatisfy(javaBeanBinding(CrossReferenceA.class, "getCrossReferenceB", "setCrossReferenceB"))
            .anySatisfy(javaBeanBinding(CrossReferenceB.class, "getCrossReferenceA", "setCrossReferenceA"));
  }

  @Test
  void pregisterHintsWhenHasUnresolvedGeneric() {
    RuntimeHints runtimeHints = registerHints(WithGeneric.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(2)
            .anySatisfy(javaBeanBinding(WithGeneric.class, "getGeneric"))
            .anySatisfy(javaBeanBinding(GenericObject.class));
  }

  @Test
  void registerHintsWhenHasNestedGenerics() {
    RuntimeHints runtimeHints = registerHints(NestedGenerics.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(2);
    assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(NestedGenerics.Nested.class)).accepts(runtimeHints);
  }

  @Test
  void registerHintsWhenHasMultipleNestedClasses() {
    RuntimeHints runtimeHints = registerHints(TripleNested.class);
    assertThat(runtimeHints.reflection().typeHints()).hasSize(3);
    assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onType(TripleNested.DoubleNested.Nested.class))
            .accepts(runtimeHints);
  }

  @Test
  void registerHintsWhenHasPackagePrivateGettersAndSetters() {
    RuntimeHints runtimeHints = registerHints(PackagePrivateGettersAndSetters.class);
    assertThat(runtimeHints.reflection().typeHints()).singleElement()
            .satisfies(javaBeanBinding(PackagePrivateGettersAndSetters.class, "getAlpha", "setAlpha", "getBravo",
                    "setBravo"));
  }

  private Consumer<TypeHint> javaBeanBinding(Class<?> type, String... expectedMethods) {
    return javaBeanBinding(type, type.getDeclaredConstructors()[0], expectedMethods);
  }

  private Consumer<TypeHint> javaBeanBinding(Class<?> type, Constructor<?> constructor, String... expectedMethods) {
    return (entry) -> {
      assertThat(entry.getType()).isEqualTo(TypeReference.of(type));
      assertThat(entry.constructors()).singleElement().satisfies(match(constructor));
      assertThat(entry.getMemberCategories()).isEmpty();
      assertThat(entry.methods()).extracting(ExecutableHint::getName).containsExactlyInAnyOrder(expectedMethods);
    };
  }

  private Consumer<TypeHint> valueObjectBinding(Class<?> type) {
    return valueObjectBinding(type, type.getDeclaredConstructors()[0]);
  }

  private Consumer<TypeHint> valueObjectBinding(Class<?> type, Constructor<?> constructor) {
    return (entry) -> {
      assertThat(entry.getType()).isEqualTo(TypeReference.of(type));
      assertThat(entry.constructors()).singleElement().satisfies(match(constructor));
      assertThat(entry.getMemberCategories()).isEmpty();
      assertThat(entry.methods()).isEmpty();
    };
  }

  private Consumer<ExecutableHint> match(Constructor<?> constructor) {
    return (executableHint) -> {
      assertThat(executableHint.getName()).isEqualTo("<init>");
      assertThat(Arrays.stream(constructor.getParameterTypes()).map(TypeReference::of).toList())
              .isEqualTo(executableHint.getParameterTypes());
    };
  }

  private RuntimeHints registerHints(Class<?>... types) {
    RuntimeHints hints = new RuntimeHints();
    BindableRuntimeHintsRegistrar.forTypes(types).registerHints(hints);
    return hints;
  }

  public static class JavaBean {

  }

  public static class WithSeveralConstructors {

    WithSeveralConstructors() {
    }

    WithSeveralConstructors(String ignored) {
    }

  }

  public static class WithMap {

    public Map<String, Address> getAddresses() {
      return Collections.emptyMap();
    }

  }

  public static class WithList {

    public List<Address> getAllAddresses() {
      return Collections.emptyList();
    }

  }

  public static class WithSimpleList {

    public List<String> getNames() {
      return Collections.emptyList();
    }

  }

  public static class WithArray {

    public Address[] getAllAddresses() {
      return new Address[0];
    }

  }

  public static class Immutable {

    @SuppressWarnings("unused")
    private final String name;

    Immutable(String name) {
      this.name = name;
    }

  }

  public static class ImmutableWithSeveralConstructors {

    @SuppressWarnings("unused")
    private final String name;

    @ConstructorBinding
    ImmutableWithSeveralConstructors(String name) {
      this.name = name;
    }

    ImmutableWithSeveralConstructors() {
      this("test");
    }

  }

  public static class ImmutableWithList {

    @SuppressWarnings("unused")
    private final List<Person> family;

    ImmutableWithList(List<Person> family) {
      this.family = family;
    }

  }

  public static class WithNested {

    static class OneLevelDown {

    }

  }

  public static class WithExternalNested {

    private String name;

    @NestedConfigurationProperty
    private SampleType sampleType;

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public SampleType getSampleType() {
      return this.sampleType;
    }

    public void setSampleType(SampleType sampleType) {
      this.sampleType = sampleType;
    }

  }

  public static class WithRecursive {

    @NestedConfigurationProperty
    private Recursive recursive;

    public Recursive getRecursive() {
      return this.recursive;
    }

    public void setRecursive(Recursive recursive) {
      this.recursive = recursive;
    }

  }

  public static class ImmutableWithRecursive {

    @NestedConfigurationProperty
    private final ImmutableRecursive recursive;

    ImmutableWithRecursive(ImmutableRecursive recursive) {
      this.recursive = recursive;
    }

  }

  public static class WithWellKnownTypes implements ApplicationContextAware, EnvironmentAware {

    private ApplicationContext applicationContext;

    private Environment environment;

    public ApplicationContext getApplicationContext() {
      return this.applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    public Environment getEnvironment() {
      return this.environment;
    }

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

  }

  public static class SampleType {

    private final Nested nested = new Nested();

    public Nested getNested() {
      return this.nested;
    }

    static class Nested {

    }

  }

  public static class PackagePrivateGettersAndSetters {

    private String alpha;

    private Map<String, String> bravo;

    String getAlpha() {
      return this.alpha;
    }

    void setAlpha(String alpha) {
      this.alpha = alpha;
    }

    Map<String, String> getBravo() {
      return this.bravo;
    }

    void setBravo(Map<String, String> bravo) {
      this.bravo = bravo;
    }

  }

  public static class Address {

  }

  public static class Person {

    @SuppressWarnings("unused")
    private final String firstName;

    @SuppressWarnings("unused")
    private final String lastName;

    @NestedConfigurationProperty
    private final Address address;

    Person(String firstName, String lastName, Address address) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.address = address;
    }

  }

  public static class Recursive {

    private Recursive recursive;

    public Recursive getRecursive() {
      return this.recursive;
    }

    public void setRecursive(Recursive recursive) {
      this.recursive = recursive;
    }

  }

  public static class ImmutableRecursive {

    @SuppressWarnings("unused")
    private final ImmutableRecursive recursive;

    ImmutableRecursive(ImmutableRecursive recursive) {
      this.recursive = recursive;
    }

  }

  public static class WithCrossReference {

    @NestedConfigurationProperty
    private CrossReferenceA crossReferenceA;

    public void setCrossReferenceA(CrossReferenceA crossReferenceA) {
      this.crossReferenceA = crossReferenceA;
    }

    public CrossReferenceA getCrossReferenceA() {
      return this.crossReferenceA;
    }

  }

  public static class CrossReferenceA {

    @NestedConfigurationProperty
    private CrossReferenceB crossReferenceB;

    public void setCrossReferenceB(CrossReferenceB crossReferenceB) {
      this.crossReferenceB = crossReferenceB;
    }

    public CrossReferenceB getCrossReferenceB() {
      return this.crossReferenceB;
    }

  }

  public static class CrossReferenceB {

    private CrossReferenceA crossReferenceA;

    public void setCrossReferenceA(CrossReferenceA crossReferenceA) {
      this.crossReferenceA = crossReferenceA;
    }

    public CrossReferenceA getCrossReferenceA() {
      return this.crossReferenceA;
    }

  }

  public static class WithGeneric {

    @NestedConfigurationProperty
    private GenericObject<?> generic;

    public GenericObject<?> getGeneric() {
      return this.generic;
    }

  }

  public static final class GenericObject<T> {

    private final T value;

    GenericObject(T value) {
      this.value = value;
    }

    public T getValue() {
      return this.value;
    }

  }

  public static class NestedGenerics {

    private final Map<String, List<Nested>> nested = new HashMap<>();

    public Map<String, List<Nested>> getNested() {
      return this.nested;
    }

    public static class Nested {

      private String field;

      public String getField() {
        return this.field;
      }

      public void setField(String field) {
        this.field = field;
      }

    }

  }

  public static class TripleNested {

    private final DoubleNested doubleNested = new DoubleNested();

    public DoubleNested getDoubleNested() {
      return this.doubleNested;
    }

    public static class DoubleNested {

      private final Nested nested = new Nested();

      public Nested getNested() {
        return this.nested;
      }

      public static class Nested {

        private String field;

        public String getField() {
          return this.field;
        }

        public void setField(String field) {
          this.field = field;
        }

      }

    }

  }

}