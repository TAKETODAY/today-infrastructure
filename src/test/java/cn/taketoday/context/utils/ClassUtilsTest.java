package cn.taketoday.context.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
import test.demo.config.Config;

/**
 * 
 * @author Today <br>
 *         2018-06-0? ?
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

        Set<AnnotationAttributes> attributes = ClassUtils.getAnnotationAttributes(Bean.class.getAnnotation(S.class), C.class);
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
        assert annotationAttributes_.getStringArray("value").length == 1;
        log.info("annotationType: [{}]", annotationAttributes_.annotationType());
        assert annotationAttributes_.annotationType() == S.class;

        Collection<AnnotationAttributes> annotationAttributes = ClassUtils.getAnnotationAttributes(Bean.class, C.class);
        log.info("annotationAttributes: [{}]", annotationAttributes);
        for (AnnotationAttributes attributes : annotationAttributes) {
            log.info("annotationType: [{}]", attributes.annotationType());
            assert attributes.annotationType() == C.class;
            if ("s".equals(attributes.getStringArray("value")[0])) {
                assert attributes.getString("scope").equals(Scope.SINGLETON);
            }
            if ("p".equals(attributes.getStringArray("value")[0])) {
                assert attributes.getString("scope").equals(Scope.PROTOTYPE);
            }
        }

        final AnnotationAttributes attr = ClassUtils.getAnnotationAttributes(C.class, Bean.class);

        assert attr.getString("scope").equals(Scope.SINGLETON);
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

    @Test
    public void tset_GetFields() {
        setProcess("getFields");

        Collection<Field> fields = ClassUtils.getFields(StandardApplicationContext.class);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            ClassUtils.getFields(StandardApplicationContext.class);
        }

        System.err.println(System.currentTimeMillis() - start);

//        for (Field field : fields) {
//            System.err.println(field);
//        }

        final Field[] fieldArray = ClassUtils.getFieldArray(StandardApplicationContext.class);

        assert fields.size() == fieldArray.length;

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
        ClassUtils.invokeMethod(method, new AutowiredOnConstructor(null));

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
            ClassUtils.invokeMethod(throwing, new AutowiredOnConstructor(null));

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
        public AutowiredOnConstructor(ApplicationContext applicationContext) {
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
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @interface MySingleton {

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
        assertEquals(ClassUtilsTest.class, ClassUtils.getUserClass(new ClassUtilsTest().getClass().getName()));

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
}
