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
package cn.taketoday.scripting.bsh;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.scripting.ScriptEvaluator;
import cn.taketoday.scripting.support.ResourceScriptSource;
import cn.taketoday.scripting.support.StaticScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class BshScriptEvaluatorTests {

  @Test
  public void testBshScriptFromString() {
    ScriptEvaluator evaluator = new BshScriptEvaluator();
    Object result = evaluator.evaluate(new StaticScriptSource("return 3 * 2;"));
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void testBshScriptFromFile() {
    ScriptEvaluator evaluator = new BshScriptEvaluator();
    Object result = evaluator.evaluate(new ResourceScriptSource(new ClassPathResource("simple.bsh", getClass())));
    assertThat(result).isEqualTo(6);
  }

  @Test
  public void testGroovyScriptWithArguments() {
    ScriptEvaluator evaluator = new BshScriptEvaluator();
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("a", 3);
    arguments.put("b", 2);
    Object result = evaluator.evaluate(new StaticScriptSource("return a * b;"), arguments);
    assertThat(result).isEqualTo(6);
  }

}
