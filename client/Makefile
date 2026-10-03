VERSION ?= 0.1.0-SNAPSHOT

.PHONY: clean
clean:
	./gradlew clean

.PHONY: lint-foss lint-gms
lint-foss:
	./gradlew lintFossDebug
lint-gms:
	./gradlew lintGmsDebug

.PHONY: build-foss-debug build-gms-debug
build-foss-debug:
	./gradlew assembleFossDebug -PversionName=$(VERSION)
build-gms-debug:
	./gradlew assembleGmsDebug -PversionName=$(VERSION)

.PHONY: build-foss-release build-gms-release
build-foss-release:
	./gradlew assembleFossRelease -PversionName=$(VERSION)
build-gms-release:
	./gradlew assembleGmsRelease -PversionName=$(VERSION)

.PHONY: test-foss test-gms
test-foss:
	./gradlew testFossDebugUnitTest
test-gms:
	./gradlew testGmsDebugUnitTest

.PHONY: integration-test
integration-test:
	./gradlew connectedDebugAndroidTest
