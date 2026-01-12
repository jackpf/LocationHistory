.PHONY: setup-buildx
setup-buildx:
	@if ! $(DOCKER) buildx ls | grep -q $(BUILDER_NAME); then \
		$(DOCKER) buildx create --name $(BUILDER_NAME) --use --bootstrap; \
	else \
		$(DOCKER) buildx use $(BUILDER_NAME); \
	fi