# Stage 1: Build the React application using Node.js
FROM node:20-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy package manifests
COPY package.json package-lock.json ./

# Install dependencies using 'ci' for a clean, reliable install
RUN npm ci

# Copy the rest of the application source code
COPY . .

# Build the application for production. This creates the /app/dist folder.
RUN npm run build

# Stage 2: Serve the application using a lightweight Nginx web server
FROM nginx:stable-alpine

# Copy the production-ready build from the 'build' stage to Nginx's web root
COPY --from=build /app/dist /usr/share/nginx/html

# Copy your custom Nginx configuration to handle React Router
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80, which is the default port for Nginx
EXPOSE 80

# The default command to start Nginx in the foreground
CMD ["nginx", "-g", "daemon off;"]