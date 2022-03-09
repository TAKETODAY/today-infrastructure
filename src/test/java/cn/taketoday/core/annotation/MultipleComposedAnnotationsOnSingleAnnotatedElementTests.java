/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import static cn.taketoday.core.annotation.AnnotatedElementUtils.findAllMergedAnnotations;
import static cn.taketoday.core.annotation.AnnotatedElementUtils.getAllMergedAnnotations;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests that verify support for finding multiple composed annotations on
 * a single annotated element.
 *
 * <p>See <a href="https://jira.spring.io/browse/SPR-13486">SPR-13486</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see AnnotatedElementUtils
 * @see AnnotatedElementUtilsTests
 * @see ComposedRepeatableAnnotationsTests
 */
class MultipleComposedAnnotationsOnSingleAnnotatedElementTests {

	@Test
	void getMultipleComposedAnnotationsOnClass() {
		assertGetAllMergedAnnotationsBehavior(MultipleComposedCachesClass.class);
	}

	@Test
	void getMultipleInheritedComposedAnnotationsOnSuperclass() {
		assertGetAllMergedAnnotationsBehavior(SubMultipleComposedCachesClass.class);
	}

	@Test
	void getMultipleNoninheritedComposedAnnotationsOnClass() {
		Class<?> element = MultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(2);

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable cacheable1 = iterator.next();
		Cacheable cacheable2 = iterator.next();
		assertThat(cacheable1.value()).isEqualTo("noninheritedCache1");
		assertThat(cacheable2.value()).isEqualTo("noninheritedCache2");
	}

	@Test
	void getMultipleNoninheritedComposedAnnotationsOnSuperclass() {
		Class<?> element = SubMultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(0);
	}

	@Test
	void getComposedPlusLocalAnnotationsOnClass() {
		assertGetAllMergedAnnotationsBehavior(ComposedPlusLocalCachesClass.class);
	}

	@Test
	void getMultipleComposedAnnotationsOnInterface() {
		Class<MultipleComposedCachesOnInterfaceClass> element = MultipleComposedCachesOnInterfaceClass.class;
		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(0);
	}

