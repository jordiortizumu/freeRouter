/*
 * Copyright 2019-present GT RARE project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed On an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#ifndef _IG_CTL_NAT_P4_
#define _IG_CTL_NAT_P4_

#ifdef HAVE_NAT

control IngressControlNAT(inout headers hdr, inout ingress_metadata_t ig_md,
                          in ingress_intrinsic_metadata_t ig_intr_md)
{

    DirectCounter< bit<64> > (CounterType_t.PACKETS_AND_BYTES) stats4;
    DirectCounter< bit<64> > (CounterType_t.PACKETS_AND_BYTES) stats6;

    action act_deny() {
    }

    action act_permit() {
        ig_md.ipv4_valid = 0;
        ig_md.ipv6_valid = 0;
        ig_md.nexthop_id = CPU_PORT;
    }


    action act_rewrite_ipv4prt17(ipv4_addr_t srcadr, ipv4_addr_t trgadr, layer4_port_t srcprt, layer4_port_t trgprt) {
        stats4.count();
        hdr.ipv4.src_addr = srcadr;
        hdr.ipv4.dst_addr = trgadr;
        hdr.udp.src_port = srcprt;
        hdr.udp.dst_port = trgprt;
        hdr.udp.checksum = 0;
        ig_md.layer4_srcprt = srcprt;
        ig_md.layer4_dstprt = trgprt;
        ig_md.natted_ipv4udp = 1;
    }

    action act_rewrite_ipv4prt6(ipv4_addr_t srcadr, ipv4_addr_t trgadr, layer4_port_t srcprt, layer4_port_t trgprt) {
        stats4.count();
        hdr.ipv4.src_addr = srcadr;
        hdr.ipv4.dst_addr = trgadr;
        hdr.tcp.src_port = srcprt;
        hdr.tcp.dst_port = trgprt;
        hdr.tcp.checksum = 0;
        ig_md.layer4_srcprt = srcprt;
        ig_md.layer4_dstprt = trgprt;
        ig_md.natted_ipv4tcp = 1;
    }

    action act_rewrite_ipv6prt17(ipv6_addr_t srcadr, ipv6_addr_t trgadr, layer4_port_t srcprt, layer4_port_t trgprt) {
        stats6.count();
        hdr.ipv6.src_addr = srcadr;
        hdr.ipv6.dst_addr = trgadr;
        hdr.udp.src_port = srcprt;
        hdr.udp.dst_port = trgprt;
        hdr.udp.checksum = 0;
        ig_md.layer4_srcprt = srcprt;
        ig_md.layer4_dstprt = trgprt;
        ig_md.natted_ipv6udp = 1;
    }

    action act_rewrite_ipv6prt6(ipv6_addr_t srcadr, ipv6_addr_t trgadr, layer4_port_t srcprt, layer4_port_t trgprt) {
        stats6.count();
        hdr.ipv6.src_addr = srcadr;
        hdr.ipv6.dst_addr = trgadr;
        hdr.tcp.src_port = srcprt;
        hdr.tcp.dst_port = trgprt;
        hdr.tcp.checksum = 0;
        ig_md.layer4_srcprt = srcprt;
        ig_md.layer4_dstprt = trgprt;
        ig_md.natted_ipv6tcp = 1;
    }

    table tbl_ipv4_nat_trns {
        key = {
ig_md.layer4_srcprt:
            exact;
ig_md.layer4_dstprt:
            exact;
hdr.ipv4.src_addr:
            exact;
hdr.ipv4.dst_addr:
            exact;
ig_md.vrf:
            exact;
hdr.ipv4.protocol:
            exact;
        }
        actions = {
            act_rewrite_ipv4prt17;
            act_rewrite_ipv4prt6;
            @defaultonly NoAction;
        }
        size = IPV4_NATTRNS_TABLE_SIZE;
        const default_action = NoAction();
        counters = stats4;
    }

    table tbl_ipv6_nat_trns {
        key = {
ig_md.layer4_srcprt:
            exact;
ig_md.layer4_dstprt:
            exact;
hdr.ipv6.src_addr:
            exact;
hdr.ipv6.dst_addr:
            exact;
ig_md.vrf:
            exact;
hdr.ipv6.next_hdr:
            exact;
        }
        actions = {
            act_rewrite_ipv6prt17;
            act_rewrite_ipv6prt6;
            @defaultonly NoAction;
        }
        size = IPV6_NATTRNS_TABLE_SIZE;
        const default_action = NoAction();
        counters = stats6;
    }

    table tbl_ipv4_nat_cfg {
        key = {
ig_md.vrf:
            exact;
hdr.ipv4.protocol:
            ternary;
hdr.ipv4.src_addr:
            ternary;
hdr.ipv4.dst_addr:
            ternary;
ig_md.layer4_srcprt:
            ternary;
ig_md.layer4_dstprt:
            ternary;
hdr.ipv4.diffserv:
            ternary;
hdr.ipv4.identification:
            ternary;
#ifdef HAVE_SGT
ig_md.sec_grp_id:
            ternary;
#endif
        }
        actions = {
            act_permit;
            act_deny;
            @defaultonly NoAction;
        }
        size = IPV4_NATACL_TABLE_SIZE;
        const default_action = NoAction();
    }

    table tbl_ipv6_nat_cfg {
        key = {
ig_md.vrf:
            exact;
hdr.ipv6.next_hdr:
            ternary;
hdr.ipv6.src_addr:
            ternary;
hdr.ipv6.dst_addr:
            ternary;
ig_md.layer4_srcprt:
            ternary;
ig_md.layer4_dstprt:
            ternary;
hdr.ipv6.traffic_class:
            ternary;
hdr.ipv6.flow_label:
            ternary;
#ifdef HAVE_SGT
ig_md.sec_grp_id:
            ternary;
#endif
        }
        actions = {
            act_permit;
            act_deny;
            @defaultonly NoAction;
        }
        size = IPV6_NATACL_TABLE_SIZE;
        const default_action = NoAction();
    }

    apply {
        if (ig_md.ipv4_valid==1)  {
            if (!tbl_ipv4_nat_trns.apply().hit) {
                tbl_ipv4_nat_cfg.apply();
            }
        } else if (ig_md.ipv6_valid==1)  {
            if (!tbl_ipv6_nat_trns.apply().hit) {
                tbl_ipv6_nat_cfg.apply();
            }
        }
    }
}

#endif

#endif // _IG_CTL_NAT_P4_

