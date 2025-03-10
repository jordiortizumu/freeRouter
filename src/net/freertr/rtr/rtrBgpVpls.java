package net.freertr.rtr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrIPv6;
import net.freertr.addr.addrPrefix;
import net.freertr.cfg.cfgBrdg;
import net.freertr.cfg.cfgIfc;
import net.freertr.clnt.clntMplsPwe;
import net.freertr.pack.packLdpPwe;
import net.freertr.tab.tabGen;
import net.freertr.tab.tabLabel;
import net.freertr.tab.tabLabelEntry;
import net.freertr.tab.tabRoute;
import net.freertr.tab.tabRouteEntry;
import net.freertr.tab.tabRouteUtil;
import net.freertr.util.bits;
import net.freertr.util.debugger;
import net.freertr.util.logger;

/**
 * bgp4 vpls
 *
 * @author matecsaba
 */
public class rtrBgpVpls implements Comparator<rtrBgpVpls> {

    /**
     * id number
     */
    public long id;

    /**
     * bridge to use
     */
    public cfgBrdg bridge;

    /**
     * source interface
     */
    public cfgIfc iface;

    /**
     * control word
     */
    public boolean ctrlWrd;

    /**
     * ve id
     */
    public int veId;

    /**
     * ve max
     */
    public int veMax;

    /**
     * set true if advertised
     */
    public boolean adverted;

    private final rtrBgp parent;

    private tabGen<rtrBgpVplsPeer> peers = new tabGen<rtrBgpVplsPeer>();

    private tabLabelEntry[] veLab;

    /**
     * create new instance
     *
     * @param p parent to use
     */
    public rtrBgpVpls(rtrBgp p) {
        parent = p;
    }

    public int compare(rtrBgpVpls o1, rtrBgpVpls o2) {
        if (o1.id < o2.id) {
            return -1;
        }
        if (o1.id > o2.id) {
            return +1;
        }
        return 0;
    }

    /**
     * generate configuration
     *
     * @param l list to append
     * @param beg beginning
     */
    public void getConfig(List<String> l, String beg) {
        beg = beg + "afi-vpls " + tabRouteUtil.rd2string(id) + " ";
        l.add(beg + "bridge-group " + bridge.name);
        l.add(beg + "ve-id " + veId + " " + veMax);
        if (ctrlWrd) {
            l.add(beg + "control-word");
        }
        if (iface != null) {
            l.add(beg + "update-source " + iface.name);
        }
    }

    /**
     * advertise this vpls
     *
     * @param tab table to use
     */
    protected void doAdvertise(tabRoute<addrIP> tab) {
        adverted = false;
        if (id == 0) {
            return;
        }
        if (bridge == null) {
            return;
        }
        if (bridge.bridgeHed.rd == 0) {
            return;
        }
        if (iface == null) {
            return;
        }
        tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
        ntry.best.extComm = new ArrayList<Long>();
        if (veId == 0) {
            if (veLab != null) {
                tabLabel.release(veLab, 12);
                veLab = null;
            }
            ntry.prefix = rtrBgpUtil.defaultRoute(parent.afiUni);
            if (ntry.prefix.network.isIPv4()) {
                addrIPv4 adr = iface.addr4;
                if (adr == null) {
                    return;
                }
                ntry.prefix = addrPrefix.ip4toIP(new addrPrefix<addrIPv4>(adr, adr.maxBits()));
            } else {
                addrIPv6 adr = iface.addr6;
                if (adr == null) {
                    return;
                }
                ntry.prefix = addrPrefix.ip6toIP(new addrPrefix<addrIPv6>(adr, adr.maxBits()));
            }
            ntry.best.extComm.add(tabRouteUtil.agi2comm(id));
        } else {
            if (veLab == null) {
                veLab = tabLabel.allocate(12, veMax);
                if (veLab == null) {
                    return;
                }
            }
            ntry.prefix = new addrPrefix<addrIP>(new addrIP(), addrIP.size * 8);
            byte[] buf = new byte[addrIP.size];
            bits.msbPutW(buf, 0, veId);
            bits.msbPutW(buf, 2, 1);
            ntry.prefix.network.fromBuf(buf, 0);
            buf[0] = 5;
            bits.msbPutD(buf, 2, (veLab[0].label << 4) | 1);
            bits.msbPutW(buf, 1, veMax);
            ntry.prefix.wildcard.fromBuf(buf, 0);
            ntry.best.extComm.add(tabRouteUtil.l2info2comm(19, 0, bridge.bridgeHed.getMTUsize()));
        }
        ntry.best.extComm.add(tabRouteUtil.rt2comm(bridge.bridgeHed.rtExp));
        ntry.best.rouSrc = rtrBgpUtil.peerOriginate;
        ntry.rouDst = bridge.bridgeHed.rd;
        tab.add(tabRoute.addType.better, ntry, true, true);
        adverted = true;
    }

