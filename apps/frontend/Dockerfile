#Dev stage (used by docker compose)
# Node 22 matches the version used in frontend-ci.yml
FROM node:22-alpine AS dev
WORKDIR /app/client

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY client/package.json client/yarn.lock ./
RUN yarn install --frozen-lockfile
COPY client/ .

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 4200
CMD ["yarn", "start", "--host", "0.0.0.0", "--disable-host-check"]

#Build stage
FROM node:22-alpine AS build
WORKDIR /app/client
COPY client/package.json client/yarn.lock ./
RUN yarn install --frozen-lockfile
COPY client/ .
RUN yarn build --configuration production

#Serve stage (nginx)
FROM nginxinc/nginx-unprivileged:alpine AS prod
COPY --from=build /app/client/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 4200
