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

package infra.aot.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstrumentedMethod}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class InstrumentedMethodTests {

  private RuntimeHints hints = new RuntimeHints();

  @Nested
  class ClassReflectionInstrumentationTests {

    RecordedInvocation stringGetClasses = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCLASSES)
            .onInstance(String.class).returnValue(String.class.getClasses()).build();

    RecordedInvocation stringGetDeclaredClasses = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCLASSES)
            .onInstance(String.class).returnValue(String.class.getDeclaredClasses()).build();

    @Test
    void classForNameShouldMatchReflectionOnType() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_FORNAME)
              .withArgument("java.lang.String").returnValue(String.class).build();
      hints.reflection().registerType(String.class);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_FORNAME, invocation);
    }

    @Test
    void classGetClassesShouldNotMatchReflectionOnType() {
      hints.reflection().registerType(String.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETCLASSES, this.stringGetClasses);
    }

    @Test
    void classGetClassesShouldMatchPublicClasses() {
      hints.reflection().registerType(String.class, MemberCategory.PUBLIC_CLASSES);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCLASSES, this.stringGetClasses);
    }

    @Test
    void classGetClassesShouldMatchDeclaredClasses() {
      hints.reflection().registerType(String.class, MemberCategory.DECLARED_CLASSES);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCLASSES, this.stringGetClasses);
    }

    @Test
    void classGetDeclaredClassesShouldMatchDeclaredClassesHint() {
      hints.reflection().registerType(String.class, MemberCategory.DECLARED_CLASSES);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCLASSES, this.stringGetDeclaredClasses);
    }

    @Test
    void classGetDeclaredClassesShouldNotMatchPublicClassesHint() {
      hints.reflection().registerType(String.class, MemberCategory.PUBLIC_CLASSES);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCLASSES, this.stringGetDeclaredClasses);
    }

    @Test
    void classLoaderLoadClassShouldMatchReflectionHintOnType() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASSLOADER_LOADCLASS)
              .onInstance(ClassLoader.getSystemClassLoader())
              .withArgument(PublicField.class.getCanonicalName()).returnValue(PublicField.class).build();
      hints.reflection().registerType(PublicField.class);
      assertThatInvocationMatches(InstrumentedMethod.CLASSLOADER_LOADCLASS, invocation);
    }

  }

  @Nested
  class ConstructorReflectionInstrumentationTests {

    RecordedInvocation stringGetConstructor;

    RecordedInvocation stringGetConstructors = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTORS)
            .onInstance(String.class).returnValue(String.class.getConstructors()).build();

    RecordedInvocation stringGetDeclaredConstructor;

    RecordedInvocation stringGetDeclaredConstructors = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS)
            .onInstance(String.class).returnValue(String.class.getDeclaredConstructors()).build();

    @BeforeEach
    public void setup() throws Exception {
      this.stringGetConstructor = RecordedInvocation.of(InstrumentedMethod.CLASS_GETCONSTRUCTOR)
              .onInstance(String.class).withArgument(new Class[0]).returnValue(String.class.getConstructor()).build();
      this.stringGetDeclaredConstructor = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR)
              .onInstance(String.class).withArgument(new Class[] { byte[].class, byte.class })
              .returnValue(String.class.getDeclaredConstructor(byte[].class, byte.class)).build();
    }

    @Test
    void classGetConstructorShouldMatchInstrospectPublicConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
    }

    @Test
    void classGetConstructorShouldMatchInvokePublicConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
    }

    @Test
    void classGetConstructorShouldMatchIntrospectDeclaredConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
    }

    @Test
    void classGetConstructorShouldMatchInvokeDeclaredConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
    }

    @Test
    void classGetConstructorShouldMatchInstrospectConstructorHint() {
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withConstructor(Collections.emptyList(), ExecutableMode.INTROSPECT));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
    }

    @Test
    void classGetConstructorShouldMatchInvokeConstructorHint() {
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTOR, this.stringGetConstructor);
    }

    @Test
    void classGetConstructorsShouldMatchIntrospectPublicConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
    }

    @Test
    void classGetConstructorsShouldMatchInvokePublicConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
    }

    @Test
    void classGetConstructorsShouldMatchIntrospectDeclaredConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
    }

    @Test
    void classGetConstructorsShouldMatchInvokeDeclaredConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
    }

    @Test
    void classGetConstructorsShouldNotMatchTypeReflectionHint() {
      hints.reflection().registerType(String.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
    }

    @Test
    void classGetConstructorsShouldNotMatchConstructorReflectionHint() throws Exception {
      hints.reflection().registerConstructor(String.class.getConstructor(), ExecutableMode.INVOKE);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETCONSTRUCTORS, this.stringGetConstructors);
    }

    @Test
    void classGetDeclaredConstructorShouldMatchIntrospectDeclaredConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
    }

    @Test
    void classGetDeclaredConstructorShouldNotMatchIntrospectPublicConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
    }

    @Test
    void classGetDeclaredConstructorShouldMatchInstrospectConstructorHint() {
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withConstructor(TypeReference.listOf(byte[].class, byte.class), ExecutableMode.INTROSPECT));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
    }

    @Test
    void classGetDeclaredConstructorShouldMatchInvokeConstructorHint() {
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withConstructor(TypeReference.listOf(byte[].class, byte.class), ExecutableMode.INVOKE));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTOR, this.stringGetDeclaredConstructor);
    }

    @Test
    void classGetDeclaredConstructorsShouldMatchIntrospectDeclaredConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
    }

    @Test
    void classGetDeclaredConstructorsShouldMatchInvokeDeclaredConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
    }

    @Test
    void classGetDeclaredConstructorsShouldNotMatchIntrospectPublicConstructorsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
    }

    @Test
    void classGetDeclaredConstructorsShouldNotMatchTypeReflectionHint() {
      hints.reflection().registerType(String.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
    }

    @Test
    void classGetDeclaredConstructorsShouldNotMatchConstructorReflectionHint() throws Exception {
      hints.reflection().registerConstructor(String.class.getConstructor(), ExecutableMode.INVOKE);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDCONSTRUCTORS, this.stringGetDeclaredConstructors);
    }

    @Test
    void constructorNewInstanceShouldMatchInvokeHintOnConstructor() throws NoSuchMethodException {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE)
              .onInstance(String.class.getConstructor()).returnValue("").build();
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withConstructor(Collections.emptyList(), ExecutableMode.INVOKE));
      assertThatInvocationMatches(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE, invocation);
    }

    @Test
    void constructorNewInstanceShouldNotMatchIntrospectHintOnConstructor() throws NoSuchMethodException {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE)
              .onInstance(String.class.getConstructor()).returnValue("").build();
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withConstructor(Collections.emptyList(), ExecutableMode.INTROSPECT));
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CONSTRUCTOR_NEWINSTANCE, invocation);
    }

  }

  @Nested
  class MethodReflectionInstrumentationTests {

    RecordedInvocation stringGetToStringMethod;

    RecordedInvocation stringGetScaleMethod;

    RecordedInvocation stringGetMethods;

    @BeforeEach
    void setup() throws Exception {
      this.stringGetToStringMethod = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHOD)
              .onInstance(String.class).withArguments("toString", new Class[0])
              .returnValue(String.class.getMethod("toString")).build();
      this.stringGetScaleMethod = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHOD)
              .onInstance(String.class).withArguments("scale", new Class[] { int.class, float.class })
              .returnValue(String.class.getDeclaredMethod("scale", int.class, float.class)).build();
      this.stringGetMethods = RecordedInvocation.of(InstrumentedMethod.CLASS_GETMETHODS)
              .onInstance(String.class).returnValue(String.class.getMethods()).build();
    }

    @Test
    void classGetDeclaredMethodShouldMatchIntrospectDeclaredMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
    }

    @Test
    void classGetDeclaredMethodShouldNotMatchIntrospectPublicMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
    }

    @Test
    void classGetDeclaredMethodShouldMatchIntrospectMethodHint() {
      List<TypeReference> parameterTypes = TypeReference.listOf(int.class, float.class);
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withMethod("scale", parameterTypes, ExecutableMode.INTROSPECT));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
    }

    @Test
    void classGetDeclaredMethodShouldMatchInvokeMethodHint() {
      List<TypeReference> parameterTypes = TypeReference.listOf(int.class, float.class);
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withMethod("scale", parameterTypes, ExecutableMode.INVOKE));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHOD, this.stringGetScaleMethod);
    }

    @Test
    void classGetDeclaredMethodsShouldMatchIntrospectDeclaredMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, this.stringGetScaleMethod);
    }

    @Test
    void classGetDeclaredMethodsShouldMatchInvokeDeclaredMethodsHint() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDMETHODS).onInstance(String.class).build();
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, invocation);
    }

    @Test
    void classGetDeclaredMethodsShouldNotMatchIntrospectPublicMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, this.stringGetScaleMethod);
    }

    @Test
    void classGetDeclaredMethodsShouldNotMatchTypeReflectionHint() {
      hints.reflection().registerType(String.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, this.stringGetScaleMethod);
    }

    @Test
    void classGetDeclaredMethodsShouldNotMatchMethodReflectionHint() throws Exception {
      hints.reflection().registerMethod(String.class.getMethod("toString"), ExecutableMode.INVOKE);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDMETHODS, this.stringGetScaleMethod);
    }

    @Test
    void classGetMethodsShouldMatchInstrospectDeclaredMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
    }

    @Test
    void classGetMethodsShouldMatchInstrospectPublicMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
    }

    @Test
    void classGetMethodsShouldMatchInvokeDeclaredMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
    }

    @Test
    void classGetMethodsShouldMatchInvokePublicMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
    }

    @Test
    void classGetMethodsShouldNotMatchForWrongType() {
      hints.reflection().registerType(Integer.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
    }

    @Test
    void classGetMethodsShouldNotMatchTypeReflectionHint() {
      hints.reflection().registerType(String.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
    }

    @Test
    void classGetMethodsShouldNotMatchMethodReflectionHint() throws Exception {
      hints.reflection().registerMethod(String.class.getMethod("toString"), ExecutableMode.INVOKE);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHODS, this.stringGetMethods);
    }

    @Test
    void classGetMethodShouldMatchInstrospectPublicMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
    }

    @Test
    void classGetMethodShouldMatchInvokePublicMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_PUBLIC_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
    }

    @Test
    void classGetMethodShouldNotMatchIntrospectDeclaredMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
    }

    @Test
    void classGetMethodShouldNotMatchInvokeDeclaredMethodsHint() {
      hints.reflection().registerType(String.class, MemberCategory.INVOKE_DECLARED_METHODS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
    }

    @Test
    void classGetMethodShouldMatchIntrospectMethodHint() {
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withMethod("toString", Collections.emptyList(), ExecutableMode.INTROSPECT));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
    }

    @Test
    void classGetMethodShouldMatchInvokeMethodHint() {
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withMethod("toString", Collections.emptyList(), ExecutableMode.INVOKE));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
    }

    @Test
    void classGetMethodShouldNotMatchInstrospectPublicMethodsHintWhenPrivate() throws Exception {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetScaleMethod);
    }

    @Test
    void classGetMethodShouldMatchInstrospectDeclaredMethodsHintWhenPrivate() {
      hints.reflection().registerType(String.class, MemberCategory.INTROSPECT_DECLARED_METHODS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetScaleMethod);
    }

    @Test
    void classGetMethodShouldNotMatchForWrongType() {
      hints.reflection().registerType(Integer.class, MemberCategory.INTROSPECT_PUBLIC_METHODS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETMETHOD, this.stringGetToStringMethod);
    }

    @Test
    void methodInvokeShouldMatchInvokeHintOnMethod() throws NoSuchMethodException {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.METHOD_INVOKE)
              .onInstance(String.class.getMethod("startsWith", String.class)).withArguments("testString", new Object[] { "test" }).build();
      hints.reflection().registerType(String.class, typeHint -> typeHint.withMethod("startsWith",
              TypeReference.listOf(String.class), ExecutableMode.INVOKE));
      assertThatInvocationMatches(InstrumentedMethod.METHOD_INVOKE, invocation);
    }

    @Test
    void methodInvokeShouldNotMatchIntrospectHintOnMethod() throws NoSuchMethodException {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.METHOD_INVOKE)
              .onInstance(String.class.getMethod("toString")).withArguments("", new Object[0]).build();
      hints.reflection().registerType(String.class, typeHint ->
              typeHint.withMethod("toString", Collections.emptyList(), ExecutableMode.INTROSPECT));
      assertThatInvocationDoesNotMatch(InstrumentedMethod.METHOD_INVOKE, invocation);
    }

  }

  @Nested
  class FieldReflectionInstrumentationTests {

    RecordedInvocation getPublicField;

    RecordedInvocation stringGetDeclaredField;

    RecordedInvocation stringGetDeclaredFields;

    RecordedInvocation stringGetFields;

    @BeforeEach
    void setup() throws Exception {
      this.getPublicField = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
              .onInstance(PublicField.class).withArgument("field")
              .returnValue(PublicField.class.getField("field")).build();
      this.stringGetDeclaredField = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELD)
              .onInstance(String.class).withArgument("value").returnValue(String.class.getDeclaredField("value")).build();
      this.stringGetDeclaredFields = RecordedInvocation.of(InstrumentedMethod.CLASS_GETDECLAREDFIELDS)
              .onInstance(String.class).returnValue(String.class.getDeclaredFields()).build();
      this.stringGetFields = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
              .onInstance(String.class).returnValue(String.class.getFields()).build();
    }

    @Test
    void classGetDeclaredFieldShouldMatchDeclaredFieldsHint() {
      hints.reflection().registerType(String.class, MemberCategory.DECLARED_FIELDS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
    }

    @Test
    void classGetDeclaredFieldShouldNotMatchPublicFieldsHint() {
      hints.reflection().registerType(String.class, typeHint -> typeHint.withMembers(MemberCategory.PUBLIC_FIELDS));
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
    }

    @Test
    void classGetDeclaredFieldShouldMatchFieldHint() {
      hints.reflection().registerType(String.class, typeHint -> typeHint.withField("value"));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELD, this.stringGetDeclaredField);
    }

    @Test
    void classGetDeclaredFieldsShouldMatchDeclaredFieldsHint() {
      hints.reflection().registerType(String.class, MemberCategory.DECLARED_FIELDS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, this.stringGetDeclaredFields);
    }

    @Test
    void classGetDeclaredFieldsShouldNotMatchPublicFieldsHint() {
      hints.reflection().registerType(String.class, MemberCategory.PUBLIC_FIELDS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, this.stringGetDeclaredFields);
    }

    @Test
    void classGetDeclaredFieldsShouldNotMatchTypeHint() {
      hints.reflection().registerType(String.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, this.stringGetDeclaredFields);
    }

    @Test
    void classGetDeclaredFieldsShouldNotMatchFieldHint() throws Exception {
      hints.reflection().registerField(String.class.getDeclaredField("value"));
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETDECLAREDFIELDS, this.stringGetDeclaredFields);
    }

    @Test
    void classGetFieldShouldMatchPublicFieldsHint() {
      hints.reflection().registerType(PublicField.class, MemberCategory.PUBLIC_FIELDS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
    }

    @Test
    void classGetFieldShouldNotMatchDeclaredFieldsHint() {
      hints.reflection().registerType(PublicField.class, MemberCategory.DECLARED_FIELDS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
    }

    @Test
    void classGetFieldShouldMatchFieldHint() {
      hints.reflection().registerType(PublicField.class, typeHint -> typeHint.withField("field"));
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, this.getPublicField);
    }

    @Test
    void classGetFieldShouldNotMatchPublicFieldsHintWhenPrivate() throws NoSuchFieldException {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
              .onInstance(String.class).withArgument("value").returnValue(null).build();
      hints.reflection().registerType(String.class, MemberCategory.PUBLIC_FIELDS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELD, invocation);
    }

    @Test
    void classGetFieldShouldMatchDeclaredFieldsHintWhenPrivate() throws NoSuchFieldException {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
              .onInstance(String.class).withArgument("value").returnValue(String.class.getDeclaredField("value")).build();
      hints.reflection().registerType(String.class, MemberCategory.DECLARED_FIELDS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELD, invocation);
    }

    @Test
    void classGetFieldShouldNotMatchForWrongType() throws Exception {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELD)
              .onInstance(String.class).withArgument("value").returnValue(null).build();
      hints.reflection().registerType(Integer.class, MemberCategory.DECLARED_FIELDS);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELD, invocation);
    }

    @Test
    void classGetFieldsShouldMatchPublicFieldsHint() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
              .onInstance(PublicField.class).build();
      hints.reflection().registerType(PublicField.class, MemberCategory.PUBLIC_FIELDS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELDS, invocation);
    }

    @Test
    void classGetFieldsShouldMatchDeclaredFieldsHint() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETFIELDS)
              .onInstance(PublicField.class).build();
      hints.reflection().registerType(PublicField.class, MemberCategory.DECLARED_FIELDS);
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETFIELDS, invocation);
    }

    @Test
    void classGetFieldsShouldNotMatchTypeHint() {
      hints.reflection().registerType(String.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELDS, this.stringGetFields);
    }

    @Test
    void classGetFieldsShouldNotMatchFieldHint() throws Exception {
      hints.reflection().registerField(String.class.getDeclaredField("value"));
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETFIELDS, this.stringGetFields);
    }

  }

  @Nested
  class ResourcesInstrumentationTests {

    @Test
    void resourceBundleGetBundleShouldMatchBundleNameHint() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE)
              .withArgument("bundleName").build();
      hints.resources().registerResourceBundle("bundleName");
      assertThatInvocationMatches(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE, invocation);
    }

    @Test
    void resourceBundleGetBundleShouldNotMatchBundleNameHintWhenWrongName() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE)
              .withArgument("bundleName").build();
      hints.resources().registerResourceBundle("wrongBundleName");
      assertThatInvocationDoesNotMatch(InstrumentedMethod.RESOURCEBUNDLE_GETBUNDLE, invocation);
    }

    @Test
    void classGetResourceShouldMatchResourcePatternWhenAbsolute() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
              .onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
      hints.resources().registerPattern("some/*");
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
    }

    @Test
    void classGetResourceShouldMatchResourcePatternWhenRelative() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
              .onInstance(InstrumentedMethodTests.class).withArgument("resource.txt").build();
      hints.resources().registerPattern("infra/aot/agent/*");
      assertThatInvocationMatches(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
    }

    @Test
    void classGetResourceShouldNotMatchResourcePatternWhenInvalid() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
              .onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
      hints.resources().registerPattern("other/*");
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
    }

    @Test
    void classGetResourceShouldNotMatchResourcePatternWhenExcluded() {
      RecordedInvocation invocation = RecordedInvocation.of(InstrumentedMethod.CLASS_GETRESOURCE)
              .onInstance(InstrumentedMethodTests.class).withArgument("/some/path/resource.txt").build();
      hints.resources().registerPattern(resourceHint -> resourceHint.includes("some/*").excludes("some/path/*"));
      assertThatInvocationDoesNotMatch(InstrumentedMethod.CLASS_GETRESOURCE, invocation);
    }

  }

  @Nested
  class ProxiesInstrumentationTests {

    RecordedInvocation newProxyInstance;

    @BeforeEach
    void setup() {
      this.newProxyInstance = RecordedInvocation.of(InstrumentedMethod.PROXY_NEWPROXYINSTANCE)
              .withArguments(ClassLoader.getSystemClassLoader(), new Class[] { AutoCloseable.class, Comparator.class }, null)
              .returnValue(Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] { AutoCloseable.class, Comparator.class }, (proxy, method, args) -> null))
              .build();
    }

    @Test
    void proxyNewProxyInstanceShouldMatchWhenInterfacesMatch() {
      hints.proxies().registerJdkProxy(AutoCloseable.class, Comparator.class);
      assertThatInvocationMatches(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
    }

    @Test
    void proxyNewProxyInstanceShouldNotMatchWhenInterfacesDoNotMatch() {
      hints.proxies().registerJdkProxy(Comparator.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
    }

    @Test
    void proxyNewProxyInstanceShouldNotMatchWhenWrongOrder() {
      hints.proxies().registerJdkProxy(Comparator.class, AutoCloseable.class);
      assertThatInvocationDoesNotMatch(InstrumentedMethod.PROXY_NEWPROXYINSTANCE, this.newProxyInstance);
    }
  }

  private void assertThatInvocationMatches(InstrumentedMethod method, RecordedInvocation invocation) {
    assertThat(method.matcher(invocation)).accepts(this.hints);
  }

  private void assertThatInvocationDoesNotMatch(InstrumentedMethod method, RecordedInvocation invocation) {
    assertThat(method.matcher(invocation)).rejects(this.hints);
  }

  static class PublicField {

    public String field;

  }

}
