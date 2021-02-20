package cn.taketoday.context.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.DefaultComponent;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.cglib.proxy.Enhancer;
import cn.taketoday.context.cglib.proxy.MethodInterceptor;
import cn.taketoday.context.cglib.proxy.MethodProxy;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Bean.C;
import cn.taketoday.context.utils.Bean.S;
import lombok.ToString;
import test.demo.config.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Today <br>
 * 2018-06-0? ?
 */
@Singleton("singleton")
@Prototype("prototype")
public class ClassUtilsTest {
  private static final Logger log = LoggerFactory.getLogger(ClassUtilsTest.class);

  private long start;

  private String process;

  public String getProcess() {
    return process;
  }

  public void setProcess(String process) {
    this.process = process;
  }

  @Before
  public void start() {
    start = System.currentTimeMillis();
  }

  @After
  public void end() {
    log.info("process:[{}] takes [{}]ms", getProcess(), (System.currentTimeMillis() - start));
  }

  public void test(String name, Integer i) {

  }

  @Test
  public void test_GetMethodArgsNames() throws NoSuchMethodException, SecurityException, IOException {
    setProcess("test_GetMethodArgsNames");
    String[] methodArgsNames = ClassUtils.getMethodArgsNames(ClassUtilsTest.class.getMethod("test", String.class, Integer.class));

    assert methodArgsNames.length > 0 : "Can't get Method Args Names";

    assert "name".equals(methodArgsNames[0]) : "Can't get Method Args Names";
    assert "i".equals(methodArgsNames[1]) : "Can't get Method Args Names";

    log.info("names: {}", Arrays.toString(methodArgsNames));
  }

  @Test
  @SuppressWarnings("unlikely-arg-type")
  public void test_GetAnnotation() throws Exception {
    setProcess("getAnnotation");

    // test: use reflect build the annotation
    Collection<Component> classAnntation = ClassUtils.getAnnotation(Config.class, Component.class, DefaultComponent.class);

    for (Component component : classAnntation) {
      log.info("component: [{}]", component);
      if (component.value().length != 0 && "prototype_config".equals(component.value()[0])) {
        assert component.scope().equals(Scope.PROTOTYPE);
      }
      else {
        assert component.scope().equals(Scope.SINGLETON);
      }
    }
    // use proxy
    Collection<C> annotations = ClassUtils.getAnnotation(Bean.class, C.class);

    List<AnnotationAttributes> attributes = ClassUtils.getAnnotationAttributes(Bean.class.getAnnotation(S.class), C.class);
    final AnnotationAttributes next = attributes.iterator().next();
    C annotation = ClassUtils.getAnnotationProxy(C.class, next);
    System.err.println(annotation);
    annotation.hashCode();
    assert annotation.annotationType() == C.class;
    assert annotation.scope().equals(Scope.SINGLETON);
    assert annotations.size() == 2;

    assert !annotation.equals(null);
    assert annotation.equals(annotation);

    assert annotation.equals(ClassUtils.getAnnotationProxy(C.class, next));

    final AnnotationAttributes clone = new AnnotationAttributes(next);

    assert !clone.equals(annotation);
    assert !clone.equals(null);
    assert clone.equals(clone);
    assert clone.equals(next);

    clone.remove("value");
    assert !clone.equals(next);
    assert !clone.equals(new AnnotationAttributes((Map<String, Object>) next));
    assert !annotation.equals(ClassUtils.getAnnotationProxy(C.class, clone));

    final AnnotationAttributes fromMap = AnnotationAttributes.fromMap(clone);
    assert fromMap.equals(clone);
    assert !fromMap.equals(new AnnotationAttributes());
    assert !fromMap.equals(new AnnotationAttributes(1));
    assert !fromMap.equals(new AnnotationAttributes(C.class));

    try {

      ClassUtils.getAnnotationAttributes(null);

      assert false;
    }
    catch (Exception e) {
      assert true;
    }
    try {
      ClassUtils.injectAttributes(next, null, next);
      assert false;
    }
    catch (Exception e) {
      assert true;
    }

  }

