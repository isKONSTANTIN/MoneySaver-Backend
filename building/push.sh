#!/bin/bash

echo "Pushing $BRANCH..."

cat ./pass | docker login https://dockreg.knst.su/ --username ms_push --password-stdin
docker build -t ms_backend.$BRANCH .
docker tag ms_backend.$BRANCH dockreg.knst.su/ms_backend.$BRANCH
docker push dockreg.knst.su/ms_backend.$BRANCH
