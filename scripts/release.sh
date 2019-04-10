#!/bin/bash

git tag -a -m "$1" $1
git push --tags
#mvn deploy -Prelease -DdeployAtEnd=true
#stupid:mvn deploy -Prelease -Dmaven.test.skip=true -pl '!comm,!core,!event-annotations,!http,!http-spring,!http-test-utils,!jms,!kafka,!metrics-elastic,!metrics-kafka,!metrics-serialization,!parentpom,!rest,!rest-test-utils,!service,!spring-support,!test-utils,!websockets,!websockets-annotations'
#   should be -N instead (test!)
