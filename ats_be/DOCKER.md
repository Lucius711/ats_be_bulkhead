# ATS Docker deployment

## Local

```bash
cp .env.example .env
mvn -DskipTests package
docker compose up -d --build
docker compose ps
```

Mỗi service có một Dockerfile riêng. Dockerfile chỉ đóng gói JAR đã build,
nên cần chạy Maven trước bước build image.

Các endpoint chính:

- API Gateway: `http://localhost:8080`
- Keycloak: `http://localhost:8180`
- Config Server: `http://localhost:8888`
- Eureka: `http://localhost:8761`
- RabbitMQ AMQP: `localhost:5672`
- RabbitMQ Management UI: `http://localhost:15672`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

## Login Keycloak bằng Postman

Tạo request `POST` tới:

```text
http://localhost:8180/realms/ats/protocol/openid-connect/token
```

Chọn `Body` → `x-www-form-urlencoded`:

```text
grant_type=password
client_id=ats-postman
username=admin
password=admin123
```

Đổi `username` và `password` theo tài khoản muốn kiểm tra:

Các tài khoản demo:

```text
admin     / admin123       -> ADMIN
recruiter / recruiter123   -> RECRUITER
candidate / candidate123   -> CANDIDATE
```

Copy trường `access_token` trong response và dùng dạng `Bearer <token>` khi gọi API.

Các service trong Docker phải dùng hostname service, không dùng `localhost`:

```text
RabbitMQ: rabbitmq:5672
MinIO: http://minio:9000
```

MinIO và RabbitMQ được khởi động sẵn cho local/dev. Các service Java kết nối
đến chúng bằng hostname của Docker service.

Local dùng Config Server native và mount thư mục `config-repo` ở chế độ read-only.
File `.env` không được đưa vào image hoặc commit lên Git.

## Production

Tạo `.env` riêng cho môi trường production và đặt tối thiểu:

```.env
DB_USERNAME=ats
DB_PASSWORD=<strong-password>
KEYCLOAK_HOSTNAME=auth.example.com
KEYCLOAK_ISSUER_URI=https://auth.example.com/realms/ats
CONFIG_GIT_URI=https://github.com/<org>/<repo>.git
CONFIG_GIT_LABEL=main
CONFIG_GIT_SEARCH_PATHS=config-repo
CONFIG_GIT_USERNAME=<git-user>
CONFIG_GIT_PASSWORD=<git-token>
```

Khởi động bằng override production:

```bash
mvn -DskipTests package
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Production dùng Git backend cho Config Server, chỉ publish API Gateway, và giữ
database/Eureka/internal services trong Docker network. TLS nên được terminate
ở reverse proxy hoặc load balancer phía trước Gateway và Keycloak.
