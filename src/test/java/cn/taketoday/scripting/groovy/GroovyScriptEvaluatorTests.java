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
package cn.taketoday.scripting.groovy;

import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.scripting.ScriptEvaluator;
import cn.taketoday.scripting.support.ResourceScriptSource;
import cn.taketoday.scripting.support.StandardScriptEvaluator;
import cn.taketoday.scripting.support.StaticScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class GroovyScriptEvaluatorTests {

  @Test
  public void testGroovyScriptFromString() {
    GroovyScriptEvaluator evaluator = new GroovyScriptEvaluator();
    evaluator.setCompilerConfiguration(null);
    Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2"));
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void testGroovyScriptFromFile() {
    ScriptEvaluator evaluator = new GroovyScriptEvaluator();
    Object result = evaluator.evaluate(new ResourceScriptSource(new ClassPathResource("simple.groovy", getClass())));
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void testGroovyScriptWithArguments() {
    ScriptEvaluator evaluator = new GroovyScriptEvaluator();
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("a", 3);
    arguments.put("b", 2);
    Object result = evaluator.evaluate(new StaticScriptSource("return a * b"), arguments);
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void testGroovyScriptWithCompilerConfiguration() {
    GroovyScriptEvaluator evaluator = new GroovyScriptEvaluator();
    MyBytecodeProcessor processor = new MyBytecodeProcessor();
    evaluator.getCompilerConfiguration().setBytecodePostprocessor(processor);
    Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2"));
    assertThat(result).isEqualTo(6);
    assertThat(processor.processed.contains("Script1")).isTrue();
  }

  @Test
  public void testGroovyScriptWithImportCustomizer() {
    GroovyScriptEvaluator evaluator = new GroovyScriptEvaluator();
    ImportCustomizer importCustomizer = new ImportCustomizer();
    importCustomizer.addStarImports("cn.taketoday.util");
    evaluator.setCompilationCustomizers(importCustomizer);
    Object result = evaluator.evaluate(new StaticScriptSource("return ResourceUtils.JAR_ENTRY_URL_PREFIX"));
    assertThat(result).isEqualTo("jar:file:");
  }

  @Test
  public void testGroovyScriptFromStringUsingJsr223() {
    StandardScriptEvaluator evaluator = new StandardScriptEvaluator();
    evaluator.setEngineName("");
    evaluator.setLanguage("Groovy");
    Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2"));
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void testGroovyScriptFromFileUsingJsr223() {
    ScriptEvaluator evaluator = new StandardScriptEvaluator();
    Object result = evaluator.evaluate(new ResourceScriptSource(new ClassPathResource("simple.groovy", getClass())));
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void testGroovyScriptWithArgumentsUsingJsr223() {
    StandardScriptEvaluator evaluator = new StandardScriptEvaluator();
    evaluator.setLanguage("Groovy");
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("a", 3);
    arguments.put("b", 2);
    Object result = evaluator.evaluate(new StaticScriptSource("return a * b"), arguments);
    assertThat(result).isEqualTo(6);
  }

}
