PROTO_DIR = ../shared/src/main/protobuf
OUT_DIR = ./client/src/gen
PLUGIN_PATH = $(PWD)/node_modules/.bin/protoc-gen-ts_proto

.PHONY: init
init:
	npm create vite@latest . -- --template react-ts

.PHONY: build-protos
build-protos:
	mkdir -p ./src/gen
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