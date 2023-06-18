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

package cn.taketoday.context.testfixture.beans;

import java.util.Locale;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.NoSuchMessageException;

public class ACATester implements ApplicationContextAware {

  private ApplicationContext ac;

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws ApplicationContextException {
    // check re-initialization
    if (this.ac != null) {
      throw new IllegalStateException("Already initialized");
    }

    // check message source availability
    if (ctx != null) {
      try {
        ctx.getMessage("code1", null, Locale.getDefault());
      }
      catch (NoSuchMessageException ex) {
        // expected
      }
    }

    this.ac = ctx;
  }

  public ApplicationContext getApplicationContext() {
    return ac;
  }

}
