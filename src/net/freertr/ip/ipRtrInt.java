package net.freertr.ip;

import java.util.Comparator;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrIPv6;
import net.freertr.cfg.cfgIfc;
import net.freertr.tab.tabIntUpdater;
import net.freertr.tab.tabListing;
import net.freertr.tab.tabRoute;
import net.freertr.tab.tabRouteEntry;
import net.freertr.tab.tabRtrmapN;
import net.freertr.tab.tabRtrplcN;

/**
 * interface import into routers
 *
 * @author matecsaba
 */
public class ipRtrInt implements Comparator<ipRtrInt> {

    /**
     * interface to import
     */
    public final cfgIfc iface;

    /**
     * route map
     */
    public tabListing<tabRtrmapN, addrIP> roumap;

    /**
     * route policy
     */
    public tabListing<tabRtrplcN, addrIP> rouplc;

    /**
     * metric
     */
    public tabIntUpdater metric;

    /**
     * tag
     */
    public tabIntUpdater tag;

    /**
     * create advertisement
     *
     * @param ifc interface to redistribute
     */
    public ipRtrInt(cfgIfc ifc) {
        iface = ifc;
    }

    public int compare(ipRtrInt o1, ipRtrInt o2) {
        return o1.iface.compare(o1.iface, o2.iface);
    }

    /**
     * filter by this redistribution
     *
     * @param afi address family
     * @param trg target table to append
     * @param src source table to use
     * @param lower forwarder
     */
    public void filter(int afi, tabRoute<addrIP> trg, tabRoute<addrIP> src, ipFwd lower) {
        addrIP adr = new addrIP();
        if (lower.ipVersion == ipCor4.protocolVersion) {
            adr.fromIPv4addr(new addrIPv4());
        } else {
            adr.fromIPv6addr(new addrIPv6());
        }
        ipFwdIface ifc = null;
        try {
            ifc = iface.getFwdIfc(adr);
        } catch (Exception e) {
        }
        if (ifc == null) {
            return;
        }
        if (!ifc.ready) {
            return;
        }
        if (ifc.network == null) {
            return;
        }
        tabRouteEntry<addrIP> ntry = src.find(ifc.network);
        if (ntry == null) {
            return;
        }
        if (metric != null) {
            ntry = ntry.copyBytes(tabRoute.addType.notyet);
            ntry.best.metric = metric.update(ntry.best.metric);
        }
        if (tag != null) {
            ntry = ntry.copyBytes(tabRoute.addType.notyet);
            ntry.best.tag = tag.update(ntry.best.tag);
        }
        tabRoute.addUpdatedEntry(tabRoute.addType.better, trg, afi, 0, ntry, true, roumap, rouplc, null);
    }
}
