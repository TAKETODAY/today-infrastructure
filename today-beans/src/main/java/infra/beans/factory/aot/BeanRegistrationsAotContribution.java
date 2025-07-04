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

package infra.beans.factory.aot;

import java.util.List;
import java.util.function.BiConsumer;

import javax.lang.model.element.Modifier;

import infra.aot.generate.GeneratedClass;
import infra.aot.generate.GeneratedMethod;
import infra.aot.generate.GeneratedMethods;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeHint;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.StandardBeanFactory;
import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;
import infra.javapoet.CodeBlock.Builder;
import infra.javapoet.MethodSpec;

/**
 * AOT contribution from a {@link BeanRegistrationsAotProcessor} used to
 * register bean definitions and aliases.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanRegistrationsAotProcessor
 * @since 4.0
 */
class BeanRegistrationsAotContribution implements BeanFactoryInitializationAotContribution {

  private static final String BEAN_FACTORY_PARAMETER_NAME = "beanFactory";

  private static final int MAX_REGISTRATIONS_PER_FILE = 5000;

  private static final int MAX_REGISTRATIONS_PER_METHOD = 1000;

  private static final ArgumentCodeGenerator argumentCodeGenerator = ArgumentCodeGenerator
          .of(StandardBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);

  private final List<Registration> registrations;

  BeanRegistrationsAotContribution(List<Registration> registrations) {
    this.registrations = registrations;
  }

  @Override
  public void applyTo(GenerationContext generationContext,
          BeanFactoryInitializationCode beanFactoryInitializationCode) {

    GeneratedClass generatedClass = createBeanFactoryRegistrationClass(generationContext);
    BeanRegistrationsCodeGenerator codeGenerator = new BeanRegistrationsCodeGenerator(generatedClass);
    GeneratedMethod generatedBeanDefinitionsMethod = generateBeanRegistrationCode(generationContext,
            generatedClass, codeGenerator);
    beanFactoryInitializationCode.addInitializer(generatedBeanDefinitionsMethod.toMethodReference());
    GeneratedMethod generatedAliasesMethod = codeGenerator.getMethods().add("registerAliases",
            this::generateRegisterAliasesMethod);
    beanFactoryInitializationCode.addInitializer(generatedAliasesMethod.toMethodReference());
    generateRegisterHints(generationContext.getRuntimeHints(), this.registrations);
  }

