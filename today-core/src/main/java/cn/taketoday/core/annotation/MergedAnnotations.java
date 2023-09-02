/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Provides access to a collection of merged annotations, usually obtained
 * from a source such as a {@link Class} or {@link Method}.
 *
 * <p>Each merged annotation represents a view where the attribute values may be
 * "merged" from different source values, typically:
 *
 * <ul>
 * <li>Explicit and Implicit {@link AliasFor @AliasFor} declarations on one or
 * more attributes within the annotation</li>
 * <li>Explicit {@link AliasFor @AliasFor} declarations for a meta-annotation</li>
 * <li>Convention based attribute aliases for a meta-annotation</li>
 * <li>From a meta-annotation declaration</li>
 * </ul>
 *
 * <p>For example, a {@code @PostMapping} annotation might be defined as follows:
 *
 * <pre>{@code
 * @Retention(RetentionPolicy.RUNTIME)
 * @RequestMapping(method = HttpMethod.POST)
 * public @interface PostMapping {
 *
 *     @AliasFor(attribute = "path")
 *     String[] value() default {};
 *
 *     @AliasFor(attribute = "value")
 *     String[] path() default {};
 * }
 * }</pre>
 *
 * <p>If a method is annotated with {@code @PostMapping("/home")} it will contain
 * merged annotations for both {@code @PostMapping} and the meta-annotation
 * {@code @RequestMapping}. The merged view of the {@code @RequestMapping}
 * annotation will contain the following attributes:
 *
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Value</th>
 * <th>Source</th>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>"/home"</td>
 * <td>Declared in {@code @PostMapping}</td>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>"/home"</td>
 * <td>Explicit {@code @AliasFor}</td>
 * </tr>
 * <tr>
 * <td>method</td>
 * <td>HttpMethod.POST</td>
 * <td>Declared in meta-annotation</td>
 * </tr>
 * </table>
 *
 * <p>{@link MergedAnnotations} can be obtained {@linkplain #from(AnnotatedElement)
 * from} any Java {@link AnnotatedElement}. They may also be used for sources that
 * don't use reflection (such as those that directly parse bytecode).
 *
 * <p>Different {@linkplain SearchStrategy search strategies} can be used to locate
 * related source elements that contain the annotations to be aggregated. For
 * example, the following code uses {@link SearchStrategy#TYPE_HIERARCHY} to
 * search for annotations on {@code MyClass} as well as in superclasses and implemented
 * interfaces.
 *
 * <pre>{@code
 * MergedAnnotations mergedAnnotations =
 *     MergedAnnotations.search(TYPE_HIERARCHY).from(MyClass.class);
 * }</pre>
 *
 * <p>From a {@link MergedAnnotations} instance you can either
 * {@linkplain #get(String) get} a single annotation, or {@linkplain #stream()
 * stream all annotations} or just those that match {@linkplain #stream(String)
 * a specific type}. You can also quickly tell if an annotation
 * {@linkplain #isPresent(String) is present}.
 *
 * <p>Here are some typical examples:
 *
 * <pre>{@code
 * // is an annotation present or meta-present?
 * mergedAnnotations.isPresent(ExampleAnnotation.class);
 *
 * // get the merged "value" attribute of ExampleAnnotation (either directly or
 * // meta-present)
 * mergedAnnotations.get(ExampleAnnotation.class).getString("value");
 *
 * // get all meta-annotations but no directly present annotations
 * mergedAnnotations.stream().filter(MergedAnnotation::isMetaPresent);
 *
 * // get all ExampleAnnotation declarations (including any meta-annotations) and
 * // print the merged "value" attributes
 * mergedAnnotations.stream(ExampleAnnotation.class)
 *     .map(mergedAnnotation -&gt; mergedAnnotation.getString("value"))
 *     .forEach(System.out::println);
 * }</pre>
 *
 * <p><b>NOTE: The {@code MergedAnnotations} API and its underlying model have
 * been designed for composable annotations in common component model,
 * with a focus on attribute aliasing and meta-annotation relationships.</b>
 * There is no support for retrieving plain Java annotations with this API;
 * please use standard Java reflection or {@link AnnotationUtils}
 * for simple annotation retrieval purposes.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MergedAnnotation
 * @see MergedAnnotationCollectors
 * @see MergedAnnotationPredicates
 * @see MergedAnnotationSelectors
 * @since 4.0
 */
public interface MergedAnnotations extends Iterable<MergedAnnotation<Annotation>> {

  /**
   * Determine if the specified annotation is either directly present or
   * meta-present.
   * <p>Equivalent to calling {@code get(annotationType).isPresent()}.
   *
   * @param annotationType the annotation type to check
   * @return {@code true} if the annotation is present
   */
  <A extends Annotation> boolean isPresent(Class<A> annotationType);

  /**
   * Determine if the specified annotation is either directly present or
   * meta-present.
   * <p>Equivalent to calling {@code get(annotationType).isPresent()}.
   *
   * @param annotationType the fully qualified class name of the annotation type
   * to check
   * @return {@code true} if the annotation is present
   */
  boolean isPresent(String annotationType);

  /**
   * Determine if the specified annotation is directly present.
   * <p>Equivalent to calling {@code get(annotationType).isDirectlyPresent()}.
   *
   * @param annotationType the annotation type to check
   * @return {@code true} if the annotation is directly present
   */
  <A extends Annotation> boolean isDirectlyPresent(Class<A> annotationType);

  /**
   * Determine if the specified annotation is directly present.
   * <p>Equivalent to calling {@code get(annotationType).isDirectlyPresent()}.
   *
   * @param annotationType the fully qualified class name of the annotation type
   * to check
   * @return {@code true} if the annotation is directly present
   */
  boolean isDirectlyPresent(String annotationType);

  /**
   * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
   * annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the annotation type to get
   * @return a {@link MergedAnnotation} instance
   */
  <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType);

  /**
   * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
   * annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the annotation type to get
   * @param predicate a predicate that must match, or {@code null} if only
   * type matching is required
   * @return a {@link MergedAnnotation} instance
   * @see MergedAnnotationPredicates
   */
  <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate);

  /**
   * Get a matching annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the annotation type to get
   * @param predicate a predicate that must match, or {@code null} if only
   * type matching is required
   * @param selector a selector used to choose the most appropriate annotation
   * within an aggregate, or {@code null} to select the
   * {@linkplain MergedAnnotationSelectors#nearest() nearest}
   * @return a {@link MergedAnnotation} instance
   * @see MergedAnnotationPredicates
   * @see MergedAnnotationSelectors
   */
  <A extends Annotation> MergedAnnotation<A> get(Class<A> annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate, @Nullable MergedAnnotationSelector<A> selector);

  /**
   * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
   * annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the fully qualified class name of the annotation type
   * to get
   * @return a {@link MergedAnnotation} instance
   */
  <A extends Annotation> MergedAnnotation<A> get(String annotationType);

  /**
   * Get the {@linkplain MergedAnnotationSelectors#nearest() nearest} matching
   * annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the fully qualified class name of the annotation type
   * to get
   * @param predicate a predicate that must match, or {@code null} if only
   * type matching is required
   * @return a {@link MergedAnnotation} instance
   * @see MergedAnnotationPredicates
   */
  <A extends Annotation> MergedAnnotation<A> get(String annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate);

  /**
   * Get a matching annotation or meta-annotation of the specified type, or
   * {@link MergedAnnotation#missing()} if none is present.
   *
   * @param annotationType the fully qualified class name of the annotation type
   * to get
   * @param predicate a predicate that must match, or {@code null} if only
   * type matching is required
   * @param selector a selector used to choose the most appropriate annotation
   * within an aggregate, or {@code null} to select the
   * {@linkplain MergedAnnotationSelectors#nearest() nearest}
   * @return a {@link MergedAnnotation} instance
   * @see MergedAnnotationPredicates
   * @see MergedAnnotationSelectors
   */
  <A extends Annotation> MergedAnnotation<A> get(String annotationType,
          @Nullable Predicate<? super MergedAnnotation<A>> predicate, @Nullable MergedAnnotationSelector<A> selector);

  /**
   * Stream all annotations and meta-annotations that match the specified
   * type. The resulting stream follows the same ordering rules as
   * {@link #stream()}.
   *
   * @param annotationType the annotation type to match
   * @return a stream of matching annotations
   */
  <A extends Annotation> Stream<MergedAnnotation<A>> stream(Class<A> annotationType);

  /**
   * Stream all annotations and meta-annotations that match the specified
   * type. The resulting stream follows the same ordering rules as
   * {@link #stream()}.
   *
   * @param annotationType the fully qualified class name of the annotation type
   * to match
   * @return a stream of matching annotations
   */
  <A extends Annotation> Stream<MergedAnnotation<A>> stream(String annotationType);

  /**
   * Stream all annotations and meta-annotations contained in this collection.
   * The resulting stream is ordered first by the
   * {@linkplain MergedAnnotation#getAggregateIndex() aggregate index} and then
   * by the annotation distance (with the closest annotations first). This ordering
   * means that, for most use-cases, the most suitable annotations appear
   * earliest in the stream.
   *
   * @return a stream of annotations
   */
  Stream<MergedAnnotation<Annotation>> stream();

  <A extends Annotation> AnnotationAttributes[] getAttributes(Class<A> annotationType);

  // Static Factory Methods

  /**
   * Create a new {@link MergedAnnotations} instance containing all
   * annotations and meta-annotations from the specified element. The
   * resulting instance will not include any inherited annotations. If you
   * want to include those as well you should use
   * {@link #from(AnnotatedElement, SearchStrategy)} with an appropriate
   * {@link SearchStrategy}.
   *
   * @param element the source element
   * @return a {@link MergedAnnotations} instance containing the element's
   * annotations
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(AnnotatedElement element) {
    return from(element, SearchStrategy.DIRECT);
  }

  /**
   * Create a new {@link MergedAnnotations} instance containing all
   * annotations and meta-annotations from the specified element and,
   * depending on the {@link SearchStrategy}, related inherited elements.
   *
   * @param element the source element
   * @param searchStrategy the search strategy to use
   * @return a {@link MergedAnnotations} instance containing the merged
   * element annotations
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy) {
    return from(element, searchStrategy, RepeatableContainers.standard());
  }

  /**
   * Create a new {@link MergedAnnotations} instance containing all
   * annotations and meta-annotations from the specified element and,
   * depending on the {@link SearchStrategy}, related inherited elements.
   *
   * @param element the source element
   * @param searchStrategy the search strategy to use
   * @param repeatableContainers the repeatable containers that may be used by
   * the element annotations or the meta-annotations
   * @return a {@link MergedAnnotations} instance containing the merged
   * element annotations
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
          RepeatableContainers repeatableContainers) {

    return from(element, searchStrategy, repeatableContainers, AnnotationFilter.PLAIN);
  }

  /**
   * Create a new {@link MergedAnnotations} instance containing all
   * annotations and meta-annotations from the specified element and,
   * depending on the {@link SearchStrategy}, related inherited elements.
   *
   * @param element the source element
   * @param searchStrategy the search strategy to use
   * @param repeatableContainers the repeatable containers that may be used by
   * the element annotations or the meta-annotations
   * @param annotationFilter an annotation filter used to restrict the
   * annotations considered
   * @return a {@link MergedAnnotations} instance containing the merged
   * annotations for the supplied element
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
          RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {

    return from(element, searchStrategy, Search.never, repeatableContainers, annotationFilter);
  }

  private static MergedAnnotations from(AnnotatedElement element, SearchStrategy searchStrategy,
          Predicate<Class<?>> searchEnclosingClass, RepeatableContainers repeatableContainers,
          AnnotationFilter annotationFilter) {

    Assert.notNull(element, "AnnotatedElement is required");
    Assert.notNull(searchStrategy, "SearchStrategy is required");
    Assert.notNull(searchEnclosingClass, "Predicate is required");
    Assert.notNull(annotationFilter, "AnnotationFilter is required");
    Assert.notNull(repeatableContainers, "RepeatableContainers is required");
    return TypeMappedAnnotations.from(element, searchStrategy, searchEnclosingClass,
            repeatableContainers, annotationFilter);
  }

  /**
   * Create a new {@link MergedAnnotations} instance from the specified
   * annotations.
   *
   * @param annotations the annotations to include
   * @return a {@link MergedAnnotations} instance containing the annotations
   * @see #from(Object, Annotation...)
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(Annotation... annotations) {
    return from(annotations, annotations);
  }

  /**
   * Create a new {@link MergedAnnotations} instance from the specified
   * annotations.
   *
   * @param source the source for the annotations. This source is used only
   * for information and logging. It does not need to <em>actually</em>
   * contain the specified annotations, and it will not be searched.
   * @param annotations the annotations to include
   * @return a {@link MergedAnnotations} instance containing the annotations
   * @see #from(Annotation...)
   * @see #from(AnnotatedElement)
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(Object source, Annotation... annotations) {
    return from(source, annotations, RepeatableContainers.standard());
  }

  /**
   * Create a new {@link MergedAnnotations} instance from the specified
   * annotations.
   *
   * @param source the source for the annotations. This source is used only
   * for information and logging. It does not need to <em>actually</em>
   * contain the specified annotations, and it will not be searched.
   * @param annotations the annotations to include
   * @param repeatableContainers the repeatable containers that may be used by
   * meta-annotations
   * @return a {@link MergedAnnotations} instance containing the annotations
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(Object source, Annotation[] annotations,
          RepeatableContainers repeatableContainers) {
    return from(source, annotations, repeatableContainers, AnnotationFilter.PLAIN);
  }

  /**
   * Create a new {@link MergedAnnotations} instance from the specified
   * annotations.
   *
   * @param source the source for the annotations. This source is used only
   * for information and logging. It does not need to <em>actually</em>
   * contain the specified annotations, and it will not be searched.
   * @param annotations the annotations to include
   * @param repeatableContainers the repeatable containers that may be used by
   * meta-annotations
   * @param annotationFilter an annotation filter used to restrict the
   * annotations considered
   * @return a {@link MergedAnnotations} instance containing the annotations
   * @see #search(SearchStrategy)
   */
  static MergedAnnotations from(Object source, Annotation[] annotations,
          RepeatableContainers repeatableContainers, AnnotationFilter annotationFilter) {
    Assert.notNull(annotationFilter, "AnnotationFilter is required");
    Assert.notNull(repeatableContainers, "RepeatableContainers is required");
    return TypeMappedAnnotations.from(source, annotations, repeatableContainers, annotationFilter);
  }

  /**
   * Create a new {@link MergedAnnotations} instance from the specified
   * collection of directly present annotations. This method allows a
   * {@link MergedAnnotations} instance to be created from annotations that
   * are not necessarily loaded using reflection. The provided annotations
   * must all be {@link MergedAnnotation#isDirectlyPresent() directly present}
   * and must have an {@link MergedAnnotation#getAggregateIndex() aggregate
   * index} of {@code 0}.
   * <p>The resulting {@link MergedAnnotations} instance will contain both the
   * specified annotations, and any meta-annotations that can be read using
   * reflection.
   *
   * @param annotations the annotations to include
   * @return a {@link MergedAnnotations} instance containing the annotations
   * @see MergedAnnotation#valueOf(ClassLoader, Object, Class, java.util.Map)
   * @see TypeMappedAnnotations#NONE
   */
  static MergedAnnotations valueOf(@Nullable Collection<MergedAnnotation<?>> annotations) {
    return MergedAnnotationsCollection.valueOf(annotations);
  }

  /**
   * Find merged annotations using the supplied {@link SearchStrategy} and a
   * fluent API for configuring and performing the search.
   * <p>See {@link Search} for details.
   *
   * @param searchStrategy the search strategy to use
   * @return a {@code Search} instance to perform the search
   */
  static Search search(SearchStrategy searchStrategy) {
    Assert.notNull(searchStrategy, "SearchStrategy is required");
    return new Search(searchStrategy);
  }

  /**
   * Fluent API for configuring the search algorithm used in the
   * {@link MergedAnnotations} model and performing a search.
   *
   * <ul>
   * <li>Configuration starts with an invocation of
   * {@link MergedAnnotations#search(SearchStrategy)}, specifying which
   * {@link SearchStrategy} to use.</li>
   * <li>Optional configuration can be provided via one of the {@code with*()}
   * methods.</li>
   * <li>The actual search is performed by invoking {@link #from(AnnotatedElement)}
   * with the source element from which the search should begin.</li>
   * </ul>
   *
   * <p>For example, the following performs a search on {@code MyClass} within
   * the entire type hierarchy of that class while ignoring repeatable annotations.
   *
   * <pre>{@code
   * MergedAnnotations mergedAnnotations =
   *     MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
   *         .withRepeatableContainers(RepeatableContainers.none())
   *         .from(MyClass.class);
   * }</pre>
   *
   * <p>If you wish to reuse search configuration to perform the same type of search
   * on multiple elements, you can save the {@code Search} instance as demonstrated
   * in the following example.
   *
   * <pre>{@code
   * Search search = MergedAnnotations.search(SearchStrategy.TYPE_HIERARCHY)
   *      .withRepeatableContainers(RepeatableContainers.none());
   *
   * MergedAnnotations mergedAnnotations = search.from(MyClass.class);
   * // do something with the MergedAnnotations for MyClass
   * mergedAnnotations = search.from(AnotherClass.class);
   * // do something with the MergedAnnotations for AnotherClass
   * }</pre>
   */
  final class Search {

    static final Predicate<Class<?>> always = clazz -> true;

    static final Predicate<Class<?>> never = clazz -> false;

    private final SearchStrategy searchStrategy;

    private Predicate<Class<?>> searchEnclosingClass = never;

    private RepeatableContainers repeatableContainers = RepeatableContainers.standard();

    private AnnotationFilter annotationFilter = AnnotationFilter.PLAIN;

    private Search(SearchStrategy searchStrategy) {
      this.searchStrategy = searchStrategy;
    }

    /**
     * Configure whether the search algorithm should search on
     * {@linkplain Class#getEnclosingClass() enclosing classes}.
     * <p>This feature is disabled by default and is only supported when using
     * {@link SearchStrategy#TYPE_HIERARCHY}.
     * <p>Enclosing classes will be recursively searched if the supplied
     * {@link Predicate} evaluates to {@code true}. Typically, the predicate
     * will be used to differentiate between <em>inner classes</em> and
     * {@code static} nested classes.
     * <ul>
     * <li>To limit the enclosing class search to inner classes, provide
     * {@link cn.taketoday.util.ClassUtils#isInnerClass(Class) ClassUtils::isInnerClass}
     * as the predicate.</li>
     * <li>To limit the enclosing class search to static nested classes, provide
     * {@link cn.taketoday.util.ClassUtils#isStaticClass(Class) ClassUtils::isStaticClass}
     * as the predicate.</li>
     * <li>To force the algorithm to always search enclosing classes, provide
     * {@code clazz -> true} as the predicate.</li>
     * <li>For any other use case, provide a custom predicate.</li>
     * </ul>
     * <p><strong>WARNING:</strong> if the supplied predicate always evaluates
     * to {@code true}, the algorithm will search recursively for annotations
     * on an enclosing class for any source type, regardless whether the source
     * type is an <em>inner class</em>, a {@code static} nested class, or a
     * nested interface. Thus, it may find more annotations than you would expect.
     *
     * @param searchEnclosingClass a predicate which evaluates to {@code true}
     * if a search should be performed on the enclosing class of the class
     * supplied to the predicate
     * @return this {@code Search} instance for chained method invocations
     * @see SearchStrategy#TYPE_HIERARCHY
     * @see #withRepeatableContainers(RepeatableContainers)
     * @see #withAnnotationFilter(AnnotationFilter)
     * @see #from(AnnotatedElement)
     */
    public Search withEnclosingClasses(Predicate<Class<?>> searchEnclosingClass) {
      Assert.notNull(searchEnclosingClass, "Predicate is required");
      Assert.state(this.searchStrategy == SearchStrategy.TYPE_HIERARCHY,
              "A custom 'searchEnclosingClass' predicate can only be combined with SearchStrategy.TYPE_HIERARCHY");
      this.searchEnclosingClass = searchEnclosingClass;
      return this;
    }

    /**
     * Configure the {@link RepeatableContainers} to use.
     * <p>Defaults to {@link RepeatableContainers#standard()}.
     *
     * @param repeatableContainers the repeatable containers that may be used
     * by annotations or meta-annotations
     * @return this {@code Search} instance for chained method invocations
     * @see #withAnnotationFilter(AnnotationFilter)
     * @see #from(AnnotatedElement)
     */
    public Search withRepeatableContainers(RepeatableContainers repeatableContainers) {
      Assert.notNull(repeatableContainers, "RepeatableContainers is required");
      this.repeatableContainers = repeatableContainers;
      return this;
    }

    /**
     * Configure the {@link AnnotationFilter} to use.
     * <p>Defaults to {@link AnnotationFilter#PLAIN}.
     *
     * @param annotationFilter an annotation filter used to restrict the
     * annotations considered
     * @return this {@code Search} instance for chained method invocations
     * @see #withRepeatableContainers(RepeatableContainers)
     * @see #from(AnnotatedElement)
     */
    public Search withAnnotationFilter(AnnotationFilter annotationFilter) {
      Assert.notNull(annotationFilter, "AnnotationFilter is required");
      this.annotationFilter = annotationFilter;
      return this;
    }

    /**
     * Perform a search for merged annotations beginning with the supplied
     * {@link AnnotatedElement} (such as a {@link Class} or {@link Method}),
     * using the configuration in this {@code Search} instance.
     *
     * @param element the source element
     * @return a new {@link MergedAnnotations} instance containing all
     * annotations and meta-annotations from the specified element and,
     * depending on the {@link SearchStrategy}, related inherited elements
     * @see #withEnclosingClasses(Predicate)
     * @see #withRepeatableContainers(RepeatableContainers)
     * @see #withAnnotationFilter(AnnotationFilter)
     * @see MergedAnnotations#from(AnnotatedElement, SearchStrategy, RepeatableContainers, AnnotationFilter)
     */
    public MergedAnnotations from(AnnotatedElement element) {
      return MergedAnnotations.from(element, this.searchStrategy, this.searchEnclosingClass,
              this.repeatableContainers, this.annotationFilter);
    }

  }

  /**
   * Search strategies supported by {@link MergedAnnotations#search(SearchStrategy)}
   * as well as {@link MergedAnnotations#from(AnnotatedElement, SearchStrategy)}
   * and variants of that method.
   *
   * <p>Each strategy creates a different set of aggregates that will be
   * combined to create the final {@link MergedAnnotations}.
   */
  enum SearchStrategy {

    /**
     * Find only directly declared annotations, without considering
     * {@link Inherited @Inherited} annotations and without searching
     * superclasses or implemented interfaces.
     */
    DIRECT,

    /**
     * Find all directly declared annotations as well as any
     * {@link Inherited @Inherited} superclass annotations.
     * <p>This strategy is only really useful when used with {@link Class}
     * types since the {@link Inherited @Inherited} annotation is ignored for
     * all other {@linkplain AnnotatedElement annotated elements}.
     * <p>This strategy does not search implemented interfaces.
     */
    INHERITED_ANNOTATIONS,

    /**
     * Find all directly declared and superclass annotations.
     * <p>This strategy is similar to {@link #INHERITED_ANNOTATIONS} except
     * the annotations do not need to be meta-annotated with
     * {@link Inherited @Inherited}.
     * <p>This strategy does not search implemented interfaces.
     */
    SUPERCLASS,

    /**
     * Perform a full search of the entire type hierarchy, including
     * superclasses and implemented interfaces.
     * <p>When combined with {@link Search#withEnclosingClasses(Predicate)},
     * {@linkplain Class#getEnclosingClass() enclosing classes} will also be
     * recursively searched if the supplied {@link Predicate} evaluates to
     * {@code true}.
     * <p>Superclass and enclosing class annotations do not need to be
     * meta-annotated with {@link Inherited @Inherited}.
     */
    TYPE_HIERARCHY

  }

}
