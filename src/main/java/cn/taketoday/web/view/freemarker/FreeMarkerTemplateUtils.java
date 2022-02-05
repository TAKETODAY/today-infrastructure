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

package cn.taketoday.web.view.freemarker;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Utility class for working with FreeMarker.
 * Provides convenience methods to process a FreeMarker template with a model.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/5 13:08
 */
public abstract class FreeMarkerTemplateUtils {

  /**
   * Process the specified FreeMarker template with the given model and write
   * the result to the given Writer.
   * <p>When using this method to prepare a text for a mail to be sent with Framework's
   * mail support, consider wrapping IO/TemplateException in MailPreparationException.
   *
   * @param model the model object, typically a Map that contains model names
   * as keys and model objects as values
   * @return the result as String
   * @throws IOException if the template wasn't found or couldn't be read
   * @throws freemarker.template.TemplateException if rendering failed
   * @see cn.taketoday.mail.MailPreparationException
   */
  public static String processTemplateIntoString(Template template, Object model)
          throws IOException, TemplateException {

    StringWriter result = new StringWriter(1024);
    template.process(model, result);
    return result.toString();
  }

}
