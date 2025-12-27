SBT ?= sbt
RUN_ARGS ?=
DOCKER ?= docker
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
	$(DOCKER) build -t $(APP_NAME):latest .

.PHONY: run
run:
	$(SBT) "run $(RUN_ARGS)"

.PHONY: docker-run
docker-run: package
	$(DOCKER) run --rm -p $(DOCKER_PORT):8080 $(APP_NAME):latest

.PHONY: docker-publish
docker-publish: package
	$(DOCKER) push $(APP_NAME):latest