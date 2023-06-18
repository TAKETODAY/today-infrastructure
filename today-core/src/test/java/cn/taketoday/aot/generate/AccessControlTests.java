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

package cn.taketoday.aot.generate;

import cn.taketoday.javapoet.ClassName;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import cn.taketoday.aot.generate.AccessControl.Visibility;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.testfixture.aot.generator.visibility.ProtectedGenericParameter;
import cn.taketoday.core.testfixture.aot.generator.visibility.ProtectedParameter;
import cn.taketoday.core.testfixture.aot.generator.visibility.PublicFactoryBean;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AccessControl}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AccessControlTests {

  @Test
  void isAccessibleWhenPublicVisibilityInSamePackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PUBLIC);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isTrue();
  }

  @Test
  void isAccessibleWhenPublicVisibilityInDifferentPackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PUBLIC);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isTrue();
  }

  @Test
  void isAccessibleWhenProtectedVisibilityInSamePackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PROTECTED);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isTrue();
  }

  @Test
  void isAccessibleWhenProtectedVisibilityInDifferentPackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PROTECTED);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isFalse();
  }

  @Test
  void isAccessibleWhenPackagePrivateVisibilityInSamePackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PACKAGE_PRIVATE);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isTrue();
  }

  @Test
  void isAccessibleWhenPackagePrivateVisibilityInDifferentPackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PACKAGE_PRIVATE);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isFalse();
  }

  @Test
  void isAccessibleWhenPrivateVisibilityInSamePackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PRIVATE);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(PublicClass.class))).isFalse();
  }

  @Test
  void isAccessibleWhenPrivateVisibilityInDifferentPackage() {
    AccessControl accessControl = new AccessControl(PublicClass.class, Visibility.PRIVATE);
    assertThat(accessControl.isAccessibleFrom(ClassName.get(String.class))).isFalse();
  }

  @Test
  void forMemberWhenPublicConstructor() throws NoSuchMethodException {
    Member member = PublicClass.class.getConstructor();
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PUBLIC);
  }

  @Test
  void forMemberWhenPackagePrivateConstructor() {
    Member member = ProtectedAccessor.class.getDeclaredConstructors()[0];
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPackagePrivateClassWithPublicConstructor() {
    Member member = PackagePrivateClass.class.getDeclaredConstructors()[0];
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPackagePrivateClassWithPublicMethod() {
    Member member = method(PackagePrivateClass.class, "stringBean");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPackagePrivateConstructorParameter() {
    Member member = ProtectedParameter.class.getConstructors()[0];
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPackagePrivateGenericOnConstructorParameter() {
    Member member = ProtectedGenericParameter.class.getConstructors()[0];
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPackagePrivateMethod() {
    Member member = method(PublicClass.class, "getProtectedMethod");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPackagePrivateMethodReturnType() {
    Member member = method(ProtectedAccessor.class, "methodWithProtectedReturnType");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPackagePrivateMethodParameter() {
    Member member = method(ProtectedAccessor.class, "methodWithProtectedParameter",
            PackagePrivateClass.class);
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPackagePrivateField() {
    Field member = field(PublicClass.class, "protectedField");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPublicFieldAndPackagePrivateFieldType() {
    Member member = field(PublicClass.class, "protectedClassField");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPrivateField() {
    Member member = field(PublicClass.class, "privateField");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPublicMethodAndPackagePrivateGenericOnReturnType() {
    Member member = method(PublicFactoryBean.class, "protectedTypeFactoryBean");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forMemberWhenPublicClassWithPackagePrivateArrayComponent() {
    Member member = field(PublicClass.class, "packagePrivateClasses");
    AccessControl accessControl = AccessControl.forMember(member);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forResolvableTypeWhenPackagePrivateGeneric() {
    ResolvableType resolvableType = PublicFactoryBean
            .resolveToProtectedGenericParameter();
    AccessControl accessControl = AccessControl.forResolvableType(resolvableType);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  @Test
  void forResolvableTypeWhenRecursiveType() {
    ResolvableType resolvableType = ResolvableType
            .fromClassWithGenerics(SelfReference.class, SelfReference.class);
    AccessControl accessControl = AccessControl.forResolvableType(resolvableType);
    assertThat(accessControl.getVisibility()).isEqualTo(Visibility.PACKAGE_PRIVATE);
  }

  private static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(type, name, parameterTypes);
    assertThat(method).isNotNull();
    return method;
  }

  private static Field field(Class<?> type, String name) {
    Field field = ReflectionUtils.findField(type, name);
    assertThat(field).isNotNull();
    return field;
  }

  static class SelfReference<T extends SelfReference<T>> {

    @SuppressWarnings({ "unchecked", "unused" })
    T getThis() {
      return (T) this;
    }

  }
}
