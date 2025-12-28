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

.PHONY: build-protos
build-protos:
	mkdir -p ./src/gen
	protoc \
		--plugin=protoc-gen-ts_proto=$(PLUGIN_PATH) \
		--ts_proto_out=./src/gen \
		--ts_proto_opt=esModuleInterop=true,outputServices=nice-grpc,outputClientImpl=false \
		--proto_path=../shared/src/main/protobuf \
		../shared/src/main/protobuf/*.proto

.PHONY: proxy
proxy:
	docker build -t grpc-proxy ./proxy
	docker run \
		-p 9123:8080 \
		--name grpc-proxy \
		--add-host host.docker.internal:host-gateway \
		grpc-proxy \
		--backend_addr=host.docker.internal:8080 \
		--run_tls_server=false \
		--allow_all_origins

.PHONY: run
run:
	npm run dev