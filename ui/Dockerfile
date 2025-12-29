FROM nginx:alpine

COPY dist /usr/share/nginx/html

COPY config/nginx.conf /etc/nginx/conf.d/default.conf

COPY entrypoint.sh /entrypoint.sh

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1 || exit 1

ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]