    /**
     * read peers in this vpls
     *
     * @param cmp computed routes
     */
    protected void doPeers(tabRoute<addrIP> cmp) {
        for (int i = 0; i < peers.size(); i++) {
            peers.get(i).needed = false;
        }
        long rt = tabRouteUtil.rt2comm(bridge.bridgeHed.rtImp);
        byte[] buf = new byte[addrIP.size];
        for (int i = 0; i < cmp.size(); i++) {
            tabRouteEntry<addrIP> ntry = cmp.get(i);
            if (ntry.best.rouSrc == rtrBgpUtil.peerOriginate) {
                continue;
            }
            if (ntry.best.extComm == null) {
                continue;
            }
            if (tabRouteUtil.findLongList(ntry.best.extComm, rt) < 0) {
                continue;
            }
            rtrBgpVplsPeer per = new rtrBgpVplsPeer(parent, this);
            if (veId == 0) {
                per.peer = ntry.prefix.network.copyBytes();
            } else {
                per.peer = ntry.best.nextHop.copyBytes();
            }
            rtrBgpVplsPeer old = peers.add(per);
            if (old != null) {
                old.needed = true;
                continue;
            }
            per.needed = true;
            if (debugger.rtrBgpEvnt) {
                logger.debug("start " + per);
            }
            if (veId != 0) {
                ntry.prefix.network.toBuffer(buf, 0);
                per.veId = bits.msbGetW(buf, 0);
                ntry.prefix.wildcard.toBuffer(buf, 0);
                per.veLab = (bits.msbGetD(buf, 2) & 0xffffff) >>> 4;
                per.brdg = bridge.bridgeHed.newIface(false, true, false);
                per.setUpper(per.brdg);
                continue;
            }
            clntMplsPwe pwom = new clntMplsPwe();
            per.clnt = pwom;
            pwom.pwType = packLdpPwe.pwtEthPort;
            pwom.pwMtu = bridge.bridgeHed.getMTUsize();
            pwom.target = "" + per.peer;
            pwom.vrf = parent.vrfCore;
            pwom.srcIfc = iface;
            pwom.vcid = tabRouteUtil.agi2comm(id);
            pwom.ctrlWrd = ctrlWrd;
            pwom.general = true;
            pwom.descr = null;
            per.brdg = bridge.bridgeHed.newIface(false, true, false);
            pwom.setUpper(per.brdg);
            pwom.workStart();
        }
        boolean[] usd = null;
        if ((veId != 0) && (veLab != null)) {
            usd = new boolean[veMax];
        }
        for (int i = peers.size() - 1; i >= 0; i--) {
            rtrBgpVplsPeer ntry = peers.get(i);
            if (ntry.needed) {
                if (usd == null) {
                    continue;
                }
                if (ntry.veId < 1) {
                    continue;
                }
                if (ntry.veId > veMax) {
                    continue;
                }
                veLab[ntry.veId - 1].setFwdPwe(12, parent.fwdCore, ntry.brdg, 0, null);
                usd[ntry.veId - 1] = true;
                continue;
            }
            if (debugger.rtrBgpEvnt) {
                logger.debug("stop " + ntry);
            }
            peers.del(ntry);
            ntry.brdg.closeUp();
            if (ntry.clnt == null) {
                continue;
            }
            ntry.clnt.workStop();
        }
        if (usd == null) {
            return;
        }
        for (int i = 0; i < usd.length; i++) {
            if (usd[i]) {
                continue;
            }
            veLab[i].setFwdDrop(12);
        }
    }

    /**
     * stop this vpls
     */
    public void doStop() {
        if (debugger.rtrBgpEvnt) {
            logger.debug("stop " + tabRouteUtil.rd2string(id));
        }
        if (veLab != null) {
            tabLabel.release(veLab, 12);
        }
        for (int i = 0; i < peers.size(); i++) {
            rtrBgpVplsPeer ntry = peers.get(i);
            ntry.brdg.closeUp();
            if (ntry.clnt == null) {
                continue;
            }
            ntry.clnt.workStop();
        }
    }

    /**
     * get peer list
     *
     * @param tab list to append
     */
    public void getPeerList(tabRoute<addrIP> tab) {
        for (int i = 0; i < peers.size(); i++) {
            rtrBgpVplsPeer nei = peers.get(i);
            if (nei == null) {
                continue;
            }
            tabRouteEntry<addrIP> ntry = new tabRouteEntry<addrIP>();
            ntry.prefix = new addrPrefix<addrIP>(nei.peer, addrIP.size * 8);
            tabRoute.addUpdatedEntry(tabRoute.addType.better, tab, rtrBgpUtil.sfiUnicast, 0, ntry, true, null, null, parent.routerAutoMesh);
        }
    }

}
