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

package cn.taketoday.web.registry.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.web.annotation.ActionMapping;

/**
 * @author TODAY 2021/4/22 22:07
 */
public class RequestPathMappingHandlerMethodRegistryTests {

  @ActionMapping(value = "/users",
                 method = HttpMethod.GET,
                 params = { "class-name=TODAY", "class-q" },
                 consumes = MediaType.APPLICATION_JSON_VALUE
  )
  static class TEST {

    @ActionMapping(value = "/{id}", consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
    }, params = { "name=TODAY", "q" })
    void mapping() {

    }

  }

  @Test
  public void mappingHandlerMethod() throws Exception {
    AnnotationAttributes mapping = new AnnotationAttributes();
    Method method = TEST.class.getDeclaredMethod("mapping");

    MergedAnnotation<ActionMapping> actionMapping = MergedAnnotations.from(method).get(ActionMapping.class);
    MergedAnnotation<ActionMapping> controllerMapping = MergedAnnotations.from(method).get(ActionMapping.class);

    RequestPathMappingHandlerRegistry registry = new RequestPathMappingHandlerRegistry();
    registry.mergeMappingAttributes(mapping, actionMapping, controllerMapping);
    System.out.println(mapping);
  }

}
