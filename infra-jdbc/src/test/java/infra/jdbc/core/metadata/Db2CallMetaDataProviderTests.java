/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc.core.metadata;

import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class Db2CallMetaDataProviderTests {

  private final DatabaseMetaData metadata = mock();

  @Test
  void usesUppercaseUserForDefaultSchemaAndDriverCaseForExplicitNames() throws SQLException {
    given(metadata.getUserName()).willReturn("db2User");
    given(metadata.supportsCatalogsInProcedureCalls()).willReturn(true);
    given(metadata.supportsSchemasInProcedureCalls()).willReturn(true);
    given(metadata.storesLowerCaseIdentifiers()).willReturn(true);
    var provider = new Db2CallMetaDataProvider(metadata);
    provider.initializeWithMetaData(metadata);

    assertThat(provider.metaDataSchemaNameToUse(null)).isEqualTo("DB2USER");
    assertThat(provider.metaDataSchemaNameToUse("OtherSchema")).isEqualTo("otherschema");
    assertThat(provider.metaDataCatalogNameToUse("MyCatalog")).isEqualTo("mycatalog");
  }

  @Test
  void retainsKnownCapabilitiesWhenDriverCannotReadMetadata() throws SQLException {
    var provider = new Db2CallMetaDataProvider(metadata);
    given(metadata.supportsCatalogsInProcedureCalls()).willReturn(false);
    given(metadata.supportsSchemasInProcedureCalls()).willReturn(true);
    given(metadata.storesUpperCaseIdentifiers()).willReturn(false);
    given(metadata.storesLowerCaseIdentifiers()).willReturn(false);
    provider.initializeWithMetaData(metadata);
    var failure = new SQLException("metadata unavailable");
    given(metadata.supportsCatalogsInProcedureCalls()).willThrow(failure);
    given(metadata.supportsSchemasInProcedureCalls()).willThrow(failure);
    given(metadata.storesUpperCaseIdentifiers()).willThrow(failure);
    given(metadata.storesLowerCaseIdentifiers()).willThrow(failure);

    provider.initializeWithMetaData(metadata);

    assertThat(provider.metaDataSchemaNameToUse(null)).isNull();
    assertThat(provider.metaDataSchemaNameToUse("MixedCase")).isEqualTo("MixedCase");
    assertThat(provider.metaDataCatalogNameToUse("ignored")).isNull();
  }

}
