/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.validation.beanvalidation;

import org.hibernate.validator.internal.constraintvalidators.bv.PatternValidator;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.OverridingClassLoader;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/24 17:27
 */
class BeanValidationBeanRegistrationAotProcessorTests {

  private final BeanValidationBeanRegistrationAotProcessor processor = new BeanValidationBeanRegistrationAotProcessor();

  private final GenerationContext generationContext = new TestGenerationContext();

  @Test
  void shouldSkipNonAnnotatedType() {
    process(EmptyClass.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
  }

  @Test
  void shouldProcessMethodParameterLevelConstraint() {
    process(MethodParameterLevelConstraint.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).hasSize(2);
    assertThat(RuntimeHintsPredicates.reflection().onType(MethodParameterLevelConstraint.class)
            .withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS)).accepts(this.generationContext.getRuntimeHints());
    assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessConstructorParameterLevelConstraint() {
    process(ConstructorParameterLevelConstraint.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).hasSize(2);
    assertThat(RuntimeHintsPredicates.reflection().onType(ConstructorParameterLevelConstraint.class)
            .withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS)).accepts(this.generationContext.getRuntimeHints());
    assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessPropertyLevelConstraint() {
    process(PropertyLevelConstraint.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).hasSize(2);
    assertThat(RuntimeHintsPredicates.reflection().onType(PropertyLevelConstraint.class)
            .withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS)).accepts(this.generationContext.getRuntimeHints());
    assertThat(RuntimeHintsPredicates.reflection().onType(ExistsValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessGenericTypeLevelConstraint() {
    process(GenericTypeLevelConstraint.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).hasSize(2);
    assertThat(RuntimeHintsPredicates.reflection().onType(GenericTypeLevelConstraint.class)
            .withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS)).accepts(this.generationContext.getRuntimeHints());
    assertThat(RuntimeHintsPredicates.reflection().onType(PatternValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessTransitiveGenericTypeLevelConstraint() {
    process(TransitiveGenericTypeLevelConstraint.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).hasSize(3);
    assertThat(RuntimeHintsPredicates.reflection().onType(TransitiveGenericTypeLevelConstraint.class)
            .withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS)).accepts(this.generationContext.getRuntimeHints());
    assertThat(RuntimeHintsPredicates.reflection().onType(Exclude.class)
            .withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS)).accepts(this.generationContext.getRuntimeHints());
    assertThat(RuntimeHintsPredicates.reflection().onType(PatternValidator.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(this.generationContext.getRuntimeHints());
  }

  @ParameterizedTest
  @ValueSource(classes = { BeanWithRecursiveIterable.class, BeanWithRecursiveMap.class, BeanWithRecursiveOptional.class })
  void shouldProcessRecursiveGenericsWithoutInfiniteRecursion(Class<?> beanClass) {
    process(beanClass);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).hasSize(1);
    assertThat(RuntimeHintsPredicates.reflection().onType(beanClass)
            .withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldSkipConstraintWithMissingDependency() throws Exception {
    MissingDependencyClassLoader classLoader = new MissingDependencyClassLoader(getClass().getClassLoader());
    Class<?> beanClass = classLoader.loadClass(ConstraintWithMissingDependency.class.getName());
    process(beanClass);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
  }

  private void process(Class<?> beanClass) {
    BeanRegistrationAotContribution contribution = createContribution(beanClass);
    if (contribution != null) {
      contribution.applyTo(this.generationContext, mock());
    }
  }

  @Nullable
  private BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    return this.processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
  }

  private static class EmptyClass { }

  @Constraint(validatedBy = { ExistsValidator.class })
  @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
  @Retention(RUNTIME)
  @Repeatable(Exists.List.class)
  @interface Exists {

    String message() default "Does not exist";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    @interface List {
      Exists[] value();
    }
  }

  static class ExistsValidator implements ConstraintValidator<Exists, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
      return true;
    }
  }

  static class MethodParameterLevelConstraint {

    @SuppressWarnings("unused")
    public String hello(@Exists String name) {
      return "Hello " + name;
    }

  }

  @SuppressWarnings("unused")
  static class ConstructorParameterLevelConstraint {

    private final String name;

    public ConstructorParameterLevelConstraint(@Exists String name) {
      this.name = name;
    }

    public String hello() {
      return "Hello " + this.name;
    }

  }

  @SuppressWarnings("unused")
  static class PropertyLevelConstraint {

    @Exists
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  static class Exclude {

    @Valid
    private List<@Pattern(regexp = "^([1-5][x|X]{2}|[1-5][0-9]{2})\\$") String> httpStatus;

    public List<String> getHttpStatus() {
      return httpStatus;
    }

    public void setHttpStatus(List<String> httpStatus) {
      this.httpStatus = httpStatus;
    }
  }

  static class GenericTypeLevelConstraint {

    private List<@Pattern(regexp = "^([1-5][x|X]{2}|[1-5][0-9]{2})\\$") String> httpStatus;

    public List<String> getHttpStatus() {
      return httpStatus;
    }

    public void setHttpStatus(List<String> httpStatus) {
      this.httpStatus = httpStatus;
    }
  }

  static class TransitiveGenericTypeLevelConstraint {

    private List<Exclude> exclude = new ArrayList<>();

    public List<Exclude> getExclude() {
      return exclude;
    }

    public void setExclude(List<Exclude> exclude) {
      this.exclude = exclude;
    }
  }

  static class BeanWithRecursiveIterable {
    Iterable<BeanWithRecursiveIterable> iterable;
  }

  static class BeanWithRecursiveMap {
    Map<BeanWithRecursiveMap, BeanWithRecursiveMap> map;
  }

  static class BeanWithRecursiveOptional {
    Optional<BeanWithRecursiveOptional> optional;
  }

  static class ConstraintWithMissingDependency {

    MissingType missingType;
  }

  static class MissingType { }

  static class MissingDependencyClassLoader extends OverridingClassLoader {

    MissingDependencyClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected boolean isEligibleForOverriding(String className) {
      return className.startsWith(BeanValidationBeanRegistrationAotProcessorTests.class.getName());
    }

    @Override
    protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
      if (name.contains("MissingType")) {
        throw new NoClassDefFoundError(name);
      }
      return super.loadClassForOverriding(name);
    }
  }

}