.PHONY: build-local
package-local:
	make -C client build-debug # Can probably be something better when client has proper build scripts
	make -C server package-local
	make -C ui package-local
	make -C ui package-local-proxy