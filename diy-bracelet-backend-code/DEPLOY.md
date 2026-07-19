# Gitee 流水线 + Docker 部署指南

## 核心原理

```
CI/CD 只传两样东西：JAR + docker-compose.yml
Dockerfile + certs → 服务器一次性放好，永不覆盖
JAR 通过 volume 挂载进容器，不需要每次重建镜像
中间件（MySQL/Redis/MinIO）由 docker-compose 管理，首次创建后不再动
```

## 一、服务器初始化（每台新服务器只做一次）

### 1.1 安装 Docker

```bash
curl -fsSL https://get.docker.com | sh
systemctl start docker
systemctl enable docker

# 安装 docker compose 插件
apt update && apt install docker-compose-plugin -y

# 配置国内镜像加速（必须，否则拉取镜像会超时）
cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerproxy.cn",
    "https://docker.1ms.run"
  ]
}
EOF
systemctl daemon-reload
systemctl restart docker
```

### 1.2 创建项目文件夹

```bash
mkdir -p /home/ubuntu/{项目名}
mkdir -p /home/ubuntu/{项目名}/uploads
```

### 1.3 上传 Dockerfile 和 certs（从本地执行）

```bash
# 替换为你自己的服务器IP和项目路径
scp Dockerfile root@服务器IP:/home/ubuntu/{项目名}/
scp -r certs root@服务器IP:/home/ubuntu/{项目名}/
```

### 1.4 停掉服务器自带的 MySQL（如有）

```bash
# 检查 3306 端口是否被占用
ss -tlnp | grep 3306

# 如果有占用，停掉它
/etc/init.d/mysqld stop
systemctl disable mysqld
```

### 1.5 首次拉取镜像（可选，防止 CI/CD 时拉取超时）

```bash
cd /home/ubuntu/{项目名}
docker compose pull
```

## 二、项目中需要的文件

项目根目录下需要以下 4 个文件（JAR 由 CI/CD 构建，不需要手动处理）：

```
项目根目录/
├── Dockerfile              ← 一次性上传到服务器，不在 CI/CD 中传输
├── docker-compose.yml      ← CI/CD 每次部署时更新
├── .workflow/xxx.yml       ← Gitee 流水线配置
├── src/main/resources/
│   ├── application.yml     ← 基础配置（占位符）
│   └── application-dev.yml ← Docker 部署用的实际值
└── certs/                  ← 一次性上传到服务器，不在 CI/CD 中传输
```

## 三、各文件怎么写

### 3.1 Dockerfile（固定模板，几乎不用改）

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN mkdir -p /app/uploads

EXPOSE 8080

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

需要改的地方：
- `EXPOSE 8080` → 改成你项目的端口
- `eclipse-temurin:17-jre` → JDK 版本不同时改（如 `eclipse-temurin:11-jre`）

### 3.2 docker-compose.yml（按需增删中间件）

```yaml
services:

  # ========== 中间件（按需增删）==========

  mysql:
    image: mysql:8.0
    container_name: {项目名}-mysql
    restart: unless-stopped
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: "{MySQL密码}"
      MYSQL_DATABASE: {数据库名}
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p{MySQL密码}"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - {项目名}-network

  redis:
    image: redis:7
    container_name: {项目名}-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - {项目名}-network

  # ========== 应用 ==========

  {项目名}-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: {项目名}-app
    ports:
      - "{应用端口}:{应用端口}"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    volumes:
      - ./app.jar:/app/app.jar
      - ./certs:/app/certs
      - ./uploads:/app/uploads
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - {项目名}-network

volumes:
  mysql-data:
  redis-data:

networks:
  {项目名}-network:
    driver: bridge
```

### 3.3 新增中间件示例

**新增 MongoDB：**

```yaml
  mongo:
    image: mongo:7
    container_name: {项目名}-mongo
    restart: unless-stopped
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db
    networks:
      - {项目名}-network
```

然后在 `volumes:` 下加 `mongo-data:`，在 App 的 `depends_on:` 下加 `mongo:`。

**新增 RabbitMQ：**

```yaml
  rabbitmq:
    image: rabbitmq:3-management
    container_name: {项目名}-rabbitmq
    restart: unless-stopped
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    networks:
      - {项目名}-network
```

**不需要的中间件直接删掉对应的 service 块即可。**

### 3.4 application.yml（基础配置，用占位符）

```yaml
server:
  port: 8080
  address: 0.0.0.0

spring:
  profiles:
    active: local
  main:
    allow-circular-references: true
  datasource:
    druid:
      driver-class-name: ${项目前缀.datasource.driver-class-name}
      url: jdbc:mysql://${项目前缀.datasource.host}:${项目前缀.datasource.port}/${项目前缀.datasource.database}?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true
      username: ${项目前缀.datasource.username}
      password: ${项目前缀.datasource.password}
  redis:
    host: ${项目前缀.redis.host}
    port: ${项目前缀.redis.port}
    database: ${项目前缀.redis.database}

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.xxx.entity
  configuration:
    map-underscore-to-camel-case: true
```

`profiles.active: local` 表示本地开发默认用 local 配置，Docker 部署时通过环境变量覆盖为 `dev`。

### 3.5 application-dev.yml（Docker 部署用，写实际值）

