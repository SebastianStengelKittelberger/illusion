# Docker Setup

Full-stack Docker Compose for the illusion / moonlight / summerlight / Elasticsearch stack.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or Docker Engine + Compose plugin)

## Ports

| Service        | Port |
|----------------|------|
| Elasticsearch  | 9200 |
| illusion       | 8079 |
| moonlight      | 8078 |
| summerlight    | 3000 |

## Commands

### Start the full stack (builds images first run)
```bash
cd /Users/sebastianstengel/work/illusion
docker compose up --build
```

### Start in the background
```bash
docker compose up --build -d
```

### Stop (keeps Elasticsearch data volume)
```bash
docker compose down
```

### Stop and wipe all data (removes the `es_data` volume)
```bash
docker compose down -v
```

### Rebuild a single service
```bash
docker compose build illusion
docker compose up -d illusion
```

## First-time data import

After the stack is running, import product data via **bosch.adapter** (run it locally against the containerised Elasticsearch):

```bash
# Point the adapter at the Docker ES instance and run it
# (exact command depends on your bosch.adapter configuration)
ELASTICSEARCH_HOST=localhost ELASTICSEARCH_PORT=9200 \
  java -jar bosch.adapter.jar
```

Once imported, illusion reads from the `bosch-products-*` and `bosch-references-*` indices automatically.

## ⚠️ Summerlight API URL note

Summerlight's API base URLs are **hardcoded** to `localhost` in `src/api/client.ts`:

```ts
const api = axios.create({ baseURL: 'http://localhost:8079/illusion' });
const moonlightApi = axios.create({ baseURL: 'http://localhost:8078/moonlight' });
```

This means:

- **When running locally** (`npm run dev`) with the backends in Docker Compose → works perfectly, because the browser connects to `localhost:8079` and `localhost:8078` which are forwarded by Docker.
- **When summerlight itself runs in Docker** → fails, because `localhost` inside the container resolves to the container itself, not the host.

### Recommended approach (option A — easiest)

Run only the backends in Docker and start summerlight locally:

```bash
# Start backends only
docker compose up elasticsearch illusion moonlight

# In a separate terminal
cd summerlight && npm run dev
```

### Alternative (option B — fully containerised)

Pass the API URLs as Vite build-time environment variables. You would need to:

1. Add `VITE_ILLUSION_URL` / `VITE_MOONLIGHT_URL` env var support to `src/api/client.ts`
2. Pass them as Docker build args in `docker-compose.yml`
3. Rebuild: `docker compose build --build-arg VITE_ILLUSION_URL=http://illusion:8079/illusion summerlight`
