APP_NAME = location-history-server
IMAGE = jackpfarrelly/$(APP_NAME)
VERSION ?= 0.1.0-SNAPSHOT
SBT ?= sbt
DOCKER ?= docker
BUILDER_NAME = $(APP_NAME)-builder
DOCKER_BUILDX = $(DOCKER) buildx build
DOCKER_PORT ?= 8080
RUN_ARGS ?=

.PHONY: clean
clean:
	$(SBT) clean

.PHONY: lint
lint:
	$(SBT) scalafmtCheckAll
	$(SBT) 'scalafix --check'

.PHONY: fmt
fmt:
	$(SBT) scalafmtCheckAll || $(SBT) scalafmtAll
	$(SBT) scalafmtSbtCheck || $(SBT) scalafmtSbt
	$(SBT) scalafix

.PHONY: test
test:
	$(SBT) test

.PHONY: integration-test
integration-test:
	$(SBT) 'IntegrationTest / test'

.PHONY: compile
compile:
	$(SBT) assembly

.PHONY: generate-local-cert
generate-local-cert:
	SERVER_NAME="localhost" ALT_SERVER_NAMES="10.0.2.2" ./bin/generate-ssl-cert.sh /tmp/certs

.PHONY: run
run:
	$(SBT) "run $(RUN_ARGS)"

.PHONY: setup-buildx
setup-buildx:
	@if ! $(DOCKER) buildx ls | grep -q $(BUILDER_NAME); then \
		$(DOCKER) buildx create --name $(BUILDER_NAME) --use; \
	else \
		$(DOCKER) buildx use $(BUILDER_NAME); \
	fi

.PHONY: package
package: compile setup-buildx
	$(DOCKER_BUILDX) --load -t $(IMAGE):latest .

# Maybe not working/needed
#.PHONY: docker-run
#docker-run:
#	$(DOCKER) run --rm -p $(DOCKER_PORT):8080 -e SERVER_NAME=localhost -e ADMIN_PASSWORD=password $(IMAGE):latest

.PHONY: release
release: compile setup-buildx
	$(DOCKER_BUILDX) --push -t $(IMAGE):latest -t $(IMAGE):$(VERSION) .
