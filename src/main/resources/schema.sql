-- Drop existing tables to recreate with new schema
DROP TABLE IF EXISTS task_dependencies;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS goals;

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

-- Workflow templates tables
CREATE TABLE workflow_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    prompt_template TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    tags TEXT[] DEFAULT '{}',
    author VARCHAR(255),
    version INT DEFAULT 1,
    is_public BOOLEAN DEFAULT true,
    usage_count INT DEFAULT 0,
    rating DECIMAL(3,2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE template_parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES workflow_templates(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    required BOOLEAN DEFAULT false,
    default_value JSONB,
    allowed_values JSONB,
    validation JSONB,
    placeholder TEXT,
    order_index INT NOT NULL,
    UNIQUE(template_id, name)
);

CREATE TABLE template_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES workflow_templates(id),
    goal_id UUID NOT NULL REFERENCES goals(id),
    parameters JSONB NOT NULL,
    user_id VARCHAR(255),
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Template indexes
CREATE INDEX idx_workflow_templates_category ON workflow_templates(category);
CREATE INDEX idx_workflow_templates_is_public ON workflow_templates(is_public);
CREATE INDEX idx_workflow_templates_tags ON workflow_templates USING GIN(tags);
CREATE INDEX idx_workflow_templates_created_at ON workflow_templates(created_at);
CREATE INDEX idx_template_parameters_template_id ON template_parameters(template_id);
CREATE INDEX idx_template_executions_template_id ON template_executions(template_id);
CREATE INDEX idx_template_executions_goal_id ON template_executions(goal_id);
CREATE INDEX idx_template_executions_user_id ON template_executions(user_id);