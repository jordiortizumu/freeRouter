description dtls test

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
int lo0
 vrf for v1
 ipv4 addr 2.2.2.2 255.255.255.255
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr 1234::1 ffff::
 exit
crypto rsakey rsa generate 2048
crypto dsakey dsa generate 1024
crypto ecdsakey ecdsa generate 256
crypto certificate dsa generate dsa dsa
crypto certificate rsa generate rsa rsa
crypto certificate ecdsa generate ecdsa ecdsa
server udptn udptn
 security rsakey rsa
 security dsakey dsa
 security ecdsakey ecdsa
 security rsacert rsa
 security dsacert dsa
 security ecdsacert ecdsa
 security protocol dtls
 vrf v1
 port 666
 exit
!

addrouter r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
!
vrf def v1
 rd 1:1
 exit
int eth1
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.0
 ipv6 addr 1234::2 ffff::
 exit
!


r1 tping 100 5 1.1.1.2 vrf v1
r2 send telnet 1.1.1.1 666 vrf v1 dtls
r2 tping 100 5 2.2.2.2 vrf v1
