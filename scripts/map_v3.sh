#!/bin/bash

# Bash v3 does not support associative arrays
# Usage: map_put map_name key value
function map_put
{
    alias "${1}$2"="$3"
}

# map_get map_name key
function map_get
{
    alias "${1}$2" | awk -F"'" '{ print $2; }'
}

# map_keys map_name
function map_keys
{
    alias -p | grep $1 | cut -d'=' -f1 | awk -F"$1" '{print $2; }'
}

