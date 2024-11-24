/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.orm.mybatis.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanNameGenerator;
import infra.context.annotation.Import;
import infra.orm.mybatis.mapper.MapperFactoryBean;
import infra.orm.mybatis.mapper.MapperScannerConfigurer;

/**
 * Use this annotation to register MyBatis mapper interfaces
 * when using Java Config. It performs when same work as
 * {@link MapperScannerConfigurer} via {@link MapperScannerRegistrar}.
 *
 * <p>
 * Either {@link #basePackageClasses} or {@link #basePackages}
 * (or its alias {@link #value}) may be specified to define
 * specific packages to scan. Since 2.0.4, If specific packages
 * are not defined, scanning will occur from the package of
 * the class that declares this annotation.
 *
 * <p>
 * Configuration example:
 * </p>
 *
 * <pre>{@code
 * @Configuration
 * @MapperScan("infra.orm.mybatis.sample.mapper")
 * public class AppConfig {
 *
 *   @Bean
 *   public DataSource dataSource() {
 *     return new EmbeddedDatabaseBuilder().addScript("schema.sql").build();
 *   }
 *
 *   @Bean
 *   public DataSourceTransactionManager transactionManager() {
 *     return new DataSourceTransactionManager(dataSource());
 *   }
 *
 *   @Bean
 *   public SqlSessionFactory sqlSessionFactory() throws Exception {
 *     SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
 *     sessionFactory.setDataSource(dataSource());
 *     return sessionFactory.getObject();
 *   }
 * }
 * }</pre>
 *
 * @author Michael Lanyon
 * @author Eduardo Macarron
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MapperScannerRegistrar
 * @see MapperFactoryBean
 * @since 4.0
 */
@Documented
@Target(ElementType.TYPE)
@Repeatable(MapperScans.class)
@Retention(RetentionPolicy.RUNTIME)
@Import(MapperScannerRegistrar.class)
public @interface MapperScan {

  /**
   * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
   * {@code @MapperScan("org.my.pkg")} instead of {@code @MapperScan(basePackages = "org.my.pkg"})}.
   *
   * @return base package names
   */
  String[] value() default {};

  /**
   * Base packages to scan for MyBatis interfaces. Note that only interfaces with at least one method will be
   * registered; concrete classes will be ignored.
   *
   * @return base package names for scanning mapper interface
   */
  String[] basePackages() default {};

  /**
   * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
   * package of each class specified will be scanned.
   * <p>
   * Consider creating a special no-op marker class or interface in each package that serves no purpose other than being
   * referenced by this attribute.
   *
   * @return classes that indicate base package for scanning mapper interface
   */
  Class<?>[] basePackageClasses() default {};

  /**
   * The {@link BeanNameGenerator} class to be used for naming detected components within the Framework container.
   *
   * @return the class of {@link BeanNameGenerator}
   */
  Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

  /**
   * This property specifies the annotation that the scanner will search for.
   * <p>
   * The scanner will register all interfaces in the base package that also have the specified annotation.
   * <p>
   * Note this can be combined with markerInterface.
   *
   * @return the annotation that the scanner will search for
   */
  Class<? extends Annotation> annotationClass() default Annotation.class;

  /**
   * This property specifies the parent that the scanner will search for.
   * <p>
   * The scanner will register all interfaces in the base package that also have the specified interface class as a
   * parent.
   * <p>
   * Note this can be combined with annotationClass.
   *
   * @return the parent that the scanner will search for
   */
  Class<?> markerInterface() default Class.class;

  /**
   * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the Framework context.
   * Usually this is only needed when you have more than one datasource.
   *
   * @return the bean name of {@code SqlSessionTemplate}
   */
  String sqlSessionTemplateRef() default "";

  /**
   * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the Framework context.
   * Usually this is only needed when you have more than one datasource.
   *
   * @return the bean name of {@code SqlSessionFactory}
   */
  String sqlSessionFactoryRef() default "";

  /**
   * Specifies a custom MapperFactoryBean to return a mybatis proxy as Framework bean.
   *
   * @return the class of {@code MapperFactoryBean}
   */
  Class<? extends MapperFactoryBean> factoryBean() default MapperFactoryBean.class;

  /**
   * Whether enable lazy initialization of mapper bean.
   *
   * <p>
   * Default is {@code false}.
   * </p>
   *
   * @return set {@code true} to enable lazy initialization
   */
  String lazyInitialization() default "";

  /**
   * Specifies the default scope of scanned mappers.
   *
   * <p>
   * Default is {@code ""} (equiv to singleton).
   * </p>
   *
   * @return the default scope
   */
  String defaultScope() default AbstractBeanDefinition.SCOPE_DEFAULT;

}