  @Test
  public void test_GetAnnotationAttributes() throws Exception {

    setProcess("test_GetAnnotationAttributes");
    S annotation = Bean.class.getAnnotation(S.class);
    AnnotationAttributes annotationAttributes_ = ClassUtils.getAnnotationAttributes(annotation);

    log.info("annotationAttributes: [{}]", annotationAttributes_);
    assertEquals(annotationAttributes_.getStringArray("value").length, 1);
    log.info("annotationType: [{}]", annotationAttributes_.annotationType());
    assertEquals(annotationAttributes_.annotationType(), S.class);

    List<AnnotationAttributes> annotationAttributes = ClassUtils.getAnnotationAttributes(Bean.class, C.class);
    log.info("annotationAttributes: [{}]", annotationAttributes);
    for (AnnotationAttributes attributes : annotationAttributes) {
      log.info("annotationType: [{}]", attributes.annotationType());
      assertEquals(attributes.annotationType(), C.class);
      if ("s".equals(attributes.getStringArray("value")[0])) {
        assertEquals(attributes.getString("scope"), Scope.SINGLETON);
      }
      if ("p".equals(attributes.getStringArray("value")[0])) {
        assertEquals(attributes.getString("scope"), Scope.PROTOTYPE);
      }
    }

    final AnnotationAttributes attr = ClassUtils.getAnnotationAttributes(C.class, Bean.class);

    assertEquals(attr.getString("scope"), Scope.SINGLETON);
  }

  @Test
  public void test_GetAnnotations() throws Exception {
    setProcess("getAnnotations");

    // test: use reflect build the annotation
    Collection<Component> components = ClassUtils.getAnnotation(Config.class, Component.class, DefaultComponent.class);

    for (Component component : components) {
      System.err.println(component);
    }
  }

