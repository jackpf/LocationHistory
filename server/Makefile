SBT ?= sbt
RUN_ARGS ?=
DOCKER ?= docker
DOCKER_BUILD_PLATFORMS = linux/amd64,linux/arm64
DOCKER_BUILDX = $(DOCKER) buildx create --use; $(DOCKER) buildx build --platform $(DOCKER_BUILD_PLATFORMS)
DOCKER_PORT ?= 8080

APP_NAME = jackpfarrelly/location-history-server

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

.PHONY: package
package: compile
	$(DOCKER_BUILDX) -t $(APP_NAME):latest .

.PHONY: run
run:
	$(SBT) "run $(RUN_ARGS)"

.PHONY: docker-run
docker-run: package
	$(DOCKER) run --rm -p $(DOCKER_PORT):8080 $(APP_NAME):latest

.PHONY: docker-publish
docker-publish: package
	$(DOCKER_BUILDX) -t $(APP_NAME):latest --push .