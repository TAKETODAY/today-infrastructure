package test.context.utils;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import test.context.props.Config_;
import test.context.utils.Bean.C;
import test.context.utils.Bean.S;
import test.demo.config.Config;

/**
 * 
 * @author Today <br>
 *         2018-06-0? ?
 */
@Slf4j
@Singleton("singleton")
@Prototype("prototype")
public class ClassUtilsTest {

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
    public void test_GetClassCache() {
        setProcess("test_GetClassCache");
        Collection<Class<?>> classCache = ClassUtils.getClassCache();
        assert classCache.size() > 0 : "cache error";
    }

    private @interface TEST {

    }

    @Test
    public void test_Scan() {
        ClassUtils.addIgnoreAnnotationClass(TEST.class);
        setProcess("test_Scan");
        Collection<Class<?>> scanPackage = ClassUtils.scan("test");
        for (Class<?> class1 : scanPackage) {
            System.err.println(class1);
        }

        System.err.println("===========================");

        ClassUtils.clearCache();

        Collection<Class<?>> scan = ClassUtils.scan("test.context.utils", "cn.taketoday");
//        for (Class<?> class1 : scan) {
//            System.err.println(class1);
//        }
        assert !scan.contains(Config_.class);
        assert scanPackage.size() > 0 : "scan error";
        // in jar

        ClassUtils.setClassCache(null);

        final Set<Class<?>> scan2 = ClassUtils.scan("com.sun.el");
        assert scan2.size() == 0;

        ClassUtils.setClassCache(null);
        ClassUtils.setIgnoreScanJarsPrefix(false);
        final Set<Class<?>> scan3 = ClassUtils.scan("com.sun.el");
        assert scan3.size() != 0;
        ClassUtils.setIgnoreScanJarsPrefix(true);

        ClassUtils.setClassCache(null);
        final Set<Class<?>> scanEmpty = ClassUtils.scan("cn.taketoday", "");

        assert scanEmpty.size() > 0;

        // don't clear cache
        assert ClassUtils.scan("").size() > 0;

        ClassUtils.setClassCache(null);

        assert ClassUtils.scan("").size() > 0; // for scanOne

        assert ClassUtils.scan("").size() > 0; // for scanOne

        ClassUtils.clearCache();
        assert ClassUtils.scan("cn.taketoday").size() == ClassUtils.getClasses("cn.taketoday").size();
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
    public void test_GetAnnotation() throws Exception {
        setProcess("getAnnotation");

        // test: use reflect build the annotation
        Collection<Component> classAnntation = ClassUtils.getAnnotation(Config.class, Component.class, DefaultComponent.class);

        for (Component component : classAnntation) {
            log.info("component: [{}]", component);
            if (component.value().length != 0 && "prototype_config".equals(component.value()[0])) {
                assert component.scope() == Scope.PROTOTYPE;
            }
            else {
                assert component.scope() == Scope.SINGLETON;
            }
        }
        // use proxy
        Collection<C> annotations = ClassUtils.getAnnotation(Bean.class, C.class);

        AnnotationAttributes attributes = ClassUtils.getAnnotationAttributes(Bean.class.getAnnotation(S.class), C.class);
        C annotation = ClassUtils.getAnnotationProxy(C.class, attributes);
        System.err.println(annotation);
        annotation.hashCode();
        assert annotation.annotationType() == C.class;
        assert annotation.scope() == Scope.SINGLETON;
        assert annotations.size() == 2;

        try {

            ClassUtils.getAnnotationAttributes(null);

            assert false;
        }
        catch (Exception e) {
            assert true;
        }
        try {
            ClassUtils.injectAttributes(attributes, null, attributes);
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
                assert attributes.getEnum("scope") == Scope.SINGLETON;
            }
            if ("q".equals(attributes.getStringArray("value")[0])) {
                assert attributes.getEnum("scope") == Scope.PROTOTYPE;
            }
        }
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
    public void isCollection() throws ClassNotFoundException {

        setProcess("isCollection");

        assert ClassUtils.isCollection(List.class);
        assert ClassUtils.isCollection(Set.class);
        assert !ClassUtils.isCollection(INNER.class);
        assert ClassUtils.isCollection(ArrayList.class);
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
        catch (ClassNotFoundException e) {
        }
        assert ClassUtils.forName("test.context.utils.ClassUtilsTest.INNER") == INNER.class;
        try {
            ClassUtils.forName("test.context.utils.ClassUtilsTest.INNERs");//
        }
        catch (ClassNotFoundException e) {
        }

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

        ClassUtils.clearCache();
        assert ClassUtils.getAnnotatedClasses(Singleton.class).size() > 0;

        final int size = ClassUtils.getImplClasses(ApplicationContext.class).size();
        final int size2 = ClassUtils.getImplClasses(ApplicationContext.class, "cn.taketoday").size();

        assert size > 0;
        assert size2 > 0;
        assert size == size2;

        ClassUtils.clearCache();

    }

    @Test
    public void testOther() throws NoSuchMethodException, SecurityException {
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

}
