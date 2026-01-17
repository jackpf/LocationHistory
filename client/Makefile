VERSION ?= 0.1.0-SNAPSHOT

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

.PHONY: test
test:
	./gradlew testDebugUnitTest

.PHONY: integration-test
integration-test:
	#./gradlew connectedDebugAndroidTest -Pandroid.experimental.androidTest.useUnifiedTestPlatform=false --no-daemon --info
	./gradlew assembleDebug assembleDebugAndroidTest
	adb install -t app/build/outputs/apk/debug/app-debug.apk
	adb install -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
	adb shell am instrument -w -e no-window true com.jackpf.locationhistory.client.test/androidx.test.runner.AndroidJUnitRunner

