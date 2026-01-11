VERSION ?= "0.1.0-SNAPSHOT"

.PHONY: clean
clean:
	./gradlew clean

.PHONY: lint
lint:
	./gradlew lint

.PHONY: build-debug
build-debug:
	./gradlew assembleDebug -PversionName=$(VERSION)

.PHONY: build-release
build-release:
	./gradlew assembleRelease -PversionName=$(VERSION)
