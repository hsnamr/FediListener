# ActivityPub Listener

A REST API service for social listening and monitoring of ActivityPub/Fediverse instances. Monitor and analyze content from federated social networks such as Friendica, Mastodon, Lemmy, PeerTube, GNU social, Pleroma and other ActivityPub-compatible platforms.

## 🎯 Overview

ActivityPub Listener is a Spring Boot-based service that enables organizations to:
- Monitor keywords, accounts, and regions across Fediverse instances
- Collect and analyze ActivityPub activities in real-time
- Retrieve analytics and insights from monitored content
- Manage monitoring configurations via REST API

**Target Platforms**: ActivityPub/Fediverse instances only (Mastodon, Lemmy, PeerTube, etc.)  
**Excluded Platforms**: Commercial social networks (Twitter/X, Facebook, Instagram, LinkedIn, TikTok)

## ✨ Features

### Monitor Management
- **CRUD Operations**: Create, read, update, and delete monitoring configurations
- **Multiple Monitor Types**:
  - Keyword-based monitoring
  - Account/profile analysis
  - Regional/geographic monitoring
  - Managed accounts and pages
- **Lifecycle Management**: Approval workflow, pause/resume functionality
- **Multi-Instance Support**: Monitor across multiple Fediverse servers simultaneously

### ActivityPub Integration
- **Protocol Compliance**: Full W3C ActivityPub 1.0 specification support
- **Federation**: Connect to and collect data from any ActivityPub-compatible instance
- **Activity Collection**: Monitor Create, Update, Delete, Announce, Like, and other activities
- **Actor Tracking**: Track users, groups, and organizations across instances
- **WebFinger Discovery**: Automatic actor and instance discovery

### Data & Analytics
- **Real-time Streaming**: Kafka-based real-time data streaming
- **Social Listening Data**: Retrieve analytics and metrics for monitors
- **Widget Support**: Various data visualization widgets (engagements, posts, topics, sentiment, etc.)
- **Advanced Filtering**: Filter by topics, sentiment, languages, accounts, and more
- **Date Range Queries**: Flexible date range filtering

### Technical Features
- **RESTful API**: Comprehensive REST API with OpenAPI documentation
- **Message Queue Integration**: Kafka for async processing and real-time updates
- **Database**: MongoDB for persistent storage
- **Authentication**: JWT-based authentication and authorization
- **Rate Limiting**: Per-instance rate limiting with exponential backoff
- **Health Monitoring**: Instance health checks and monitoring

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Applications                      │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP/REST API
                     │
┌────────────────────▼────────────────────────────────────────┐
│         ActivityPub Listener Service (Spring Boot)          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  REST API Controllers                                 │  │
│  │  Business Logic Layer                                 │  │
│  │  ActivityPub Integration Layer                        │  │
│  │  Data Access Layer (JPA)                              │  │
│  └──────────────────────────────────────────────────────┘  │
└──────┬───────────────────┬───────────────────┬──────────────┘
       │                   │                   │
