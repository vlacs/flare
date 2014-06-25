#!/bin/bash
lein voom freshen
lein voom build-deps

if [ ! -f flare-conf.edn ]; then
  echo "\nI didn't find your flare-conf.edn, I'll copy it for you.\n"
  cp flare-conf-dist.edn flare-conf.edn
fi

lein test
