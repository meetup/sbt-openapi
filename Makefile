CI_BUILD_NUMBER ?= $(USER)-SNAPSHOT

TARGET ?= __package-sbt

VERSION ?= 0.0.$(CI_BUILD_NUMBER)

help:
	@echo Public targets:
	@grep -E '^[^_]{2}[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo "Private targets: (use at own risk)"
	@grep -E '^__[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[35m%-20s\033[0m %s\n", $$1, $$2}'

version: ## Print the current version.
	@echo $(VERSION)
