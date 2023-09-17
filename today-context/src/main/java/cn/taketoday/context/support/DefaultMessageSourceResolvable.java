/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.io.Serializable;

import cn.taketoday.context.MessageSourceResolvable;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Framework's default implementation of the {@link MessageSourceResolvable} interface.
 * Offers an easy way to store all the necessary values needed to resolve
 * a message via a {@link cn.taketoday.context.MessageSource}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.context.MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultMessageSourceResolvable implements MessageSourceResolvable, Serializable {

  @Nullable
  private final String[] codes;

  @Nullable
  private final Object[] arguments;

  @Nullable
  private final String defaultMessage;

  /**
   * Create a new DefaultMessageSourceResolvable.
   *
   * @param code the code to be used to resolve this message
   */
  public DefaultMessageSourceResolvable(String code) {
    this(new String[] { code }, null, null);
  }

  /**
   * Create a new DefaultMessageSourceResolvable.
   *
   * @param codes the codes to be used to resolve this message
   */
  public DefaultMessageSourceResolvable(String[] codes) {
    this(codes, null, null);
  }

  /**
   * Create a new DefaultMessageSourceResolvable.
   *
   * @param codes the codes to be used to resolve this message
   * @param defaultMessage the default message to be used to resolve this message
   */
  public DefaultMessageSourceResolvable(String[] codes, String defaultMessage) {
    this(codes, null, defaultMessage);
  }

  /**
   * Create a new DefaultMessageSourceResolvable.
   *
   * @param codes the codes to be used to resolve this message
   * @param arguments the array of arguments to be used to resolve this message
   */
  public DefaultMessageSourceResolvable(String[] codes, Object[] arguments) {
    this(codes, arguments, null);
  }

  /**
   * Create a new DefaultMessageSourceResolvable.
   *
   * @param codes the codes to be used to resolve this message
   * @param arguments the array of arguments to be used to resolve this message
   * @param defaultMessage the default message to be used to resolve this message
   */
  public DefaultMessageSourceResolvable(
          @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

    this.codes = codes;
    this.arguments = arguments;
    this.defaultMessage = defaultMessage;
  }

  /**
   * Copy constructor: Create a new instance from another resolvable.
   *
   * @param resolvable the resolvable to copy from
   */
  public DefaultMessageSourceResolvable(MessageSourceResolvable resolvable) {
    this(resolvable.getCodes(), resolvable.getArguments(), resolvable.getDefaultMessage());
  }

  /**
   * Return the default code of this resolvable, that is,
   * the last one in the codes array.
   */
  @Nullable
  public String getCode() {
    return (this.codes != null && this.codes.length > 0 ? this.codes[this.codes.length - 1] : null);
  }

  @Override
  @Nullable
  public String[] getCodes() {
    return this.codes;
  }

  @Override
  @Nullable
  public Object[] getArguments() {
    return this.arguments;
  }

  @Override
  @Nullable
  public String getDefaultMessage() {
    return this.defaultMessage;
  }

  /**
   * Indicate whether the specified default message needs to be rendered for
   * substituting placeholders and/or {@link java.text.MessageFormat} escaping.
   *
   * @return {@code true} if the default message may contain argument placeholders;
   * {@code false} if it definitely does not contain placeholders or custom escaping
   * and can therefore be simply exposed as-is
   * @see #getDefaultMessage()
   * @see #getArguments()
   * @see AbstractMessageSource#renderDefaultMessage
   */
  public boolean shouldRenderDefaultMessage() {
    return true;
  }

  /**
   * Build a default String representation for this MessageSourceResolvable:
   * including codes, arguments, and default message.
   */
  protected final String resolvableToString() {
    StringBuilder result = new StringBuilder(64);
    result.append("codes [").append(StringUtils.arrayToDelimitedString(this.codes, ","));
    result.append("]; arguments [").append(StringUtils.arrayToDelimitedString(this.arguments, ","));
    result.append("]; default message [").append(this.defaultMessage).append(']');
    return result.toString();
  }

  /**
   * The default implementation exposes the attributes of this MessageSourceResolvable.
   * <p>To be overridden in more specific subclasses, potentially including the
   * resolvable content through {@code resolvableToString()}.
   *
   * @see #resolvableToString()
   */
  @Override
  public String toString() {
    return getClass().getName() + ": " + resolvableToString();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof MessageSourceResolvable otherResolvable)) {
      return false;
    }
    return (ObjectUtils.nullSafeEquals(getCodes(), otherResolvable.getCodes()) &&
            ObjectUtils.nullSafeEquals(getArguments(), otherResolvable.getArguments()) &&
            ObjectUtils.nullSafeEquals(getDefaultMessage(), otherResolvable.getDefaultMessage()));
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHash(getCode(), getArguments(), getDefaultMessage());
  }

}
