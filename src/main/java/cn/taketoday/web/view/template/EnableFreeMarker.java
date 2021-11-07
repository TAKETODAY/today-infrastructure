/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.view.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.core.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Configuration;

/**
 * @author TODAY 2021/3/24 21:50
 * @since 3.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Import(FreeMarkerConfig.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface EnableFreeMarker {

}

@Configuration
class FreeMarkerConfig {

  @Props(prefix = "web.mvc.view.")
  @Order(Ordered.LOWEST_PRECEDENCE - 100)
  @MissingBean(type = AbstractFreeMarkerTemplateRenderer.class)
  @ConditionalOnClass({ "freemarker.template.Configuration" })
  FreeMarkerTemplateRenderer freeMarkerTemplateRenderer() {
    return new FreeMarkerTemplateRenderer();
  }

}
