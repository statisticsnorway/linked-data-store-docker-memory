#!/usr/bin/env bash

ENV_FILE='docker-compose.env'
if [ -f $ENV_FILE ]; then
    export $(grep -v '^#' $ENV_FILE | xargs -0)
fi

docker-compose -f docker-compose.yml up --remove-orphans
