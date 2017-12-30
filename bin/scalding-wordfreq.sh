#!/usr/bin/env bash

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

yarn jar ./babar-scalding/target/babar-scalding-1.0-SNAPSHOT.jar \
    com.bhnte.babar.scalding.wordcount.WordFreqJob \
    --hdfs \
    --input "hdfs://localhost:54310/wordcount/input.txt" \
    --output "hdfs://localhost:54310/wordcount/output" \
    --agentJar "$SCRIPT_PATH/../babar-agent/target/babar-agent-1.0-SNAPSHOT.jar"

rm -rf wordcount_output