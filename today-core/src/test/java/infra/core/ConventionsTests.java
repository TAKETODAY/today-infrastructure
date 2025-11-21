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

package infra.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.tests.sample.objects.TestObject;
import infra.util.ReflectionUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link Conventions}.
 *
 * @author Rob Harrop
 * @author Sam Brannen
 */
class ConventionsTests {

  @Test
  void simpleObject() {
    assertThat(Conventions.getVariableName(new TestObject())).as("Incorrect singular variable name").isEqualTo("testObject");
    assertThat(Conventions.getVariableNameForParameter(getMethodParameter(TestObject.class))).as("Incorrect singular variable name").isEqualTo("testObject");
    assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(TestObject.class))).as("Incorrect singular variable name").isEqualTo("testObject");
  }

  @Test
  void array() {
    Object actual = Conventions.getVariableName(new TestObject[0]);
    assertThat(actual).as("Incorrect plural array form").isEqualTo("testObjectList");
  }

  @Test
  void list() {
    assertThat(Conventions.getVariableName(Collections.singletonList(new TestObject()))).as("Incorrect plural List form").isEqualTo("testObjectList");
    assertThat(Conventions.getVariableNameForParameter(getMethodParameter(List.class))).as("Incorrect plural List form").isEqualTo("testObjectList");
    assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(List.class))).as("Incorrect plural List form").isEqualTo("testObjectList");
  }

  @Test
  void emptyList() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            Conventions.getVariableName(new ArrayList<>()));
  }

  @Test
  void set() {
    assertThat(Conventions.getVariableName(Collections.singleton(new TestObject()))).as("Incorrect plural Set form").isEqualTo("testObjectList");
    assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Set.class))).as("Incorrect plural Set form").isEqualTo("testObjectList");
    assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Set.class))).as("Incorrect plural Set form").isEqualTo("testObjectList");
  }

  @Test
  void reactiveParameters() {
    assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Mono.class))).isEqualTo("testObjectMono");
    assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Flux.class))).isEqualTo("testObjectFlux");
    assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Single.class))).isEqualTo("testObjectSingle");
    assertThat(Conventions.getVariableNameForParameter(getMethodParameter(Observable.class))).isEqualTo("testObjectObservable");
  }

  @Test
  void reactiveReturnTypes() {
    assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Mono.class))).isEqualTo("testObjectMono");
    assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Flux.class))).isEqualTo("testObjectFlux");
    assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Single.class))).isEqualTo("testObjectSingle");
    assertThat(Conventions.getVariableNameForReturnType(getMethodForReturnType(Observable.class))).isEqualTo("testObjectObservable");
  }

  @Test
  void attributeNameToPropertyName() {
    assertThat(Conventions.attributeNameToPropertyName("transaction-manager")).isEqualTo("transactionManager");
    assertThat(Conventions.attributeNameToPropertyName("pointcut-ref")).isEqualTo("pointcutRef");
    assertThat(Conventions.attributeNameToPropertyName("lookup-on-startup")).isEqualTo("lookupOnStartup");
  }

  @Test
  void getQualifiedAttributeName() {
    String baseName = "foo";
    Class<String> cls = String.class;
    String desiredResult = "java.lang.String.foo";
    assertThat(Conventions.getQualifiedAttributeName(cls, baseName)).isEqualTo(desiredResult);
  }

  @Test
  void nullValueThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            Conventions.getVariableName(null));
  }

  @Test
  void emptyCollectionThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            Conventions.getVariableName(Collections.emptyList()));
  }

  @Test
  void collectionWithNullElementThrowsException() {
    List<Object> list = Collections.singletonList(null);
    assertThatIllegalStateException().isThrownBy(() ->
            Conventions.getVariableName(list));
  }

  @Test
  void proxyClassReturnsInterfaceName() {
    Object proxy = Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] { Runnable.class },
            (p, m, args) -> null);
    assertThat(Conventions.getVariableName(proxy)).isEqualTo("runnable");
  }

  @Test
  void arrayComponentNameIsPluralizedCorrectly() {
    String[] array = new String[0];
    assertThat(Conventions.getVariableName(array)).isEqualTo("stringList");
  }

  @Test
  void innerClassNameIsHandledCorrectly() {
    InnerTestClass inner = new InnerTestClass();
    assertThat(Conventions.getVariableName(inner)).isEqualTo("innerTestClass");
  }

  @Test
  void nullAttributeNameThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            Conventions.attributeNameToPropertyName(null));
  }

  @Test
  void nullClassForQualifiedNameThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            Conventions.getQualifiedAttributeName(null, "test"));
  }

  @Test
  void nullAttributeForQualifiedNameThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            Conventions.getQualifiedAttributeName(String.class, null));
  }

  @Test
  void returnTypeWithNonEmptyCollectionValue() {
    Method method = ReflectionUtils.getMethod(TestBean.class, "handleToList", (Class<?>[]) null);
    List<TestObject> value = Collections.singletonList(new TestObject());
    assertThat(Conventions.getVariableNameForReturnType(method, value))
            .isEqualTo("testObjectList");
  }

  @Test
  void getVariableNameForObjectArray() {
    TestObject[] array = new TestObject[0];
    assertThat(Conventions.getVariableName(array)).isEqualTo("testObjectList");
  }

  @Test
  void getVariableNameForPrimitiveArray() {
    int[] array = new int[0];
    assertThat(Conventions.getVariableName(array)).isEqualTo("intList");
  }

  @Test
  void getVariableNameForProxyObject() {
    Runnable proxy = (Runnable) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] { Runnable.class },
            (proxy1, method, args) -> null);

    assertThat(Conventions.getVariableName(proxy)).isEqualTo("runnable");
  }

  @Test
  void getVariableNameForInnerClass() {
    ConventionsTests.InnerTestClass inner = new ConventionsTests.InnerTestClass();
    assertThat(Conventions.getVariableName(inner)).isEqualTo("innerTestClass");
  }

  @Test
  void getVariableNameForAnonymousClass() {
    Object anonymous = new Object() { };
    assertThat(Conventions.getVariableName(anonymous)).isEqualTo("object");
  }

  @Test
  void getVariableNameForTypedCollection() {
    List<String> typedList = Collections.singletonList("test");
    assertThat(Conventions.getVariableName(typedList)).isEqualTo("stringList");
  }

  @Test
  void getVariableNameForUntypedCollectionWithNonNullElements() {
    List<Object> untypedList = Collections.singletonList("test");
    assertThat(Conventions.getVariableName(untypedList)).isEqualTo("stringList");
  }

  @Test
  void getVariableNameForParameterWithReactiveTypes() {
    Parameter monoParam = getMethodParameter(Mono.class);
    Parameter fluxParam = getMethodParameter(Flux.class);
    Parameter singleParam = getMethodParameter(Single.class);
    Parameter observableParam = getMethodParameter(Observable.class);

    assertThat(Conventions.getVariableNameForParameter(monoParam)).isEqualTo("testObjectMono");
    assertThat(Conventions.getVariableNameForParameter(fluxParam)).isEqualTo("testObjectFlux");
    assertThat(Conventions.getVariableNameForParameter(singleParam)).isEqualTo("testObjectSingle");
    assertThat(Conventions.getVariableNameForParameter(observableParam)).isEqualTo("testObjectObservable");
  }

  @Test
  void getVariableNameForParameterWithArrayType() {
    Parameter arrayParam = Arrays.stream(TestBean.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("handle"))
            .findFirst()
            .map(m -> m.getParameters()[0])
            .orElseThrow();

    assertThat(Conventions.getVariableNameForParameter(arrayParam)).isEqualTo("testObject");
  }

  @Test
  void getVariableNameForParameterWithCollectionType() {
    Parameter listParam = getMethodParameter(List.class);
    assertThat(Conventions.getVariableNameForParameter(listParam)).isEqualTo("testObjectList");
  }

  @Test
  void getVariableNameForReturnTypeWithReactiveTypes() {
    Method monoMethod = getMethodForReturnType(Mono.class);
    Method fluxMethod = getMethodForReturnType(Flux.class);
    Method singleMethod = getMethodForReturnType(Single.class);
    Method observableMethod = getMethodForReturnType(Observable.class);

    assertThat(Conventions.getVariableNameForReturnType(monoMethod)).isEqualTo("testObjectMono");
    assertThat(Conventions.getVariableNameForReturnType(fluxMethod)).isEqualTo("testObjectFlux");
    assertThat(Conventions.getVariableNameForReturnType(singleMethod)).isEqualTo("testObjectSingle");
    assertThat(Conventions.getVariableNameForReturnType(observableMethod)).isEqualTo("testObjectObservable");
  }

  @Test
  void getVariableNameForReturnTypeWithCollectionType() {
    Method listMethod = getMethodForReturnType(List.class);
    assertThat(Conventions.getVariableNameForReturnType(listMethod)).isEqualTo("testObjectList");
  }

  @Test
  void attributeNameToPropertyNameWithoutHyphens() {
    assertThat(Conventions.attributeNameToPropertyName("simple")).isEqualTo("simple");
  }

  @Test
  void attributeNameToPropertyNameWithMultipleHyphens() {
    assertThat(Conventions.attributeNameToPropertyName("multi-word-name")).isEqualTo("multiWordName");
  }

  @Test
  void attributeNameToPropertyNameWithLeadingHyphen() {
    assertThat(Conventions.attributeNameToPropertyName("-name")).isEqualTo("Name");
  }

  @Test
  void attributeNameToPropertyNameWithTrailingHyphen() {
    assertThat(Conventions.attributeNameToPropertyName("name-")).isEqualTo("name");
  }

  @Test
  void getQualifiedAttributeNameWithNestedClass() {
    String result = Conventions.getQualifiedAttributeName(InnerTestClass.class, "field");
    assertThat(result).isEqualTo("infra.core.ConventionsTests$InnerTestClass.field");
  }

  @Test
  void getVariableNameForParameterWithMethodParameter() throws Exception {
    Method method = TestBean.class.getDeclaredMethod("handle", TestObject.class, List.class, Set.class,
            Mono.class, Flux.class, Single.class, Observable.class);
    MethodParameter methodParameter = new MethodParameter(method, 0);

    assertThat(Conventions.getVariableNameForParameter(methodParameter)).isEqualTo("testObject");
  }

  @Test
  void getVariableNameForParameterWithMethodParameterForList() throws Exception {
    Method method = TestBean.class.getDeclaredMethod("handle", TestObject.class, List.class, Set.class,
            Mono.class, Flux.class, Single.class, Observable.class);
    MethodParameter methodParameter = new MethodParameter(method, 1);

    assertThat(Conventions.getVariableNameForParameter(methodParameter)).isEqualTo("testObjectList");
  }

  @Test
  void getVariableNameForParameterWithMethodParameterForReactiveTypes() throws Exception {
    Method method = TestBean.class.getDeclaredMethod("handle", TestObject.class, List.class, Set.class,
            Mono.class, Flux.class, Single.class, Observable.class);

    MethodParameter monoParam = new MethodParameter(method, 3);
    MethodParameter fluxParam = new MethodParameter(method, 4);
    MethodParameter singleParam = new MethodParameter(method, 5);
    MethodParameter observableParam = new MethodParameter(method, 6);

    assertThat(Conventions.getVariableNameForParameter(monoParam)).isEqualTo("testObjectMono");
    assertThat(Conventions.getVariableNameForParameter(fluxParam)).isEqualTo("testObjectFlux");
    assertThat(Conventions.getVariableNameForParameter(singleParam)).isEqualTo("testObjectSingle");
    assertThat(Conventions.getVariableNameForParameter(observableParam)).isEqualTo("testObjectObservable");
  }

  @Test
  void getVariableNameForReturnTypeWithUntypedCollectionAndNonNullValue() throws Exception {
    Method method = TestBean.class.getMethod("handleToList");
    List<TestObject> value = Collections.singletonList(new TestObject());
    assertThat(Conventions.getVariableNameForReturnType(method, value)).isEqualTo("testObjectList");
  }

  @Test
  void attributeNameToPropertyNameWithSingleCharacter() {
    assertThat(Conventions.attributeNameToPropertyName("a")).isEqualTo("a");
  }

  @Test
  void attributeNameToPropertyNameWithConsecutiveHyphens() {
    assertThat(Conventions.attributeNameToPropertyName("a--b")).isEqualTo("aB");
  }

  @Test
  void attributeNameToPropertyNameWithOnlyHyphens() {
    assertThat(Conventions.attributeNameToPropertyName("--")).isEqualTo("");
  }

  @Test
  void getQualifiedAttributeNameWithPrimitiveType() {
    String result = Conventions.getQualifiedAttributeName(int.class, "value");
    assertThat(result).isEqualTo("int.value");
  }

  @Test
  void getClassForValueWithSpecialSubclass() {
    // Creating a special subclass simulating the behavior described in the method comments
    class SpecialSubclass extends TestObject {
      // This nested class has $ in its name but no declaring class
    }

    SpecialSubclass instance = new SpecialSubclass();
    String className = instance.getClass().getName();
    // Verify our assumption about the class name
    assertThat(className).contains("$");
    assertThat(instance.getClass().getDeclaringClass()).isNull();

    // We can't easily test the exact logic without mocking or complex reflection,
    // but we can at least verify the method doesn't throw
    assertThatCode(() -> {
      // Access private method via reflection
      java.lang.reflect.Method method = Conventions.class.getDeclaredMethod("getClassForValue", Object.class);
      method.setAccessible(true);
      method.invoke(null, instance);
    }).doesNotThrowAnyException();
  }

  private static class InnerTestClass {
  }

  private static Parameter getMethodParameter(Class<?> parameterType) {
    Method method = ReflectionUtils.getMethod(TestBean.class, "handle", (Class<?>[]) null);
    for (int i = 0; i < method.getParameterCount(); i++) {
      if (parameterType.equals(method.getParameterTypes()[i])) {
        return method.getParameters()[i];
      }
    }
    throw new IllegalArgumentException("Parameter type not found: " + parameterType);
  }

  private static Method getMethodForReturnType(Class<?> returnType) {
    return Arrays.stream(TestBean.class.getMethods())
            .filter(method -> method.getReturnType().equals(returnType))
            .findFirst()
            .orElseThrow(() ->
                    new IllegalArgumentException("Unique return type not found: " + returnType));
  }

  @SuppressWarnings("unused")
  private static class TestBean {

    public void handle(TestObject to,
            List<TestObject> toList, Set<TestObject> toSet,
            Mono<TestObject> toMono, Flux<TestObject> toFlux,
            Single<TestObject> toSingle, Observable<TestObject> toObservable) { }

    public TestObject handleTo() { return null; }

    public List<TestObject> handleToList() { return null; }

    public Set<TestObject> handleToSet() { return null; }

    public Mono<TestObject> handleToMono() { return null; }

    public Flux<TestObject> handleToFlux() { return null; }

    public Single<TestObject> handleToSingle() { return null; }

    public Observable<TestObject> handleToObservable() { return null; }

  }

}
