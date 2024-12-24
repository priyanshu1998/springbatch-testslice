#!/bin/bash
set -e

# This script is used to drop and recreate the meta-data tables
sh -c "export PGPASSWORD='default1' && psql -h localhost -U postgres -d postgres -f src/sql/schema-drop-postgresql.sql"
sh -c "export PGPASSWORD='default1' && psql -h localhost -U postgres -d postgres  -f src/sql/schema-postgresql.sql"