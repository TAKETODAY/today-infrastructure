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

package infra.context.aot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import infra.aot.generate.GeneratedClass;
import infra.aot.generate.GeneratedMethods;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.ContextAnnotationAutowireCandidateResolver;
import infra.context.support.GenericApplicationContext;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.TypeName;
import infra.javapoet.TypeSpec;
import infra.lang.Nullable;

/**
 * Internal code generator to create the {@link ApplicationContextInitializer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ApplicationContextInitializationCodeGenerator implements BeanFactoryInitializationCode {

  private static final String INITIALIZE_METHOD = "initialize";

  private static final String APPLICATION_CONTEXT_VARIABLE = "applicationContext";

  private final GenericApplicationContext applicationContext;

  public final GeneratedClass generatedClass;

  private final List<MethodReference> initializers = new ArrayList<>();

  ApplicationContextInitializationCodeGenerator(GenericApplicationContext applicationContext, GenerationContext generationContext) {
    this.applicationContext = applicationContext;
    this.generatedClass = generationContext.getGeneratedClasses()
            .addForFeature("ApplicationContextInitializer", this::generateType);
    this.generatedClass.reserveMethodNames(INITIALIZE_METHOD);
  }

  private void generateType(TypeSpec.Builder type) {
    type.addJavadoc("{@link $T} to restore an application context based on previous AOT processing.",
            ApplicationContextInitializer.class);
    type.addModifiers(Modifier.PUBLIC);
    type.addSuperinterface(ApplicationContextInitializer.class);
    type.addMethod(generateInitializeMethod());
  }

  private MethodSpec generateInitializeMethod() {
    MethodSpec.Builder method = MethodSpec.methodBuilder(INITIALIZE_METHOD);
    method.addAnnotation(Override.class);
    method.addModifiers(Modifier.PUBLIC);
    method.addParameter(ConfigurableApplicationContext.class, APPLICATION_CONTEXT_VARIABLE);
    method.addCode(generateInitializeCode());
    return method.build();
  }

  private CodeBlock generateInitializeCode() {
    CodeBlock.Builder code = CodeBlock.builder();
    code.addStatement("$T $L = $L.unwrapFactory(StandardBeanFactory.class)",
            StandardBeanFactory.class, BEAN_FACTORY_VARIABLE, APPLICATION_CONTEXT_VARIABLE);
    code.addStatement("$L.setAutowireCandidateResolver(new $T())", BEAN_FACTORY_VARIABLE, ContextAnnotationAutowireCandidateResolver.class);
    code.addStatement("$L.setDependencyComparator($T.INSTANCE)", BEAN_FACTORY_VARIABLE, AnnotationAwareOrderComparator.class);
    code.add(generateActiveProfilesInitializeCode());
    ArgumentCodeGenerator argCodeGenerator = createInitializerMethodArgumentCodeGenerator();
    for (MethodReference initializer : this.initializers) {
      code.addStatement(initializer.toInvokeCodeBlock(argCodeGenerator, this.generatedClass.getName()));
    }
    return code.build();
  }

  private CodeBlock generateActiveProfilesInitializeCode() {
    CodeBlock.Builder code = CodeBlock.builder();
    ConfigurableEnvironment environment = this.applicationContext.getEnvironment();
    if (!Arrays.equals(environment.getActiveProfiles(), environment.getDefaultProfiles())) {
      for (String activeProfile : environment.getActiveProfiles()) {
        code.addStatement("$L.getEnvironment().addActiveProfile($S)", APPLICATION_CONTEXT_VARIABLE, activeProfile);
      }
    }
    return code.build();
  }

  static ArgumentCodeGenerator createInitializerMethodArgumentCodeGenerator() {
    return ArgumentCodeGenerator.from(new InitializerMethodArgumentCodeGenerator());
  }

  @Override
  public GeneratedMethods getMethods() {
    return this.generatedClass.getMethods();
  }

  @Override
  public void addInitializer(MethodReference methodReference) {
    this.initializers.add(methodReference);
  }

  private static final class InitializerMethodArgumentCodeGenerator implements Function<TypeName, CodeBlock> {

    @Override
    @Nullable
    public CodeBlock apply(TypeName typeName) {
      return typeName instanceof ClassName className ? apply(className) : null;
    }

    @Nullable
    private CodeBlock apply(ClassName className) {
      String name = className.canonicalName();
      if (name.equals(StandardBeanFactory.class.getName())
              || name.equals(ConfigurableBeanFactory.class.getName())) {
        return CodeBlock.of(BEAN_FACTORY_VARIABLE);
      }
      else if (name.equals(ConfigurableEnvironment.class.getName())
              || name.equals(Environment.class.getName())) {
        return CodeBlock.of("$L.getEnvironment()", APPLICATION_CONTEXT_VARIABLE);
      }
      else if (name.equals(ResourceLoader.class.getName())) {
        return CodeBlock.of(APPLICATION_CONTEXT_VARIABLE);
      }
      return null;
    }
  }

}
