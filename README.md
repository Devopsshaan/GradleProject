# 🛒 E-Commerce Microservices Platform

A production-ready microservices-based e-commerce platform built with Spring Boot and Gradle for learning DevOps practices.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green)
![Gradle](https://img.shields.io/badge/Gradle-8.5-blue)
![License](https://img.shields.io/badge/License-MIT-blue)

## 🏗️ Architecture

```
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │    (Port 8080)  │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
           ┌────────▼────────┐     ┌────────▼────────┐     ┌────────▼────────┐
           │ Product Service │     │  Order Service  │     │Inventory Service│
           │   (Port 8081)   │     │   (Port 8082)   │     │   (Port 8083)   │
           └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
                    │                        │                        │
                    └────────────────────────┼────────────────────────┘
                                             │
        ┌──────────────┬─────────────────────┼─────────────────────┬──────────────┐
        │              │                     │                     │              │
   ┌────▼────┐   ┌────▼────┐          ┌─────▼─────┐          ┌────▼────┐   ┌────▼────┐
   │PostgreSQL│   │  Redis  │          │   Kafka   │          │ Eureka  │   │Prometheus│
   └──────────┘   └─────────┘          └───────────┘          └─────────┘   └──────────┘
```

## 🚀 Microservices

| Service | Port | Description |
|---------|------|-------------|
| **Discovery Server** | 8761 | Eureka service registry |
| **API Gateway** | 8080 | Central entry point with routing, rate limiting |
| **Product Service** | 8081 | Product catalog management |
| **Order Service** | 8082 | Order processing and management |
| **Inventory Service** | 8083 | Stock management with reservations |

## 🎯 DevOps Features

### Built-in DevOps Capabilities
- ✅ **Gradle Multi-Project Build** - Efficient multi-module builds
- ✅ **Jib Container Builds** - Daemonless Docker builds
- ✅ **Service Discovery** - Eureka for dynamic service registration
- ✅ **API Gateway** - Spring Cloud Gateway with circuit breakers
- ✅ **Event-Driven Architecture** - Kafka for async communication
- ✅ **Distributed Caching** - Redis for caching and rate limiting
- ✅ **Health Checks** - Kubernetes-ready probes
- ✅ **Prometheus Metrics** - Custom business metrics
- ✅ **Circuit Breakers** - Resilience4j for fault tolerance
- ✅ **GitHub Actions CI/CD** - Complete pipeline
- ✅ **Docker Compose** - Full local environment

## 📁 Project Structure

```
ecommerce-platform/
├── api-gateway/           # API Gateway service
├── discovery-server/      # Eureka Discovery Server
├── product-service/       # Product catalog microservice
├── order-service/         # Order management microservice
├── inventory-service/     # Inventory management microservice
├── k8s/                   # Kubernetes manifests
│   ├── base/              # Base configurations
│   └── overlays/          # Environment-specific configs
├── helm/                  # Helm charts
├── monitoring/            # Prometheus & Grafana configs
├── scripts/               # Utility scripts
├── .github/workflows/     # CI/CD pipelines
├── build.gradle           # Root Gradle build file
├── settings.gradle        # Multi-project settings
├── docker-compose.yml     # Local development stack
└── gradle.properties      # Gradle properties
```

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Gradle 8.5+ (or use wrapper)
- Docker & Docker Compose
- (Optional) Kubernetes cluster

### Local Development

```bash
# Clone the repository
git clone <repository-url>
cd ecommerce-platform

# Build all services
./gradlew build

# Run tests with coverage
./gradlew testAll

# Start infrastructure only
docker-compose up -d postgres redis kafka zookeeper

# Run Discovery Server first
./gradlew :discovery-server:bootRun

# Run other services (in separate terminals)
./gradlew :api-gateway:bootRun
./gradlew :product-service:bootRun
./gradlew :order-service:bootRun
./gradlew :inventory-service:bootRun
```

### Docker Compose (Full Stack)

```bash
# Build and start everything
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop everything
docker-compose down -v
```

### Access Points

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Product Swagger | http://localhost:8081/swagger-ui.html |
| Order Swagger | http://localhost:8082/swagger-ui.html |
| Inventory Swagger | http://localhost:8083/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin123) |

## 📡 API Endpoints

### Through API Gateway (http://localhost:8080)

```bash
# Products
GET    /api/v1/products
POST   /api/v1/products
GET    /api/v1/products/{id}
GET    /api/v1/products/search?q=keyword

# Orders
GET    /api/v1/orders
POST   /api/v1/orders
GET    /api/v1/orders/{id}
PATCH  /api/v1/orders/{id}/status

# Inventory
GET    /api/v1/inventory/{sku}
GET    /api/v1/inventory/check?skus=SKU1,SKU2
POST   /api/v1/inventory/reserve
POST   /api/v1/inventory/confirm
```

## 🎓 DevOps Learning Activities

### Phase 1: Gradle & Building
```bash
# Clean build all modules
./gradlew clean build

# Build specific service
./gradlew :product-service:build

# Run tests with coverage
./gradlew testAll

# View dependency tree
./gradlew dependencies

# Build without tests (faster)
./gradlew build -x test
```

### Phase 2: Docker & Containerization
```bash
# Build images with Jib (no Docker daemon needed!)
./gradlew jibDockerBuild

# Build specific service
./gradlew :product-service:jibDockerBuild

# Push to registry
./gradlew jib -Djib.to.image=your-registry/product-service

# Run with Docker Compose
docker-compose up -d
docker-compose ps
docker-compose logs -f product-service
```

### Phase 3: Service Discovery & Gateway
```bash
# Start Eureka and watch services register
./gradlew :discovery-server:bootRun

# Open http://localhost:8761 and see services appear

# Test API Gateway routing
curl http://localhost:8080/api/v1/products
curl http://localhost:8080/api/v1/orders
```

### Phase 4: Inter-Service Communication
```bash
# Create a product
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"PROD-001","name":"Test Product","price":29.99,"category":"Electronics"}'

# Add inventory
curl -X POST http://localhost:8083/api/v1/inventory \
  -H "Content-Type: application/json" \
  -d '{"sku":"PROD-001","quantityOnHand":100}'

# Create order (triggers inventory reservation)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "customerEmail": "test@example.com",
    "items": [{"productId":1,"productSku":"PROD-001","productName":"Test","quantity":2,"unitPrice":29.99}],
    "shippingAddress": {"fullName":"John Doe","phone":"123456","addressLine1":"123 St","city":"NYC","state":"NY","postalCode":"10001","country":"USA"}
  }'
```

### Phase 5: Monitoring & Observability
```bash
# Check Prometheus metrics
curl http://localhost:8081/actuator/prometheus | grep products

# Useful Prometheus queries:
# - products_created_total
# - orders_created_total
# - inventory_reservations_created
# - http_server_requests_seconds_count

# Create Grafana dashboard for:
# - Request rate per service
# - Error rate
# - Response time percentiles
# - JVM memory usage
```

### Phase 6: Resilience & Circuit Breakers
```bash
# Stop inventory service
docker-compose stop inventory-service

# Try to create order - see circuit breaker activate
curl -X POST http://localhost:8080/api/v1/orders ...

# Check circuit breaker status
curl http://localhost:8080/actuator/health | jq '.components.circuitBreakers'

# Restart service - circuit breaker recovers
docker-compose start inventory-service
```

### Phase 7: Kafka & Event-Driven
```bash
# View Kafka topics
docker exec -it ecommerce-kafka kafka-topics --list --bootstrap-server localhost:9092

# Watch product events
docker exec -it ecommerce-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic product-events \
  --from-beginning

# Create product and see event
curl -X POST http://localhost:8080/api/v1/products ...
```

### Phase 8: Kubernetes Deployment
```bash
# Start Minikube
minikube start

# Build and load images
./gradlew jibDockerBuild
minikube image load ecommerce/product-service:latest

# Deploy
kubectl apply -k k8s/overlays/dev/

# Check status
kubectl get pods -n ecommerce
kubectl logs -f deployment/product-service -n ecommerce

# Port forward
kubectl port-forward svc/api-gateway 8080:8080 -n ecommerce
```

## 🛠️ Gradle Commands Reference

| Command | Description |
|---------|-------------|
| `./gradlew build` | Build all projects |
| `./gradlew clean` | Clean build directories |
| `./gradlew test` | Run all tests |
| `./gradlew testAll` | Run tests with aggregate coverage |
| `./gradlew jacocoRootReport` | Generate coverage report |
| `./gradlew jibDockerBuild` | Build Docker images locally |
| `./gradlew jib` | Build and push to registry |
| `./gradlew bootRun` | Run Spring Boot app |
| `./gradlew dependencies` | Show dependency tree |
| `./gradlew :service:task` | Run task for specific service |

## 📊 Metrics Available

### Business Metrics
- `products_created_total` - Total products created
- `products_updated_total` - Total products updated
- `orders_created_total` - Total orders created
- `orders_cancelled_total` - Total orders cancelled
- `inventory_reservations_created` - Stock reservations
- `inventory_low_stock_count` - Low stock items gauge

### Technical Metrics
- JVM memory, GC, threads
- HTTP request count, duration
- Circuit breaker state
- Database connection pool
- Cache hit/miss rates

## 🔧 Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | varies | Service port |
| `SPRING_PROFILES_ACTIVE` | default | Active profile |
| `DB_HOST` | localhost | Database host |
| `DB_USER` | postgres | Database user |
| `DB_PASSWORD` | password | Database password |
| `REDIS_HOST` | localhost | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `EUREKA_URI` | http://localhost:8761/eureka | Eureka server |

## 📝 License

MIT License - feel free to use for learning!

---

**Happy Learning! 🎉**
