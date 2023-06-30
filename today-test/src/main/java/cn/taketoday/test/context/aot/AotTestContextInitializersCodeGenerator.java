/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.aot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedClass;
import cn.taketoday.aot.generate.GeneratedClasses;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.javapoet.TypeName;
import cn.taketoday.javapoet.TypeSpec;
import cn.taketoday.javapoet.WildcardTypeName;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.MultiValueMap;

/**
 * Internal code generator for mappings used by {@link AotTestContextInitializers}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AotTestContextInitializersCodeGenerator {

  private static final Logger logger = LoggerFactory.getLogger(AotTestContextInitializersCodeGenerator.class);

  // ApplicationContextInitializer
  private static final ClassName CONTEXT_INITIALIZER = ClassName.get(ApplicationContextInitializer.class);

  // Supplier<ApplicationContextInitializer>
  private static final ParameterizedTypeName CONTEXT_INITIALIZER_SUPPLIER = ParameterizedTypeName
          .get(ClassName.get(Supplier.class), CONTEXT_INITIALIZER);

  // Map<String, Supplier<ApplicationContextInitializer>>
  private static final TypeName CONTEXT_INITIALIZER_SUPPLIER_MAP = ParameterizedTypeName
          .get(ClassName.get(Map.class), ClassName.get(String.class), CONTEXT_INITIALIZER_SUPPLIER);

  // Class<ApplicationContextInitializer>
  private static final ParameterizedTypeName CONTEXT_INITIALIZER_CLASS = ParameterizedTypeName.get(
          ClassName.get(Class.class), WildcardTypeName.subtypeOf(ClassName.get(ApplicationContextInitializer.class)));

  // Map<String, Class<ApplicationContextInitializer>>
  private static final TypeName CONTEXT_INITIALIZER_CLASS_MAP = ParameterizedTypeName
          .get(ClassName.get(Map.class), ClassName.get(String.class), CONTEXT_INITIALIZER_CLASS);

  private static final String GENERATED_SUFFIX = "Generated";

  // TODO Consider an alternative means for specifying the name of the generated class.
  // Ideally we would generate a class named: cn.taketoday.test.context.aot.GeneratedAotTestContextInitializers
  static final String GENERATED_MAPPINGS_CLASS_NAME = AotTestContextInitializers.class.getName() + "__" + GENERATED_SUFFIX;

  static final String GET_CONTEXT_INITIALIZERS_METHOD_NAME = "getContextInitializers";

  static final String GET_CONTEXT_INITIALIZER_CLASSES_METHOD_NAME = "getContextInitializerClasses";

  private final MultiValueMap<ClassName, Class<?>> initializerClassMappings;

  private final GeneratedClass generatedClass;

  AotTestContextInitializersCodeGenerator(
          MultiValueMap<ClassName, Class<?>> initializerClassMappings, GeneratedClasses generatedClasses) {

    this.initializerClassMappings = initializerClassMappings;
    this.generatedClass = generatedClasses.addForFeature(GENERATED_SUFFIX, this::generateType);
  }

  GeneratedClass getGeneratedClass() {
    return this.generatedClass;
  }

  private void generateType(TypeSpec.Builder type) {
    logger.debug("Generating AOT test mappings in {}", generatedClass.getName().reflectionName());
    type.addJavadoc("Generated mappings for {@link $T}.", AotTestContextInitializers.class);
    type.addModifiers(Modifier.PUBLIC);
    type.addMethod(contextInitializersMappingMethod());
    type.addMethod(contextInitializerClassesMappingMethod());
  }

  private MethodSpec contextInitializersMappingMethod() {
    MethodSpec.Builder method = MethodSpec.methodBuilder(GET_CONTEXT_INITIALIZERS_METHOD_NAME);
    method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    method.returns(CONTEXT_INITIALIZER_SUPPLIER_MAP);
    method.addCode(generateContextInitializersMappingCode());
    return method.build();
  }

  private CodeBlock generateContextInitializersMappingCode() {
    CodeBlock.Builder code = CodeBlock.builder();
    code.addStatement("$T map = new $T<>()", CONTEXT_INITIALIZER_SUPPLIER_MAP, HashMap.class);
    this.initializerClassMappings.forEach((className, testClasses) -> {
      List<String> testClassNames = testClasses.stream().map(Class::getName).toList();
      logger.trace("Generating mapping from AOT context initializer supplier [{}] to test classes {}",
              className.reflectionName(), testClassNames);
      testClassNames.forEach(testClassName ->
              code.addStatement("map.put($S, () -> new $T())", testClassName, className));
    });
    code.addStatement("return map");
    return code.build();
  }

  private MethodSpec contextInitializerClassesMappingMethod() {
    MethodSpec.Builder method = MethodSpec.methodBuilder(GET_CONTEXT_INITIALIZER_CLASSES_METHOD_NAME);
    method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    method.returns(CONTEXT_INITIALIZER_CLASS_MAP);
    method.addCode(generateContextInitializerClassesMappingCode());
    return method.build();
  }

  private CodeBlock generateContextInitializerClassesMappingCode() {
    CodeBlock.Builder code = CodeBlock.builder();
    code.addStatement("$T map = new $T<>()", CONTEXT_INITIALIZER_CLASS_MAP, HashMap.class);
    this.initializerClassMappings.forEach((className, testClasses) -> {
      List<String> testClassNames = testClasses.stream().map(Class::getName).toList();
      logger.trace("Generating mapping from AOT context initializer class [{}] to test classes {}",
              className.reflectionName(), testClassNames);
      testClassNames.forEach(testClassName ->
              code.addStatement("map.put($S, $T.class)", testClassName, className));
    });
    code.addStatement("return map");
    return code.build();
  }

}
