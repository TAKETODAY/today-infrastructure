/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.framework.reactive;

import java.util.Map;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.config.WebMvcConfiguration;
import cn.taketoday.web.view.template.AbstractFreeMarkerTemplateViewResolver;
import cn.taketoday.web.view.template.RequestContextParametersHashModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultMapAdapter;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.utility.ObjectWrapperWithAPISupport;

/**
 * @author TODAY <br>
 *         2019-11-22 13:25
 */
@Props(prefix = "web.mvc.view.")
public class ReactiveFreeMarkerTemplateViewResolver
        extends AbstractFreeMarkerTemplateViewResolver implements WebMvcConfiguration {

  public static final String KEY_REQUEST_PARAMETERS = "RequestParameters";

  public ReactiveFreeMarkerTemplateViewResolver(Configuration configuration) {
    this(new DefaultObjectWrapper(configuration.getIncompatibleImprovements()), configuration);
  }

  @Autowired
  public ReactiveFreeMarkerTemplateViewResolver(ObjectWrapper wrapper, Configuration configuration) {
    setConfiguration(configuration);
    setObjectWrapper(wrapper);
  }

  /**
   * Create Model Attributes.
   *
   * @param context
   *            Current request context
   * @return {@link TemplateHashModel}
   */
  protected TemplateHashModel createModel(RequestContext context) {
    final ObjectWrapper wrapper = getObjectWrapper();

    final Map<String, Object> attributes = context.asMap();

    // Create hash model wrapper for request
    attributes.put(KEY_REQUEST_PARAMETERS, new RequestContextParametersHashModel(wrapper, context));

    return DefaultMapAdapter.adapt(attributes, (ObjectWrapperWithAPISupport) wrapper);
  }

}
