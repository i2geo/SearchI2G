#!/bin/bash
TMP=`mktemp ./TMPXXXXXX`
wget --output-document=- 2>/dev/null "http://i2geo.net/xwiki/bin/view/$1?xpage=xml&language=$2" > $TMP
xsltproc fetchContent.xslt $TMP
rm $TMP
