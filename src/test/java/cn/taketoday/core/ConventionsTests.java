/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.objects.TestObject;
import cn.taketoday.util.ReflectionUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
