package test.context.utils;

import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentImpl;
import cn.taketoday.context.utils.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import test.domain.Config;

/**
 * 
 * @author Today <br>
 *         2018-06-0? ?
 */
public class ClassUtilsTest {

	private long start;

	@Before
	public void start() {
		start = System.currentTimeMillis();
	}

	@After
	public void end() {
		System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
	}

	public void test(String name, Integer i) {

	}

	@Test
	public void test_ClassCache() {
		Collection<Class<?>> classCache = ClassUtils.getClassCache();

		assert classCache.size() > 0 : "cache error";
	}

	@Test
	public void test_ScanPackage() {
		Collection<Class<?>> scanPackage = ClassUtils.scanPackage("cn.taketoday.test");

		assert scanPackage.size() > 0 : "scanPackage error";
	}

	@Test
	public void test_GetMethodArgsNames() throws NoSuchMethodException, SecurityException, IOException {

		String[] methodArgsNames = ClassUtils
				.getMethodArgsNames(ClassUtilsTest.class.getMethod("test", String.class, Integer.class));

		assert methodArgsNames.length > 0 : "Can't get Method Args Names";
		System.out.println(Arrays.toString(methodArgsNames));
	}

	@Test
	public void test_GetClassAnntation() throws Exception {

		Component[] classAnntation = ClassUtils.getClassAnntation(Config.class, Component.class, ComponentImpl.class);
		for (Component componentImpl : classAnntation) {

			System.out.println(componentImpl);
		}
	}

	@Test
	public void test_ClassAnntation() throws Exception {
		URL uri = new URL("file:/D:/");

		URLConnection openConnection = uri.openConnection();

		openConnection.connect();
		InputStream inputStream = openConnection.getInputStream();

		System.out.println(inputStream.available());
		byte[] b = new byte[inputStream.available()];
		inputStream.read(b);
		System.err.println(new String(b));
		System.err.println(uri);
	}
	
	@Test
	public void test_ClassAnntation_() throws Exception {
		System.out.println(ClassUtils.getClassLoader().getResource(""));
	}

	
	
}