  @Test
  public void test_GetAnnotationArray() throws Exception {
    setProcess("getAnnotationArray");

    // test: use reflect build the annotation
    Component[] components = ClassUtils.getAnnotationArray(Config.class, Component.class, DefaultComponent.class);

    final Component[] annotationArray = ClassUtils.getAnnotationArray(Config.class, Component.class);
    assert components.length > 0;
    assert annotationArray.length > 0;
    assert annotationArray.length == annotationArray.length;

  }

//    public static void main(String[] args) {
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < 10; i++) {
//            try (ApplicationContext applicationContext = new StandardApplicationContext("", "")) {
//                System.err.println(applicationContext.getBean(User.class));
//            }
//        }
//        System.err.println(System.currentTimeMillis() - start + "ms");
//    }

  @Test
  public void resolvePrimitiveClassName() {
    setProcess("resolvePrimitiveClassName");

    assert ClassUtils.resolvePrimitiveClassName("java.lang.Float") == null;
    assert ClassUtils.resolvePrimitiveClassName("float") == float.class;
    assert ClassUtils.resolvePrimitiveClassName(null) == null;
  }

  @Test
  public void forName() throws ClassNotFoundException {
    setProcess("forName");

    assert ClassUtils.forName("java.lang.Float") == Float.class;
    assert ClassUtils.forName("float") == float.class;
    assert ClassUtils.forName("java.lang.String[]") == String[].class;
    assert ClassUtils.forName("[Ljava.lang.String;") == String[].class;
    assert ClassUtils.forName("[[Ljava.lang.String;") == String[][].class;

    try {
      ClassUtils.forName("Float");
    }
    catch (ClassNotFoundException e) {}
    assert ClassUtils.forName("cn.taketoday.context.utils.ClassUtilsTest.INNER") == INNER.class;
    try {
      ClassUtils.forName("cn.taketoday.context.utils.ClassUtilsTest.INNERs");//
    }
    catch (ClassNotFoundException e) {}

  }

  private static class INNER {

  }

  @Test
  public void isPresent() {
    setProcess("isPresent");

    assert ClassUtils.isPresent("java.lang.Float");
    assert !ClassUtils.isPresent("Float");
  }

  @Test
  public void testAutowiredOnConstructor() {

    setProcess("AutowiredOnConstructor");

    try (ApplicationContext applicationContext = new StandardApplicationContext("", "")) {
      System.err.println(applicationContext.getBean(AutowiredOnConstructor.class));
      applicationContext.registerBean("testAutowiredOnConstructorThrow", AutowiredOnConstructorThrow.class);

      try {
        applicationContext.getBean(AutowiredOnConstructorThrow.class);

        assert false;
      }
      catch (Exception e) {
        assert true;
      }
    }
  }

  @Test
  public void testIsAnnotationPresent() {
    setProcess("isAnnotationPresent");

    assert ClassUtils.isAnnotationPresent(AutowiredOnConstructor.class, Singleton.class);
    assert ClassUtils.isAnnotationPresent(AutowiredOnConstructor.class, MySingleton.class);

    assert ClassUtils.loadClass("") == null;
  }

  @Test
  public void testOther() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
    setProcess("invokeMethod");

    final Method method = AutowiredOnConstructor.class.getDeclaredMethod("test");
    ReflectionUtils.accessInvokeMethod(method, new AutowiredOnConstructor(null));

    assert ClassUtils.newInstance(ClassUtilsTest.class.getName()) != null;

    try {
      ClassUtils.newInstance("not found");
      assert false;
    }
    catch (Exception e) {
      assert true;
    }
    try {

      final Method throwing = AutowiredOnConstructor.class.getDeclaredMethod("throwing");
      ReflectionUtils.accessInvokeMethod(throwing, new AutowiredOnConstructor(null));

      assert false;
    }
    catch (Exception e) {
      assert true;
    }
  }

  @MySingleton
  @SuppressWarnings("unused")
  private static class AutowiredOnConstructor {

    @Autowired
    private AutowiredOnConstructor(ApplicationContext applicationContext) {
      System.err.println("init");
    }

    private void test() {
      System.err.println("test");
    }

    private void throwing() {
      throw new ContextException();
    }
  }

  @Singleton
  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD }) @interface MySingleton {

  }

  static class AutowiredOnConstructorThrow {

    public AutowiredOnConstructorThrow(ApplicationContext applicationContext) {
      System.err.println("init");
      throw new ContextException();
    }

  }

  // v2.1.7 test code
  // ----------------------------------------

  @Test
  public void testGetUserClass() {
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(getClass()));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(getClass().getName()));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(ClassUtilsTest.class.getName()));

    Enhancer enhancer = new Enhancer();

    enhancer.setCallback(new MethodInterceptor() {
      @Override
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        return null;
      }
    });

    enhancer.setSuperclass(ClassUtilsTest.class);

    final Object create = enhancer.create();

    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(create));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(create.getClass()));
    assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(create.getClass().getName()));
  }

  // ------

  static class Genericity<T> { }

  static class Generic extends Genericity<Integer> {
    List<?> list;
    List<String> stringList;
    Map<String, Object> stringMap;

    Generic(List<String> stringList,
            Map<String, Object> stringMap) { }

    void generic(List<String> stringList,
                 Map<String, Object> stringMap) { }
  }

  @Test
  public void testGetGenerics() {
    setProcess("testGetGenerics");

    assertThat(ClassUtils.getGenerics(Generic.class, Genericity.class))
            .isNotNull()
            .hasSize(1)
            .contains(Integer.class);

    Field stringList = ReflectionUtils.findField(Generic.class, "stringList");

    assertThat(ClassUtils.getGenerics(stringList))
            .isNotNull()
            .hasSize(1)
            .contains(String.class);

    Field stringMap = ReflectionUtils.findField(Generic.class, "stringMap");

    assertThat(ClassUtils.getGenerics(stringMap))
            .isNotNull()
            .hasSize(2)
            .contains(String.class, Object.class);

    // param
    Constructor<Generic> constructor = ClassUtils.getSuitableConstructor(Generic.class);
    Parameter[] parameters = constructor.getParameters();

    assertThat(ClassUtils.getGenerics(parameters[0]))
            .isNotNull()
            .hasSize(1)
            .contains(String.class);

    assertThat(ClassUtils.getGenerics(parameters[1]))
            .isNotNull()
            .hasSize(2)
            .contains(String.class, Object.class);

    Method method = ReflectionUtils.findMethod(Generic.class, "generic");

    assertThat(ClassUtils.getGenerics(method.getParameters()[0]))
            .isNotNull()
            .hasSize(1)
            .contains(String.class);

    assertThat(ClassUtils.getGenerics(method.getParameters()[1]))
            .isNotNull()
            .hasSize(2)
            .contains(String.class, Object.class);

    Field list = ReflectionUtils.findField(Generic.class, "list");

    final Type[] genericityClass = ClassUtils.getGenerics(list);
    assertThat(genericityClass)
            .isNotNull()
            .hasSize(1);

    assertThat(genericityClass[0])
            .isInstanceOf(WildcardType.class);

  }

  // fix bug

  interface Interface<T> {

  }

  interface Interface1<T> {

  }

  interface Interface2<T> {

  }

  interface NestedGenericInterface extends Interface<String> {

  }

  static abstract class Abs {

  }

  static abstract class GenericAbs implements Interface<String> {

  }

  static class AbsGeneric extends Abs
          implements Interface<String>,
                     Interface1<Integer>,
                     Interface2<Interface<String>> {

  }

  static class GenericAbsGeneric extends GenericAbs
          implements Interface1<Integer>, Interface2<Interface<String>> {

  }

  static class NestedGenericInterfaceBean extends GenericAbs
          implements NestedGenericInterface, Interface1<Integer>, Interface2<Interface<String>> {

  }

  static class NoGeneric {

  }

