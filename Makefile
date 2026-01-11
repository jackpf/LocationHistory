VERSION ?= $(error VERSION var is not set)

.PHONY: package-local
package-local:
	make -C client build-debug # Can probably be something better when client has proper build scripts
	make -C server package-local
	make -C ui package-local
	make -C ui package-local-proxy

.PHONY: release
release:
#	git tag v1.0.0
#	git push origin v1.0.0
	echo $(VERSION)