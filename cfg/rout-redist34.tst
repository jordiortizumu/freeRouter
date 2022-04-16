description redistribution with tag

addrouter r1
int eth1 eth 0000.0000.1111 $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
router isis4 1
 vrf v1
 net 22.4444.0000.1111.00
 red conn tag set 1000
 exit
router isis6 1
 vrf v1
 net 22.6666.0000.1111.00
 red conn tag set 1000
 exit
int lo1
 vrf for v1
 ipv4 addr 2.2.2.1 255.255.255.255
 ipv6 addr 4321::1 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1.11
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.252
 router isis4 1 ena
 exit
int eth1.12
 vrf for v1
 ipv6 addr 1234:1::1 ffff:ffff::
 router isis6 1 ena
 exit
!

addrouter r2
int eth1 eth 0000.0000.2222 $1b$ $1a$
!
vrf def v1
 rd 1:1
 exit
route-map rm1
 sequence 10 act deny
 sequence 10 match tag 2000-4000
 sequence 20 act perm
 exit
router isis4 1
 vrf v1
 net 22.4444.0000.2222.00
 red conn
 both route-map-from rm1
 exit
router isis6 1
 vrf v1
 net 22.6666.0000.2222.00
 red conn
 both route-map-from rm1
 exit
int lo1
 vrf for v1
 ipv4 addr 2.2.2.2 255.255.255.255
 ipv6 addr 4321::2 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff
 exit
int eth1.11
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.252
 router isis4 1 ena
 exit
int eth1.12
 vrf for v1
 ipv6 addr 1234:1::2 ffff:ffff::
 router isis6 1 ena
 exit
!



r1 tping 100 20 2.2.2.2 vrf v1
r1 tping 100 20 4321::2 vrf v1
r2 tping 100 20 2.2.2.1 vrf v1
r2 tping 100 20 4321::1 vrf v1

r1 send conf t
r1 send router isis4 1
r1 send red conn tag set 3000
r1 send exit
r1 send router isis6 1
r1 send red conn tag set 3000
r1 send exit
r1 send end

r1 tping 100 20 2.2.2.2 vrf v1
r1 tping 100 20 4321::2 vrf v1
r2 tping 0 20 2.2.2.1 vrf v1
r2 tping 0 20 4321::1 vrf v1

r1 send conf t
r1 send router isis4 1
r1 send red conn tag set 5000
r1 send exit
r1 send router isis6 1
r1 send red conn tag set 5000
r1 send exit
r1 send end

r1 tping 100 20 2.2.2.2 vrf v1
r1 tping 100 20 4321::2 vrf v1
r2 tping 100 20 2.2.2.1 vrf v1
r2 tping 100 20 4321::1 vrf v1
