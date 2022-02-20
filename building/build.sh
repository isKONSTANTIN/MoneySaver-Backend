#!/bin/bash

cd ../

./gradlew build
cp build/libs/MoneySaver.jar ./building/MoneySaver-raw.jar

cd building

java -jar allatori.jar allatori.xml

rm MoneySaver-raw.jar
