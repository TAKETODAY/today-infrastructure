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

package cn.taketoday.context.condition;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * A message associated with a {@link ConditionOutcome}. Provides a fluent builder style
 * API to encourage consistency across all condition messages.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public final class ConditionMessage {

  private final String message;

  private ConditionMessage() {
    this(null);
  }

  private ConditionMessage(String message) {
    this.message = message;
  }

  private ConditionMessage(ConditionMessage prior, String message) {
    this.message = prior.isEmpty() ? message : prior + "; " + message;
  }

  /**
   * Return {@code true} if the message is empty.
   *
   * @return if the message is empty
   */
  public boolean isEmpty() {
    return StringUtils.isEmpty(this.message);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ConditionMessage)) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    return ObjectUtils.nullSafeEquals(((ConditionMessage) obj).message, this.message);
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.message);
  }

  @Override
  public String toString() {
    return this.message != null ? this.message : "";
  }

  /**
   * Return a new {@link ConditionMessage} based on the instance and an appended
   * message.
   *
   * @param message the message to append
   * @return a new {@link ConditionMessage} instance
   */
  public ConditionMessage append(String message) {
    if (StringUtils.isEmpty(message)) {
      return this;
    }
    if (StringUtils.isEmpty(this.message)) {
      return new ConditionMessage(message);
    }

    return new ConditionMessage(this.message + " " + message);
  }

  /**
   * Return a new builder to construct a new {@link ConditionMessage} based on the
   * instance and a new condition outcome.
   *
   * @param condition the condition
   * @param details details of the condition
   * @return a {@link Builder} builder
   * @see #andCondition(String, Object...)
   * @see #forCondition(Class, Object...)
   */
  public Builder andCondition(Class<? extends Annotation> condition, Object... details) {
    Assert.notNull(condition, "Condition must not be null");
    return andCondition("@" + ClassUtils.getShortName(condition), details);
  }

  /**
   * Return a new builder to construct a new {@link ConditionMessage} based on the
   * instance and a new condition outcome.
   *
   * @param condition the condition
   * @param details details of the condition
   * @return a {@link Builder} builder
   * @see #andCondition(Class, Object...)
   * @see #forCondition(String, Object...)
   */
  public Builder andCondition(String condition, Object... details) {
    Assert.notNull(condition, "Condition must not be null");
    String detail = StringUtils.arrayToString(details, " ");
    if (StringUtils.isNotEmpty(detail)) {
      return new Builder(condition + " " + detail);
    }
    return new Builder(condition);
  }

  /**
   * Factory method to return a new empty {@link ConditionMessage}.
   *
   * @return a new empty {@link ConditionMessage}
   */
  public static ConditionMessage empty() {
    return new ConditionMessage();
  }

  /**
   * Factory method to create a new {@link ConditionMessage} with a specific message.
   *
   * @param message the source message (may be a format string if {@code args} are
   * specified)
   * @param args format arguments for the message
   * @return a new {@link ConditionMessage} instance
   */
  public static ConditionMessage of(String message, Object... args) {
    if (ObjectUtils.isEmpty(args)) {
      return new ConditionMessage(message);
    }
    return new ConditionMessage(String.format(message, args));
  }

  /**
   * Factory method to create a new {@link ConditionMessage} comprised of the specified
   * messages.
   *
   * @param messages the source messages (may be {@code null})
   * @return a new {@link ConditionMessage} instance
   */
  public static ConditionMessage of(Collection<? extends ConditionMessage> messages) {
    ConditionMessage result = new ConditionMessage();
    if (messages != null) {
      for (ConditionMessage message : messages) {
        result = new ConditionMessage(result, message.toString());
      }
    }
    return result;
  }

  /**
   * Factory method for a builder to construct a new {@link ConditionMessage} for a
   * condition.
   *
   * @param condition the condition
   * @param details details of the condition
   * @return a {@link Builder} builder
   * @see #forCondition(String, Object...)
   * @see #andCondition(String, Object...)
   */
  public static Builder forCondition(Class<? extends Annotation> condition, Object... details) {
    return new ConditionMessage().andCondition(condition, details);
  }

  /**
   * Factory method for a builder to construct a new {@link ConditionMessage} for a
   * condition.
   *
   * @param condition the condition
   * @param details details of the condition
   * @return a {@link Builder} builder
   * @see #forCondition(Class, Object...)
   * @see #andCondition(String, Object...)
   */
  public static Builder forCondition(String condition, Object... details) {
    return new ConditionMessage().andCondition(condition, details);
  }

  /**
   * Builder used to create a {@link ConditionMessage} for a condition.
   */
  public final class Builder {

    private final String condition;

    private Builder(String condition) {
      this.condition = condition;
    }

    /**
     * Indicate that an exact result was found. For example
     * {@code foundExactly("foo")} results in the message "found foo".
     *
     * @param result the result that was found
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage foundExactly(Object result) {
      return found("").items(result);
    }

    /**
     * Indicate that one or more results were found. For example
     * {@code found("bean").items("x")} results in the message "found bean x".
     *
     * @param article the article found
     * @return an {@link ItemsBuilder}
     */
    public ItemsBuilder found(String article) {
      return found(article, article);
    }

    /**
     * Indicate that one or more results were found. For example
     * {@code found("bean", "beans").items("x", "y")} results in the message "found
     * beans x, y".
     *
     * @param singular the article found in singular form
     * @param plural the article found in plural form
     * @return an {@link ItemsBuilder}
     */
    public ItemsBuilder found(String singular, String plural) {
      return new ItemsBuilder(this, "found", singular, plural);
    }

    /**
     * Indicate that one or more results were not found. For example
     * {@code didNotFind("bean").items("x")} results in the message "did not find bean
     * x".
     *
     * @param article the article found
     * @return an {@link ItemsBuilder}
     */
    public ItemsBuilder didNotFind(String article) {
      return didNotFind(article, article);
    }

    /**
     * Indicate that one or more results were found. For example
     * {@code didNotFind("bean", "beans").items("x", "y")} results in the message "did
     * not find beans x, y".
     *
     * @param singular the article found in singular form
     * @param plural the article found in plural form
     * @return an {@link ItemsBuilder}
     */
    public ItemsBuilder didNotFind(String singular, String plural) {
      return new ItemsBuilder(this, "did not find", singular, plural);
    }

    /**
     * Indicates a single result. For example {@code resultedIn("yes")} results in the
     * message "resulted in yes".
     *
     * @param result the result
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage resultedIn(Object result) {
      return because("resulted in " + result);
    }

    /**
     * Indicates something is available. For example {@code available("money")}
     * results in the message "money is available".
     *
     * @param item the item that is available
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage available(String item) {
      return because(item + " is available");
    }

    /**
     * Indicates something is not available. For example {@code notAvailable("time")}
     * results in the message "time is not available".
     *
     * @param item the item that is not available
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage notAvailable(String item) {
      return because(item + " is not available");
    }

    /**
     * Indicates the reason. For example {@code because("running Linux")} results in
     * the message "running Linux".
     *
     * @param reason the reason for the message
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage because(String reason) {
      if (StringUtils.isNotEmpty(reason)) {
        return new ConditionMessage(
                ConditionMessage.this,
                StringUtils.isNotEmpty(this.condition) ? this.condition + " " + reason : reason);
      }
      return new ConditionMessage(ConditionMessage.this, this.condition);
    }

  }

  /**
   * Builder used to create an {@link ItemsBuilder} for a condition.
   */
  public final class ItemsBuilder {

    private final Builder condition;

    private final String reason;

    private final String singular;

    private final String plural;

    private ItemsBuilder(Builder condition, String reason, String singular, String plural) {
      this.condition = condition;
      this.reason = reason;
      this.singular = singular;
      this.plural = plural;
    }

    /**
     * Used when no items are available. For example
     * {@code didNotFind("any beans").atAll()} results in the message "did not find
     * any beans".
     *
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage atAll() {
      return items(Collections.emptyList());
    }

    /**
     * Indicate the items. For example
     * {@code didNotFind("bean", "beans").items("x", "y")} results in the message "did
     * not find beans x, y".
     *
     * @param items the items (may be {@code null})
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage items(Object... items) {
      return items(Style.NORMAL, items);
    }

    /**
     * Indicate the items. For example
     * {@code didNotFind("bean", "beans").items("x", "y")} results in the message "did
     * not find beans x, y".
     *
     * @param style the render style
     * @param items the items (may be {@code null})
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage items(Style style, Object... items) {
      return items(style, (items != null) ? Arrays.asList(items) : null);
    }

    /**
     * Indicate the items. For example
     * {@code didNotFind("bean", "beans").items(Collections.singleton("x")} results in
     * the message "did not find bean x".
     *
     * @param items the source of the items (may be {@code null})
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage items(Collection<?> items) {
      return items(Style.NORMAL, items);
    }

    /**
     * Indicate the items with a {@link Style}. For example
     * {@code didNotFind("bean", "beans").items(Style.QUOTE, Collections.singleton("x")}
     * results in the message "did not find bean 'x'".
     *
     * @param style the render style
     * @param items the source of the items (may be {@code null})
     * @return a built {@link ConditionMessage}
     */
    public ConditionMessage items(Style style, Collection<?> items) {
      Assert.notNull(style, "Style must not be null");
      StringBuilder message = new StringBuilder(this.reason);
      items = style.applyTo(items);
      if ((this.condition == null || items == null || items.size() <= 1)
              && StringUtils.isNotEmpty(this.singular)) {
        message.append(" ").append(this.singular);
      }
      else if (StringUtils.isNotEmpty(this.plural)) {
        message.append(" ").append(this.plural);
      }
      if (items != null && !items.isEmpty()) {
        message.append(" ").append(StringUtils.collectionToString(items, ", "));
      }
      return this.condition.because(message.toString());
    }

  }

  /**
   * Render styles.
   */
  public enum Style {

    /**
     * Render with normal styling.
     */
    NORMAL {
      @Override
      protected Object applyToItem(Object item) {
        return item;
      }

    },

    /**
     * Render with the item surrounded by quotes.
     */
    QUOTE {
      @Override
      protected String applyToItem(Object item) {
        return (item != null) ? "'" + item + "'" : null;
      }

    };

    public Collection<?> applyTo(Collection<?> items) {
      if (items == null) {
        return null;
      }
      List<Object> result = new ArrayList<>(items.size());
      for (Object item : items) {
        result.add(applyToItem(item));
      }
      return result;
    }

    protected abstract Object applyToItem(Object item);

  }

}
