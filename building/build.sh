#!/bin/bash

cd ../

java -jar building/sbt-launch.jar assembly
cp target/scala-2.13/MoneySaver-assembly-0.1.jar ./building/

cd building

java -jar allatori.jar  allatori.xml

rm MoneySaver-assembly-0.1.jar
