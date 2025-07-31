# FairTA - Fair Task Allocation

A Kotlin-based optimization tool for solving fair task allocation problems using CPLEX optimization solver. This project implements algorithms to allocate tasks from multiple sources to targets while ensuring fairness constraints.

## Research Context

This project serves as an implementation and experimental platform for the research paper on **ε-fairness models** in multi-agent task assignment problems. The work extends the model of ε-fairness to a countably infinite family of convex models, providing theoretical and computational insights into fairness-constrained optimization.

### Key Research Contributions

- **ε-fairness extension**: Controls dispersion of decision variables via coefficient of variation
- **Convex model family**: Derived from first principles through classical equivalence of finite-dimensional norms
- **Single parameter control**: Governed by ε ∈ [0,1] where ε = 0 imposes no dispersion constraints and ε = 1 forces zero dispersion
- **Theoretical characterization**: Comprehensive analysis of model relationships, relative strength, and inclusion properties
- **Computational evaluation**: Comparison of different models in terms of computation time and dispersion control effectiveness

## Overview

FairTA is designed to solve task allocation problems where:
- Multiple sources (agents) need to allocate tasks to multiple targets
- Each target must be assigned to exactly one source
- Fairness constraints are enforced using p-norm convex cones (L2, L3, L5, L10, L∞)
- The solution balances efficiency with fairness across sources using ε-fairness parameter

## Features

- **ε-fairness implementation**: Controls dispersion via coefficient of variation
- **Multi-objective optimization**: Balances efficiency and fairness
- **Flexible p-norm constraints**: Supports any integral value of p norms and also infinity
- **Configurable fairness coefficient**: Adjustable ε parameter (0.0 to 1.0)
- **Theoretical model validation**: Implements convex models from first principles
- **CPLEX integration**: Uses IBM CPLEX for high-performance optimization
- **JSON output**: Results are saved in structured JSON format
- **Command-line interface**: Easy-to-use CLI with various parameters
- **Computational evaluation**: Comprehensive testing framework for model comparison

## Prerequisites

- **Java 21** or higher
- **IBM CPLEX** optimization solver
- **Gradle** (included via wrapper)

## Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd FairTA
   ```

2. Configure CPLEX:
   - Install IBM CPLEX Studio
   - Update the CPLEX paths in `app/build.gradle.kts`:
     ```kotlin
     val cplexJarPath = "/path/to/your/CPLEX/cplex.jar"
     val cplexLibPath = "/path/to/your/CPLEX/lib"
     ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

## Usage

### Basic Usage

Run the application with default parameters:
```bash
./gradlew run
```

### Command Line Options

```bash
./gradlew run --args="[options]"
```

Available options:
- `-s, --numSource`: Number of sources/agents (default: 5)
- `-t, --numTarget`: Number of targets (default: 100)
- `-n, --instanceName`: Instance file name
- `-path, --instancePath`: Path to instance directory
- `-p, --pNorm`: P-norm value (default: "2") (can be any integer and also "inf")
- `-fc, --fairnessCoefficient`: ε-fairness coefficient (0.0 to 1.0) (default: 0.5)
- `-time, --timeLimit`: Time limit in seconds for CPLEX (default: 3600)
- `-r, --outputPath`: Output directory path

### Examples

Run with custom parameters:
```bash
./gradlew run --args="-s 5 -t 250 -p 2 -fc 0.7 -time 900"
```

Run with specific instance:
```bash
./gradlew run --args="-n instance_5_250_1.txt -path ./data/instances/instance_5_250/ -fc 0.5 -p inf"
```

### Research Experiments

The test script implements the experimental framework from the research paper:
```bash
chmod +x tests.sh
./tests.sh
```

This runs comprehensive experiments across:
- **ε-fairness coefficients**: 0.1, 0.3, 0.5, 0.7, 0.9, 0.99 (dispersion control levels)
- **P-norms**: 2, 3, 5, 10, inf (different convex model families)
- **Problem instances**: 1-50 (5 agents, 250 targets)
- **Time limits**: 900 seconds per instance

### Batch Testing

Use the provided test script to run multiple parameter combinations:
```bash
chmod +x tests.sh
./tests.sh
```

The test script runs combinations of:
- ε-fairness coefficients: 0.1, 0.3, 0.5, 0.7, 0.9, 0.99
- P-norms: 2, 3, 5, 10, inf
- Instances: 11-50 (5 agents, 250 targets)

## Project Structure

```
FairTA/
├── app/
│   ├── src/main/kotlin/fairTA/
│   │   ├── main/           # Main application logic
│   │   ├── solver/         # Optimization solver
│   │   └── data/           # Data models and DTOs
│   ├── data/
│   │   ├── instances/      # Problem instances
│   │   └── results/        # Output results
│   └── build.gradle.kts    # Build configuration
├── tests.sh                # Batch testing script
└── README.md              # This file
```

## Key Components

### Solver
The core optimization engine that:
- Formulates the task allocation problem as a mixed-integer program
- Implements ε-fairness constraints using p-norm convex cones.
- Uses lazy callbacks in CPLEX to outer approximate the p-norm cones.

### Controller
Manages the application flow:
- Parses command-line arguments
- Loads problem instances
- Coordinates solver execution
- Handles output generation

### Data Models
- `Instance`: Represents the problem data
- `Result`: Contains solution and metadata
- `Parameters`: Configuration settings

## Output

Results are saved as JSON files with the naming convention:
```
{instance_name}-p-{p_norm}-fc-{fairness_coefficient}.json
```

Example: `instance_5_250_1-p-2-fc-50.json`

The results include:
- **Solution quality**: Objective function values and feasibility
- **Computational performance**: Solve time and iterations
- **Fairness metrics**: Dispersion measures and coefficient of variation
- **Model comparison**: Performance across different p-norms and ε values

## Configuration

### CPLEX Settings
- Memory allocation: 22GB maximum
- Time limits: Configurable via command line
- Library path: Set in `build.gradle.kts`

### JVM Settings
- Initial heap: 32MB
- Maximum heap: 22GB
- Library path: Points to CPLEX installation

## Development

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
```

### Cleaning
```bash
./gradlew clean
./gradlew cleanLogs
```

## Dependencies

- **Kotlin**: Primary language
- **CPLEX**: IBM optimization solver for mixed-integer programming
- **Clikt**: Command-line interface
- **JGraphT**: Graph algorithms for task assignment modeling
- **Kotlinx Serialization**: JSON handling for results
- **Kotlin Logging**: Logging framework
- **Kotlinx Collections**: Immutable data structures
- **Kotlinx DateTime**: Temporal data handling

## Additional notes

CPLEX and other dependencies have been set up correctly in "build.gradle".
In case some dependencies need access to config files, the files can be placed
in "app/src/main/resources". This folder already holds "simplelogger.properties", the config
file for the "simplelogger" logging utility.
