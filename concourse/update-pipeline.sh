#!/bin/bash
# update the concourse pipeline with this script.

fly_status=`fly status -t devex`
if [ "$fly_status" == "logged in successfully" ]
then
   echo $fly_status
else
    fly login -t devex -n devex -c https://concourse.faunadb.net/
fi

fly -t devex set-pipeline -p jvm-driver-release-v4 -c concourse/pipeline.yml
