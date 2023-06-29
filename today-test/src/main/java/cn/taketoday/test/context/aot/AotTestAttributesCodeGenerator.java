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
import java.util.Map;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedClass;
import cn.taketoday.aot.generate.GeneratedClasses;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.javapoet.TypeName;
import cn.taketoday.javapoet.TypeSpec;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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
  // Ideally we would generate a class named: cn.taketoday.test.context.aot.GeneratedAotTestAttributes
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
