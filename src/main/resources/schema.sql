-- Drop existing tables to recreate with new schema
-- Drop all tables in dependency order
DROP TABLE IF EXISTS task_dependencies CASCADE;
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS goals CASCADE;
DROP TABLE IF EXISTS simple_templates CASCADE;

-- Create tables for the agentic workflow engine with dependency support

CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query TEXT NOT NULL,
    summary TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    result TEXT,
    status VARCHAR(20) NOT NULL,
    blocking_dependencies UUID[] DEFAULT '{}',
    informational_dependencies UUID[] DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE task_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    depends_on_task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    dependency_type VARCHAR(20) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(task_id, depends_on_task_id)
);

-- Create indexes for better performance
CREATE INDEX idx_tasks_goal_id ON tasks(goal_id);
CREATE INDEX idx_goals_status ON goals(status);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_goals_created_at ON goals(created_at);
CREATE INDEX idx_tasks_created_at ON tasks(created_at);
CREATE INDEX idx_tasks_blocking_dependencies ON tasks USING GIN(blocking_dependencies);
CREATE INDEX idx_tasks_informational_dependencies ON tasks USING GIN(informational_dependencies);
CREATE INDEX idx_task_dependencies_task_id ON task_dependencies(task_id);
CREATE INDEX idx_task_dependencies_depends_on ON task_dependencies(depends_on_task_id);
CREATE INDEX idx_task_dependencies_type ON task_dependencies(dependency_type);

-- Phase 1: Simple template table without complex data types
CREATE TABLE IF NOT EXISTS simple_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    prompt_template TEXT NOT NULL,
    author VARCHAR(255),
    is_public BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);