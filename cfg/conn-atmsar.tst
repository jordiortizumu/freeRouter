description atmsar encapsulation

addrouter r1
int atm1 atm - $1a$ $1b$
!
vrf def v1
 rd 1:1
 exit
int atm1
 atmsar vpi 123
 atmsar vci 1234
 vrf for v1
 ipv4 addr 1.1.1.1 255.255.255.0
 ipv6 addr 1234::1 ffff::
 exit
!

addrouter r2
int atm1 atm - $1b$ $1a$
!
vrf def v1
 rd 1:1
 exit
int atm1
 atmsar vpi 123
 atmsar vci 1234
 vrf for v1
 ipv4 addr 1.1.1.2 255.255.255.0
 ipv6 addr 1234::2 ffff::
 exit
!

r1 tping 100 5 1.1.1.2 vrf v1
r2 tping 100 5 1.1.1.1 vrf v1
r1 tping 100 5 1234::2 vrf v1
r2 tping 100 5 1234::1 vrf v1

r1 output show interface atm1 full
output ../binTmp/conn-atmsar.html
<html><body bgcolor="#000000" text="#FFFFFF" link="#00FFFF" vlink="#00FFFF" alink="#00FFFF">
here is the interface:
<pre>
<!>show:0
</pre>
</body></html>
!
