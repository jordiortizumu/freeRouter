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

#ifndef _EG_CTL_HAIRPIN_P4_
#define _EG_CTL_HAIRPIN_P4_

control EgressControlHairpin(inout headers hdr, inout ingress_metadata_t eg_md,
                             in egress_intrinsic_metadata_t eg_intr_md,
                             inout egress_intrinsic_metadata_for_deparser_t eg_dprsr_md)
{


    action act_set_recir(SubIntId_t port) {
//        eg_intr_md.egress_spec = (PortId_t)port;
        hdr.cpu.setValid();
        hdr.cpu._padding = 0;
        hdr.cpu.port = port;
    }

    table tbl_hairpin {
        key = {
eg_md.output_id:
            exact;
        }
        actions = {
            act_set_recir;
            @defaultonly NoAction;
        }
        size = BUNDLE_TABLE_SIZE;
        const default_action = NoAction();
    }

    apply {
        tbl_hairpin.apply();
    }
}

#endif // _EG_CTL_HAIRPIN_P4_

