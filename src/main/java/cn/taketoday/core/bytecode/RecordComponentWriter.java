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
package cn.taketoday.core.bytecode;

final class RecordComponentWriter extends RecordComponentVisitor {
  /** Where the constants used in this RecordComponentWriter must be stored. */
  private final SymbolTable symbolTable;

  // Note: fields are ordered as in the record_component_info structure, and those related to
  // attributes are ordered as in Section 4.7 of the JVMS.

  /** The name_index field of the Record attribute. */
  private final int nameIndex;

  /** The descriptor_index field of the the Record attribute. */
  private final int descriptorIndex;

  /**
   * The signature_index field of the Signature attribute of this record component, or 0 if there is
   * no Signature attribute.
   */
  private int signatureIndex;

  /**
   * The last runtime visible annotation of this record component. The previous ones can be accessed
   * with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeVisibleAnnotation;

  /**
   * The last runtime invisible annotation of this record component. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeInvisibleAnnotation;

  /**
   * The last runtime visible type annotation of this record component. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

  /**
   * The last runtime invisible type annotation of this record component. The previous ones can be
   * accessed with the {@link AnnotationWriter#previousAnnotation} field. May be {@literal null}.
   */
  private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

  /**
   * The first non standard attribute of this record component. The next ones can be accessed with
   * the {@link Attribute#nextAttribute} field. May be {@literal null}.
   *
   * <p><b>WARNING</b>: this list stores the attributes in the <i>reverse</i> order of their visit.
   * firstAttribute is actually the last attribute visited in {@link #visitAttribute(Attribute)}.
   * The {@link #putRecordComponentInfo(ByteVector)} method writes the attributes in the order
   * defined by this list, i.e. in the reverse order specified by the user.
   */
  private Attribute firstAttribute;

  /**
   * Constructs a new {@link RecordComponentWriter}.
   *
   * @param symbolTable where the constants used in this RecordComponentWriter must be stored.
   * @param name the record component name.
   * @param descriptor the record component descriptor (see {@link Type}).
   * @param signature the record component signature. May be {@literal null}.
   */
  RecordComponentWriter(
          final SymbolTable symbolTable,
          final String name,
          final String descriptor,
          final String signature) {
    this.symbolTable = symbolTable;
    this.nameIndex = symbolTable.addConstantUtf8(name);
    this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
    if (signature != null) {
      this.signatureIndex = symbolTable.addConstantUtf8(signature);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the FieldVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleAnnotation =
              AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation);
    }
    else {
      return lastRuntimeInvisibleAnnotation =
              AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation);
    }
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (visible) {
      return lastRuntimeVisibleTypeAnnotation =
              AnnotationWriter.create(
                      symbolTable, typeRef, typePath, descriptor, lastRuntimeVisibleTypeAnnotation);
    }
    else {
      return lastRuntimeInvisibleTypeAnnotation =
              AnnotationWriter.create(
                      symbolTable, typeRef, typePath, descriptor, lastRuntimeInvisibleTypeAnnotation);
    }
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    // Store the attributes in the <i>reverse</i> order of their visit by this method.
    attribute.nextAttribute = firstAttribute;
    firstAttribute = attribute;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns the size of the record component JVMS structure generated by this
   * RecordComponentWriter. Also adds the names of the attributes of this record component in the
   * constant pool.
   *
   * @return the size in bytes of the record_component_info of the Record attribute.
   */
  int computeRecordComponentInfoSize() {
    // name_index, descriptor_index and attributes_count fields use 6 bytes.
    int size = 6;
    size += Attribute.computeAttributesSize(symbolTable, 0, signatureIndex);
    size +=
            AnnotationWriter.computeAnnotationsSize(
                    lastRuntimeVisibleAnnotation,
                    lastRuntimeInvisibleAnnotation,
                    lastRuntimeVisibleTypeAnnotation,
                    lastRuntimeInvisibleTypeAnnotation);
    if (firstAttribute != null) {
      size += firstAttribute.computeAttributesSize(symbolTable);
    }
    return size;
  }

  /**
   * Puts the content of the record component generated by this RecordComponentWriter into the given
   * ByteVector.
   *
   * @param output where the record_component_info structure must be put.
   */
  void putRecordComponentInfo(final ByteVector output) {
    output.putShort(nameIndex).putShort(descriptorIndex);
    // Compute and put the attributes_count field.
    // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
    int attributesCount = 0;
    if (signatureIndex != 0) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeVisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (lastRuntimeInvisibleTypeAnnotation != null) {
      ++attributesCount;
    }
    if (firstAttribute != null) {
      attributesCount += firstAttribute.getAttributeCount();
    }
    output.putShort(attributesCount);
    Attribute.putAttributes(symbolTable, 0, signatureIndex, output);
    AnnotationWriter.putAnnotations(
            symbolTable,
            lastRuntimeVisibleAnnotation,
            lastRuntimeInvisibleAnnotation,
            lastRuntimeVisibleTypeAnnotation,
            lastRuntimeInvisibleTypeAnnotation,
            output);
    if (firstAttribute != null) {
      firstAttribute.putAttributes(symbolTable, output);
    }
  }

  /**
   * Collects the attributes of this record component into the given set of attribute prototypes.
   *
   * @param attributePrototypes a set of attribute prototypes.
   */
  final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
    attributePrototypes.addAttributes(firstAttribute);
  }
}
