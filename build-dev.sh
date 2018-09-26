#!/usr/bin/env bash

mvn clean verify dependency:copy-dependencies &&\
docker build -t lds-memory:dev -f Dockerfile-dev .
