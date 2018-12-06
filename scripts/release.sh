#!/bin/bash

git tag -a -m "$1" $1
git push --tags
#mvn deploy -Prelease
