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

package cn.taketoday.context.support;

import java.util.Locale;

import cn.taketoday.context.HierarchicalMessageSource;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.context.NoSuchMessageException;
import cn.taketoday.lang.Nullable;

/**
 * Empty {@link MessageSource} that delegates all calls to the parent MessageSource.
 * If no parent is available, it simply won't resolve any message.
 *
 * <p>Used as placeholder by AbstractApplicationContext, if the context doesn't
 * define its own MessageSource. Not intended for direct use in applications.
 *
 * @author Juergen Hoeller
 * @see AbstractApplicationContext
 * @since 4.0
 */
public class DelegatingMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

  @Nullable
  private MessageSource parentMessageSource;

  @Override
  public void setParentMessageSource(@Nullable MessageSource parent) {
    this.parentMessageSource = parent;
  }

  @Override
  @Nullable
  public MessageSource getParentMessageSource() {
    return this.parentMessageSource;
  }

  @Override
  @Nullable
  public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
    if (this.parentMessageSource != null) {
      return this.parentMessageSource.getMessage(code, args, defaultMessage, locale);
    }
    else if (defaultMessage != null) {
      return renderDefaultMessage(defaultMessage, args, locale);
    }
    else {
      return null;
    }
  }

  @Override
  public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
    if (this.parentMessageSource != null) {
      return this.parentMessageSource.getMessage(code, args, locale);
    }
    else {
      throw new NoSuchMessageException(code, locale);
    }
  }

  @Override
  public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
    if (this.parentMessageSource != null) {
      return this.parentMessageSource.getMessage(resolvable, locale);
    }
    else {
      if (resolvable.getDefaultMessage() != null) {
        return renderDefaultMessage(resolvable.getDefaultMessage(), resolvable.getArguments(), locale);
      }
      String[] codes = resolvable.getCodes();
      String code = (codes != null && codes.length > 0 ? codes[0] : "");
      throw new NoSuchMessageException(code, locale);
    }
  }

  @Override
  public String toString() {
    return this.parentMessageSource != null ? this.parentMessageSource.toString() : "Empty MessageSource";
  }

}
