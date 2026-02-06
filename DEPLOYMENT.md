# FreelanceKG - Руководство по деплою

## Требования

- Docker и Docker Compose
- Минимум 2GB RAM
- Проброшенные порты на белый IP (или Nginx Proxy Manager)

---

## Быстрый старт

### 1. Клонирование репозитория

```bash
git clone https://github.com/raiymbek2048/freelance.git
cd freelance
```

### 2. Создание файла .env

```bash
cp .env.example .env
nano .env
```

Заполни обязательные поля:

```env
# База данных
DB_USERNAME=postgres
DB_PASSWORD=ВашСекретныйПароль123!

# JWT (минимум 64 символа)
JWT_SECRET=ОченьДлинныйСекретныйКлючМинимум64СимволаДляБезопасности123456789!

# Google OAuth (получить на https://console.cloud.google.com/apis/credentials)
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret

# URLs - ВАЖНО! Укажи свои адреса
APP_BASE_URL=http://YOUR_IP:BACKEND_PORT
FRONTEND_URL=http://YOUR_IP:FRONTEND_PORT
```

### 3. Настройка URL в Dockerfile

Отредактируй `frontend/Dockerfile`:

```dockerfile
# Build args for API URLs
ARG VITE_API_URL=http://YOUR_IP:BACKEND_PORT/api/v1
ARG VITE_WS_URL=http://YOUR_IP:BACKEND_PORT/ws
```

### 4. Запуск

```bash
docker compose up -d --build
```

### 5. Проверка

```bash
docker compose ps
docker compose logs -f
```

---

## Архитектура

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   postgres   │  │   backend    │  │   frontend   │  │
│  │   (5433)     │◄─┤   (8080)     │◄─┤   (3000)     │  │
│  │              │  │  Spring Boot │  │    Nginx     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Варианты деплоя

### Вариант A: Прямой доступ по IP

Проброс портов:
- `БЕЛЫЙ_IP:1000` → `ЛОКАЛЬНЫЙ_IP:3000` (frontend)
- `БЕЛЫЙ_IP:1002` → `ЛОКАЛЬНЫЙ_IP:8080` (backend)

В `.env`:
```env
APP_BASE_URL=http://БЕЛЫЙ_IP:1002
FRONTEND_URL=http://БЕЛЫЙ_IP:1000
```

В `frontend/Dockerfile`:
```dockerfile
ARG VITE_API_URL=http://БЕЛЫЙ_IP:1002/api/v1
ARG VITE_WS_URL=http://БЕЛЫЙ_IP:1002/ws
```

### Вариант B: Через домен + Nginx Proxy Manager

Проброс:
- `БЕЛЫЙ_IP:80/443` → `NPM_SERVER`

В NPM добавить Proxy Hosts:
| Domain | Forward IP | Port | Websockets | SSL |
|--------|-----------|------|------------|-----|
| `yourdomain.kg` | `ЛОКАЛЬНЫЙ_IP` | `3000` | ❌ | Let's Encrypt |
| `api.yourdomain.kg` | `ЛОКАЛЬНЫЙ_IP` | `8080` | ✅ | Let's Encrypt |

В `.env`:
```env
APP_BASE_URL=https://api.yourdomain.kg
FRONTEND_URL=https://yourdomain.kg
```

В `frontend/Dockerfile`:
```dockerfile
ARG VITE_API_URL=https://api.yourdomain.kg/api/v1
ARG VITE_WS_URL=https://api.yourdomain.kg/ws
```

---

## Полезные команды

```bash
# Просмотр логов
docker compose logs -f backend
docker compose logs -f frontend

# Перезапуск
docker compose restart

# Полная пересборка
docker compose down
docker compose up -d --build

# Очистка и пересоздание БД
docker compose down -v  # ВНИМАНИЕ: удалит данные!
docker compose up -d --build

# Вход в контейнер
docker exec -it freelance-kg-backend sh
docker exec -it freelance-kg-frontend sh

# Проверка здоровья
curl http://localhost:8080/actuator/health
curl http://localhost:3000
```

---

## Google OAuth настройка

1. Перейди на https://console.cloud.google.com/apis/credentials
2. Создай OAuth 2.0 Client ID
3. Добавь Authorized redirect URIs:
   - `http://YOUR_IP:BACKEND_PORT/api/v1/auth/oauth2/callback/google`
   - или `https://api.yourdomain.kg/api/v1/auth/oauth2/callback/google`
4. Добавь Authorized JavaScript origins:
   - `http://YOUR_IP:FRONTEND_PORT`
   - или `https://yourdomain.kg`

---

## Troubleshooting

### Backend не стартует

```bash
docker compose logs backend
```

Частые причины:
- Неверный пароль БД (удали volume: `docker volume rm freelance_postgres_data`)
- Пустой GOOGLE_CLIENT_ID (закомментируй OAuth в `application.yml`)

### Frontend не подключается к API

Проверь:
1. VITE_API_URL в Dockerfile указывает на правильный адрес
2. CORS настроен (в `SecurityConfig.java` уже `*`)
3. Backend доступен: `curl http://localhost:8080/actuator/health`

### WebSocket не работает

- Включи Websockets Support в Nginx Proxy Manager
- Проверь VITE_WS_URL в Dockerfile

---

## Контакты

Репозиторий: https://github.com/raiymbek2048/freelance
