# SHACTOR Setup Guide

Quick guide to set up the SHACTOR project on your local machine.

## 🐳 Quick Start with Docker (Recommended)

**The easiest way to get started is using our pre-built Docker image:**

```bash
docker pull dleidenfrost/shactor-app:latest

docker run -d \
  --name shactor \
  -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPARQL_ENDPOINT_URL=http://host.docker.internal:7200/ \
  -e SPARQL_REPOSITORY=LUBM-ScaleFactor-1 \
  dleidenfrost/shactor-app:latest
```

**For detailed Docker setup instructions, see [DOCKER.md](DOCKER.md).**

---

## Manual Setup

If you prefer to run SHACTOR without Docker, follow these steps:

## What you need

- **Java 11+** and **Maven 3.6+**
- **GraphDB** (for the SPARQL database)
- **Git**

## Setup in 5 steps

### 1. Clone the project
```bash
git clone <repository-url>
cd demo-shactor
```

### 2. Configure settings

**Easiest method:**
```bash
# Copy example configuration
cp application.properties.example src/main/resources/application.properties
```

Then open `src/main/resources/application.properties` and enter your local paths:

```properties
# Enter your dataset paths here
dataset.lubm.mini.path=/your/path/to/lubm-mini.nt
dataset.dbpedia.path=/your/path/to/dbpedia_ml.nt

# SPARQL endpoint (usually localhost)
sparql.endpoint.default.url=http://localhost:7200/
sparql.endpoint.default.repository=LUBM-ScaleFactor-1
```

### 3. Get the datasets

Create a `datasets/` folder and obtain the files:
- `lubm-mini.nt` (for testing)
- `dbpedia_ml.nt`, `lubm.n3`, `yago.n3` (for full functionality)

**Note:** Ask your supervisor for the dataset files.

### 4. Set up GraphDB

1. [Download GraphDB](https://www.ontotext.com/products/graphdb/) and install it
2. Start GraphDB → `http://localhost:7200`
3. Create repositories:
   - `LUBM-ScaleFactor-1`
   - `DBPEDIA_ML`
   - `LUBM`
   - `Yago_EngWiki`
4. Import the datasets into the corresponding repositories

### 5. Run the project

```bash
mvn clean compile
mvn spring-boot:run
```

→ Open `http://localhost:8080`

## Common issues

| Problem | Solution |
|---------|----------|
| "application.properties not found" | Repeat step 2 — copy the file |
| "Dataset file not found" | Check paths in the configuration |
| "Cannot connect to SPARQL endpoint" | Is GraphDB running? Repository names correct? |
| Build error | Run `mvn clean install` |

## Configuration in detail

The most important settings in `application.properties`:

```properties
# Dataset paths (adjust!)
dataset.lubm.mini.path=${LUBM_MINI_PATH:./datasets/lubm-mini.nt}
dataset.dbpedia.path=${DBPEDIA_PATH:./datasets/dbpedia_ml.nt}

# SPARQL endpoints
sparql.endpoint.default.url=${SPARQL_ENDPOINT_URL:http://localhost:7200/}
sparql.endpoint.remote.url=${SPARQL_REMOTE_URL:http://10.92.0.34:7200/}

# Repository names (must match GraphDB)
repository.lubm.mini=${REPO_LUBM_MINI:LUBM-ScaleFactor-1}
repository.dbpedia=${REPO_DBPEDIA:DBPEDIA_ML}
```

## Alternative: environment variables

Instead of editing `application.properties`, you can set environment variables:

```bash
export LUBM_MINI_PATH="/your/path/to/lubm-mini.nt"
export SPARQL_ENDPOINT_URL="http://localhost:7200/"
export SPARQL_REPOSITORY="LUBM-ScaleFactor-1"
```

## Why this configuration?

**Before:** Every developer had to change hardcoded paths in the source code  
**Now:** Just adjust a single configuration file → much easier!

## First steps after setup

1. Test with the LUBM-Mini dataset
2. Check the SPARQL connection
3. Try shape extraction

If you run into problems: check the logs or contact the team! 🚀