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

package cn.taketoday.bytecode.commons;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ByteVector;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Label;

/**
 * A ModuleHashes attribute. This attribute is specific to the OpenJDK and may change in the future.
 *
 * @author Remi Forax
 */
public final class ModuleHashesAttribute extends Attribute {

  /** The name of the hashing algorithm. */
  public String algorithm;

  /** A list of module names. */
  public List<String> modules;

  /** The hash of the modules in {@link #modules}. The two lists must have the same size. */
  public List<byte[]> hashes;

  /**
   * Constructs a new {@link ModuleHashesAttribute}.
   *
   * @param algorithm the name of the hashing algorithm.
   * @param modules a list of module names.
   * @param hashes the hash of the modules in 'modules'. The two lists must have the same size.
   */
  public ModuleHashesAttribute(
          final String algorithm, final List<String> modules, final List<byte[]> hashes) {
    super("ModuleHashes");
    this.algorithm = algorithm;
    this.modules = modules;
    this.hashes = hashes;
  }

  /**
   * Constructs an empty {@link ModuleHashesAttribute}. This object can be passed as a prototype to
   * the {@link ClassReader#accept(ClassVisitor, Attribute[], int)} method.
   */
  public ModuleHashesAttribute() {
    this(null, null, null);
  }

  @Override
  protected Attribute read(
          final ClassReader classReader,
          final int offset,
          final int length,
          final char[] charBuffer,
          final int codeAttributeOffset,
          final Label[] labels) {
    int currentOffset = offset;

    String hashAlgorithm = classReader.readUTF8(currentOffset, charBuffer);
    currentOffset += 2;

    int numModules = classReader.readUnsignedShort(currentOffset);
    currentOffset += 2;

    ArrayList<String> moduleList = new ArrayList<>(numModules);
    ArrayList<byte[]> hashList = new ArrayList<>(numModules);

    for (int i = 0; i < numModules; ++i) {
      String module = classReader.readModule(currentOffset, charBuffer);
      currentOffset += 2;
      moduleList.add(module);

      int hashLength = classReader.readUnsignedShort(currentOffset);
      currentOffset += 2;
      byte[] hash = new byte[hashLength];
      for (int j = 0; j < hashLength; ++j) {
        hash[j] = (byte) classReader.readByte(currentOffset);
        currentOffset += 1;
      }
      hashList.add(hash);
    }
    return new ModuleHashesAttribute(hashAlgorithm, moduleList, hashList);
  }

  @Override
  protected ByteVector write(
          final ClassWriter classWriter,
          final byte[] code,
          final int codeLength,
          final int maxStack,
          final int maxLocals) {
    ByteVector byteVector = new ByteVector();
    byteVector.putShort(classWriter.newUTF8(algorithm));
    List<String> modules = this.modules;
    if (modules == null) {
      byteVector.putShort(0);
    }
    else {
      final List<byte[]> hashes = this.hashes;

      int numModules = modules.size();
      byteVector.putShort(numModules);
      for (int i = 0; i < numModules; ++i) {
        String module = modules.get(i);
        byte[] hash = hashes.get(i);
        byteVector.putShort(classWriter.newModule(module))
                .putShort(hash.length)
                .putByteArray(hash, 0, hash.length);
      }
    }
    return byteVector;
  }
}