	@Test
	void getMultipleComposedAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("multipleComposedCachesMethod");
		assertGetAllMergedAnnotationsBehavior(element);
	}

	@Test
	void getComposedPlusLocalAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("composedPlusLocalCachesMethod");
		assertGetAllMergedAnnotationsBehavior(element);
	}

	@Test
	@Disabled("Disabled since some Java 8 updates handle the bridge method differently")
	void getMultipleComposedAnnotationsOnBridgeMethod() throws Exception {
		Set<Cacheable> cacheables = getAllMergedAnnotations(getBridgeMethod(), Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(0);
	}

	@Test
	void findMultipleComposedAnnotationsOnClass() {
		assertFindAllMergedAnnotationsBehavior(MultipleComposedCachesClass.class);
	}

	@Test
	void findMultipleInheritedComposedAnnotationsOnSuperclass() {
		assertFindAllMergedAnnotationsBehavior(SubMultipleComposedCachesClass.class);
	}

	@Test
	void findMultipleNoninheritedComposedAnnotationsOnClass() {
		Class<?> element = MultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = findAllMergedAnnotations(element, Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(2);

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable cacheable1 = iterator.next();
		Cacheable cacheable2 = iterator.next();
		assertThat(cacheable1.value()).isEqualTo("noninheritedCache1");
		assertThat(cacheable2.value()).isEqualTo("noninheritedCache2");
	}

	@Test
	void findMultipleNoninheritedComposedAnnotationsOnSuperclass() {
		Class<?> element = SubMultipleNoninheritedComposedCachesClass.class;
		Set<Cacheable> cacheables = findAllMergedAnnotations(element, Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(2);

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable cacheable1 = iterator.next();
		Cacheable cacheable2 = iterator.next();
		assertThat(cacheable1.value()).isEqualTo("noninheritedCache1");
		assertThat(cacheable2.value()).isEqualTo("noninheritedCache2");
	}

	@Test
	void findComposedPlusLocalAnnotationsOnClass() {
		assertFindAllMergedAnnotationsBehavior(ComposedPlusLocalCachesClass.class);
	}

	@Test
	void findMultipleComposedAnnotationsOnInterface() {
		assertFindAllMergedAnnotationsBehavior(MultipleComposedCachesOnInterfaceClass.class);
	}

	@Test
	void findComposedCacheOnInterfaceAndLocalCacheOnClass() {
		assertFindAllMergedAnnotationsBehavior(ComposedCacheOnInterfaceAndLocalCacheClass.class);
	}

	@Test
	void findMultipleComposedAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("multipleComposedCachesMethod");
		assertFindAllMergedAnnotationsBehavior(element);
	}

	@Test
	void findComposedPlusLocalAnnotationsOnMethod() throws Exception {
		AnnotatedElement element = getClass().getDeclaredMethod("composedPlusLocalCachesMethod");
		assertFindAllMergedAnnotationsBehavior(element);
	}

	@Test
	void findMultipleComposedAnnotationsOnBridgeMethod() throws Exception {
		assertFindAllMergedAnnotationsBehavior(getBridgeMethod());
	}

	/**
	 * Bridge/bridged method setup code copied from
	 * {@link cn.taketoday.core.BridgeMethodResolverTests#withGenericParameter()}.
	 */
	Method getBridgeMethod() throws NoSuchMethodException {
		Method[] methods = StringGenericParameter.class.getMethods();
		Method bridgeMethod = null;
		Method bridgedMethod = null;

		for (Method method : methods) {
			if ("getFor".equals(method.getName()) && !method.getParameterTypes()[0].equals(Integer.class)) {
				if (method.getReturnType().equals(Object.class)) {
					bridgeMethod = method;
				}
				else {
					bridgedMethod = method;
				}
			}
		}
		assertThat(bridgeMethod != null && bridgeMethod.isBridge()).isTrue();
		boolean condition = bridgedMethod != null && !bridgedMethod.isBridge();
		assertThat(condition).isTrue();

		return bridgeMethod;
	}

	private void assertGetAllMergedAnnotationsBehavior(AnnotatedElement element) {
		assertThat(element).isNotNull();

		Set<Cacheable> cacheables = getAllMergedAnnotations(element, Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(2);

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable fooCacheable = iterator.next();
		Cacheable barCacheable = iterator.next();
		assertThat(fooCacheable.key()).isEqualTo("fooKey");
		assertThat(fooCacheable.value()).isEqualTo("fooCache");
		assertThat(barCacheable.key()).isEqualTo("barKey");
		assertThat(barCacheable.value()).isEqualTo("barCache");
	}

	private void assertFindAllMergedAnnotationsBehavior(AnnotatedElement element) {
		assertThat(element).isNotNull();

		Set<Cacheable> cacheables = findAllMergedAnnotations(element, Cacheable.class);
		assertThat(cacheables).isNotNull();
		assertThat(cacheables.size()).isEqualTo(2);

		Iterator<Cacheable> iterator = cacheables.iterator();
		Cacheable fooCacheable = iterator.next();
		Cacheable barCacheable = iterator.next();
		assertThat(fooCacheable.key()).isEqualTo("fooKey");
		assertThat(fooCacheable.value()).isEqualTo("fooCache");
		assertThat(barCacheable.key()).isEqualTo("barKey");
		assertThat(barCacheable.value()).isEqualTo("barCache");
	}


	// -------------------------------------------------------------------------

	/**
	 * Mock of {@code cn.taketoday.cache.annotation.Cacheable}.
	 */
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface Cacheable {

		@AliasFor("cacheName")
		String value() default "";

		@AliasFor("value")
		String cacheName() default "";

		String key() default "";
	}

	@Cacheable("fooCache")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface FooCache {

		@AliasFor(annotation = Cacheable.class)
		String key() default "";
	}

	@Cacheable("barCache")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@interface BarCache {

		@AliasFor(annotation = Cacheable.class)
		String key();
	}

	@Cacheable("noninheritedCache1")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface NoninheritedCache1 {

		@AliasFor(annotation = Cacheable.class)
		String key() default "";
	}

	@Cacheable("noninheritedCache2")
	@Target({ ElementType.METHOD, ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface NoninheritedCache2 {

		@AliasFor(annotation = Cacheable.class)
		String key() default "";
	}

	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private static class MultipleComposedCachesClass {
	}

	private static class SubMultipleComposedCachesClass extends MultipleComposedCachesClass {
	}

	@NoninheritedCache1
	@NoninheritedCache2
	private static class MultipleNoninheritedComposedCachesClass {
	}

	private static class SubMultipleNoninheritedComposedCachesClass extends MultipleNoninheritedComposedCachesClass {
	}

	@Cacheable(cacheName = "fooCache", key = "fooKey")
	@BarCache(key = "barKey")
	private static class ComposedPlusLocalCachesClass {
	}

	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private interface MultipleComposedCachesInterface {
	}

	private static class MultipleComposedCachesOnInterfaceClass implements MultipleComposedCachesInterface {
	}

	@Cacheable(cacheName = "fooCache", key = "fooKey")
	private interface ComposedCacheInterface {
	}

	@BarCache(key = "barKey")
	private static class ComposedCacheOnInterfaceAndLocalCacheClass implements ComposedCacheInterface {
	}


	@FooCache(key = "fooKey")
	@BarCache(key = "barKey")
	private void multipleComposedCachesMethod() {
	}

	@Cacheable(cacheName = "fooCache", key = "fooKey")
	@BarCache(key = "barKey")
	private void composedPlusLocalCachesMethod() {
	}


	public interface GenericParameter<T> {

		T getFor(Class<T> cls);
	}

	@SuppressWarnings("unused")
	private static class StringGenericParameter implements GenericParameter<String> {

		@FooCache(key = "fooKey")
		@BarCache(key = "barKey")
		@Override
		public String getFor(Class<String> cls) {
			return "foo";
		}

		public String getFor(Integer integer) {
			return "foo";
		}
	}

}