┌──────▼──────┐   ┌────────▼────────┐  ┌──────▼──────────────┐
│  Database   │   │  Message Queue  │  │  Fediverse Instances │
│  (MongoDB)   │   │    (Kafka)       │  │  - mastodon.social   │
│             │   │                 │  │  - lemmy.ml          │
│             │   │                 │  │  - peertube.io       │
└─────────────┘   └─────────────────┘  └─────────────────────┘
```

## 🛠️ Technology Stack

### Core Framework
- **Java**: 17 or higher (LTS)
- **Spring Boot**: 3.2.x
- **Spring Web**: REST API
- **Spring Data JPA**: Database access
- **Spring Kafka**: Kafka integration

### ActivityPub Libraries
- **activityPub4j**: W3C ActivityPub implementation for Java/Spring Boot
- **Mastodon4J**: Mastodon API client library
- **BigBone**: Alternative Mastodon client (if needed)

### Database & Messaging
- **MongoDB**: 7.0+ (primary database)
- **Apache Kafka**: 3.5+ (message queue)

### Additional Dependencies
- **Jackson**: JSON processing
- **Bean Validation**: Input validation
- **Lombok**: Reduce boilerplate
- **JWT**: Authentication
- **JUnit 5 + Mockito**: Testing

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.8+ or Gradle 7+
- MongoDB 7.0+
- Apache Kafka 3.5+ (with Zookeeper)
- Docker & Docker Compose (optional, for local development)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ActivityPubListener
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Start dependencies with Docker Compose**
   ```bash
   docker-compose up -d
   ```
   This starts MongoDB, Zookeeper, and Kafka containers.

4. **Build and run the application**
   ```bash
   ./mvnw spring-boot:run
   # or
   ./gradlew bootRun
   ```

5. **Verify the service is running**
   ```bash
   curl -H "API-Version: v1" http://localhost:8080/api/actuator/health
   ```

### Configuration

Key environment variables:

```bash
# Database
MONGODB_URI=mongodb://localhost:27017/activitypub_listener
MONGODB_DATABASE=activitypub_listener

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP_ID=activitypub-listener

# ActivityPub
ACTIVITYPUB_USER_AGENT=ActivityPubListener/1.0
ACTIVITYPUB_DEFAULT_RATE_LIMIT=300

# JWT
JWT_PUBLIC_KEY=your_jwt_public_key
JWT_ISSUER=AMRITECH
```

## 📡 API Overview

### Base URL
```
http://localhost:8080/api
```

### API Versioning
All endpoints use header-based versioning. Include the `API-Version` header in your requests:
```http
API-Version: v1
```

If no version header is provided, the API defaults to `v1`.

### Authentication
All endpoints require JWT authentication:
```http
Authorization: Bearer <jwt_token>
```

### Monitor Management

#### Create Monitor
```http
POST /api/monitors
API-Version: v1
Content-Type: application/json

{
  "name": "Brand Monitoring",
  "monitor_type_id": 1,
  "product_id": 1,
  "data_sources": [1],
  "monitor_options": {
    "KEYWORD": {
      "MASTODON": {
        "keywords": "brand, product",
        "spam_keywords": "spam, test"
      }
    }
  },
  "languages": "en,ar",
  "exact_search": false
}
```

#### List Monitors
```http
GET /api/monitors?page=1&per_page=10&search=brand
API-Version: v1
```

#### Get Monitor
```http
GET /api/monitors/{id}
API-Version: v1
```

#### Update Monitor
```http
PUT /api/monitors/{id}
API-Version: v1
Content-Type: application/json
```

#### Delete Monitor
```http
DELETE /api/monitors/{id}
API-Version: v1
```

#### Pause/Resume Monitor
```http
POST /api/monitors/{id}/pause
API-Version: v1

POST /api/monitors/{id}/resume
API-Version: v1
```

### Social Listening Data

#### Request Social Listening Data
```http
POST /api/social-listening/data
API-Version: v1
Content-Type: application/json

{
  "monitor_id": 123,
  "data_source": "MASTODON",
  "page_name": "account_page",
  "start_date": 1706352000,
  "end_date": 1706438400,
  "filters": {
    "topics": [1, 2],
    "sentiment": ["positive"],
    "languages": ["en"]
  },
  "widgets_names": ["engagements", "posts", "topics"],
  "page_number": 1
}
```

#### Get Available Widgets
```http
GET /api/social-listening/widgets?monitor_id=123&data_source=MASTODON&page_name=account_page
API-Version: v1
```

### ActivityPub-Specific Endpoints

#### List Fediverse Instances
```http
GET /api/instances
API-Version: v1
```

#### Discover Actor
```http
GET /api/actors/discover?resource=acct:user@mastodon.social
API-Version: v1
```

#### Get Actor Activities
```http
GET /api/actors/{actor_id}/activities?page=1&per_page=20
API-Version: v1
```

## 📊 Monitor Types

### 1. Keyword Monitor
Monitor content containing specific keywords across Fediverse instances.

**Use Cases**:
- Brand mention tracking
- Topic monitoring
- Trend analysis

### 2. Account Analysis Monitor
Monitor specific actors (users, groups, communities) and their activities.

**Use Cases**:
- Competitor monitoring
- Influencer tracking
- Community analysis

### 3. Regional Monitor
Monitor content based on geographic regions (if location data is available).

**Use Cases**:
- Geographic trend analysis
- Regional sentiment monitoring

## 🔧 Development

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/activitypub/listener/
│   │       ├── ActivityPubListenerApplication.java
│   │       ├── config/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── model/
│   │       ├── dto/
│   │       ├── exception/
│   │       └── activitypub/
│   └── resources/
│       ├── application.yml
│       └── db/migration/
└── test/
```

