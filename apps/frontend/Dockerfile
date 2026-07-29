#Dev stage (used by docker compose)
# Node 22 matches the version used in frontend-ci.yml
FROM node:22-alpine AS dev

## this is the root inside the container
WORKDIR /app

# Create a non-root user 
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the dependency files 
COPY package.json yarn.lock ./

# copy frontend package.json so Yarn is aware of the workspace we using 
COPY apps/frontend/client/package.json apps/frontend/client/package.json

# Install dependencies 
RUN yarn install --frozen-lockfile

# Copy angular application 
COPY . .

RUN chown -R appuser:appgroup /app
USER appuser

WORKDIR /app/apps/frontend/client 

EXPOSE 4200
CMD ["yarn", "start", "--host", "0.0.0.0"]

# Build stagea
FROM node:22-alpine AS build
WORKDIR /app

# Copy the dependency files 
COPY package.json yarn.lock ./

# copy frontend package.json so Yarn is aware of the workspace we using 
COPY apps/frontend/client/package.json apps/frontend/client/package.json

# Install dependencies 
RUN yarn install --frozen-lockfile

# Copy angular application 
COPY . .

WORKDIR /app/apps/frontend/client 

# Create a build 
RUN yarn build --configuration production

# Serve stage (nginx)
FROM nginxinc/nginx-unprivileged:alpine AS prod

# Copy angular application 
COPY --from=build /app/apps/frontend/client/dist/client/browser/ /usr/share/nginx/html/

COPY apps/frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Nginx port 
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]                                                      


# UPDATE THE DOCKERFILE BECAUSE DOCKER SAID IT ONLY RECEIEVED 15kb which is too extrememly small for our setup

