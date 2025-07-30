# Database Migration Guide - Story 4

## Overview
Story 4 refactored the database schema management to use **Flyway** for proper database migrations instead of manual schema files.

## Migration Structure

### V1: Initial Schema
- **File**: `src/main/resources/db/migration/V1__initial_schema.sql`
- **Purpose**: Creates core workflow tables (goals, tasks, task_dependencies, templates)
- **Applied**: On fresh database or when baseline-on-migrate=true

### V2: Parameter Schema (Story 4)
- **File**: `src/main/resources/db/migration/V2__add_parameter_schema.sql`
- **Purpose**: Adds parameter storage tables (template_parameters, parameter_metadata, parameter_validation_rules)
- **Features**:
  - Template versioning with `updated_at` triggers
  - Comprehensive parameter metadata storage
  - JSONB validation rules storage
  - Performance indexes

### V3: Trip Planner Data Migration
- **File**: `src/main/resources/db/migration/V3__migrate_trip_planner_parameters.sql`
- **Purpose**: Migrates hardcoded Trip Planner parameters to database
- **Creates**:
  - "Simple Trip Planner" template
  - 5 parameters with metadata and validation rules
  - Proper parameter ordering and grouping

## How to Apply Migrations

### Using Docker Compose (Recommended)
```bash
# 1. Start PostgreSQL with Docker Compose
docker-compose up -d postgres

# 2. Wait for database to be ready
docker-compose logs postgres

# 3. Run application - Flyway will automatically apply all migrations
mvn clean install
./mvnw spring-boot:run
```

### Fresh Database Setup (Manual)
```bash
# 1. Start Docker PostgreSQL
docker-compose up -d postgres

# 2. For fresh start, clean the database
docker-compose down -v  # Removes volumes
docker-compose up -d postgres

# 3. Run application - all migrations will be applied
./mvnw spring-boot:run
```

### Existing Database Migration
```bash
# 1. Backup existing data (if needed)
docker exec agentic-workflow-postgres pg_dump -U postgres agentic_workflow > backup.sql

# 2. For clean migration, restart with fresh volumes
docker-compose down -v
docker-compose up -d postgres

# 3. Run application - all migrations will be applied
./mvnw spring-boot:run
```

### Manual Flyway Commands
```bash
# Check migration status
./mvnw flyway:info

# Apply pending migrations
./mvnw flyway:migrate

# Validate applied migrations
./mvnw flyway:validate

# Clean database (development only!)
./mvnw flyway:clean
```

## Configuration

### Production (`application.yaml`)
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
    clean-disabled: true  # Safety: no clean in production
```

### Test (`application-test.yaml`)
```yaml
spring:
  # Uses same Docker PostgreSQL database as development
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/agentic_workflow}
    username: ${DATABASE_USERNAME:postgres}
    password: ${DATABASE_PASSWORD:password}
  flyway:
    enabled: true
    clean-disabled: false  # Allow clean in tests for fresh state
```

## Verification

### 1. Check Flyway Schema History
```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### 2. Verify Parameter Tables
```sql
-- Check templates
SELECT id, name, version FROM templates;

-- Check parameters
SELECT tp.name, tp.type, tp.required, tp.display_order 
FROM template_parameters tp 
JOIN templates t ON tp.template_id = t.id 
WHERE t.name = 'Simple Trip Planner'
ORDER BY tp.display_order;

-- Check metadata
SELECT pm.placeholder, pm.help_text, pm.display_group
FROM parameter_metadata pm
JOIN template_parameters tp ON pm.parameter_id = tp.id
JOIN templates t ON tp.template_id = t.id
WHERE t.name = 'Simple Trip Planner';
```

### 3. Run Test Script
```bash
./scripts/local/test-basic-api.sh
```

The test script will verify:
- ✅ Story 4 database parameter storage
- ✅ Parameters loaded from database (not hardcoded)
- ✅ Metadata generated and stored
- ✅ Validation rules persisted

## Benefits of Flyway Migration

1. **Version Control**: Database schema changes are versioned alongside code
2. **Automatic Application**: Migrations run automatically on startup
3. **Rollback Safety**: Track which migrations have been applied
4. **Team Consistency**: Everyone gets the same database structure
5. **Production Safety**: Controlled, repeatable deployments
6. **Audit Trail**: Complete history of database changes

## Troubleshooting

### Migration Checksum Mismatch
```bash
# If you modified a migration file after it was applied
./mvnw flyway:repair
```

### Start Fresh (Development Only)
```bash
# Clean all data and reapply migrations
./mvnw flyway:clean
./mvnw flyway:migrate
```

### Manual Baseline (Existing Database)
```bash
# Mark current state as baseline
./mvnw flyway:baseline
./mvnw flyway:migrate
```