# Custom Claude Code Agents for Ways of Working Enforcement

**Status:** In Review  
**Created:** 2025-07-29  
**Author:** Claude Code Session  
**Epic:** Development Process Improvement  

## Problem Statement

Our current development process relies on manual adherence to the Ways of Working defined in CLAUDE.md. While comprehensive, this process is prone to human error and inconsistent execution, leading to:

- Incomplete story implementations missing acceptance criteria
- Inconsistent commit message formats and GitHub issue management
- Test coverage gaps and incomplete Definition of Done checking
- Manual story refinement sessions that could be systematized
- Inconsistent branch naming and PR management

## Proposed Solution

Leverage Claude Code's custom agent configuration to create specialized agents that enforce our Ways of Working through automated guidance and validation.

## Agent Architecture

### 1. **Story Management Agent** (`story-manager`)
**Purpose:** Guides story implementation from start to completion

**Capabilities:**
- Validates story acceptance criteria before starting implementation
- Ensures proper branch naming (`feature/story-{number}-{description}`)
- Guides implementation according to Definition of Done checklist
- Validates test coverage requirements (70%+ instruction, 60%+ branch)
- Ensures proper commit message format with Claude Code attribution

**Configuration:**
```json
{
  "name": "story-manager",
  "description": "Guides story implementation according to Ways of Working",
  "system_prompt": "You are a story management specialist. Always validate acceptance criteria, ensure proper branch naming, guide Definition of Done completion, and enforce test coverage requirements before marking stories complete.",
  "auto_invoke": ["story", "feature", "implement", "acceptance criteria"]
}
```

### 2. **Git & PR Enforcement Agent** (`git-enforcer`)
**Purpose:** Ensures consistent git practices and PR management

**Capabilities:**
- Validates commit messages follow conventional format
- Ensures Claude Code attribution in commits
- Guides proper PR creation with detailed descriptions
- Validates branch protection rules compliance
- Ensures PR merges only to `develop` branch (not `main`)

**Configuration:**
```json
{
  "name": "git-enforcer", 
  "description": "Enforces git and PR best practices",
  "system_prompt": "You are a git workflow specialist. Always ensure proper commit message format, Claude Code attribution, correct branch targeting, and comprehensive PR descriptions that link to GitHub issues.",
  "auto_invoke": ["commit", "pull request", "merge", "branch"]
}
```

### 3. **Story Refinement Agent** (`story-refiner`)
**Purpose:** Conducts systematic story refinement sessions

**Capabilities:**
- Reviews completed story impact on upcoming stories
- Identifies necessary GitHub issue updates
- Suggests architecture changes affecting future stories
- Updates acceptance criteria based on new learnings
- Ensures story alignment with current system state

**Configuration:**
```json
{
  "name": "story-refiner",
  "description": "Conducts story refinement sessions after story completion",
  "system_prompt": "You are a story refinement specialist. After each story completion, systematically review impact on upcoming stories, identify GitHub issue updates needed, and ensure future stories align with current system architecture.",
  "auto_invoke": ["story complete", "refinement", "epic review"]
}
```

### 4. **Testing & Coverage Agent** (`test-guardian`)
**Purpose:** Ensures comprehensive testing and coverage

**Capabilities:**
- Validates test coverage meets 70%/60% thresholds
- Identifies untested code paths and missing test classes
- Ensures integration tests cover new functionality
- Validates script updates for new features
- Guides test-driven development practices

**Configuration:**
```json
{
  "name": "test-guardian",
  "description": "Ensures comprehensive test coverage and quality",
  "system_prompt": "You are a testing specialist. Always validate test coverage meets thresholds, identify missing test classes, ensure scripts test new features, and guide TDD practices.",
  "auto_invoke": ["test", "coverage", "mvnw test", "junit"]
}
```

### 5. **Documentation Guardian** (`doc-guardian`)
**Purpose:** Maintains consistent documentation and API specs

