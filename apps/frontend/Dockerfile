FROM node:24-alpine AS build
WORKDIR /app

# Copy dependencies first to use docker caching
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile 

# Copy code and build the static files - need to use ngnix to serve 
COPY . .
RUN yarn build 

# Use to serve in production, never uses ng serve and helps when the users do refresh, they wont see 404
# Port 8080 
FROM nginx:alpine
EXPOSE 8080 
# 
COPY --from=build src/dist/timesheets-ai/browser /usr/share/nginx/html
CMD ["nginx", "-g", "daemon off;"]

# description: Docker can build images automatically 
# by reading the instructions from a Dockerfile. 
# A Dockerfile is a text document that contains all the commands 
# a user could call on the command line to assemble an image.

##################### Explaining the structure #########################

# FROM: Tells Docker which Operating System to start with
    # alphine is used to minimize size
    # build is the container that has the node modules and finished files in dist so we only want files in dist folder. 
# WORKDIR: Same thing as cd, create a folder and moves into the folder.
# COPY: Moves (duplicate) the files from your laptop into the container 
# RUN: Executes a command inside the container (yarn)
# CMD: The 'Start Button'. 

# Note: when you run yarn build, it creates a folder called dist/, and in there are static files: index.html, styles.css and main.js 
# nginx helps with reducing costs and better security 