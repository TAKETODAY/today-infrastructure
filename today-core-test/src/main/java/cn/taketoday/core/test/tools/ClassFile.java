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

package cn.taketoday.core.test.tools;

import java.io.IOException;

import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.FileCopyUtils;

/**
 * In memory representation of a Java class.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public final class ClassFile {

  private static final String CLASS_SUFFIX = ".class";

  private final String name;

  private final byte[] content;

  private ClassFile(String name, byte[] content) {
    this.name = name;
    this.content = content;
  }

  /**
   * Return the fully qualified name of the class.
   *
   * @return the class name
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the bytecode content.
   *
   * @return the class content
   */
  public byte[] getContent() {
    return this.content;
  }

  /**
   * Factory method to create a new {@link ClassFile} from the given
   * {@code content}.
   *
   * @param name the fully qualified name of the class
   * @param content the bytecode of the class
   * @return a {@link ClassFile} instance
   */
  public static ClassFile of(String name, byte[] content) {
    return new ClassFile(name, content);
  }

  /**
   * Factory method to create a new {@link ClassFile} from the given
   * {@link InputStreamSource}.
   *
   * @param name the fully qualified name of the class
   * @param inputStreamSource the bytecode of the class
   * @return a {@link ClassFile} instance
   */
  public static ClassFile of(String name, InputStreamSource inputStreamSource) {
    return of(name, toBytes(inputStreamSource));
  }

  /**
   * Return the name of a class based on its relative path.
   *
   * @param path the path of the class
   * @return the class name
   */
  public static String toClassName(String path) {
    Assert.hasText(path, "'path' must not be empty");
    if (!path.endsWith(CLASS_SUFFIX)) {
      throw new IllegalArgumentException("Path '" + path + "' must end with '.class'");
    }
    String name = path.replace('/', '.');
    return name.substring(0, name.length() - CLASS_SUFFIX.length());
  }

  private static byte[] toBytes(InputStreamSource inputStreamSource) {
    try {
      return FileCopyUtils.copyToByteArray(inputStreamSource.getInputStream());
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to read content", ex);
    }
  }

}
