# ShEx Integration Implementation Plan

## Overview
This plan provides a step-by-step approach to implement Shape Expression Language (ShEx) support in the Shactor project, with emphasis on debugging, testing, and incremental development.

## Phase 1: Setup and Foundation
### 1.2 Create Base Interface Structure
- [ ] **Create ShapeFormatter interface**
  - **File**: `src/main/java/shactor/utils/formatters/ShapeFormatter.java`
  - **Test**: Compile project with `mvn compile`
  - **Debug**: Check for compilation errors

### 1.3 Unit Test Setup
- [ ] **Create test structure**
  ```bash
  mkdir -p src/test/java/shactor/utils/formatters
  ```
  - **Create**: `ShapeFormatterTest.java`
  - **Verification**: Run `mvn test` (should pass with empty tests)

## 1: SHACL Formatter Implementation (Days 2-3)

### 2.1 Extract Existing Logic
- [ ] **Create ShaclFormatter class**
  - **Extract**: Logic from `Utils.constructModelForGivenNodeShapesAndTheirPropertyShapes()`
  - **Test**: Create unit test with sample NS/PS data
  - **Debug**: Compare output with original method

### 2.2 Verification Steps
- [ ] **Test with real data**
  ```java
  @Test
  public void testShaclFormatterWithRealData() {
      // Use actual NS/PS objects from existing tests
  }
  ```
  - **Command**: `mvn test -Dtest=ShaclFormatterTest`
  - **Debug**: Log input/output for comparison

## Phase 3: ShEx Formatter Implementation (Days 3-5)

### 3.1 Basic ShEx Structure
- [ ] **Create ShExFormatter class**
  - **Start**: Simple node shape generation
  - **Test**: Unit test for basic structure
  - **Debug**: Print generated ShEx syntax

### 3.2 Property Constraints
- [ ] **Implement property handling**
  - **Features**: Basic datatypes, IRI references
  - **Test**: Test each constraint type separately
  - **Debug**: Validate ShEx syntax with online tools

### 3.3 OR-List Support
- [ ] **Implement complex constraints**
  - **Features**: Handle ShaclOrListItem structures
  - **Test**: Create test cases for OR constraints
  - **Debug**: Manual ShEx validation

## Phase 4: Integration and Testing (Days 5-7)

### 4.1 Factory Pattern Implementation
- [ ] **Create ShapeFormatterFactory**
  - **Test**: Unit tests for factory methods
  - **Debug**: Test unknown format handling

### 4.2 Utils Class Integration
- [ ] **Modify Utils.constructModel... method**
  - **Add**: Format parameter support
  - **Test**: Integration tests with both formats
  - **Debug**: Compare outputs side-by-side

### 4.3 End-to-End Testing
- [ ] **Test complete pipeline**
  ```bash
  # Start application and test manually
  mvn spring-boot:run
  ```
  - **Verification**: Generate both SHACL and ShEx outputs
  - **Debug**: Check file contents and formats

## Phase 5: GUI Integration (Days 7-9)

### 5.1 Format Selection UI
- [ ] **Add format selection to IndexView**
  - **Component**: RadioButtonGroup for format selection
  - **Test**: UI component tests
  - **Debug**: Check value propagation

### 5.2 Parameter Passing
- [ ] **Implement format parameter flow**
  - **Path**: IndexView → SelectionView → Utils
  - **Test**: Integration tests for parameter flow
  - **Debug**: Add logging at each step

## Debugging and Validation Guidelines

### Testing Strategy
1. **Unit Tests First**: Test each component in isolation
2. **Integration Tests**: Test component interactions
3. **Manual Testing**: Verify UI and file outputs
4. **Regression Testing**: Ensure SHACL functionality unchanged

### Debugging Tools
```bash
# Enable debug logging
export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
mvn spring-boot:run

# Test specific components
mvn test -Dtest=ShExFormatterTest -Dspring.profiles.active=test
```

### Validation Commands
```bash
# Compile and test
mvn clean compile test

# Run application
mvn spring-boot:run

# Check generated files
ls -la target/classes/
```

## Best Practices

### Code Quality
- [ ] **Follow existing code style**
- [ ] **Add comprehensive JavaDoc**
- [ ] **Use meaningful variable names**
- [ ] **Handle edge cases and null values**

### Error Handling
- [ ] **Add try-catch blocks for file operations**
- [ ] **Validate input parameters**
- [ ] **Provide meaningful error messages**

### Performance
- [ ] **Profile memory usage with large datasets**
- [ ] **Optimize string concatenation in ShEx generation**
- [ ] **Test with various dataset sizes**

## Success Criteria

### Functional Requirements
- [ ] Generate valid ShEx syntax
- [ ] Maintain existing SHACL functionality
- [ ] Support all constraint types
- [ ] Handle OR-lists correctly

### Quality Requirements
- [ ] 90%+ test coverage for new code
- [ ] No performance regression
- [ ] Clean, maintainable code
- [ ] Comprehensive documentation

## Rollback Plan
If issues arise:
1. **Revert to backup**: Use created backup
2. **Feature flag**: Disable ShEx temporarily
3. **Gradual rollout**: Test with subset of users

## Timeline Summary
- **Days 1-2**: Setup and foundation
- **Days 3-5**: Core implementation
- **Days 6-7**: Integration testing
- **Days 8-9**: GUI and final testing
- **Day 10**: Documentation and deployment

## Next Steps After Completion
1. **Performance optimization**
2. **Additional ShEx features**
3. **User feedback integration**
4. **Documentation updates**