```yaml
{项目前缀}:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: mysql          # Docker 服务名，不是 localhost
    port: 3306           # Docker 内部端口，不是宿主机映射端口
    database: {数据库名}
    username: root
    password: {MySQL密码}
  redis:
    host: redis          # Docker 服务名
    port: 6379
    database: 0
```

**关键点**：host 字段用 Docker 服务名（`mysql`、`redis`），不是 `localhost`。因为在 Docker 网络中，`localhost` 指的是容器自身。

## 四、Gitee 流水线配置

文件路径：`.workflow/{项目名}.yml`

```yaml
version: '1.0'
name: {项目名}
displayName: {项目名大写}
triggers:
  trigger: auto
  push:
    branches:
      prefix:
        - master
stages:
  - name: stage-build
    displayName: 构建
    strategy: naturally
    trigger: auto
    executor: []
    steps:
      - step: build@maven
        name: build_artifact
        displayName: Maven 构建
        jdkVersion: '17'          # 改成你项目的 JDK 版本
        mavenVersion: 3.8.8       # 改成你需要的 Maven 版本
        commands:
          - mvn clean package -Dmaven.test.skip=true -U -e -X -B
        artifacts:
          - name: build_artifact
            path:
              - {启动模块}/target/{启动模块}.jar   # JAR 路径
              - docker-compose.yml
        settings: []
        caches:
          - ~/.m2
        notify: []
        strategy:
          retry: '0'
  - name: stage-deploy
    displayName: 部署
    strategy: naturally
    trigger: auto
    executor: []
    steps:
      - step: deploy@agent
        name: build_artifact
        displayName: 主机部署
        hostGroupID:
          ID: {主机分组ID}           # Gitee 主机管理中的分组 ID
          hostID:
            - {主机ID}               # Gitee 主机管理中的主机 UUID
        deployArtifact:
          - name: build_artifact
            target: /home/ubuntu/{项目名}
            source: build
            dependArtifact: build_artifact
        script:
          - cd /home/ubuntu/{项目名}
          - tar -zxf build_artifact.tar.gz
          - find . -name "{启动模块}-{版本}.jar" -exec mv {} ./app.jar \;
          - ls -R
          - docker compose up -d --force-recreate {项目名}-app
          - docker image prune -f
        notify: []
        strategy:
          retry: '0'
```

## 五、部署操作清单

### 首次部署

```
1. 服务器：安装 Docker + 配置镜像加速
2. 服务器：创建 /home/ubuntu/{项目名}/ 和 uploads/
3. 本地：scp Dockerfile + certs 到服务器
4. Gitee：创建流水线，导入 .workflow/*.yml，填写主机 ID
5. 服务器：docker compose pull（预拉取镜像）
6. 本地：git push → 触发 CI/CD
7. 服务器：docker exec 导入 SQL 表结构
8. 服务器：docker compose ps 确认所有容器 running
```

### 后续部署

```
本地：git push → CI/CD 自动构建部署，无需手动操作
```

### 服务器上每次要改的文件

```
Dockerfile          → 一次性放好，永不覆盖
certs/              → 一次性放好，永不覆盖
docker-compose.yml  → CI/CD 每次覆盖（所以要改中间件配置请改项目里的源文件）
app.jar             → CI/CD 每次覆盖
uploads/            → 持久化，永不覆盖
MySQL 数据           → Docker 数据卷，永不覆盖
```

## 六、常见问题

| 问题 | 解决方案 |
|------|---------|
| 镜像拉取超时/403 | 配置 `/etc/docker/daemon.json` 镜像加速器 |
| MySQL 端口 3306 被占用 | 停掉服务器自带的 MySQL：`/etc/init.d/mysqld stop` |
| App 启动报连接 MySQL 失败 | application-dev.yml 的 host 用 Docker 服务名 `mysql`，不是 `localhost` |
| Gitee 流水线名称报错 | displayName 不能有空格，用驼峰式如 `MyProject` |
| JAR 路径找不到 | 检查 artifacts path 中的模块名和版本号是否与 pom.xml 一致 |

## 七、要改的参数速查表

| 位置 | 参数 | 说明 |
|------|------|------|
| docker-compose.yml | `{项目名}` | 所有容器名、网络名、卷名 |
| docker-compose.yml | `MYSQL_ROOT_PASSWORD` | MySQL 密码 |
| docker-compose.yml | `MYSQL_DATABASE` | 数据库名 |
| docker-compose.yml | 端口映射 | 按需调整宿主机端口 |
| Dockerfile | `EXPOSE` | 应用端口 |
| Dockerfile | `eclipse-temurin:17-jre` | JDK 版本 |
| application-dev.yml | `host: mysql` | MySQL 地址（Docker 服务名） |
| application-dev.yml | `password` | MySQL 密码（与 docker-compose 一致） |
| application-dev.yml | `host: redis` | Redis 地址（Docker 服务名） |
| .workflow/*.yml | `jdkVersion` | JDK 版本 |
| .workflow/*.yml | `mavenVersion` | Maven 版本 |
| .workflow/*.yml | `{启动模块}/target/{启动模块}.jar` | JAR 路径 |
| .workflow/*.yml | `hostID` | Gitee 主机 ID |
| .workflow/*.yml | `target: /home/ubuntu/{项目名}` | 服务器部署路径 |
