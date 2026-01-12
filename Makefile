VERSION ?= $(error VERSION var is not set)
TEST_SERVER ?= $(error TEST_SERVER var is not set)

.PHONY: clean
clean:
	make -C server clean
	make -C client clean
	make -C ui clean

.PHONY: package-local
package-local:
	VERSION=test make -C server package-local
	#make -C client build-debug
	#make -C ui package-local
	#make -C ui package-local-proxy

.PHONY: check-version
check-version:
	@echo "$(VERSION)" | grep -Eq '^v[0-9]+\.[0-9]+\.[0-9]+$$' || \
		(echo "Error: VERSION must follow the pattern v[0-9]+.[0-9]+.[0-9]+, got: $(VERSION)"; exit 1)
	@echo "Version $(VERSION) is valid"

.PHONY: check-status
check-status:
	@if [ -n "$$(git status --porcelain)" ]; then \
		echo "Error: Working directory is dirty. Commit or stash changes first."; \
		exit 1; \
	fi

.PHONY: release
release: check-status check-version
	@echo "Preparing release $(VERSION)..."
	git checkout main
	git pull origin main
	git tag $(VERSION)
	git push origin $(VERSION)
	@echo "✅ $(VERSION) pushed!"

.PHONY: push-test
push-test:
	@echo "Pushing latest tags to $(TEST_SERVER)..."
	docker save jackpfarrelly/location-history-server:test | ssh $(TEST_SERVER) "docker load"
	#docker save jackpfarrelly/location-history-ui:latest | ssh $(TEST_SERVER) "docker load"
	#docker save jackpfarrelly/location-history-ui-proxy:latest | ssh $(TEST_SERVER) "docker load"
	@echo "✅ Pushed!"

	@echo "♻️ Recreating containers..."
	ssh $(TEST_SERVER) "docker compose up -d --force-recreate location-history-server location-history-ui location-history-ui-proxy"
	@echo "✅ Deployed!"
