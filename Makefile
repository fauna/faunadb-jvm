RUNTIME_IMAGE ?= openjdk:8-slim
DOCKER_RUN_FLAGS = -it --rm

ifdef FAUNA_ROOT_KEY
DOCKER_RUN_FLAGS += -e FAUNA_ROOT_KEY=$(FAUNA_ROOT_KEY)
endif

ifdef FAUNA_DOMAIN
DOCKER_RUN_FLAGS += -e FAUNA_DOMAIN=$(FAUNA_DOMAIN)
endif

ifdef FAUNA_SCHEME
DOCKER_RUN_FLAGS += -e FAUNA_SCHEME=$(FAUNA_SCHEME)
endif

ifdef FAUNA_PORT
DOCKER_RUN_FLAGS += -e FAUNA_PORT=$(FAUNA_PORT)
endif

ifdef FAUNA_TIMEOUT
DOCKER_RUN_FLAGS += -e FAUNA_TIMEOUT=$(FAUNA_TIMEOUT)
endif

test:
	sbt android:package test

jenkins-test:
	sbt clean; \
	sbt android:package; \
	sbt test; \
	result=$$?; \
	cp */target/test-reports/*.xml results/; \
	exit $$result

docker-wait:
	dockerize -wait $(FAUNA_SCHEME)://$(FAUNA_DOMAIN):$(FAUNA_PORT)/ping -timeout $(FAUNA_TIMEOUT)
