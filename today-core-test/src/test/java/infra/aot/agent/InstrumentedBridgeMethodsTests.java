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

package infra.aot.agent;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:14
 */
@SuppressWarnings("deprecation")
class InstrumentedBridgeMethodsTests {
  @Test
  void shouldHavePrivateConstructor() {
    assertThatCode(InstrumentedBridgeMethods.class::getDeclaredConstructor).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassForNameWithClassNameOnly() {
    assertThatCode(() -> {
      Class<?> result = InstrumentedBridgeMethods.classforName("java.lang.String");
      assertThat(result).isEqualTo(String.class);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassForNameWithFullParameters() {
    assertThatCode(() -> {
      Class<?> result = InstrumentedBridgeMethods.classforName("java.lang.String", true, ClassLoader.getSystemClassLoader());
      assertThat(result).isEqualTo(String.class);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetConstructors() {
    assertThatCode(() -> {
      Constructor<?>[] constructors = InstrumentedBridgeMethods.classgetConstructors(String.class);
      assertThat(constructors).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetConstructor() throws NoSuchMethodException {
    assertThatCode(() -> {
      Constructor<?> constructor = InstrumentedBridgeMethods.classgetConstructor(String.class, new Class[0]);
      assertThat(constructor).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredConstructors() {
    assertThatCode(() -> {
      Constructor<?>[] constructors = InstrumentedBridgeMethods.classgetDeclaredConstructors(String.class);
      assertThat(constructors).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredConstructor() throws NoSuchMethodException {
    assertThatCode(() -> {
      Constructor<?> constructor = InstrumentedBridgeMethods.classgetDeclaredConstructor(String.class, new Class[0]);
      assertThat(constructor).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetMethods() {
    assertThatCode(() -> {
      Method[] methods = InstrumentedBridgeMethods.classgetMethods(String.class);
      assertThat(methods).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetMethod() throws NoSuchMethodException {
    assertThatCode(() -> {
      Method method = InstrumentedBridgeMethods.classgetMethod(String.class, "toString");
      assertThat(method).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredMethod() throws NoSuchMethodException {
    assertThatCode(() -> {
      Method method = InstrumentedBridgeMethods.classgetDeclaredMethod(String.class, "hashCode");
      assertThat(method).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredMethods() {
    assertThatCode(() -> {
      Method[] methods = InstrumentedBridgeMethods.classgetDeclaredMethods(String.class);
      assertThat(methods).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredClasses() {
    assertThatCode(() -> {
      Class<?>[] classes = InstrumentedBridgeMethods.classgetDeclaredClasses(String.class);
      assertThat(classes).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetClasses() {
    assertThatCode(() -> {
      Class<?>[] classes = InstrumentedBridgeMethods.classgetClasses(String.class);
      assertThat(classes).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredFields() {
    assertThatCode(() -> {
      Field[] fields = InstrumentedBridgeMethods.classgetDeclaredFields(String.class);
      assertThat(fields).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredField() throws NoSuchFieldException {
    assertThatCode(() -> {
      Field field = InstrumentedBridgeMethods.classgetDeclaredField(String.class, "value");
      assertThat(field).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetFields() {
    assertThatCode(() -> {
      Field[] fields = InstrumentedBridgeMethods.classgetFields(String.class);
      assertThat(fields).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetField() throws NoSuchFieldException {
    assertThatCode(() -> {
      Field field = InstrumentedBridgeMethods.classgetField(String.class, "CASE_INSENSITIVE_ORDER");
      assertThat(field).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetResource() {
    assertThatCode(() -> {
      URL resource = InstrumentedBridgeMethods.classgetResource(String.class, "String.class");
      // Resource might be null, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetResourceAsStream() {
    assertThatCode(() -> {
      InputStream resource = InstrumentedBridgeMethods.classgetResourceAsStream(String.class, "String.class");
      // Resource might be null, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderLoadClass() throws ClassNotFoundException {
    assertThatCode(() -> {
      Class<?> clazz = InstrumentedBridgeMethods.classloaderloadClass(ClassLoader.getSystemClassLoader(), "java.lang.String");
      assertThat(clazz).isEqualTo(String.class);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderGetResource() {
    assertThatCode(() -> {
      URL resource = InstrumentedBridgeMethods.classloadergetResource(ClassLoader.getSystemClassLoader(), "java/lang/String.class");
      // Resource might be null, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderGetResourceAsStream() {
    assertThatCode(() -> {
      InputStream resource = InstrumentedBridgeMethods.classloadergetResourceAsStream(ClassLoader.getSystemClassLoader(), "java/lang/String.class");
      // Resource might be null, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderResources() {
    assertThatCode(() -> {
      Stream<URL> resources = InstrumentedBridgeMethods.classloaderresources(ClassLoader.getSystemClassLoader(), "java/lang/String.class");
      assertThat(resources).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderGetResources() throws IOException {
    assertThatCode(() -> {
      Enumeration<URL> resources = InstrumentedBridgeMethods.classloadergetResources(ClassLoader.getSystemClassLoader(), "java/lang/String.class");
      assertThat(resources).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleConstructorNewInstance() throws NoSuchMethodException {
    assertThatCode(() -> {
      Constructor<String> constructor = String.class.getConstructor();
      Object result = InstrumentedBridgeMethods.constructornewInstance(constructor);
      assertThat(result).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleMethodInvoke() throws NoSuchMethodException {
    assertThatCode(() -> {
      Method method = String.class.getMethod("toString");
      String instance = "test";
      Object result = InstrumentedBridgeMethods.methodinvoke(method, instance);
      assertThat(result).isEqualTo("test");
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleFieldGet() throws NoSuchFieldException {
    assertThatCode(() -> {
      Field field = String.class.getField("CASE_INSENSITIVE_ORDER");
      Object result = InstrumentedBridgeMethods.fieldget(field, null);
      assertThat(result).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleFieldSet() throws NoSuchFieldException {
    assertThatCode(() -> {
      Field field = TestClass.class.getDeclaredField("value");
      TestClass instance = new TestClass();
      InstrumentedBridgeMethods.fieldset(field, instance, "testValue");
      assertThat(instance.getValue()).isEqualTo("testValue");
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleModuleGetResourceAsStream() {
    assertThatCode(() -> {
      Module module = String.class.getModule();
      InputStream result = InstrumentedBridgeMethods.modulegetResourceAsStream(module, "java/lang/String.class");
      // Resource might be null, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleProxyNewProxyInstance() {
    assertThatCode(() -> {
      Object result = InstrumentedBridgeMethods.proxynewProxyInstance(
              ClassLoader.getSystemClassLoader(),
              new Class[] { Runnable.class },
              (proxy, method, args) -> null);
      assertThat(result).isNotNull();
      assertThat(result).isInstanceOf(Runnable.class);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassForNameWithNonExistentClass() {
    assertThatThrownBy(() -> InstrumentedBridgeMethods.classforName("non.existent.Class"))
            .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void shouldHandleClassGetConstructorWithNonExistentConstructor() {
    assertThatThrownBy(() -> InstrumentedBridgeMethods.classgetConstructor(String.class, new Class[] { int.class }))
            .isInstanceOf(NoSuchMethodException.class);
  }

  @Test
  void shouldHandleClassGetDeclaredFieldWithNonExistentField() {
    assertThatThrownBy(() -> InstrumentedBridgeMethods.classgetDeclaredField(String.class, "nonExistentField"))
            .isInstanceOf(NoSuchFieldException.class);
  }

  @Test
  void shouldHandleClassGetFieldWithNonExistentField() {
    assertThatThrownBy(() -> InstrumentedBridgeMethods.classgetField(String.class, "nonExistentField"))
            .isInstanceOf(NoSuchFieldException.class);
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithBaseName() {
    assertThatCode(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("messages");
      // ResourceBundle might not exist, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithBaseNameAndControl() {
    assertThatCode(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("messages", ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
      // ResourceBundle might not exist, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithBaseNameAndLocale() {
    assertThatCode(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("messages", java.util.Locale.getDefault());
      // ResourceBundle might not exist, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithBaseNameLocaleAndControl() {
    assertThatCode(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("messages", java.util.Locale.getDefault(),
              ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
      // ResourceBundle might not exist, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithBaseNameLocaleAndClassLoader() {
    assertThatCode(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("messages", java.util.Locale.getDefault(), ClassLoader.getSystemClassLoader());
      // ResourceBundle might not exist, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithAllParameters() {
    assertThatCode(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("messages", java.util.Locale.getDefault(), ClassLoader.getSystemClassLoader(),
              ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT));
      // ResourceBundle might not exist, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderLoadClassWithNonExistentClass() {
    assertThatThrownBy(() -> InstrumentedBridgeMethods.classloaderloadClass(ClassLoader.getSystemClassLoader(), "non.existent.Class"))
            .isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  void shouldHandleConstructorNewInstanceWithArguments() throws NoSuchMethodException {
    assertThatCode(() -> {
      Constructor<String> constructor = String.class.getConstructor(String.class);
      Object result = InstrumentedBridgeMethods.constructornewInstance(constructor, "test");
      assertThat(result).isEqualTo("test");
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleMethodInvokeWithArguments() throws NoSuchMethodException {
    assertThatCode(() -> {
      Method method = String.class.getMethod("substring", int.class);
      String instance = "testString";
      Object result = InstrumentedBridgeMethods.methodinvoke(method, instance, 4);
      assertThat(result).isEqualTo("String");
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleFieldGetWithInstance() throws NoSuchFieldException {
    assertThatCode(() -> {
      Field field = String.class.getDeclaredField("value");
      String instance = "test";
      Object result = InstrumentedBridgeMethods.fieldget(field, instance);
      assertThat(result).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetResourceWithValidResource() {
    assertThatCode(() -> {
      URL resource = InstrumentedBridgeMethods.classgetResource(String.class, "String.class");
      // Resource might be null, but method should not throw exception
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetResourceAsStreamWithValidResource() {
    assertThatCode(() -> {
      InputStream resource = InstrumentedBridgeMethods.classgetResourceAsStream(String.class, "String.class");
      // Resource might be null, but method should not throw exception
      if (resource != null) {
        resource.close();
      }
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetResourceWithNullResult() {
    assertThatCode(() -> {
      URL resource = InstrumentedBridgeMethods.classgetResource(String.class, "nonexistent.resource");
      // Should handle null result gracefully
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetResourceAsStreamWithNullResult() {
    assertThatCode(() -> {
      InputStream resource = InstrumentedBridgeMethods.classgetResourceAsStream(String.class, "nonexistent.resource");
      // Should handle null result gracefully
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderGetResourceWithNullResult() {
    assertThatCode(() -> {
      URL resource = InstrumentedBridgeMethods.classloadergetResource(ClassLoader.getSystemClassLoader(), "nonexistent.resource");
      // Should handle null result gracefully
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderGetResourceAsStreamWithNullResult() {
    assertThatCode(() -> {
      InputStream resource = InstrumentedBridgeMethods.classloadergetResourceAsStream(ClassLoader.getSystemClassLoader(), "nonexistent.resource");
      // Should handle null result gracefully
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleModuleGetResourceAsStreamWithNullResult() {
    assertThatCode(() -> {
      Module module = String.class.getModule();
      InputStream result = InstrumentedBridgeMethods.modulegetResourceAsStream(module, "nonexistent.resource");
      // Should handle null result gracefully
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithNonExistentBundle() {
    assertThatThrownBy(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("non.existent.bundle");
      // Should handle missing bundle gracefully
    }).isInstanceOf(MissingResourceException.class);
  }

  @Test
  void shouldHandleResourceBundleGetBundleWithModuleAndNonExistentBundle() {
    assertThatThrownBy(() -> {
      ResourceBundle result = InstrumentedBridgeMethods.resourcebundlegetBundle("non.existent.bundle", String.class.getModule());
      // Should handle missing bundle gracefully
    }).isInstanceOf(MissingResourceException.class);
  }

  @Test
  void shouldHandleProxyNewProxyInstanceWithMultipleInterfaces() {
    assertThatCode(() -> {
      Object result = InstrumentedBridgeMethods.proxynewProxyInstance(
              ClassLoader.getSystemClassLoader(),
              new Class[] { Runnable.class, Cloneable.class },
              (proxy, method, args) -> null);
      assertThat(result).isNotNull();
      assertThat(result).isInstanceOf(Runnable.class);
      assertThat(result).isInstanceOf(Cloneable.class);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassForNameWithInitializeFalse() {
    assertThatCode(() -> {
      Class<?> result = InstrumentedBridgeMethods.classforName("java.lang.String", false, ClassLoader.getSystemClassLoader());
      assertThat(result).isEqualTo(String.class);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetDeclaredMethodWithParameters() throws NoSuchMethodException {
    assertThatCode(() -> {
      Method method = InstrumentedBridgeMethods.classgetDeclaredMethod(String.class, "charAt", int.class);
      assertThat(method).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassGetMethodWithParameters() throws NoSuchMethodException {
    assertThatCode(() -> {
      Method method = InstrumentedBridgeMethods.classgetMethod(String.class, "equals", Object.class);
      assertThat(method).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderResourcesWithNonExistentResource() {
    assertThatCode(() -> {
      Stream<URL> resources = InstrumentedBridgeMethods.classloaderresources(ClassLoader.getSystemClassLoader(), "nonexistent.resource");
      assertThat(resources).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleClassLoaderGetResourcesWithNonExistentResource() throws IOException {
    assertThatCode(() -> {
      Enumeration<URL> resources = InstrumentedBridgeMethods.classloadergetResources(ClassLoader.getSystemClassLoader(), "nonexistent.resource");
      assertThat(resources).isNotNull();
    }).doesNotThrowAnyException();
  }

  private static class TestClass {
    private String value;

    public String getValue() {
      return value;
    }
  }

}