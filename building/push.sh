#!/bin/bash

cat ./pass | docker login https://dockreg.knst.su/ --username ms_push --password-stdin
docker tag ms_backend dockreg.knst.su/ms_backend
docker push dockreg.knst.su/ms_backend