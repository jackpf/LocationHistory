VERSION ?= $(error VERSION var is not set)

.PHONY: clean
clean:
	make -C server clean
	make -C client clean
	make -C ui clean

.PHONY: package-local
package-local:
	make -C server package-local
	make -C client build-debug
	make -C ui package-local
	make -C ui package-local-proxy

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
