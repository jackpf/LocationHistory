APP_NAME = location-history-ui
IMAGE = jackpfarrelly/$(APP_NAME)
VERSION ?= 0.1.0-SNAPSHOT
NPM ?= npm
DOCKER ?= docker
BUILDER_NAME = $(APP_NAME)-builder
DOCKER_BUILDX = $(DOCKER) buildx build
DOCKER_PORT ?= 8081

.PHONY: init
init:
	$(NPM) create vite@latest . -- --template react-ts

.PHONY: clean
clean:
	rm -rf node_modules package-lock.json dist src/gen

.PHONY: install
install:
	$(NPM) install

.PHONY: build-protos
build-protos:
	mkdir -p ./src/gen
	protoc \
		--plugin=protoc-gen-ts_proto=$(CURDIR)/node_modules/.bin/protoc-gen-ts_proto \
		--ts_proto_out=esModuleInterop=true,outputServices=generic-definitions,outputClientImpl=false:./src/gen \
		--proto_path=../shared/src/main/protobuf \
		../shared/src/main/protobuf/*.proto

.PHONY: build
build: install build-protos
	$(NPM) run build

.PHONY: init-local-env
init-local-env:
	@printf "VITE_PROXY_URL=http://localhost:9123\nVITE_MAP_TYPE=openstreetmaps\n" > .env.local

.PHONY: run
run: build
	$(NPM) run dev

.PHONY: package
package: build
	$(DOCKER_BUILDX) --load -t $(IMAGE):latest .

#.PHONY: docker-run
#docker-run: package-local
#	$(DOCKER) run --rm -p $(DOCKER_PORT):80 $(IMAGE):latest

.PHONY: setup-buildx
setup-buildx:
	@if ! $(DOCKER) buildx ls | grep -q $(BUILDER_NAME); then \
		$(DOCKER) buildx create --name $(BUILDER_NAME) --use; \
	else \
		$(DOCKER) buildx use $(BUILDER_NAME); \
	fi

.PHONY: release
release: build setup-buildx
	$(DOCKER_BUILDX) --push -t $(IMAGE):latest -t $(IMAGE):$(VERSION) .

.PHONY: package-proxy
package-proxy: setup-buildx
	$(DOCKER_BUILDX) --load -t $(IMAGE)-proxy:latest -f proxy.Dockerfile .

.PHONY: release-proxy
release-proxy: setup-buildx
	$(DOCKER_BUILDX) --push -t $(IMAGE)-proxy:latest -t $(IMAGE)-proxy:$(VERSION) -f proxy.Dockerfile .

.PHONY: run-proxy
run-proxy: package-proxy
	docker run --rm \
		-p 9123:8080 \
		--name grpc-proxy \
		--add-host host.docker.internal:host-gateway \
		$(IMAGE)-proxy \
		--backend_addr=host.docker.internal:8081 \
		--run_tls_server=false \
		--allow_all_origins