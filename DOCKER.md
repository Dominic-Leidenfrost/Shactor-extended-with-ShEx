# SHACTOR Docker Setup Guide

This guide explains how to run the SHACTOR project using Docker and Docker Compose.

## Table of Contents

- [Quick Start with Pre-built Image](#quick-start-with-pre-built-image)
- [Docker Compose Setup](#docker-compose-setup)
- [Configuration Parameters](#configuration-parameters)
- [Troubleshooting](#troubleshooting)
- [Prerequisites](#prerequisites)

---

## Quick Start with Pre-built Image

The fastest way to get started is using our official Docker Hub image.

**Note:** The Docker image runs Vaadin in development mode to avoid licensing restrictions.

### Pull the Image

```bash
docker pull dleidenfrost/shactor-app:latest
```

### Basic Usage (with External GraphDB)

If you already have GraphDB running on your host machine at `http://localhost:7200`:

```bash
docker run -d \
  --name shactor \
  -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPARQL_ENDPOINT_URL=http://host.docker.internal:7200/ \
  -e SPARQL_REPOSITORY=LUBM-ScaleFactor-1 \
  dleidenfrost/shactor-app:latest
```

Access SHACTOR at **http://localhost:8080**

### Complete Setup with Datasets

To use SHACTOR with local datasets and GraphDB on your host machine:

```bash
docker run -d \
  --name shactor \
  -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPARQL_ENDPOINT_URL=http://host.docker.internal:7200/ \
  -e SPARQL_REPOSITORY=LUBM-ScaleFactor-1 \
  -e REPO_LUBM_MINI=LUBM-ScaleFactor-1 \
  -e REPO_DBPEDIA=DBPEDIA_ML \
  -e REPO_LUBM=LUBM \
  -e REPO_YAGO=Yago_EngWiki \
  -e POSTPROCESSING_ENABLED=true \
  -e PORT=8080 \
  -e DATASET_LUBM_MINI_PATH=/app/datasets/lubm-mini.nt \
  -e DATASET_DBPEDIA_PATH=/app/datasets/instance_types_en.nt \
  -v /Users/your-username/path/to/lubm-dataset/lubm-mini.nt:/app/datasets/lubm-mini.nt:ro \
  -v /Users/your-username/path/to/dbpedia-dataset/instance_types_en.nt:/app/datasets/instance_types_en.nt:ro \
  -v $(pwd)/evaluation:/app/evaluation \
  -v $(pwd)/libs/Output:/app/libs/Output \
  dleidenfrost/shactor-app:latest
```

**Important:** Replace `/Users/your-username/path/to/` with your actual dataset paths.

**Command Explanation:**

| Parameter | Description |
|-----------|-------------|
| `-d` | Run container in background (detached mode) |
| `--name shactor` | Name the container "shactor" for easy reference |
| `-p 8080:8080` | Map port 8080 from container to host |
| `--add-host=host.docker.internal:host-gateway` | Allow container to access services on host machine |
| `-e SPARQL_ENDPOINT_URL=...` | Set GraphDB endpoint URL to host machine |
| `-e SPARQL_REPOSITORY=...` | Set default repository name |
| `-e REPO_*=...` | Configure repository names for different datasets |
| `-e POSTPROCESSING_ENABLED=true` | Enable SHACL shape post-processing |
| `-e DATASET_*_PATH=...` | Tell SHACTOR where to find dataset files inside container |
| `-v /host/path:/container/path:ro` | Mount dataset files read-only into container |
| `-v $(pwd)/evaluation:/app/evaluation` | Mount evaluation directory for query results |
| `-v $(pwd)/libs/Output:/app/libs/Output` | Mount output directory for generated SHACL shapes |
| `dleidenfrost/shactor-app:latest` | The Docker image to run |

**Key Points:**
- The `--add-host` flag is critical for connecting to GraphDB on your host machine
- Dataset files must be mounted individually with their exact paths
- Environment variables `DATASET_*_PATH` must match the container paths in the `-v` mounts
- Output directories are mounted to persist generated results outside the container

---

## Docker Compose Setup

For a complete setup with both SHACTOR and GraphDB:

### Quick Start

```bash
docker-compose up -d
```

This will start both GraphDB and SHACTOR services.

### Access the Services

- **SHACTOR Application**: http://localhost:8080
- **GraphDB Workbench**: http://localhost:7200

### Stop Services

```bash
docker-compose down
```

---

## Configuration Parameters

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPARQL_ENDPOINT_URL` | GraphDB SPARQL endpoint URL | `http://graphdb:7200/` |
| `SPARQL_REPOSITORY` | Default SPARQL repository | `LUBM-ScaleFactor-1` |
| `REPO_LUBM_MINI` | LUBM Mini repository name | `LUBM-ScaleFactor-1` |
| `REPO_DBPEDIA` | DBpedia repository name | `DBPEDIA_ML` |
| `REPO_LUBM` | LUBM full repository name | `LUBM` |
| `REPO_YAGO` | YAGO repository name | `Yago_EngWiki` |
| `DATASET_LUBM_MINI_PATH` | Path to LUBM mini dataset | `/app/datasets/lubm-mini.nt` |
| `DATASET_DBPEDIA_PATH` | Path to DBpedia dataset | `/app/datasets/instance_types_en.nt` |
| `PORT` | Application HTTP port | `8080` |
| `POSTPROCESSING_ENABLED` | Enable SHACL post-processing | `true` |

### Volume Mounts

| Local Path | Container Path | Purpose |
|-----------|----------------|---------|
| Dataset files | `/app/datasets/` | RDF dataset files (read-only) |
| `./evaluation` | `/app/evaluation` | Evaluation queries and results |
| `./libs/Output` | `/app/libs/Output` | Generated SHACL shapes and logs |

---

## Troubleshooting

### SHACTOR cannot connect to GraphDB

**Problem:** "Cannot connect to SPARQL endpoint"

**Solutions:**
1. Check GraphDB is running: `curl http://localhost:7200/repositories`
2. Verify `--add-host=host.docker.internal:host-gateway` is set
3. Ensure repository names match exactly in GraphDB
4. Check logs: `docker logs shactor`

### Dataset files not found

**Problem:** "Preflight failed: file does not exist"

**Solutions:**
1. Verify dataset files exist on host
2. Check volume mounts are correct
3. Ensure `DATASET_*_PATH` environment variables are set
4. Mount individual files, not just directories:
   ```bash
   -v /path/to/lubm-mini.nt:/app/datasets/lubm-mini.nt:ro
   ```

### Port already in use

**Problem:** Port 8080 or 7200 already in use

**Solutions:**
1. Find what's using the port: `lsof -i :8080`
2. Stop the conflicting service
3. Or use a different port: `-p 9090:8080`

### Container exits immediately

**Solutions:**
1. Check logs: `docker logs shactor`
2. Verify all required environment variables are set
3. Ensure GraphDB is accessible

### Clean Restart

```bash
# Stop and remove container
docker stop shactor && docker rm shactor

# Pull latest image
docker pull dleidenfrost/shactor-app:latest

# Start fresh with your configuration
docker run -d ...
```

---

## Prerequisites

- Docker (version 20.10 or higher)
- Docker Compose (version 2.0 or higher, if using Docker Compose)
- At least 4GB of free RAM
- GraphDB running with repositories configured (if using standalone Docker)

Check your installation:
```bash
docker --version
docker-compose --version
```

---

## Initial GraphDB Setup

Before using SHACTOR, set up GraphDB repositories:

1. Open GraphDB at http://localhost:7200
2. Go to **Setup** → **Repositories** → **Create new repository**
3. Create repositories: `LUBM-ScaleFactor-1`, `DBPEDIA_ML`, `LUBM`, `Yago_EngWiki`
4. Import your datasets into the appropriate repositories

---

## Container Management

### View Logs
```bash
docker logs -f shactor
```

### Stop/Start Container
```bash
docker stop shactor
docker start shactor
docker restart shactor
```

### Remove Container
```bash
docker rm -f shactor
```

### Execute Commands in Container
```bash
docker exec -it shactor sh
```

---

## Building Locally (Optional)

To build the image yourself:

```bash
# Build the image
docker build -t shactor-local:latest .

# Run the locally built image
docker run -d \
  --name shactor \
  -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPARQL_ENDPOINT_URL=http://host.docker.internal:7200/ \
  -e SPARQL_REPOSITORY=LUBM-ScaleFactor-1 \
  shactor-local:latest
```

---

## Docker Compose Configuration

### Sample docker-compose.yml

```yaml
version: '3.8'

services:
  graphdb:
    image: ontotext/graphdb:10.8.9
    container_name: shactor-graphdb
    ports:
      - "7200:7200"
    volumes:
      - graphdb-data:/opt/graphdb/home
    environment:
      GDB_HEAP_SIZE: 2g

  shactor:
    image: dleidenfrost/shactor-app:latest
    container_name: shactor-app
    ports:
      - "8080:8080"
    environment:
      SPARQL_ENDPOINT_URL: http://graphdb:7200/
      SPARQL_REPOSITORY: LUBM-ScaleFactor-1
      REPO_LUBM_MINI: LUBM-ScaleFactor-1
      REPO_DBPEDIA: DBPEDIA_ML
      DATASET_LUBM_MINI_PATH: /app/datasets/lubm-mini.nt
      DATASET_DBPEDIA_PATH: /app/datasets/instance_types_en.nt
      POSTPROCESSING_ENABLED: "true"
    volumes:
      - /path/to/lubm-mini.nt:/app/datasets/lubm-mini.nt:ro
      - /path/to/instance_types_en.nt:/app/datasets/instance_types_en.nt:ro
      - ./evaluation:/app/evaluation
      - ./libs/Output:/app/libs/Output
    depends_on:
      - graphdb

volumes:
  graphdb-data:
```

### Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f shactor

# Rebuild and start
docker-compose up --build
```

---

## Additional Resources

- [README.md](README.md) - General information about SHACTOR
- [SETUP.md](SETUP.md) - Manual setup instructions
- [Docker Hub](https://hub.docker.com/r/dleidenfrost/shactor-app) - Official image
- [Research Paper](https://dl.acm.org/doi/10.1145/3555041.3589723) - SIGMOD 2023
