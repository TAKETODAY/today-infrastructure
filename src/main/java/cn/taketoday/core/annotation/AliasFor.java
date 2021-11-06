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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.lang.Constant;

/**
 * {@code @AliasFor} is an annotation that is used to declare aliases for
 * annotation attributes.
 *
 * <h3>Usage Scenarios</h3>
 * <ul>
 * <li><strong>Explicit aliases within an annotation</strong>: within a single
 * annotation, {@code @AliasFor} can be declared on a pair of attributes to
 * signal that they are interchangeable aliases for each other.</li>
 * <li><strong>Explicit alias for attribute in meta-annotation</strong>: if the
 * {@link #annotation} attribute of {@code @AliasFor} is set to a different
 * annotation than the one that declares it, the {@link #attribute} is
 * interpreted as an alias for an attribute in a meta-annotation (i.e., an
 * explicit meta-annotation attribute override). This enables fine-grained
 * control over exactly which attributes are overridden within an annotation
 * hierarchy. In fact, with {@code @AliasFor} it is even possible to declare
 * an alias for the {@code value} attribute of a meta-annotation.</li>
 * <li><strong>Implicit aliases within an annotation</strong>: if one or
 * more attributes within an annotation are declared as attribute overrides
 * for the same meta-annotation attribute (either directly or transitively),
 * those attributes will be treated as a set of <em>implicit</em> aliases
 * for each other, resulting in behavior analogous to that for explicit
 * aliases within an annotation.</li>
 * </ul>
 *
 * <h3>Usage Requirements</h3>
 * <p>Like with any annotation in Java, the mere presence of {@code @AliasFor}
 * on its own will not enforce alias semantics. For alias semantics to be
 * enforced, annotations must be <em>loaded</em> via {@link MergedAnnotations}.
 *
 * <h3>Implementation Requirements</h3>
 * <ul>
 * <li><strong>Explicit aliases within an annotation</strong>:
 * <ol>
 * <li>Each attribute that makes up an aliased pair should be annotated with
 * {@code @AliasFor}, and either {@link #attribute} or {@link #value} must
 * reference the <em>other</em> attribute in the pair. it is technically
 * possible to annotate only one of the attributes in an
 * aliased pair; however, it is recommended to annotate both attributes in an
 * aliased pair for better documentation.</li>
 * <li>Aliased attributes must declare the same return type.</li>
 * <li>Aliased attributes must declare a default value.</li>
 * <li>Aliased attributes must declare the same default value.</li>
 * <li>{@link #annotation} should not be declared.</li>
 * </ol>
 * </li>
 * <li><strong>Explicit alias for attribute in meta-annotation</strong>:
 * <ol>
 * <li>The attribute that is an alias for an attribute in a meta-annotation
 * must be annotated with {@code @AliasFor}, and {@link #attribute} must
 * reference the attribute in the meta-annotation.</li>
 * <li>Aliased attributes must declare the same return type.</li>
 * <li>{@link #annotation} must reference the meta-annotation.</li>
 * <li>The referenced meta-annotation must be <em>meta-present</em> on the
 * annotation class that declares {@code @AliasFor}.</li>
 * </ol>
 * </li>
 * <li><strong>Implicit aliases within an annotation</strong>:
 * <ol>
 * <li>Each attribute that belongs to a set of implicit aliases must be
 * annotated with {@code @AliasFor}, and {@link #attribute} must reference
 * the same attribute in the same meta-annotation (either directly or
 * transitively via other explicit meta-annotation attribute overrides
 * within the annotation hierarchy).</li>
 * <li>Aliased attributes must declare the same return type.</li>
 * <li>Aliased attributes must declare a default value.</li>
 * <li>Aliased attributes must declare the same default value.</li>
 * <li>{@link #annotation} must reference an appropriate meta-annotation.</li>
 * <li>The referenced meta-annotation must be <em>meta-present</em> on the
 * annotation class that declares {@code @AliasFor}.</li>
 * </ol>
 * </li>
 * </ul>
 *
 * <h3>Example: Explicit Aliases within an Annotation</h3>
 * <p>In {@code @ContextConfiguration}, {@code value} and {@code locations}
 * are explicit aliases for each other.
 *
 * <pre class="code"> public &#064;interface ContextConfiguration {
 *
 *    &#064;AliasFor("locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor("value")
 *    String[] locations() default {};
 *
 *    // ...
 * }</pre>
 *
 * <h3>Example: Explicit Alias for Attribute in Meta-annotation</h3>
 * <p>In {@code @XmlTestConfig}, {@code xmlFiles} is an explicit alias for
 * {@code locations} in {@code @ContextConfiguration}. In other words,
 * {@code xmlFiles} overrides the {@code locations} attribute in
 * {@code @ContextConfiguration}.
 *
 * <pre class="code"> &#064;ContextConfiguration
 * public &#064;interface XmlTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles();
 * }</pre>
 *
 * <h3>Example: Implicit Aliases within an Annotation</h3>
 * <p>In {@code @MyTestConfig}, {@code value}, {@code groovyScripts}, and
 * {@code xmlFiles} are all explicit meta-annotation attribute overrides for
 * the {@code locations} attribute in {@code @ContextConfiguration}. These
 * three attributes are therefore also implicit aliases for each other.
 *
 * <pre class="code"> &#064;ContextConfiguration
 * public &#064;interface MyTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] groovyScripts() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles() default {};
 * }</pre>
 *
 * <h3>Example: Transitive Implicit Aliases within an Annotation</h3>
 * <p>In {@code @GroovyOrXmlTestConfig}, {@code groovy} is an explicit
 * override for the {@code groovyScripts} attribute in {@code @MyTestConfig};
 * whereas, {@code xml} is an explicit override for the {@code locations}
 * attribute in {@code @ContextConfiguration}. Furthermore, {@code groovy}
 * and {@code xml} are transitive implicit aliases for each other, since they
 * both effectively override the {@code locations} attribute in
 * {@code @ContextConfiguration}.
 *
 * <pre class="code"> &#064;MyTestConfig
 * public &#064;interface GroovyOrXmlTestConfig {
 *
 *    &#064;AliasFor(annotation = MyTestConfig.class, attribute = "groovyScripts")
 *    String[] groovy() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xml() default {};
 * }</pre>
 *
 * <h3>Annotations Supporting Attribute Aliases</h3>
 * <p>several annotations within core have been updated to use {@code @AliasFor}
 * to configure their internal attribute aliases. Consult the Javadoc for
 * individual annotations as well as the reference manual for details.
 *
 * @author Sam Brannen
 * @author TODAY
 * @see MergedAnnotations
 * @see SynthesizedAnnotation
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AliasFor {

  /**
   * Alias for {@link #attribute}.
   * <p>Intended to be used instead of {@link #attribute} when {@link #annotation}
   * is not declared &mdash; for example: {@code @AliasFor("value")} instead of
   * {@code @AliasFor(attribute = "value")}.
   */
  @AliasFor("attribute")
  String value() default Constant.BLANK;

  /**
   * The name of the attribute that <em>this</em> attribute is an alias for.
   *
   * @see #value
   */
  @AliasFor("value")
  String attribute() default Constant.BLANK;

  /**
   * The type of annotation in which the aliased {@link #attribute} is declared.
   * <p>Defaults to {@link Annotation}, implying that the aliased attribute is
   * declared in the same annotation as <em>this</em> attribute.
   */
  Class<? extends Annotation> annotation() default Annotation.class;

}
