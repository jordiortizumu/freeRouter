#!/bin/sh
java -Xmx512m -jar rtr.jar test tester p4lang- other p4lang1.ini summary slot 122 paralell 10 retry 16 url http://sources.freertr.org/cfg/ $@
./te.sh
