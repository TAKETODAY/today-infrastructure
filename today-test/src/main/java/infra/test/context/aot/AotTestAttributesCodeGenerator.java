/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.aot;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Modifier;

import infra.aot.generate.GeneratedClass;
import infra.aot.generate.GeneratedClasses;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.ParameterizedTypeName;
import infra.javapoet.TypeName;
import infra.javapoet.TypeSpec;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Internal code generator for {@link AotTestAttributes}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AotTestAttributesCodeGenerator {

  private static final Logger logger = LoggerFactory.getLogger(AotTestAttributesCodeGenerator.class);

  // Map<String, String>
  private static final TypeName MAP_TYPE = ParameterizedTypeName.get(Map.class, String.class, String.class);

  private static final String GENERATED_SUFFIX = "Generated";

  // TODO Consider an alternative means for specifying the name of the generated class.
  // Ideally we would generate a class named: infra.test.context.aot.GeneratedAotTestAttributes
  static final String GENERATED_ATTRIBUTES_CLASS_NAME = AotTestAttributes.class.getName() + "__" + GENERATED_SUFFIX;

  static final String GENERATED_ATTRIBUTES_METHOD_NAME = "getAttributes";

  private final Map<String, String> attributes;

  private final GeneratedClass generatedClass;

  AotTestAttributesCodeGenerator(Map<String, String> attributes, GeneratedClasses generatedClasses) {
    this.attributes = attributes;
    this.generatedClass = generatedClasses.addForFeature(GENERATED_SUFFIX, this::generateType);
  }

  GeneratedClass getGeneratedClass() {
    return this.generatedClass;
  }

  private void generateType(TypeSpec.Builder type) {
    logger.debug("Generating AOT test attributes in {}", generatedClass.getName().reflectionName());
    type.addJavadoc("Generated map for {@link $T}.", AotTestAttributes.class);
    type.addModifiers(Modifier.PUBLIC);
    type.addMethod(generateMethod());
  }

  private MethodSpec generateMethod() {
    MethodSpec.Builder method = MethodSpec.methodBuilder(GENERATED_ATTRIBUTES_METHOD_NAME);
    method.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    method.returns(MAP_TYPE);
    method.addCode(generateCode());
    return method.build();
  }

  private CodeBlock generateCode() {
    CodeBlock.Builder code = CodeBlock.builder();
    code.addStatement("$T map = new $T<>()", MAP_TYPE, HashMap.class);
    this.attributes.forEach((key, value) -> {
      logger.trace("Storing AOT test attribute: {} = {}", key, value);
      code.addStatement("map.put($S, $S)", key, value);
    });
    code.addStatement("return map");
    return code.build();
  }

}
