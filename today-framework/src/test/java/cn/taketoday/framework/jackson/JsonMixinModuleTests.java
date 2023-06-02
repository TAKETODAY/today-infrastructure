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

package cn.taketoday.framework.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.framework.jackson.scan.a.RenameMixInClass;
import cn.taketoday.framework.jackson.scan.b.RenameMixInAbstractClass;
import cn.taketoday.framework.jackson.scan.c.RenameMixInInterface;
import cn.taketoday.framework.jackson.scan.d.EmptyMixInClass;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/29 21:40
 */
class JsonMixinModuleTests {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void jsonWithModuleWithRenameMixInClassShouldBeMixedIn() throws Exception {
    load(RenameMixInClass.class);
    JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
    assertMixIn(module, new Name("spring"), "{\"username\":\"spring\"}");
    assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
  }

  @Test
  void jsonWithModuleWithEmptyMixInClassShouldNotBeMixedIn() throws Exception {
    load(EmptyMixInClass.class);
    JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
    assertMixIn(module, new Name("spring"), "{\"name\":\"spring\"}");
    assertMixIn(module, new NameAndAge("spring", 100), "{\"name\":\"spring\",\"age\":100}");
  }

  @Test
  void jsonWithModuleWithRenameMixInAbstractClassShouldBeMixedIn() throws Exception {
    load(RenameMixInAbstractClass.class);
    JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
    assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
  }

  @Test
  void jsonWithModuleWithRenameMixInInterfaceShouldBeMixedIn() throws Exception {
    load(RenameMixInInterface.class);
    JsonMixinModule module = this.context.getBean(JsonMixinModule.class);
    assertMixIn(module, new NameAndAge("spring", 100), "{\"age\":100,\"username\":\"spring\"}");
  }

  private void load(Class<?>... basePackageClasses) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBean(JsonMixinModule.class, () -> createJsonMixinModule(context, basePackageClasses));
    context.refresh();
    this.context = context;
  }

  private JsonMixinModule createJsonMixinModule(AnnotationConfigApplicationContext context,
          Class<?>... basePackageClasses) {
    List<String> basePackages = Arrays.stream(basePackageClasses).map(ClassUtils::getPackageName).toList();
    JsonMixinModuleEntries entries = JsonMixinModuleEntries.scan(context, basePackages);
    JsonMixinModule jsonMixinModule = new JsonMixinModule();
    jsonMixinModule.registerEntries(entries, context.getClassLoader());
    return jsonMixinModule;
  }

  private void assertMixIn(Module module, Name value, String expectedJson) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(module);
    String json = mapper.writeValueAsString(value);
    assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
  }

}