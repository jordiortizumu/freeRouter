from ..bf_gbl_env.var_env import *


def writeMroute6rules(
    self, op_type, vrf, sess, dip, sip, ingr, port, subif, smac, dmac
):
    if self.mcast == False:
        return
    nodid = (sess << 16 ) | subif;
    if op_type != 3:
        self.mcast_nid.append(nodid)
        self.mcast_xid.append(0)
    tbl_global_path = "eg_ctl.eg_ctl_mcast"
    tbl_name = "%s.tbl_mcast" % (tbl_global_path)
    tbl_action_name = "%s.act_rawip" % (tbl_global_path)
    key_field_list = [
        gc.KeyTuple("hdr.internal.clone_session", sess),
        gc.KeyTuple("eg_intr_md.egress_rid", subif),
    ]
    data_field_list = [
         gc.DataTuple("src_mac_addr", smac),
         gc.DataTuple("dst_mac_addr", dmac),
    ]
    key_annotation_fields = {}
    data_annotation_fields = {"dst_mac_addr": "mac", "src_mac_addr": "mac"}
    self._processEntryFromControlPlane(
        op_type,
        tbl_name,
        key_field_list,
        data_field_list,
        tbl_action_name,
        key_annotation_fields,
        data_annotation_fields,
    )
    self._processMcastNodeFromControlPlane(
        op_type,
        nodid,
        subif,
        self.hairpin2recirc(port),
    )
