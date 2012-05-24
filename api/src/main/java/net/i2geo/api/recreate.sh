#!/bin/bash

DIR=`which $0`
cd `dirname $DIR`
langs=`./fetchlanguages.sh`
TMP=`mktemp ./TMPXXXXXX`
for l in $langs ; do
  ./fetchpageSource.sh Translations/SKBi18n $l > $TMP
  native2ascii -encoding utf-8 $TMP SKBi18nPhrases_${l}.properties
  rm $TMP
done
