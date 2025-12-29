APP_NAME = jackpfarrelly/location-history-ui
DOCKER ?= docker
DOCKER_BUILD_PLATFORMS = linux/amd64,linux/arm64
DOCKER_BUILDX = $(DOCKER) buildx create --use; $(DOCKER) buildx build
DOCKER_BUILDX_MULTI = $(DOCKER_BUILDX) --platform $(DOCKER_BUILD_PLATFORMS)
DOCKER_BUILDX_LOCAL = $(DOCKER_BUILDX) --load
DOCKER_PORT ?= 8081

.PHONY: init
init:
	npm create vite@latest . -- --template react-ts

.PHONY: clean
clean:
	rm -rf node_modules package-lock.json dist src/gen

.PHONY: install
install:
	npm install

.PHONY: build-protos
build-protos:
	mkdir -p ./src/gen
	protoc \
		--plugin=protoc-gen-ts_proto=$(CURDIR)/node_modules/.bin/protoc-gen-ts_proto \
		--ts_proto_out=esModuleInterop=true,outputServices=generic-definitions,outputClientImpl=false:./src/gen \
		--proto_path=../shared/src/main/protobuf \
		../shared/src/main/protobuf/*.proto

.PHONY: run
run:
	npm run dev

.PHONY: build
build: install build-protos
	npm run build

.PHONY: package
package: build
	$(DOCKER_BUILDX_MULTI) -t $(APP_NAME):latest .

.PHONY: package-local
package-local: build
	$(DOCKER_BUILDX_LOCAL) -t $(APP_NAME):latest .

.PHONY: docker-run
docker-run: package-local
	$(DOCKER) run --rm -p $(DOCKER_PORT):80 $(APP_NAME):latest

.PHONY: docker-publish
docker-publish: package
	$(DOCKER_BUILDX_MULTI) -t $(APP_NAME):latest --push .

.PHONY: package-proxy
package-proxy:
	$(DOCKER_BUILDX_MULTI) -t $(APP_NAME)-proxy:latest -f proxy.Dockerfile .

.PHONY: package-local-proxy
package-local-proxy:
	$(DOCKER_BUILDX_LOCAL) -t $(APP_NAME)-proxy:latest -f proxy.Dockerfile .

.PHONY: docker-publish-proxy
docker-publish-proxy:
	$(DOCKER_BUILDX_MULTI) -t $(APP_NAME)-proxy:latest --push -f proxy.Dockerfile .

.PHONY: run-proxy
run-proxy: package-local-proxy
	docker run --rm \
		-p 9123:8080 \
		--name grpc-proxy \
		--add-host host.docker.internal:host-gateway \
		$(APP_NAME)-proxy \
		--backend_addr=100.103.216.83:7070 \
		--run_tls_server=false \
		--allow_all_origins