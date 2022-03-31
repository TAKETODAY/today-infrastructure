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
package cn.taketoday.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.lang.Component;

/**
 * Indicates that a bean should be given preference when multiple candidates
 * are qualified to autowire a single-valued dependency. If exactly one
 * 'primary' bean exists among the candidates, it will be the autowired value.
 *
 * <p>This annotation is semantically equivalent to the {@code <bean>} element's
 * {@code primary} attribute in Spring XML.
 *
 * <p>May be used on any class directly or indirectly annotated with
 * {@code @Component} or on methods annotated with @{@link Bean}.
 *
 * <h2>Example</h2>
 * <pre class="code">
 * &#064;Component
 * public class FooService {
 *
 *     private FooRepository fooRepository;
 *
 *     &#064;Autowired
 *     public FooService(FooRepository fooRepository) {
 *         this.fooRepository = fooRepository;
 *     }
 * }
 *
 * &#064;Component
 * public class JdbcFooRepository extends FooRepository {
 *
 *     public JdbcFooRepository(DataSource dataSource) {
 *         // ...
 *     }
 * }
 *
 * &#064;Primary
 * &#064;Component
 * public class HibernateFooRepository extends FooRepository {
 *
 *     public HibernateFooRepository(SessionFactory sessionFactory) {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>Because {@code HibernateFooRepository} is marked with {@code @Primary},
 * it will be injected preferentially over the jdbc-based variant assuming both
 * are present as beans within the same Spring application context, which is
 * often the case when component-scanning is applied liberally.
 *
 * <p>Note that using {@code @Primary} at the class level has no effect unless
 * component-scanning is being used. If a {@code @Primary}-annotated class is
 * declared via XML, {@code @Primary} annotation metadata is ignored, and
 * {@code <bean primary="true|false"/>} is respected instead.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author TODAY<br>
 * 2019-08-31 13:50
 * @see Lazy
 * @see Bean
 * @see ComponentScan
 * @see Component
 * @since 3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Primary {

}