/*
  @Test
  public void test() {
    final Class<NoGeneric> noGeneric = NoGeneric.class;
    final Class<AbsGeneric> absGenericClass = AbsGeneric.class;
    final Class<GenericAbsGeneric> genericAbsGeneric = GenericAbsGeneric.class;
    final Class<NestedGenericInterfaceBean> nestedGenericInterfaceBeanClass = NestedGenericInterfaceBean.class;

    // 直接在第一级接口
    final java.lang.reflect.Type[] genericInterfaces = absGenericClass.getGenericInterfaces();

    for (final Type genericInterface : genericInterfaces) {
//      System.out.println(genericInterface);
    }
    // 在父类上找
    final Type genericSuperclass = genericAbsGeneric.getGenericSuperclass();

//    System.out.println(genericSuperclass);
    // 第一级没有
    final Type[] genericInterfaces1 = nestedGenericInterfaceBeanClass.getGenericInterfaces();
    for (final Type genericInterface : genericInterfaces1) {
//      System.out.println(genericInterface);
    }

    // 没有
//    System.out.println(Arrays.toString(noGeneric.getGenericInterfaces()));

    //

    for (final Type genericInterface : genericInterfaces) {
      System.out.println(genericInterface.getClass());
    }

  }*/

  @Test
  public void testGetGenericsInterface() {
    setProcess("testGetGenericsInterface");
    final Type[] generics = ClassUtils.getGenerics(AbsGeneric.class, Interface.class);
    final Type[] generics1 = ClassUtils.getGenerics(GenericAbsGeneric.class, Interface.class);
    final Type[] generics2 = ClassUtils.getGenerics(NestedGenericInterfaceBean.class, Interface.class);

    System.out.println(Arrays.toString(generics));
    System.out.println(Arrays.toString(generics1));
    System.out.println(Arrays.toString(generics2));

    assertThat(generics1)
            .hasSize(1)
            .isEqualTo(generics)
            .isEqualTo(generics2)
            .contains(String.class)
    ;

    assertThat(ClassUtils.getGenerics(NoGeneric.class))
            .isNull();

  }

  static class TestNewInstanceBean { }

  @ToString
  static class TestNewInstanceBeanProvidedArgs {
    Integer integer;

    TestNewInstanceBeanProvidedArgs(Integer integer) {
      this.integer = integer;
    }
  }

  //
  @Test
  public void testNewInstance() {
    final TestNewInstanceBean testNewInstanceBean = ClassUtils.newInstance(TestNewInstanceBean.class);

    System.out.println(testNewInstanceBean);

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      final TestNewInstanceBeanProvidedArgs providedArgs = ClassUtils
              .newInstance(TestNewInstanceBeanProvidedArgs.class, context, new Object[] { 1, "TODAY" });

      System.out.println(providedArgs);
    }
  }

}
