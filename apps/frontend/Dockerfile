#Dev stage (used by docker compose)
# Node 22 matches the version used in frontend-ci.yml
FROM node:22-alpine AS dev
WORKDIR /app/client

# Create a non-root user 
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the dependency files 
COPY client/package.json client/yarn.lock ./

# Install dependencies 
RUN yarn install --frozen-lockfile

# Copy angular application 
COPY client/ .

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 4200
CMD ["yarn", "start", "--host", "0.0.0.0"]

# Build stage
FROM node:22-alpine AS build
WORKDIR /app/client

# Copy the dependency files 
COPY client/package.json client/yarn.lock ./

# Install dependencies 
RUN yarn install --frozen-lockfile

# Copy angular application 
COPY client/ .

# Create a build 
RUN yarn build --configuration production

# Serve stage (nginx)
FROM nginxinc/nginx-unprivileged:alpine AS prod

# Copy angular application 
COPY --from=build /app/client/dist/client/browser/ /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Nginx port 
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]                                                      0
