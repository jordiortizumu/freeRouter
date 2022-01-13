from ..bf_gbl_env.var_env import *


def writeCopp4Rules(
    self, op_type, pri, act, pr, prm, sa, sam, da, dam, sp, spm, dp, dpm, ts, tsm, fl, flm
):
    tbl_global_path = "ig_ctl.ig_ctl_copp"
    tbl_name = "%s.tbl_ipv4_copp" % (tbl_global_path)
    tbl_action_name = "%s.act_%s" % (tbl_global_path, act)
    key_field_list = [
        gc.KeyTuple("$MATCH_PRIORITY", pri),
        gc.KeyTuple("hdr.ipv4.protocol", pr, prm),
        gc.KeyTuple("hdr.ipv4.src_addr", sa, sam),
        gc.KeyTuple("hdr.ipv4.dst_addr", da, dam),
        gc.KeyTuple("ig_md.layer4_srcprt", sp, spm),
        gc.KeyTuple("ig_md.layer4_dstprt", dp, dpm),
        gc.KeyTuple("hdr.ipv4.diffserv", ts, tsm),
        gc.KeyTuple("hdr.ipv4.identification", fl, flm),
    ]
    data_field_list = []
    key_annotation_fields = {
        "hdr.ipv4.src_addr": "ipv4",
        "hdr.ipv4.dst_addr": "ipv4",
    }
    data_annotation_fields = {}
    self._processEntryFromControlPlane(
        op_type,
        tbl_name,
        key_field_list,
        data_field_list,
        tbl_action_name,
        key_annotation_fields,
        data_annotation_fields,
    )
