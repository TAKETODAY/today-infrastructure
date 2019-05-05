package test.context.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentImpl;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import test.context.props.Config_;
import test.context.utils.Bean.C;
import test.context.utils.Bean.S;
import test.demo.config.Config;
import test.demo.config.User;

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

	@Test
	public void test_Scan() {
		setProcess("test_Scan");
		Collection<Class<?>> scanPackage = ClassUtils.scan("test");
		for (Class<?> class1 : scanPackage) {
			System.err.println(class1);
		}

		Collection<Class<?>> scan = ClassUtils.scan("test.context.utils");
		assert !scan.contains(Config_.class);
		assert scanPackage.size() > 0 : "scan error";
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
		Collection<Component> classAnntation = ClassUtils.getAnnotation(Config.class, Component.class, ComponentImpl.class);

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
		Collection<Component> components = ClassUtils.getAnnotation(Config.class, Component.class, ComponentImpl.class);

		for (Component component : components) {
			System.err.println(component);
		}
	}

	@Test
	public void test_GetAnnotationArray() throws Exception {
		setProcess("getAnnotationArray");

		// test: use reflect build the annotation
		Component[] components = ClassUtils.getAnnotationArray(Config.class, Component.class, ComponentImpl.class);

		for (Component component : components) {
			System.err.println(component);
		}
	}

	@Test
	public void tset_GetFields() {
		setProcess("getFields");

		Collection<Field> fields = ClassUtils.getFields(StandardApplicationContext.class);

		long start = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			ClassUtils.getFields(StandardApplicationContext.class);
		}

		System.err.println(System.currentTimeMillis() - start);

		for (Field field : fields) {
			System.err.println(field);
		}
	}

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			try (ApplicationContext applicationContext = new StandardApplicationContext("","")) {
				System.err.println(applicationContext.getBean(User.class));
			}
		}
		System.err.println(System.currentTimeMillis() - start + "ms");
	}

	public String getProcess() {
		return process;
	}

	public void setProcess(String process) {
		this.process = process;
	}

}
