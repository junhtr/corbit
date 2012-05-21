#!/bin/sh

function usage {
  echo 'Usage: ./create_dict.sh (source-file) (output-file)'
  exit -1
}

if [ $# -lt 2 ]; then
  usage
fi

java -enableassertions -server -Xmx10g -XX:+AggressiveOpts -XX:+UseParallelGC -classpath "bin;lib/pcj-1.2.jar;lib/patricia-trie-0.6.jar" corbit.segdep.Program CreateDict ctb7 $1 $2

