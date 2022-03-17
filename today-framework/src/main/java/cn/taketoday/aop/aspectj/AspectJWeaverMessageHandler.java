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

package cn.taketoday.aop.aspectj;

import org.aspectj.bridge.AbortException;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessage.Kind;
import org.aspectj.bridge.IMessageHandler;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Implementation of AspectJ's {@link IMessageHandler} interface that
 * routes AspectJ weaving messages through the same logging system as the
 * regular Framework messages.
 *
 * <p>Pass the option...
 *
 * <p><code class="code">-XmessageHandlerClass:cn.taketoday.aop.aspectj.AspectJWeaverMessageHandler</code>
 *
 * <p>to the weaver; for example, specifying the following in a
 * "{@code META-INF/aop.xml} file:
 *
 * <p><code class="code">&lt;weaver options="..."/&gt;</code>
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AspectJWeaverMessageHandler implements IMessageHandler {

  private static final String AJ_ID = "[AspectJ] ";

  private static final Logger logger = LoggerFactory.getLogger("AspectJ Weaver");

  @Override
  public boolean handleMessage(IMessage message) throws AbortException {
    Kind messageKind = message.getKind();
    if (messageKind == IMessage.DEBUG) {
      if (logger.isDebugEnabled()) {
        logger.debug(makeMessageFor(message));
        return true;
      }
    }
    else if (messageKind == IMessage.INFO || messageKind == IMessage.WEAVEINFO) {
      if (logger.isInfoEnabled()) {
        logger.info(makeMessageFor(message));
        return true;
      }
    }
    else if (messageKind == IMessage.WARNING) {
      if (logger.isWarnEnabled()) {
        logger.warn(makeMessageFor(message));
        return true;
      }
    }
    else if (messageKind == IMessage.ERROR || messageKind == IMessage.ABORT) {
      if (logger.isErrorEnabled()) {
        logger.error(makeMessageFor(message));
        return true;
      }
    }
    return false;
  }

  private String makeMessageFor(IMessage aMessage) {
    return AJ_ID + aMessage.getMessage();
  }

  @Override
  public boolean isIgnoring(Kind messageKind) {
    // We want to see everything, and allow configuration of log levels dynamically.
    return false;
  }

  @Override
  public void dontIgnore(Kind messageKind) {
    // We weren't ignoring anything anyway...
  }

  @Override
  public void ignore(Kind kind) {
    // We weren't ignoring anything anyway...
  }

}
