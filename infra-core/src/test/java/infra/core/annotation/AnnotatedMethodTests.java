package infra.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import infra.core.MethodParameter;
import infra.util.ReflectionUtils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/24 22:11
 */
class AnnotatedMethodTests {

  @Test
  void shouldFindAnnotationOnMethodInGenericAbstractSuperclass() {
    Method processTwo = getMethod("processTwo", String.class);

    AnnotatedMethod annotatedMethod = new AnnotatedMethod(processTwo);

    assertThat(annotatedMethod.hasMethodAnnotation(Handler.class)).isTrue();
  }

  @Test
  void shouldFindAnnotationOnMethodInGenericInterface() {
    Method processOneAndTwo = getMethod("processOneAndTwo", Long.class, Object.class);

    AnnotatedMethod annotatedMethod = new AnnotatedMethod(processOneAndTwo);

    assertThat(annotatedMethod.hasMethodAnnotation(Handler.class)).isTrue();
  }

  @Test
  void shouldFindAnnotationOnMethodParameterInGenericAbstractSuperclass() {
    // Prerequisites for gh-35349
    Method abstractMethod = ReflectionUtils.findMethod(GenericAbstractSuperclass.class, "processTwo", Object.class);
    assertThat(abstractMethod).isNotNull();
    assertThat(Modifier.isAbstract(abstractMethod.getModifiers())).as("abstract").isTrue();
    assertThat(Modifier.isPublic(abstractMethod.getModifiers())).as("public").isFalse();

    Method processTwo = getMethod("processTwo", String.class);

    AnnotatedMethod annotatedMethod = new AnnotatedMethod(processTwo);
    MethodParameter[] methodParameters = annotatedMethod.getMethodParameters();

    assertThat(methodParameters).hasSize(1);
    assertThat(methodParameters[0].hasParameterAnnotation(Param.class)).isTrue();
  }

  @Test
  void shouldFindAnnotationOnMethodParameterInGenericInterface() {
    Method processOneAndTwo = getMethod("processOneAndTwo", Long.class, Object.class);

    AnnotatedMethod annotatedMethod = new AnnotatedMethod(processOneAndTwo);
    MethodParameter[] methodParameters = annotatedMethod.getMethodParameters();

    assertThat(methodParameters).hasSize(2);
    assertThat(methodParameters[0].hasParameterAnnotation(Param.class)).isFalse();
    assertThat(methodParameters[1].hasParameterAnnotation(Param.class)).isTrue();
  }

  @Test
  void shouldFindProvidedArgumentWhenMatchingTypeExists() {
    Method method = ReflectionUtils.findMethod(GenericInterfaceImpl.class, "processTwo", String.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object[] providedArgs = { "testString", 123, new Object() };
    Object result = AnnotatedMethod.findProvidedArgument(parameter, providedArgs);

    assertThat(result).isEqualTo("testString");
  }

  @Test
  void shouldReturnNullWhenNoMatchingTypeExists() {
    Method method = ReflectionUtils.findMethod(GenericInterfaceImpl.class, "processTwo", String.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object[] providedArgs = { 123, 456L, new Object() };
    Object result = AnnotatedMethod.findProvidedArgument(parameter, providedArgs);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullWhenProvidedArgsIsNull() {
    Method method = ReflectionUtils.findMethod(GenericInterfaceImpl.class, "processTwo", String.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object result = AnnotatedMethod.findProvidedArgument(parameter, null);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullWhenProvidedArgsIsEmpty() {
    Method method = ReflectionUtils.findMethod(GenericInterfaceImpl.class, "processTwo", String.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object result = AnnotatedMethod.findProvidedArgument(parameter, new Object[0]);

    assertThat(result).isNull();
  }

  @Test
  void shouldFindProvidedArgumentWithInheritance() {
    Method method = ReflectionUtils.findMethod(GenericInterfaceImpl.class, "processTwo", String.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object[] providedArgs = { new StringBuilder("test") };
    Object result = AnnotatedMethod.findProvidedArgument(parameter, providedArgs);

    // StringBuilder is not a String, so should return null
    assertThat(result).isNull();
  }

  @Test
  void shouldFindFirstMatchingArgumentWhenMultipleExist() {
    Method method = ReflectionUtils.findMethod(GenericInterfaceImpl.class, "processTwo", String.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object[] providedArgs = { "first", "second", "third" };
    Object result = AnnotatedMethod.findProvidedArgument(parameter, providedArgs);

    assertThat(result).isEqualTo("first");
  }

  @Test
  void shouldHandlePrimitiveTypeParameters() {
    class TestClass {
      public void primitiveMethod(int value) { }
    }

    Method method = ReflectionUtils.findMethod(TestClass.class, "primitiveMethod", int.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object[] providedArgs = { 42, "string", new Object() };
    Object result = AnnotatedMethod.findProvidedArgument(parameter, providedArgs);

    assertThat(result).isEqualTo(42);
  }

  @Test
  void shouldHandleWrapperTypeParameters() {
    class TestClass {
      public void wrapperMethod(Integer value) { }
    }

    Method method = ReflectionUtils.findMethod(TestClass.class, "wrapperMethod", Integer.class);
    AnnotatedMethod annotatedMethod = new AnnotatedMethod(method);
    MethodParameter parameter = annotatedMethod.getMethodParameters()[0];

    Object[] providedArgs = { Integer.valueOf(42), "string", new Object() };
    Object result = AnnotatedMethod.findProvidedArgument(parameter, providedArgs);

    assertThat(result).isEqualTo(Integer.valueOf(42));
  }

  private static Method getMethod(String name, Class<?>... parameterTypes) {
    Class<?> clazz = GenericInterfaceImpl.class;
    Method method = ReflectionUtils.findMethod(clazz, name, parameterTypes);
    if (method == null) {
      String parameterNames = stream(parameterTypes).map(Class::getName).collect(joining(", "));
      throw new IllegalStateException("Expected method not found: %s#%s(%s)"
              .formatted(clazz.getSimpleName(), name, parameterNames));
    }
    return method;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Handler {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Param {
  }

  interface GenericInterface<A, B> {

    @Handler
    void processOneAndTwo(A value1, @Param B value2);
  }

  abstract static class GenericAbstractSuperclass<C> implements GenericInterface<Long, C> {

    @Override
    public void processOneAndTwo(Long value1, C value2) {
    }

    @Handler
    // Intentionally NOT public
    abstract void processTwo(@Param C value);
  }

  static class GenericInterfaceImpl extends GenericAbstractSuperclass<String> {

    @Override
    void processTwo(String value) {
    }
  }

}