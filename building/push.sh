#!/bin/bash

BRANCH=$CI_COMMIT_BRANCH
BRANCH=${BRANCH:-$(git rev-parse --abbrev-ref HEAD)}

echo "Pushing $BRANCH..."

docker build -t ms_backend .
cat ./pass | docker login https://dockreg.knst.su/ --username ms_push --password-stdin
docker tag ms_backend dockreg.knst.su/ms_backend.$BRANCH
docker push dockreg.knst.su/ms_backend.$BRANCH
