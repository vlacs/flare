#!/bin/bash
lein voom freshen
lein voom build-deps

if [ ! -f flare-config.edn ]; then
  echo "\nI didn't find your flare-conf.edn, I'll copy it for you.\n"
  cp flare-config-dist.edn flare-config.edn
fi

lein test
