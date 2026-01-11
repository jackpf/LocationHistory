VERSION ?= $(error VERSION var is not set)

.PHONY: package-local
package-local:
	make -C client build-debug # Can probably be something better when client has proper build scripts
	make -C server package-local
	make -C ui package-local
	make -C ui package-local-proxy

.PHONY: check-version
check-version:
	@echo "$(VERSION)" | grep -Eq '^v[0-9]+\.[0-9]+\.[0-9]+$$' || \
		(echo "Error: VERSION must follow the pattern v[0-9]+.[0-9]+.[0-9]+, got: $(VERSION)"; exit 1)
	@echo "Version $(VERSION) is valid"

.PHONY: release
release: check-version
	git tag v$(VERSION)
	git push origin v$(VERSION)
