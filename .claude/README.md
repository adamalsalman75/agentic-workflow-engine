# Claude Code Custom Agents

This directory contains custom agent configurations for enforcing Ways of Working in the agentic-workflow-engine project.

## Configured Agents

### 1. 🎯 story-manager
- **Purpose**: Guides story implementation according to Ways of Working
- **Triggers**: "story", "feature", "implement", "acceptance criteria"
- **Responsibilities**: 
  - Validates acceptance criteria
  - Ensures proper branch naming
  - Guides Definition of Done
  - Enforces test coverage

### 2. 🛡️ git-guardian  
- **Purpose**: Enforces git workflow rules and branch protections
- **Triggers**: "git push", "commit", "branch", "merge"
- **Responsibilities**:
  - Prevents direct pushes to main
  - Requires permission for develop pushes
  - Enforces branch naming conventions
  - Validates commit message format

### 3. 🔄 story-refinement
- **Purpose**: Conducts story refinement sessions after completion
- **Triggers**: "story complete", "refinement", "story done"
- **Responsibilities**:
  - Reviews completed work impact
  - Updates upcoming stories
  - Identifies necessary adjustments
  - Maintains backlog accuracy

### 4. ✅ test-guardian
- **Purpose**: Ensures comprehensive testing and quality standards
- **Triggers**: "test", "coverage", "junit", "write tests"
- **Responsibilities**:
  - Enforces 70% coverage minimum
  - Validates test quality
  - Ensures all test types are present
  - Checks test execution

### 5. 📚 documentation-keeper
- **Purpose**: Maintains comprehensive and accurate documentation
- **Triggers**: "document", "readme", "claude.md", "update docs"
- **Responsibilities**:
  - Keeps CLAUDE.md current
  - Updates README.md
  - Ensures code documentation
  - Validates PR descriptions

## Usage

These agents will automatically activate when their trigger words are detected in the conversation. They work behind the scenes to:

1. **Provide guided assistance** for complex workflows
2. **Prevent common mistakes** before they happen
3. **Enforce standards** consistently across all work
4. **Maintain quality** throughout the development process

## Configuration

The agents are configured as individual Markdown files in the `.claude/agents/` directory with YAML frontmatter:

```yaml
---
name: agent-name
description: Agent purpose
auto_invoke:
  - trigger1
  - trigger2
---
```

Each agent activates automatically when their trigger patterns are detected in the conversation.

## Benefits

✅ **Consistent Enforcement** - Rules applied uniformly
✅ **Proactive Guidance** - Prevents issues before they occur
✅ **Focused Expertise** - Each agent specializes in one area
✅ **Clean Context** - Main conversation stays uncluttered
✅ **Improved Velocity** - Fewer mistakes, faster delivery