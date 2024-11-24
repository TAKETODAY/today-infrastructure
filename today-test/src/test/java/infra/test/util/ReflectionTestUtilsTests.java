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

package infra.test.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import infra.aop.framework.ProxyFactory;
import infra.aop.support.AopUtils;
import infra.lang.Nullable;
import infra.test.util.subpackage.Component;
import infra.test.util.subpackage.LegacyEntity;
import infra.test.util.subpackage.Person;
import infra.test.util.subpackage.PersonEntity;
import infra.test.util.subpackage.StaticFields;
import infra.test.util.subpackage.StaticMethods;

import static infra.test.util.ReflectionTestUtils.invokeGetterMethod;
import static infra.test.util.ReflectionTestUtils.invokeMethod;
import static infra.test.util.ReflectionTestUtils.invokeSetterMethod;
import static infra.test.util.ReflectionTestUtils.setField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link ReflectionTestUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
class ReflectionTestUtilsTests {

  private static final Float PI = Float.valueOf((float) 22 / 7);

  private final Person person = new PersonEntity();

  private final Component component = new Component();

  private final LegacyEntity entity = new LegacyEntity();

  @BeforeEach
  void resetStaticFields() {
    StaticFields.reset();
    StaticMethods.reset();
  }

  @Test
  void setFieldWithNullTargetObject() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> setField((Object) null, "id", Long.valueOf(99)))
            .withMessageStartingWith("Either targetObject or targetClass");
  }

  @Test
  void getFieldWithNullTargetObject() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> getField(null, "id"))
            .withMessageStartingWith("Either targetObject or targetClass");
  }

  @Test
  void setFieldWithNullTargetClass() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> setField(null, "id", Long.valueOf(99)))
            .withMessageStartingWith("Either targetObject or targetClass");
  }

  @Test
  void getFieldWithNullTargetClass() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> getField(null, "id"))
            .withMessageStartingWith("Either targetObject or targetClass");
  }

  @Test
  void setFieldWithNullNameAndNullType() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> setField(person, null, Long.valueOf(99), null))
            .withMessageStartingWith("Either name or type");
  }

  @Test
  void setFieldWithBogusName() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> setField(person, "bogus", Long.valueOf(99), long.class))
            .withMessageStartingWith("Could not find field 'bogus'");
  }

  @Test
  void setFieldWithWrongType() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> setField(person, "id", Long.valueOf(99), String.class))
            .withMessageStartingWith("Could not find field");
  }

  @Test
  void setFieldAndGetFieldForStandardUseCases() throws Exception {
    assertSetFieldAndGetFieldBehavior(this.person);
  }

  @Test
  void setFieldAndGetFieldViaJdkDynamicProxy() throws Exception {
    ProxyFactory pf = new ProxyFactory(this.person);
    pf.addInterface(Person.class);
    Person proxy = (Person) pf.getProxy();
    assertThat(AopUtils.isJdkDynamicProxy(proxy)).as("Proxy is a JDK dynamic proxy").isTrue();
    assertSetFieldAndGetFieldBehaviorForProxy(proxy, this.person);
  }

  @Test
  void setFieldAndGetFieldViaCglibProxy() throws Exception {
    ProxyFactory pf = new ProxyFactory(this.person);
    pf.setProxyTargetClass(true);
    Person proxy = (Person) pf.getProxy();
    assertThat(AopUtils.isCglibProxy(proxy)).as("Proxy is a CGLIB proxy").isTrue();
    assertSetFieldAndGetFieldBehaviorForProxy(proxy, this.person);
  }

  private static void assertSetFieldAndGetFieldBehavior(Person person) {
    // Set reflectively
    setField(person, "id", Long.valueOf(99), long.class);
    setField(person, "name", "Tom");
    setField(person, "age", Integer.valueOf(42));
    setField(person, "eyeColor", "blue", String.class);
    setField(person, "likesPets", Boolean.TRUE);
    setField(person, "favoriteNumber", PI, Number.class);

    // Get reflectively
    assertThat(getField(person, "id")).isEqualTo(Long.valueOf(99));
    assertThat(getField(person, "name")).isEqualTo("Tom");
    assertThat(getField(person, "age")).isEqualTo(Integer.valueOf(42));
    assertThat(getField(person, "eyeColor")).isEqualTo("blue");
    assertThat(getField(person, "likesPets")).isEqualTo(Boolean.TRUE);
    assertThat(getField(person, "favoriteNumber")).isEqualTo(PI);

    // Get directly
    assertThat(person.getId()).as("ID (private field in a superclass)").isEqualTo(99);
    assertThat(person.getName()).as("name (protected field)").isEqualTo("Tom");
    assertThat(person.getAge()).as("age (private field)").isEqualTo(42);
    assertThat(person.getEyeColor()).as("eye color (package private field)").isEqualTo("blue");
    assertThat(person.likesPets()).as("'likes pets' flag (package private boolean field)").isTrue();
    assertThat(person.getFavoriteNumber()).as("'favorite number' (package field)").isEqualTo(PI);
  }

  @Nullable
  static Object getField(Object targetObject, String name) {
    return ReflectionTestUtils.getField(targetObject, name);
  }

  @Nullable
  static Object getField(Class<?> targetClass, String name) {
    return ReflectionTestUtils.getField(targetClass, name);
  }

  private static void assertSetFieldAndGetFieldBehaviorForProxy(Person proxy, Person target) {
    assertSetFieldAndGetFieldBehavior(proxy);

    // Get directly from Target
    assertThat(target.getId()).as("ID (private field in a superclass)").isEqualTo(99);
    assertThat(target.getName()).as("name (protected field)").isEqualTo("Tom");
    assertThat(target.getAge()).as("age (private field)").isEqualTo(42);
    assertThat(target.getEyeColor()).as("eye color (package private field)").isEqualTo("blue");
    assertThat(target.likesPets()).as("'likes pets' flag (package private boolean field)").isTrue();
    assertThat(target.getFavoriteNumber()).as("'favorite number' (package field)").isEqualTo(PI);
  }

  @Test
  void setFieldWithNullValuesForNonPrimitives() throws Exception {
    // Fields must be non-null to start with
    setField(person, "name", "Tom");
    setField(person, "eyeColor", "blue", String.class);
    setField(person, "favoriteNumber", PI, Number.class);
    assertThat(person.getName()).isNotNull();
    assertThat(person.getEyeColor()).isNotNull();
    assertThat(person.getFavoriteNumber()).isNotNull();

    // Set to null
    setField(person, "name", null, String.class);
    setField(person, "eyeColor", null, String.class);
    setField(person, "favoriteNumber", null, Number.class);

    assertThat(person.getName()).as("name (protected field)").isNull();
    assertThat(person.getEyeColor()).as("eye color (package private field)").isNull();
    assertThat(person.getFavoriteNumber()).as("'favorite number' (package field)").isNull();
  }

  @Test
  void setFieldWithNullValueForPrimitiveLong() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> setField(person, "id", null, long.class));
  }

  @Test
  void setFieldWithNullValueForPrimitiveInt() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> setField(person, "age", null, int.class));
  }

  @Test
  void setFieldWithNullValueForPrimitiveBoolean() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> setField(person, "likesPets", null, boolean.class));
  }

  @Test
  void setStaticFieldViaClass() throws Exception {
    setField(StaticFields.class, "publicField", "xxx");
    setField(StaticFields.class, "privateField", "yyy");

    assertThat(StaticFields.publicField).as("public static field").isEqualTo("xxx");
    assertThat(StaticFields.getPrivateField()).as("private static field").isEqualTo("yyy");
  }

  @Test
  void setStaticFieldViaClassWithExplicitType() throws Exception {
    setField(StaticFields.class, "publicField", "xxx", String.class);
    setField(StaticFields.class, "privateField", "yyy", String.class);

    assertThat(StaticFields.publicField).as("public static field").isEqualTo("xxx");
    assertThat(StaticFields.getPrivateField()).as("private static field").isEqualTo("yyy");
  }

  @Test
  void setStaticFieldViaInstance() throws Exception {
    StaticFields staticFields = new StaticFields();
    setField(staticFields, null, "publicField", "xxx", null);
    setField(staticFields, null, "privateField", "yyy", null);

    assertThat(StaticFields.publicField).as("public static field").isEqualTo("xxx");
    assertThat(StaticFields.getPrivateField()).as("private static field").isEqualTo("yyy");
  }

  @Test
  void getStaticFieldViaClass() throws Exception {
    assertThat(getField(StaticFields.class, "publicField")).as("public static field").isEqualTo("public");
    assertThat(getField(StaticFields.class, "privateField")).as("private static field").isEqualTo("private");
  }

  @Test
  void getStaticFieldViaInstance() throws Exception {
    StaticFields staticFields = new StaticFields();
    assertThat(getField(staticFields, "publicField")).as("public static field").isEqualTo("public");
    assertThat(getField(staticFields, "privateField")).as("private static field").isEqualTo("private");
  }

  @Test
  void invokeSetterMethodAndInvokeGetterMethodWithExplicitMethodNames() throws Exception {
    invokeSetterMethod(person, "setId", Long.valueOf(1), long.class);
    invokeSetterMethod(person, "setName", "Jerry", String.class);
    invokeSetterMethod(person, "setAge", Integer.valueOf(33), int.class);
    invokeSetterMethod(person, "setEyeColor", "green", String.class);
    invokeSetterMethod(person, "setLikesPets", Boolean.FALSE, boolean.class);
    invokeSetterMethod(person, "setFavoriteNumber", Integer.valueOf(42), Number.class);

    assertThat(person.getId()).as("ID (protected method in a superclass)").isEqualTo(1);
    assertThat(person.getName()).as("name (private method)").isEqualTo("Jerry");
    assertThat(person.getAge()).as("age (protected method)").isEqualTo(33);
    assertThat(person.getEyeColor()).as("eye color (package private method)").isEqualTo("green");
    assertThat(person.likesPets()).as("'likes pets' flag (protected method for a boolean)").isFalse();
    assertThat(person.getFavoriteNumber()).as("'favorite number' (protected method for a Number)").isEqualTo(Integer.valueOf(42));

    assertThat(invokeGetterMethod(person, "getId")).isEqualTo(Long.valueOf(1));
    assertThat(invokeGetterMethod(person, "getName")).isEqualTo("Jerry");
    assertThat(invokeGetterMethod(person, "getAge")).isEqualTo(Integer.valueOf(33));
    assertThat(invokeGetterMethod(person, "getEyeColor")).isEqualTo("green");
    assertThat(invokeGetterMethod(person, "likesPets")).isEqualTo(Boolean.FALSE);
    assertThat(invokeGetterMethod(person, "getFavoriteNumber")).isEqualTo(Integer.valueOf(42));
  }

  @Test
  void invokeSetterMethodAndInvokeGetterMethodWithJavaBeanPropertyNames() throws Exception {
    invokeSetterMethod(person, "id", Long.valueOf(99), long.class);
    invokeSetterMethod(person, "name", "Tom");
    invokeSetterMethod(person, "age", Integer.valueOf(42));
    invokeSetterMethod(person, "eyeColor", "blue", String.class);
    invokeSetterMethod(person, "likesPets", Boolean.TRUE);
    invokeSetterMethod(person, "favoriteNumber", PI, Number.class);

    assertThat(person.getId()).as("ID (protected method in a superclass)").isEqualTo(99);
    assertThat(person.getName()).as("name (private method)").isEqualTo("Tom");
    assertThat(person.getAge()).as("age (protected method)").isEqualTo(42);
    assertThat(person.getEyeColor()).as("eye color (package private method)").isEqualTo("blue");
    assertThat(person.likesPets()).as("'likes pets' flag (protected method for a boolean)").isTrue();
    assertThat(person.getFavoriteNumber()).as("'favorite number' (protected method for a Number)").isEqualTo(PI);

    assertThat(invokeGetterMethod(person, "id")).isEqualTo(Long.valueOf(99));
    assertThat(invokeGetterMethod(person, "name")).isEqualTo("Tom");
    assertThat(invokeGetterMethod(person, "age")).isEqualTo(Integer.valueOf(42));
    assertThat(invokeGetterMethod(person, "eyeColor")).isEqualTo("blue");
    assertThat(invokeGetterMethod(person, "likesPets")).isEqualTo(Boolean.TRUE);
    assertThat(invokeGetterMethod(person, "favoriteNumber")).isEqualTo(PI);
  }

  @Test
  void invokeSetterMethodWithNullValuesForNonPrimitives() throws Exception {
    invokeSetterMethod(person, "name", null, String.class);
    invokeSetterMethod(person, "eyeColor", null, String.class);
    invokeSetterMethod(person, "favoriteNumber", null, Number.class);

    assertThat(person.getName()).as("name (private method)").isNull();
    assertThat(person.getEyeColor()).as("eye color (package private method)").isNull();
    assertThat(person.getFavoriteNumber()).as("'favorite number' (protected method for a Number)").isNull();
  }

  @Test
  void invokeSetterMethodWithNullValueForPrimitiveLong() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> invokeSetterMethod(person, "id", null, long.class));
  }

  @Test
  void invokeSetterMethodWithNullValueForPrimitiveInt() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> invokeSetterMethod(person, "age", null, int.class));
  }

  @Test
  void invokeSetterMethodWithNullValueForPrimitiveBoolean() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> invokeSetterMethod(person, "likesPets", null, boolean.class));
  }

  @Test
  void invokeMethodWithAutoboxingAndUnboxing() {
    // IntelliJ IDEA 11 won't accept int assignment here
    Integer difference = invokeMethod(component, "subtract", 5, 2);
    assertThat(difference.intValue()).as("subtract(5, 2)").isEqualTo(3);
  }

  @Test
  @Disabled("[SPR-8644] MethodInvoker.findMatchingMethod() does not currently support var-args")
  void invokeMethodWithPrimitiveVarArgs() {
    // IntelliJ IDEA 11 won't accept int assignment here
    Integer sum = invokeMethod(component, "add", 1, 2, 3, 4);
    assertThat(sum.intValue()).as("add(1,2,3,4)").isEqualTo(10);
  }

  @Test
  void invokeMethodWithPrimitiveVarArgsAsSingleArgument() {
    // IntelliJ IDEA 11 won't accept int assignment here
    Integer sum = invokeMethod(component, "add", new int[] { 1, 2, 3, 4 });
    assertThat(sum.intValue()).as("add(1,2,3,4)").isEqualTo(10);
  }

  @Test
  void invokeMethodSimulatingLifecycleEvents() {
    assertThat(component.getNumber()).as("number").isNull();
    assertThat(component.getText()).as("text").isNull();

    // Simulate autowiring a configuration method
    invokeMethod(component, "configure", Integer.valueOf(42), "enigma");
    assertThat(component.getNumber()).as("number should have been configured").isEqualTo(Integer.valueOf(42));
    assertThat(component.getText()).as("text should have been configured").isEqualTo("enigma");

    // Simulate @PostConstruct life-cycle event
    invokeMethod(component, "init");
    // assertions in init() should succeed

    // Simulate @PreDestroy life-cycle event
    invokeMethod(component, "destroy");
    assertThat(component.getNumber()).as("number").isNull();
    assertThat(component.getText()).as("text").isNull();
  }

  @Test
  void invokeInitMethodBeforeAutowiring() {
    assertThatIllegalStateException()
            .isThrownBy(() -> invokeMethod(component, "init"))
            .withMessageStartingWith("number is required");
  }

  @Test
  void invokeMethodWithIncompatibleArgumentTypes() {
    assertThatIllegalStateException()
            .isThrownBy(() -> invokeMethod(component, "subtract", "foo", 2.0))
            .withMessageStartingWith("Method not found");
  }

  @Test
  void invokeMethodWithTooFewArguments() {
    assertThatIllegalStateException()
            .isThrownBy(() -> invokeMethod(component, "configure", Integer.valueOf(42)))
            .withMessageStartingWith("Method not found");
  }

  @Test
  void invokeMethodWithTooManyArguments() {
    assertThatIllegalStateException()
            .isThrownBy(() -> invokeMethod(component, "configure", Integer.valueOf(42), "enigma", "baz", "quux"))
            .withMessageStartingWith("Method not found");
  }

  @Test
    // SPR-14363
  void getFieldOnLegacyEntityWithSideEffectsInToString() {
    Object collaborator = getField(entity, "collaborator");
    assertThat(collaborator).isNotNull();
  }

  @Test
    // SPR-9571 and SPR-14363
  void setFieldOnLegacyEntityWithSideEffectsInToString() {
    String testCollaborator = "test collaborator";
    setField(entity, "collaborator", testCollaborator, Object.class);
    assertThat(entity.toString().contains(testCollaborator)).isTrue();
  }

  @Test
    // SPR-14363
  void invokeMethodOnLegacyEntityWithSideEffectsInToString() {
    invokeMethod(entity, "configure", Integer.valueOf(42), "enigma");
    assertThat(entity.getNumber()).as("number should have been configured").isEqualTo(Integer.valueOf(42));
    assertThat(entity.getText()).as("text should have been configured").isEqualTo("enigma");
  }

  @Test
    // SPR-14363
  void invokeGetterMethodOnLegacyEntityWithSideEffectsInToString() {
    Object collaborator = invokeGetterMethod(entity, "collaborator");
    assertThat(collaborator).isNotNull();
  }

  @Test
    // SPR-14363
  void invokeSetterMethodOnLegacyEntityWithSideEffectsInToString() {
    String testCollaborator = "test collaborator";
    invokeSetterMethod(entity, "collaborator", testCollaborator);
    assertThat(entity.toString().contains(testCollaborator)).isTrue();
  }

  @Test
  void invokeStaticMethodWithNullTargetClass() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> invokeMethod(null, null))
            .withMessage("Target class is required");
  }

  @Test
  void invokeStaticMethodWithNullMethodName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> invokeMethod(getClass(), null))
            .withMessage("Method name must not be empty");
  }

  @Test
  void invokeStaticMethodWithEmptyMethodName() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> invokeMethod(getClass(), "  "))
            .withMessage("Method name must not be empty");
  }

  @Test
  void invokePublicStaticVoidMethodWithArguments() {
    assertThat(StaticMethods.getPublicMethodValue()).isEqualTo("public");

    String testCollaborator = "test collaborator";
    invokeMethod(StaticMethods.class, "publicMethod", testCollaborator);
    assertThat(StaticMethods.getPublicMethodValue()).isEqualTo(testCollaborator);
  }

  @Test
  void invokePublicStaticMethodWithoutArguments() {
    assertThat(StaticMethods.getPublicMethodValue()).isEqualTo("public");

    String result = invokeMethod(StaticMethods.class, "publicMethod");
    assertThat(result).isEqualTo(StaticMethods.getPublicMethodValue());
  }

  @Test
  void invokePrivateStaticVoidMethodWithArguments() {
    assertThat(StaticMethods.getPrivateMethodValue()).isEqualTo("private");

    String testCollaborator = "test collaborator";
    invokeMethod(StaticMethods.class, "privateMethod", testCollaborator);
    assertThat(StaticMethods.getPrivateMethodValue()).isEqualTo(testCollaborator);
  }

  @Test
  void invokePrivateStaticMethodWithoutArguments() {
    assertThat(StaticMethods.getPrivateMethodValue()).isEqualTo("private");

    String result = invokeMethod(StaticMethods.class, "privateMethod");
    assertThat(result).isEqualTo(StaticMethods.getPrivateMethodValue());
  }

  @Test
  void invokeStaticMethodWithNullTargetObjectAndNullTargetClass() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> invokeMethod(null, (Class<?>) null, "id"))
            .withMessage("Either 'targetObject' or 'targetClass' for the method must be specified");
  }

}
