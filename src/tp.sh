#!/bin/sh
java -Xmx256m -jar rtr.jar test tester p4lang- other ../../img/p4lang.img e1000 1024 summary retry url http://sources.nop.hu/cfg/ $1 $2 $3 $4 $5 $6 $7 $8
