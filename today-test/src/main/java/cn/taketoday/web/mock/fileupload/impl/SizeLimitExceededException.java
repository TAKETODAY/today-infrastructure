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
package cn.taketoday.web.mock.fileupload.impl;

/**
 * Thrown to indicate that the request size exceeds the configured maximum.
 */
public class SizeLimitExceededException
        extends SizeException {

  /**
   * The exceptions UID, for serializing an instance.
   */
  private static final long serialVersionUID = -2474893167098052828L;

  /**
   * Constructs a {@code SizeExceededException} with
   * the specified detail message, and actual and permitted sizes.
   *
   * @param message The detail message.
   * @param actual The actual request size.
   * @param permitted The maximum permitted request size.
   */
  public SizeLimitExceededException(final String message, final long actual,
          final long permitted) {
    super(message, actual, permitted);
  }

}