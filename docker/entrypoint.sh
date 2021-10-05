#!/usr/bin/env bash

# do not exec, because exec disables traps
if [[ "$#" != "0" ]]; then
  "$@"
else
  bash --login
fi