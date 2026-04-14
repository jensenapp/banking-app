# Stage 1: Build the React application
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve the app with Nginx
FROM nginx:alpine
# 複製自訂的 Nginx 設定檔 (處理 SPA 路由)
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 將編譯好的靜態檔案複製到 Nginx 的預設目錄
COPY --from=build /app/dist /usr/share/nginx/html
# 暴露容器內的 80 port
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]