/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.annotation.config.jsonb;

import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnResource;
import cn.taketoday.stereotype.Component;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JSON-B.
 *
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass(Jsonb.class)
@ConditionalOnResource({ "classpath:META-INF/services/jakarta.json.bind.spi.JsonbProvider",
        "classpath:META-INF/services/jakarta.json.spi.JsonProvider" })
public class JsonbAutoConfiguration {

  @Component
  @ConditionalOnMissingBean
  static Jsonb jsonb() {
    return JsonbBuilder.create();
  }

}