**Capabilities:**
- Updates README.md for new features and API changes
- Ensures CLAUDE.md stays current with process improvements
- Validates API documentation matches implementation
- Updates script documentation for new functionality
- Ensures architectural diagrams reflect current state

**Configuration:**
```json
{
  "name": "doc-guardian",
  "description": "Maintains comprehensive and current documentation",
  "system_prompt": "You are a documentation specialist. Always ensure README.md, CLAUDE.md, API docs, and architectural diagrams stay current with implementation changes.",
  "auto_invoke": ["README", "documentation", "API", "endpoint"]
}
```

## Implementation Plan

### Phase 1: Agent Creation (1-2 hours)
1. Create agent configuration files in `.claude/agents/`
2. Define system prompts and auto-invoke patterns
3. Test agent activation and behavior
4. Refine prompts based on initial testing

### Phase 2: Integration Testing (1 hour)
1. Test agents during a complete story implementation cycle
2. Validate agent coordination and handoffs
3. Ensure agents don't conflict or over-activate
4. Fine-tune auto-invoke patterns

### Phase 3: Documentation & Training (30 minutes)
1. Update CLAUDE.md with agent usage guidelines
2. Document agent responsibilities and when to invoke
3. Create troubleshooting guide for agent conflicts
4. Add agent management to Definition of Done

## Expected Benefits

### 🎯 **Consistency**
- Standardized story implementation process
- Consistent git practices and commit messages
- Uniform PR and issue management

### ⚡ **Efficiency**
- Automated guidance reduces manual process checking
- Proactive validation catches issues early
- Systematic refinement sessions improve planning

### 📈 **Quality**
- Enforced test coverage thresholds
- Comprehensive Definition of Done validation
- Up-to-date documentation and API specs

### 🧠 **Learning**
- New team members guided through proper processes
- Institutional knowledge encoded in agent behavior
- Continuous process improvement through agent refinement

## Technical Considerations

### Agent Coordination
- **Hierarchy:** `story-manager` leads, others support specific areas
- **Handoffs:** Clear triggers for when agents activate
- **Conflict Resolution:** Priority system for overlapping responsibilities

### Customization
- Agent prompts can be refined based on team feedback
- Auto-invoke patterns adjustable for different project phases
- System prompts can incorporate project-specific guidelines

### Monitoring
- Track agent activation frequency and effectiveness
- Monitor for over-activation or user frustration
- Collect feedback for prompt improvements

## Success Metrics

### Process Adherence
- 100% of stories follow Definition of Done checklist
- 95%+ proper commit message format compliance
- Zero direct pushes to `main` branch

### Quality Metrics
- Maintain 70%+ test coverage consistently
- All new features include updated script testing
- Documentation stays current with implementation

### Efficiency Gains
- Reduced time for story refinement sessions
- Faster onboarding for new team members
- Fewer process-related rework cycles

## Risk Mitigation

### Over-Automation Risk
- **Risk:** Agents become annoying or overly restrictive
- **Mitigation:** Careful tuning of auto-invoke patterns, user feedback loops

### Agent Conflicts
- **Risk:** Multiple agents activating simultaneously
- **Mitigation:** Clear responsibility boundaries, hierarchical activation

### Process Rigidity
- **Risk:** Agents enforce outdated or suboptimal processes
- **Mitigation:** Regular agent prompt reviews, easy customization

## Next Steps

1. **Approval:** Review and approve this proposal
2. **Prototype:** Create initial agent configurations
3. **Test:** Validate agents during next story implementation
4. **Iterate:** Refine based on real-world usage
5. **Document:** Update Ways of Working with agent integration

## Conclusion

Custom Claude Code agents represent a significant opportunity to systematize and improve our development process. By encoding our Ways of Working into intelligent agents, we can ensure consistent, high-quality delivery while reducing manual overhead and improving team efficiency.

The proposed agent architecture provides comprehensive coverage of our development lifecycle while maintaining flexibility for future refinement and adaptation.