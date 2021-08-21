/*
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
package cn.taketoday.web.view;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import cn.taketoday.core.Constant;
import cn.taketoday.util.MediaType;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;

/**
 * @author TODAY <br>
 * 2019-07-17 13:31
 * @see JsonSequence
 */
public abstract class MessageConverter {

  /** for write string */
  private Charset charset = Constant.DEFAULT_CHARSET;

  /**
   * Write message to client
   *
   * @param context
   *         Current request context
   * @param message
   *         The message write to client
   *
   * @throws IOException
   *         If any input output exception occurred
   */
  public void write(RequestContext context, Object message) throws IOException {
    if (message != null) {
      if (message instanceof CharSequence) {
        writeStringInternal(context, message.toString());
      }
      else {
        if (message instanceof JsonSequence) {
          message = ((JsonSequence) message).getJSON();
        }
        applyContentType(context);
        writeInternal(context, message);
      }
    }
    else {
      writeNullInternal(context);
    }
  }

  protected void applyContentType(RequestContext context) {
    context.setContentType(MediaType.APPLICATION_JSON_VALUE);
  }

  protected void writeStringInternal(RequestContext context, String message) throws IOException {
    final OutputStreamWriter writer = new OutputStreamWriter(context.getOutputStream(), charset);
    writer.write(message);
    writer.flush();
  }

  protected void writeNullInternal(RequestContext context) throws IOException { }

  /**
   * Write none null message
   */
  abstract void writeInternal(RequestContext context, Object noneNullMessage) throws IOException;

  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  public Charset getCharset() {
    return charset;
  }

  /**
   * Read The request body and convert it to Target object
   *
   * @param context
   *         Current request context
   * @param parameter
   *         Handler method parameter
   *
   * @return The handler method parameter object
   *
   * @throws IOException
   *         If any input output exception occurred
   */
  public abstract Object read(RequestContext context, MethodParameter parameter) throws IOException;

}
