# Reachability metadata test repository

This fixture keeps the legacy split configuration files to verify that a local
file repository is copied into the executable jar without changing its format.

The schemas in `schemas/` are copied from the
[GraalVM Reachability Metadata repository](https://github.com/oracle/graalvm-reachability-metadata/tree/master/metadata/schemas),
which is distributed under CC0 1.0 Universal. Native Build Tools requires the
library/framework list schema major version 1 and library index schema major
version 2, even when the metadata itself uses the legacy format.
