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
package infra.bytecode.signature;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import infra.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SignatureReader tests.
 *
 * @author Eric Bruneton
 */
public class SignatureReaderTest extends AsmTest {

  @ParameterizedTest
  @MethodSource({
          "infra.bytecode.signature.SignaturesProviders#classSignatures",
          "infra.bytecode.signature.SignaturesProviders#methodSignatures"
  })
  public void testAccept_validClassOrMethodSignature(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    SignatureVisitor signatureVisitor =
            new SignatureVisitor() { };

    Executable acceptVisitor = () -> signatureReader.accept(signatureVisitor);

    assertDoesNotThrow(acceptVisitor);
  }

  @ParameterizedTest
  @MethodSource("infra.bytecode.signature.SignaturesProviders#fieldSignatures")
  public void testAccept_validFieldSignature(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    SignatureVisitor signatureVisitor =
            new SignatureVisitor() { };

    Executable acceptVisitor = () -> signatureReader.acceptType(signatureVisitor);

    assertDoesNotThrow(acceptVisitor);
  }

  @Test
  public void testAccept_invalidSignature() {
    String invalidSignature = "-";
    SignatureReader signatureReader = new SignatureReader(invalidSignature);
    SignatureVisitor signatureVisitor =
            new SignatureVisitor() { };

    Executable acceptVisitor = () -> signatureReader.accept(signatureVisitor);

    assertThrows(IllegalArgumentException.class, acceptVisitor);
  }
}
