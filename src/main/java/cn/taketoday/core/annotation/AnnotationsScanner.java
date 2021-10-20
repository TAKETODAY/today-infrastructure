/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.core.annotation;

import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;

/**
 * Scanner to search for relevant annotations in the annotation hierarchy of an
 * {@link AnnotatedElement}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see cn.taketoday.core.annotation.AnnotationsProcessor
 */
abstract class AnnotationsScanner {

	private static final Annotation[] NO_ANNOTATIONS = {};

	private static final Method[] NO_METHODS = {};


	private static final Map<AnnotatedElement, Annotation[]> declaredAnnotationCache =
			new ConcurrentReferenceHashMap<>(256);

	private static final Map<Class<?>, Method[]> baseTypeMethodsCache =
			new ConcurrentReferenceHashMap<>(256);


	private AnnotationsScanner() {
	}


	/**
	 * Scan the hierarchy of the specified element for relevant annotations and
	 * call the processor as required.
	 * @param context an optional context object that will be passed back to the
	 * processor
	 * @param source the source element to scan
	 * @param searchStrategy the search strategy to use
	 * @param processor the processor that receives the annotations
	 * @return the result of {@link cn.taketoday.core.annotation.AnnotationsProcessor#finish(Object)}
	 */
	@Nullable
	static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy,
			cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		R result = process(context, source, searchStrategy, processor);
		return processor.finish(result);
	}

	@Nullable
	private static <C, R> R process(C context, AnnotatedElement source,
			SearchStrategy searchStrategy, cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		if (source instanceof Class) {
			return processClass(context, (Class<?>) source, searchStrategy, processor);
		}
		if (source instanceof Method) {
			return processMethod(context, (Method) source, searchStrategy, processor);
		}
		return processElement(context, source, processor);
	}

	@Nullable
	private static <C, R> R processClass(C context, Class<?> source,
			SearchStrategy searchStrategy, cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		switch (searchStrategy) {
			case DIRECT:
				return processElement(context, source, processor);
			case INHERITED_ANNOTATIONS:
				return processClassInheritedAnnotations(context, source, searchStrategy, processor);
			case SUPERCLASS:
				return processClassHierarchy(context, source, processor, false, false);
			case TYPE_HIERARCHY:
				return processClassHierarchy(context, source, processor, true, false);
			case TYPE_HIERARCHY_AND_ENCLOSING_CLASSES:
				return processClassHierarchy(context, source, processor, true, true);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
	}

	@Nullable
	private static <C, R> R processClassInheritedAnnotations(C context, Class<?> source,
			SearchStrategy searchStrategy, cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		try {
			if (isWithoutHierarchy(source, searchStrategy)) {
				return processElement(context, source, processor);
			}
			Annotation[] relevant = null;
			int remaining = Integer.MAX_VALUE;
			int aggregateIndex = 0;
			Class<?> root = source;
			while (source != null && source != Object.class && remaining > 0 &&
					!hasPlainJavaAnnotationsOnly(source)) {
				R result = processor.doWithAggregate(context, aggregateIndex);
				if (result != null) {
					return result;
				}
				Annotation[] declaredAnnotations = getDeclaredAnnotations(source, true);
				if (relevant == null && declaredAnnotations.length > 0) {
					relevant = root.getAnnotations();
					remaining = relevant.length;
				}
				for (int i = 0; i < declaredAnnotations.length; i++) {
					if (declaredAnnotations[i] != null) {
						boolean isRelevant = false;
						for (int relevantIndex = 0; relevantIndex < relevant.length; relevantIndex++) {
							if (relevant[relevantIndex] != null &&
									declaredAnnotations[i].annotationType() == relevant[relevantIndex].annotationType()) {
								isRelevant = true;
								relevant[relevantIndex] = null;
								remaining--;
								break;
							}
						}
						if (!isRelevant) {
							declaredAnnotations[i] = null;
						}
					}
				}
				result = processor.doWithAnnotations(context, aggregateIndex, source, declaredAnnotations);
				if (result != null) {
					return result;
				}
				source = source.getSuperclass();
				aggregateIndex++;
			}
		}
		catch (Throwable ex) {
			AnnotationUtils.handleIntrospectionFailure(source, ex);
		}
		return null;
	}

	@Nullable
	private static <C, R> R processClassHierarchy(C context, Class<?> source,
                                                cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor, boolean includeInterfaces, boolean includeEnclosing) {

		return processClassHierarchy(context, new int[] {0}, source, processor,
				includeInterfaces, includeEnclosing);
	}

	@Nullable
	private static <C, R> R processClassHierarchy(C context, int[] aggregateIndex, Class<?> source,
                                                cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor, boolean includeInterfaces, boolean includeEnclosing) {

		try {
			R result = processor.doWithAggregate(context, aggregateIndex[0]);
			if (result != null) {
				return result;
			}
			if (hasPlainJavaAnnotationsOnly(source)) {
				return null;
			}
			Annotation[] annotations = getDeclaredAnnotations(source, false);
			result = processor.doWithAnnotations(context, aggregateIndex[0], source, annotations);
			if (result != null) {
				return result;
			}
			aggregateIndex[0]++;
			if (includeInterfaces) {
				for (Class<?> interfaceType : source.getInterfaces()) {
					R interfacesResult = processClassHierarchy(context, aggregateIndex,
						interfaceType, processor, true, includeEnclosing);
					if (interfacesResult != null) {
						return interfacesResult;
					}
				}
			}
			Class<?> superclass = source.getSuperclass();
			if (superclass != Object.class && superclass != null) {
				R superclassResult = processClassHierarchy(context, aggregateIndex,
					superclass, processor, includeInterfaces, includeEnclosing);
				if (superclassResult != null) {
					return superclassResult;
				}
			}
			if (includeEnclosing) {
				// Since merely attempting to load the enclosing class may result in
				// automatic loading of sibling nested classes that in turn results
				// in an exception such as NoClassDefFoundError, we wrap the following
				// in its own dedicated try-catch block in order not to preemptively
				// halt the annotation scanning process.
				try {
					Class<?> enclosingClass = source.getEnclosingClass();
					if (enclosingClass != null) {
						R enclosingResult = processClassHierarchy(context, aggregateIndex,
							enclosingClass, processor, includeInterfaces, true);
						if (enclosingResult != null) {
							return enclosingResult;
						}
					}
				}
				catch (Throwable ex) {
					AnnotationUtils.handleIntrospectionFailure(source, ex);
				}
			}
		}
		catch (Throwable ex) {
			AnnotationUtils.handleIntrospectionFailure(source, ex);
		}
		return null;
	}

	@Nullable
	private static <C, R> R processMethod(C context, Method source,
			SearchStrategy searchStrategy, cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		switch (searchStrategy) {
			case DIRECT:
			case INHERITED_ANNOTATIONS:
				return processMethodInheritedAnnotations(context, source, processor);
			case SUPERCLASS:
				return processMethodHierarchy(context, new int[] {0}, source.getDeclaringClass(),
						processor, source, false);
			case TYPE_HIERARCHY:
			case TYPE_HIERARCHY_AND_ENCLOSING_CLASSES:
				return processMethodHierarchy(context, new int[] {0}, source.getDeclaringClass(),
						processor, source, true);
		}
		throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
	}

	@Nullable
	private static <C, R> R processMethodInheritedAnnotations(C context, Method source,
			cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		try {
			R result = processor.doWithAggregate(context, 0);
			return (result != null ? result :
				processMethodAnnotations(context, 0, source, processor));
		}
		catch (Throwable ex) {
			AnnotationUtils.handleIntrospectionFailure(source, ex);
		}
		return null;
	}

	@Nullable
	private static <C, R> R processMethodHierarchy(C context, int[] aggregateIndex,
                                                 Class<?> sourceClass, cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor, Method rootMethod,
                                                 boolean includeInterfaces) {

		try {
			R result = processor.doWithAggregate(context, aggregateIndex[0]);
			if (result != null) {
				return result;
			}
			if (hasPlainJavaAnnotationsOnly(sourceClass)) {
				return null;
			}
			boolean calledProcessor = false;
			if (sourceClass == rootMethod.getDeclaringClass()) {
				result = processMethodAnnotations(context, aggregateIndex[0],
					rootMethod, processor);
				calledProcessor = true;
				if (result != null) {
					return result;
				}
			}
			else {
				for (Method candidateMethod : getBaseTypeMethods(context, sourceClass)) {
					if (candidateMethod != null && isOverride(rootMethod, candidateMethod)) {
						result = processMethodAnnotations(context, aggregateIndex[0],
							candidateMethod, processor);
						calledProcessor = true;
						if (result != null) {
							return result;
						}
					}
				}
			}
			if (Modifier.isPrivate(rootMethod.getModifiers())) {
				return null;
			}
			if (calledProcessor) {
				aggregateIndex[0]++;
			}
			if (includeInterfaces) {
				for (Class<?> interfaceType : sourceClass.getInterfaces()) {
					R interfacesResult = processMethodHierarchy(context, aggregateIndex,
						interfaceType, processor, rootMethod, true);
					if (interfacesResult != null) {
						return interfacesResult;
					}
				}
			}
			Class<?> superclass = sourceClass.getSuperclass();
			if (superclass != Object.class && superclass != null) {
				R superclassResult = processMethodHierarchy(context, aggregateIndex,
					superclass, processor, rootMethod, includeInterfaces);
				if (superclassResult != null) {
					return superclassResult;
				}
			}
		}
		catch (Throwable ex) {
			AnnotationUtils.handleIntrospectionFailure(rootMethod, ex);
		}
		return null;
	}

	private static <C> Method[] getBaseTypeMethods(C context, Class<?> baseType) {
		if (baseType == Object.class || hasPlainJavaAnnotationsOnly(baseType)) {
			return NO_METHODS;
		}

		Method[] methods = baseTypeMethodsCache.get(baseType);
		if (methods == null) {
			boolean isInterface = baseType.isInterface();
			methods = isInterface ? baseType.getMethods() : ReflectionUtils.getDeclaredMethods(baseType);
			int cleared = 0;
			for (int i = 0; i < methods.length; i++) {
				if ((!isInterface && Modifier.isPrivate(methods[i].getModifiers())) ||
						hasPlainJavaAnnotationsOnly(methods[i]) ||
						getDeclaredAnnotations(methods[i], false).length == 0) {
					methods[i] = null;
					cleared++;
				}
			}
			if (cleared == methods.length) {
				methods = NO_METHODS;
			}
			baseTypeMethodsCache.put(baseType, methods);
		}
		return methods;
	}

	private static boolean isOverride(Method rootMethod, Method candidateMethod) {
		return (!Modifier.isPrivate(candidateMethod.getModifiers()) &&
				candidateMethod.getName().equals(rootMethod.getName()) &&
				hasSameParameterTypes(rootMethod, candidateMethod));
	}

	private static boolean hasSameParameterTypes(Method rootMethod, Method candidateMethod) {
		if (candidateMethod.getParameterCount() != rootMethod.getParameterCount()) {
			return false;
		}
		Class<?>[] rootParameterTypes = rootMethod.getParameterTypes();
		Class<?>[] candidateParameterTypes = candidateMethod.getParameterTypes();
		if (Arrays.equals(candidateParameterTypes, rootParameterTypes)) {
			return true;
		}
		return hasSameGenericTypeParameters(rootMethod, candidateMethod,
				rootParameterTypes);
	}

	private static boolean hasSameGenericTypeParameters(
			Method rootMethod, Method candidateMethod, Class<?>[] rootParameterTypes) {

		Class<?> sourceDeclaringClass = rootMethod.getDeclaringClass();
		Class<?> candidateDeclaringClass = candidateMethod.getDeclaringClass();
		if (!candidateDeclaringClass.isAssignableFrom(sourceDeclaringClass)) {
			return false;
		}
		for (int i = 0; i < rootParameterTypes.length; i++) {
			Class<?> resolvedParameterType = ResolvableType.forMethodParameter(
					candidateMethod, i, sourceDeclaringClass).resolve();
			if (rootParameterTypes[i] != resolvedParameterType) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	private static <C, R> R processMethodAnnotations(C context, int aggregateIndex, Method source,
			cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		Annotation[] annotations = getDeclaredAnnotations(source, false);
		R result = processor.doWithAnnotations(context, aggregateIndex, source, annotations);
		if (result != null) {
			return result;
		}
		Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(source);
		if (bridgedMethod != source) {
			Annotation[] bridgedAnnotations = getDeclaredAnnotations(bridgedMethod, true);
			for (int i = 0; i < bridgedAnnotations.length; i++) {
				if (ObjectUtils.containsElement(annotations, bridgedAnnotations[i])) {
					bridgedAnnotations[i] = null;
				}
			}
			return processor.doWithAnnotations(context, aggregateIndex, source, bridgedAnnotations);
		}
		return null;
	}

	@Nullable
	private static <C, R> R processElement(C context, AnnotatedElement source,
			cn.taketoday.core.annotation.AnnotationsProcessor<C, R> processor) {

		try {
			R result = processor.doWithAggregate(context, 0);
			return (result != null ? result : processor.doWithAnnotations(
				context, 0, source, getDeclaredAnnotations(source, false)));
		}
		catch (Throwable ex) {
			AnnotationUtils.handleIntrospectionFailure(source, ex);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement source, Class<A> annotationType) {
		Annotation[] annotations = getDeclaredAnnotations(source, false);
		for (Annotation annotation : annotations) {
			if (annotation != null && annotationType == annotation.annotationType()) {
				return (A) annotation;
			}
		}
		return null;
	}

	static Annotation[] getDeclaredAnnotations(AnnotatedElement source, boolean defensive) {
		boolean cached = false;
		Annotation[] annotations = declaredAnnotationCache.get(source);
		if (annotations != null) {
			cached = true;
		}
		else {
			annotations = source.getDeclaredAnnotations();
			if (annotations.length != 0) {
				boolean allIgnored = true;
				for (int i = 0; i < annotations.length; i++) {
					Annotation annotation = annotations[i];
					if (isIgnorable(annotation.annotationType()) ||
							!AttributeMethods.forAnnotationType(annotation.annotationType()).isValid(annotation)) {
						annotations[i] = null;
					}
					else {
						allIgnored = false;
					}
				}
				annotations = (allIgnored ? NO_ANNOTATIONS : annotations);
				if (source instanceof Class || source instanceof Member) {
					declaredAnnotationCache.put(source, annotations);
					cached = true;
				}
			}
		}
		if (!defensive || annotations.length == 0 || !cached) {
			return annotations;
		}
		return annotations.clone();
	}

	private static boolean isIgnorable(Class<?> annotationType) {
		return cn.taketoday.core.annotation.AnnotationFilter.PLAIN.matches(annotationType);
	}

	static boolean isKnownEmpty(AnnotatedElement source, SearchStrategy searchStrategy) {
		if (hasPlainJavaAnnotationsOnly(source)) {
			return true;
		}
		if (searchStrategy == SearchStrategy.DIRECT || isWithoutHierarchy(source, searchStrategy)) {
			if (source instanceof Method && ((Method) source).isBridge()) {
				return false;
			}
			return getDeclaredAnnotations(source, false).length == 0;
		}
		return false;
	}

	static boolean hasPlainJavaAnnotationsOnly(@Nullable Object annotatedElement) {
		if (annotatedElement instanceof Class) {
			return hasPlainJavaAnnotationsOnly((Class<?>) annotatedElement);
		}
		else if (annotatedElement instanceof Member) {
			return hasPlainJavaAnnotationsOnly(((Member) annotatedElement).getDeclaringClass());
		}
		else {
			return false;
		}
	}

	static boolean hasPlainJavaAnnotationsOnly(Class<?> type) {
		return (type.getName().startsWith("java.") || type == Ordered.class);
	}

	private static boolean isWithoutHierarchy(AnnotatedElement source, SearchStrategy searchStrategy) {
		if (source == Object.class) {
			return true;
		}
		if (source instanceof Class) {
			Class<?> sourceClass = (Class<?>) source;
			boolean noSuperTypes = (sourceClass.getSuperclass() == Object.class &&
					sourceClass.getInterfaces().length == 0);
			return (searchStrategy == SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES ? noSuperTypes &&
					sourceClass.getEnclosingClass() == null : noSuperTypes);
		}
		if (source instanceof Method sourceMethod) {
			return (Modifier.isPrivate(sourceMethod.getModifiers()) ||
					isWithoutHierarchy(sourceMethod.getDeclaringClass(), searchStrategy));
		}
		return true;
	}

	static void clearCache() {
		declaredAnnotationCache.clear();
		baseTypeMethodsCache.clear();
	}

}
