#!/bin/sh
echo signing
java -Xmx256m -jar rtr.jar show version number | dos2unix | head -n 1 > rtr.txt
echo -n "version="
cat rtr.txt
java -Xmx256m -jar rtr.jar test verfile http://10.5.1.10/freertr2.key rtr.zip@../rtr.zip
java -Xmx256m -jar rtr.jar test vercore http://10.5.1.10/freertr2.key http://10.5.1.10/freertr2.key
echo verifying
java -Xmx256m -jar rtr.jar exec flash verify
