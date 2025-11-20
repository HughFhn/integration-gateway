# Stage 1: Build the React application
FROM node:18 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY . ./
RUN npm run build

# Stage 2: Serve the application with Nginx
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html
COPY --from=build /app/build .
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
