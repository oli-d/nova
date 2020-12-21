#!/bin/bash

#
# Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
#
# This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
# work for additional information regarding copyright ownership. You may also obtain a copy of the license at
#
#      https://squaredesk.ch/license/oss/LICENSE
#

git tag -a -m "$1" $1
git push --tags
#mvn deploy -Prelease -DdeployAtEnd=true
#stupid:mvn deploy -Prelease -Dmaven.test.skip=true -pl '!comm,!core,!event-annotations,!http,!http-spring,!http-test-utils,!jms,!kafka,!metrics-elastic,!metrics-kafka,!metrics-serialization,!parentpom,!rest,!rest-test-utils,!service,!spring-support,!test-utils,!websockets,!websockets-annotations'
#   should be -N instead (test!)
