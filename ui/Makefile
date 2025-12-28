PROTO_DIR = ../shared/src/main/protobuf
OUT_DIR = ./client/src/gen
PLUGIN_PATH = $(PWD)/node_modules/.bin/protoc-gen-ts_proto

.PHONY: init
init:
	npm create vite@latest . -- --template react-ts
	npm install
	npm install leaflet react-leaflet
	npm install -D @types/leaflet
	npm install --save-dev ts-proto
	npm install --save nice-grpc-web
	npm install vite-plugin-checker typescript --save-dev

.PHONY: build-protos
.PHONY: build-protos
build-protos:
	mkdir -p ./client/src/gen
	protoc \
		--plugin=protoc-gen-ts_proto=$(PLUGIN_PATH) \
		--ts_proto_out=esModuleInterop=true,outputServices=generic-definitions,outputClientImpl=false:./src/gen \
		--proto_path=../shared/src/main/protobuf \
		../shared/src/main/protobuf/*.proto

.PHONY: proxy
proxy:
	docker build -t grpc-proxy ./proxy
	docker run --rm \
		-p 9123:8080 \
		--name grpc-proxy \
		--add-host host.docker.internal:host-gateway \
		grpc-proxy \
		--backend_addr=100.103.216.83:9123 \
		--run_tls_server=false \
		--allow_all_origins

.PHONY: run
run:
	npm run dev