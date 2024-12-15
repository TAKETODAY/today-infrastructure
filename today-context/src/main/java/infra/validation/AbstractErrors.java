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

package infra.validation;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * Abstract implementation of the {@link Errors} interface.
 * Provides nested path handling but does not define concrete management
 * of {@link ObjectError ObjectErrors} and {@link FieldError FieldErrors}.
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractBindingResult
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class AbstractErrors implements Errors, Serializable {

  private String nestedPath = "";

  private final Deque<String> nestedPathStack = new ArrayDeque<>();

  @Override
  public void setNestedPath(@Nullable String nestedPath) {
    doSetNestedPath(nestedPath);
    this.nestedPathStack.clear();
  }

  @Override
  public String getNestedPath() {
    return this.nestedPath;
  }

  @Override
  public void pushNestedPath(String subPath) {
    this.nestedPathStack.push(getNestedPath());
    doSetNestedPath(getNestedPath() + subPath);
  }

  @Override
  public void popNestedPath() throws IllegalStateException {
    try {
      String formerNestedPath = this.nestedPathStack.pop();
      doSetNestedPath(formerNestedPath);
    }
    catch (NoSuchElementException ex) {
      throw new IllegalStateException("Cannot pop nested path: no nested path on stack");
    }
  }

  /**
   * Actually set the nested path.
   * Delegated to by setNestedPath and pushNestedPath.
   */
  protected void doSetNestedPath(@Nullable String nestedPath) {
    if (nestedPath == null) {
      nestedPath = "";
    }
    nestedPath = canonicalFieldName(nestedPath);
    if (!nestedPath.isEmpty() && !nestedPath.endsWith(NESTED_PATH_SEPARATOR)) {
      nestedPath += NESTED_PATH_SEPARATOR;
    }
    this.nestedPath = nestedPath;
  }

  /**
   * Transform the given field into its full path,
   * regarding the nested path of this instance.
   */
  protected String fixedField(@Nullable String field) {
    if (StringUtils.isNotEmpty(field)) {
      return getNestedPath() + canonicalFieldName(field);
    }
    else {
      String path = getNestedPath();
      return path.endsWith(NESTED_PATH_SEPARATOR)
             ? path.substring(0, path.length() - NESTED_PATH_SEPARATOR.length())
             : path;
    }
  }

  /**
   * Determine the canonical field name for the given field.
   * <p>The default implementation simply returns the field name as-is.
   *
   * @param field the original field name
   * @return the canonical field name
   */
  protected String canonicalFieldName(String field) {
    return field;
  }

  @Override
  public List<FieldError> getFieldErrors(String field) {
    List<FieldError> fieldErrors = getFieldErrors();
    ArrayList<FieldError> result = new ArrayList<>();
    String fixedField = fixedField(field);
    for (FieldError fieldError : fieldErrors) {
      if (isMatchingFieldError(fixedField, fieldError)) {
        result.add(fieldError);
      }
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Check whether the given FieldError matches the given field.
   *
   * @param field the field that we are looking up FieldErrors for
   * @param fieldError the candidate FieldError
   * @return whether the FieldError matches the given field
   */
  protected boolean isMatchingFieldError(String field, FieldError fieldError) {
    if (field.equals(fieldError.getField())) {
      return true;
    }
    // Optimization: use charAt and regionMatches instead of endsWith and startsWith
    int endIndex = field.length() - 1;
    return (endIndex >= 0 && field.charAt(endIndex) == '*'
            && (endIndex == 0 || field.regionMatches(0, fieldError.getField(), 0, endIndex)));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append(": ").append(getErrorCount()).append(" errors");
    for (ObjectError error : getAllErrors()) {
      sb.append('\n').append(error);
    }
    return sb.toString();
  }

}
