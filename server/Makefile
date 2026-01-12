APP_NAME = location-history-server
IMAGE = jackpfarrelly/$(APP_NAME)
VERSION ?= 0.1.0-SNAPSHOT
DOCKER ?= docker
BUILDER_NAME = $(APP_NAME)-builder
DOCKER_BUILDX = $(DOCKER) buildx build $(if $(PLATFORM),--platform $(PLATFORM),)
USE_DOCKER ?= false
PLATFORM ?=
RUN_ARGS ?=
ifeq ($(USE_DOCKER),true)
    SBT = docker volume create build-cache && $(DOCKER) run --rm \
    	$(if $(PLATFORM),--platform $(PLATFORM),) \
    	-v .:/app \
    	-v build-cache:/root \
    	-w /app \
    	sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.0_3.7.4 sbt
else
    SBT = sbt
endif

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
		$(DOCKER) buildx create --name $(BUILDER_NAME) --use --bootstrap; \
	else \
		$(DOCKER) buildx use $(BUILDER_NAME); \
	fi

.PHONY: package
package: compile setup-buildx
	$(DOCKER_BUILDX) --load -t $(IMAGE):latest .

.PHONY: release
release: compile setup-buildx
	$(DOCKER_BUILDX) --push -t $(IMAGE):latest -t $(IMAGE):$(VERSION) .
