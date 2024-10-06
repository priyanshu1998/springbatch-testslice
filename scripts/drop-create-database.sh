#!/bin/bash
set -e

# This script is used to drop and recreate the meta-data tables
sh -c "export PGPASSWORD='mysecretpassword' && psql -h localhost -U postgres -d postgres -f src/sql/schema-drop-postgresql.sql"
sh -c "export PGPASSWORD='mysecretpassword' && psql -h localhost -U postgres -d postgres  -f src/sql/schema-postgresql.sql"