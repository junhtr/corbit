#!/bin/sh

mode=$1
shift

function usage {
  echo 'Usage: ./corbit.sh <sp|spd|d|pd> (arguments..)'
  echo 'sp:  joint word segmentation and POS tagging model by Zhang & Clark (2010)'
  echo 'spd: joint word segmentation, POS tagging, and dependency parsing model by Hatori et al. (2012)'
  echo 'd:   dependency parsing model by Huang & Sagae (2010) or Zhang & Nivre (2011)'
  echo 'pd:  joint POS tagging and dependency parsing model by Hatori et al. (2011)'
  exit -1
}

if [ ${#mode} -eq 0 ]; then
  usage
elif [ $mode = 'sp' ]; then
  class='corbit.segdep.Program'
  option='--no-parse'
elif [ $mode = 'spd' ]; then
  class='corbit.segdep.Program'
  option=''
elif [ $mode = 'pd' ]; then
  class='corbit.tagdep.Program'
  option=''
elif [ $mode = 'd' ]; then
  class='corbit.tagdep.Program'
  option='--assign-gold --gold-pos'
else
  usage
fi

#java -enableassertions -server -Xmx10g -XX:+AggressiveOpts -XX:+UseParallelGC -classpath "bin;lib/pcj-1.2.jar;lib/patricia-trie-0.6.jar" $class $@ $option
java -server -Xmx10g -XX:+AggressiveOpts -XX:+UseParallelGC -classpath "bin;lib/pcj-1.2.jar;lib/patricia-trie-0.6.jar" $class $@ --parallel 8 $option

