# Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Build args for API URLs (default to relative paths for local dev)
ARG VITE_API_URL=/api/v1
ARG VITE_WS_URL=/ws

# Set as env vars for build
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_WS_URL=$VITE_WS_URL

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copy source files
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html
RUN chmod -R 644 /usr/share/nginx/html/* && find /usr/share/nginx/html -type d -exec chmod 755 {} \;

# Expose ports
EXPOSE 80 443

# Install curl for healthcheck
RUN apk add --no-cache curl

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
