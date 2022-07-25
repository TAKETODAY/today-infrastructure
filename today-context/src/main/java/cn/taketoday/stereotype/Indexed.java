/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that the annotated element represents a stereotype for the index.
 *
 * <p>The {@code CandidateComponentsIndex} is an alternative to classpath
 * scanning that uses a metadata file generated at compilation time. The
 * index allows retrieving the candidate components (i.e. fully qualified
 * name) based on a stereotype. This annotation instructs the generator to
 * index the element on which the annotated element is present or if it
 * implements or extends from the annotated element. The stereotype is the
 * fully qualified name of the annotated element.
 *
 * <p>Consider the default {@link Component} annotation that is meta-annotated
 * with this annotation. If a component is annotated with {@link Component},
 * an entry for that component will be added to the index using the
 * {@code Component} stereotype.
 *
 * <p>This annotation is also honored on meta-annotations. Consider this
 * custom annotation:
 * <pre class="code">
 * package com.example;
 *
 * &#064;Target(ElementType.TYPE)
 * &#064;Retention(RetentionPolicy.RUNTIME)
 * &#064;Documented
 * &#064;Indexed
 * &#064;Service
 * public @interface PrivilegedService { ... }
 * </pre>
 *
 * If the above annotation is present on a type, it will be indexed with two
 * stereotypes: {@code Component} and
 * {@code com.example.PrivilegedService}. While {@link Service} isn't directly
 * annotated with {@code Indexed}, it is meta-annotated with {@link Component}.
 *
 * <p>It is also possible to index all implementations of a certain interface or
 * all the subclasses of a given class by adding {@code @Indexed} on it.
 *
 * Consider this base interface:
 * <pre class="code">
 * package com.example;
 *
 * &#064;Indexed
 * public interface AdminService { ... }
 * </pre>
 *
 * Now, consider an implementation of this {@code AdminService} somewhere:
 * <pre class="code">
 * package com.example.foo;
 *
 * import com.example.AdminService;
 *
 * public class ConfigurationAdminService implements AdminService { ... }
 * </pre>
 *
 * Because this class implements an interface that is indexed, it will be
 * automatically included with the {@code com.example.AdminService} stereotype.
 * If there are more {@code @Indexed} interfaces and/or superclasses in the
 * hierarchy, the class will map to all their stereotypes.
 *
 * @author Stephane Nicoll
 * @author TODAY 2021/10/20 16:22
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Indexed {

}
