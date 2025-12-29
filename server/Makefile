APP_NAME = jackpfarrelly/location-history-server
SBT ?= sbt
RUN_ARGS ?=
DOCKER ?= docker
DOCKER_BUILD_PLATFORMS = linux/amd64,linux/arm64
DOCKER_BUILDX = $(DOCKER) buildx create --use; $(DOCKER) buildx build
DOCKER_BUILDX_MULTI = $(DOCKER_BUILDX) --platform $(DOCKER_BUILD_PLATFORMS)
DOCKER_BUILDX_LOCAL = $(DOCKER_BUILDX) --load
DOCKER_PORT ?= 8080

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

.PHONY: run
run:
	$(SBT) "run $(RUN_ARGS)"

.PHONY: package
package: compile
	$(DOCKER_BUILDX_MULTI) -t $(APP_NAME):latest .

.PHONY: package-local
package-local: compile
	$(DOCKER_BUILDX_LOCAL) -t $(APP_NAME):latest .

.PHONY: docker-run
docker-run: package-local
	$(DOCKER) run --rm -p $(DOCKER_PORT):8080 $(APP_NAME):latest

.PHONY: docker-publish
docker-publish: package
	$(DOCKER_BUILDX_MULTI) -t $(APP_NAME):latest --push .