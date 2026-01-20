DOCKER ?= docker
BUILDER_NAME = $(APP_NAME)-builder
DOCKER_BUILDX = $(DOCKER) buildx build $(if $(PLATFORM),--platform $(PLATFORM))
USE_DOCKER ?= false
PLATFORM ?=
ifeq ($(USE_DOCKER),true)
    SBT = docker volume create build-cache && $(DOCKER) run --rm \
    	$(if $(PLATFORM),--platform $(PLATFORM),) \
    	-v .:/app \
    	-v build-cache:/root \
    	-w /app \
    	sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.0_3.7.4 sbt
else
    SBT ?= sbt
endif

.PHONY: clean
clean:
	$(SBT) clean

.PHONY: compile
compile:
	$(SBT) compile

.PHONY: publish
publish:
	$(SBT) publishLocal publishM2