  private GeneratedMethod generateBeanRegistrationCode(GenerationContext generationContext, GeneratedClass mainGeneratedClass, BeanRegistrationsCodeGenerator mainCodeGenerator) {
    if (this.registrations.size() < MAX_REGISTRATIONS_PER_FILE) {
      return generateBeanRegistrationClass(generationContext, mainCodeGenerator, 0, this.registrations.size());
    }
    else {
      return mainGeneratedClass.getMethods().add("registerBeanDefinitions", method -> {
        method.addJavadoc("Register the bean definitions.");
        method.addModifiers(Modifier.PUBLIC);
        method.addParameter(StandardBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
        CodeBlock.Builder body = CodeBlock.builder();
        Registration.doWithSlice(this.registrations, MAX_REGISTRATIONS_PER_FILE, (start, end) -> {
          GeneratedClass sliceGeneratedClass = createBeanFactoryRegistrationClass(generationContext);
          BeanRegistrationsCodeGenerator sliceCodeGenerator = new BeanRegistrationsCodeGenerator(sliceGeneratedClass);
          GeneratedMethod generatedMethod = generateBeanRegistrationClass(generationContext, sliceCodeGenerator, start, end);
          body.addStatement(generatedMethod.toMethodReference().toInvokeCodeBlock(argumentCodeGenerator));
        });
        method.addCode(body.build());
      });
    }
  }

  private GeneratedMethod generateBeanRegistrationClass(GenerationContext generationContext,
          BeanRegistrationsCodeGenerator codeGenerator, int start, int end) {

    return codeGenerator.getMethods().add("registerBeanDefinitions", method -> {
      method.addJavadoc("Register the bean definitions.");
      method.addModifiers(Modifier.PUBLIC);
      method.addParameter(StandardBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
      List<Registration> sliceRegistrations = this.registrations.subList(start, end);
      new BeanDefinitionsRegistrationGenerator(
              generationContext, codeGenerator, sliceRegistrations, start).generateBeanRegistrationsCode(method);
    });
  }

  private static GeneratedClass createBeanFactoryRegistrationClass(GenerationContext generationContext) {
    return generationContext.getGeneratedClasses()
            .addForFeature("BeanFactoryRegistrations", type -> {
              type.addJavadoc("Register bean definitions for the bean factory.");
              type.addModifiers(Modifier.PUBLIC);
            });
  }

  private void generateRegisterAliasesMethod(MethodSpec.Builder method) {
    method.addJavadoc("Register the aliases.");
    method.addModifiers(Modifier.PUBLIC);
    method.addParameter(StandardBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
    var code = CodeBlock.builder();
    this.registrations.forEach(registration -> {
      for (String alias : registration.aliases()) {
        code.addStatement("$L.registerAlias($S, $S)", BEAN_FACTORY_PARAMETER_NAME,
                registration.beanName(), alias);
      }
    });
    method.addCode(code.build());
  }

  private void generateRegisterHints(RuntimeHints runtimeHints, List<Registration> registrations) {
    registrations.forEach(registration -> {
      ReflectionHints hints = runtimeHints.reflection();
      Class<?> beanClass = registration.registeredBean.getBeanClass();
      hints.registerType(beanClass);
      hints.registerForInterfaces(beanClass, TypeHint.Builder::withMembers);
    });
  }

  /**
   * Gather the necessary information to register a particular bean.
   *
   * @param registeredBean the bean to register
   * @param methodGenerator the {@link BeanDefinitionMethodGenerator} to use
   * @param aliases the bean aliases, if any
   */
  record Registration(RegisteredBean registeredBean, BeanDefinitionMethodGenerator methodGenerator, String[] aliases) {

    String beanName() {
      return this.registeredBean.getBeanName();
    }

    /**
     * Invoke an action for each slice of the given {@code registrations}. The
     * {@code action} is invoked for each slice with the start and end index of the
     * given list of registrations. Elements to process can be retrieved using
     * {@link List#subList(int, int)}.
     *
     * @param registrations the registrations to process
     * @param sliceSize the size of a slice
     * @param action the action to invoke for each slice
     */
    static void doWithSlice(List<Registration> registrations, int sliceSize,
            BiConsumer<Integer, Integer> action) {

      int index = 0;
      int end = 0;
      while (end < registrations.size()) {
        int start = index * sliceSize;
        end = Math.min(start + sliceSize, registrations.size());
        action.accept(start, end);
        index++;
      }
    }

  }

  /**
   * {@link BeanRegistrationsCode} with generation support.
   */
  static class BeanRegistrationsCodeGenerator implements BeanRegistrationsCode {

    private final GeneratedClass generatedClass;

    public BeanRegistrationsCodeGenerator(GeneratedClass generatedClass) {
      this.generatedClass = generatedClass;
    }

    @Override
    public ClassName getClassName() {
      return this.generatedClass.getName();
    }

    @Override
    public GeneratedMethods getMethods() {
      return this.generatedClass.getMethods();
    }

  }

  /**
   * Generate code for bean registrations. Limited to {@value #MAX_REGISTRATIONS_PER_METHOD}
   * beans per method to avoid hitting a limit.
   */
  static final class BeanDefinitionsRegistrationGenerator {

    private final GenerationContext generationContext;

    private final BeanRegistrationsCodeGenerator codeGenerator;

    private final List<Registration> registrations;

    private final int globalStart;

    BeanDefinitionsRegistrationGenerator(GenerationContext generationContext,
            BeanRegistrationsCodeGenerator codeGenerator, List<Registration> registrations, int globalStart) {

      this.generationContext = generationContext;
      this.codeGenerator = codeGenerator;
      this.registrations = registrations;
      this.globalStart = globalStart;
    }

    void generateBeanRegistrationsCode(MethodSpec.Builder method) {
      if (this.registrations.size() <= 1000) {
        generateRegisterBeanDefinitionMethods(method, this.registrations);
      }
      else {
        Builder code = CodeBlock.builder();
        code.add("// Registration is sliced to avoid exceeding size limit\n");
        Registration.doWithSlice(this.registrations, MAX_REGISTRATIONS_PER_METHOD,
                (start, end) -> {
                  GeneratedMethod sliceMethod = generateSliceMethod(start, end);
                  code.addStatement(sliceMethod.toMethodReference().toInvokeCodeBlock(
                          argumentCodeGenerator, this.codeGenerator.getClassName()));
                });
        method.addCode(code.build());
      }
    }

    private GeneratedMethod generateSliceMethod(int start, int end) {
      String description = "Register the bean definitions from %s to %s."
              .formatted(this.globalStart + start, this.globalStart + end - 1);
      List<Registration> slice = this.registrations.subList(start, end);
      return this.codeGenerator.getMethods().add("registerBeanDefinitions", method -> {
        method.addJavadoc(description);
        method.addModifiers(Modifier.PRIVATE);
        method.addParameter(StandardBeanFactory.class, BEAN_FACTORY_PARAMETER_NAME);
        generateRegisterBeanDefinitionMethods(method, slice);
      });
    }

    private void generateRegisterBeanDefinitionMethods(MethodSpec.Builder method,
            Iterable<Registration> registrations) {

      var code = CodeBlock.builder();
      registrations.forEach(registration -> {
        try {
          CodeBlock methodInvocation = generateBeanRegistration(registration);
          code.addStatement("$L.registerBeanDefinition($S, $L)",
                  BEAN_FACTORY_PARAMETER_NAME, registration.beanName(), methodInvocation);
        }
        catch (AotException ex) {
          throw ex;
        }
        catch (Exception ex) {
          throw new AotBeanProcessingException(registration.registeredBean,
                  "failed to generate code for bean definition", ex);
        }
      });
      method.addCode(code.build());
    }

    private CodeBlock generateBeanRegistration(Registration registration) {
      MethodReference beanDefinitionMethod = registration.methodGenerator
              .generateBeanDefinitionMethod(this.generationContext, this.codeGenerator);
      return beanDefinitionMethod.toInvokeCodeBlock(
              ArgumentCodeGenerator.none(), this.codeGenerator.getClassName());
    }
  }

}