### Running Tests
```bash
./mvnw test
# or
./gradlew test
```

### Code Style
- Follow Java coding conventions
- Use Lombok to reduce boilerplate
- Write comprehensive unit and integration tests

### External Resources
- [W3C ActivityPub Specification](https://www.w3.org/TR/activitypub/)
- [ActivityStreams 2.0](https://www.w3.org/TR/activitystreams-core/)
- [WebFinger Protocol](https://webfinger.net/)
- [NodeInfo Specification](https://nodeinfo.diaspora.software/)

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Write tests for new features
- Follow the existing code style
- Update documentation as needed
- Ensure all tests pass before submitting

## 📝 License

See [LICENSE](LICENSE) file for details.

## 🐛 Known Issues & Limitations

- **Rate Limiting**: Each Fediverse instance has different rate limits. The service implements per-instance rate limiting, but aggressive monitoring may hit limits.
- **Content Format**: ActivityPub content may be in HTML or Markdown. Content extraction and normalization may be needed.
- **Federation Delays**: Activities may arrive with delays due to federation. The service handles out-of-order activities.
- **Instance Diversity**: Different ActivityPub implementations (Mastodon, Lemmy, PeerTube) may require different handling.

## 🗺️ Roadmap

- [x] Project specification and planning
- [x] **Phase 1: Foundation & Core Infrastructure** ✅
  - [x] Spring Boot project setup with Maven
  - [x] Database schema with MongoDB collections
  - [x] JPA entities and repositories (9 entities, 7 repositories)
  - [x] REST API for monitor management (7 endpoints)
  - [x] ActivityPub client integration (WebFinger, actor discovery)
  - [x] Docker Compose with MongoDB and Kafka
  - [x] Error handling and validation
- [ ] Phase 2: ActivityPub Data Collection
  - [ ] ActivityPub polling implementation
  - [ ] Data collection engine
  - [ ] Kafka integration for activity streaming
  - [ ] Instance management
- [ ] Phase 3: Monitor Types & Business Logic
  - [ ] Keyword monitor implementation
  - [ ] Account analysis monitor
  - [ ] Regional monitor
  - [ ] Monitor lifecycle management
- [ ] Phase 4: Social Listening Data & Analytics
  - [ ] Social listening API endpoints
  - [ ] Analytics integration
  - [ ] Metrics and widgets support
- [ ] Phase 5: Advanced Features (ActivityPub Server)
  - [ ] ActivityPub server implementation
  - [ ] Enhanced federation
  - [ ] Performance optimization
  - [ ] Monitoring & observability
- [ ] Phase 6: Testing & Documentation
  - [ ] Unit and integration tests
  - [ ] API documentation (OpenAPI/Swagger)
  - [ ] Deployment guides

See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for detailed roadmap.

## 📞 Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Check the documentation in `SPECIFICATION.md` and `IMPLEMENTATION_PLAN.md`

## 🙏 Acknowledgments

- [activityPub4j](https://github.com/msummers/activityPub4j) - ActivityPub Java library
- [Mastodon4J](https://github.com/Mastodon4J/Mastodon4J) - Mastodon API client
- The Fediverse community for the ActivityPub protocol

---

**Built with ❤️ for the Fediverse**
