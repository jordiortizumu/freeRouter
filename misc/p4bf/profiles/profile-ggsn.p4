#ifdef PROFILE_GGSN
#define HAVE_ALPM
#define HAVE_GTP
#define HAVE_TUN
##define HAVE_MPLS
##define HAVE_FLOWSPEC
##define HAVE_COPP
##define HAVE_INACL
##define HAVE_INQOS
##define HAVE_OUTACL
##define HAVE_OUTQOS
##define HAVE_PBR

#define PORT_TABLE_SIZE                24576

#define BUNDLE_TABLE_SIZE                      128

#define VLAN_TABLE_SIZE                        512

#define IPV4_LPM_TABLE_SIZE            147456

#define IPV6_LPM_TABLE_SIZE            36864

#define IPV4_HOST_TABLE_SIZE                   256
#define IPV6_HOST_TABLE_SIZE                   256

#define NEXTHOP_TABLE_SIZE             24576


#define IPV4_TUNNEL_TABLE_SIZE         24576
#define IPV6_TUNNEL_TABLE_SIZE         12288
#define _TABLE_SIZE_P4_
#endif
