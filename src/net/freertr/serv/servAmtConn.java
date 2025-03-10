package net.freertr.serv;

import java.util.Comparator;
import net.freertr.addr.addrIP;
import net.freertr.addr.addrIPv4;
import net.freertr.addr.addrIPv6;
import net.freertr.addr.addrMac;
import net.freertr.addr.addrType;
import net.freertr.cfg.cfgIfc;
import net.freertr.ifc.ifcDn;
import net.freertr.ifc.ifcEther;
import net.freertr.ifc.ifcNull;
import net.freertr.ifc.ifcUp;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipIcmp6;
import net.freertr.ip.ipMhost4;
import net.freertr.pack.packHolder;
import net.freertr.prt.prtGenConn;
import net.freertr.util.bits;
import net.freertr.util.counter;
import net.freertr.util.state;

/**
 * automatic multicast tunneling (rfc7450) protocol connection
 *
 * @author matecsaba
 */
public class servAmtConn implements ifcDn, Comparator<servAmtConn> {

    /**
     * connection
     */
    public prtGenConn conn;

    /**
     * lower layer
     */
    public servAmt lower;

    /**
     * counter
     */
    public counter cntr = new counter();

    /**
     * upper layer
     */
    public ifcUp upper = new ifcNull();

    /**
     * interface
     */
    public cfgIfc acesIfc;

    /**
     * nonce
     */
    public int nonce;

    /**
     * creation time
     */
    public long created;

    /**
     * response mac
     */
    public addrMac respmc = addrMac.getRandom();

    public int compare(servAmtConn o1, servAmtConn o2) {
        return o1.conn.compare(o1.conn, o2.conn);
    }

    /**
     * create connection
     *
     * @param id connection to use
     * @param parent lower layer
     */
    public servAmtConn(prtGenConn id, servAmt parent) {
        conn = id;
        lower = parent;
    }

    public String toString() {
        return "amt with " + conn.peerAddr;
    }

    /**
     * get forwarder
     *
     * @return forwarder used
     */
    public ipFwd getFwder() {
        return lower.srvVrf.getFwd(conn.peerAddr);
    }

    /**
     * get local address
     *
     * @return address
     */
    public addrIP getAddrLoc() {
        return conn.iface.addr.copyBytes();
    }

    /**
     * get remote address
     *
     * @return address
     */
    public addrIP getAddrRem() {
        return conn.peerAddr.copyBytes();
    }

    /**
     * get local port number
     *
     * @return session id, 0 if no session
     */
    public int getPortLoc() {
        return conn.portLoc;
    }

    /**
     * get remote port number
     *
     * @return session id, 0 if no session
     */
    public int getPortRem() {
        return conn.portRem;
    }

    /**
     * received one packet
     *
     * @param pck packet
     */
    public void doRecv(packHolder pck) {
        cntr.rx(pck);
        int typ = pck.getByte(0);
        switch (typ) {
            case 1: // discovery
                nonce = pck.msbGetD(4);
                pck.clear();
                pck.msbPutD(0, 0x02000000);
                pck.msbPutD(4, nonce);
                pck.putSkip(8);
                if (conn.iface.addr.isIPv4()) {
                    pck.putAddr(0, conn.iface.addr.toIPv4());
                    pck.putSkip(addrIPv4.size);
                } else {
                    pck.putAddr(0, conn.iface.addr.toIPv6());
                    pck.putSkip(addrIPv6.size);
                }
                pck.merge2beg();
                pck.putDefaults();
                conn.send2net(pck);
                break;
            case 3: // request
                nonce = pck.msbGetD(4);
                break;
            case 5: // membership
                pck.getSkip(12);
                typ = ifcEther.guessEtherType(pck);
                if (typ < 0) {
                    cntr.drop(pck, counter.reasons.badVal);
                    return;
                }
                pck.msbPutW(0, typ);
                pck.putSkip(2);
                pck.merge2beg();
                upper.recvPack(pck);
                break;
            case 6: // multicast
                pck.getSkip(2);
                typ = ifcEther.guessEtherType(pck);
                if (typ < 0) {
                    cntr.drop(pck, counter.reasons.badVal);
                    return;
                }
                pck.msbPutW(0, typ);
                pck.putSkip(2);
                pck.merge2beg();
                upper.recvPack(pck);
                break;
        }
    }

    /**
     * start connection
     */
    public void doStartup() {
        acesIfc = lower.tempIfc.cloneStart(this);
        setUpper(acesIfc.ethtyp);
        acesIfc.ethtyp.setState(state.states.up);
        created = bits.getTime();
    }

    public void sendPack(packHolder pck) {
        pck.merge2beg();
        pck.getSkip(2); // ethertype
        if ((pck.IPprt == ipMhost4.protoNum) || (pck.IPprt == ipIcmp6.protoNum)) {
            pck.msbPutW(0, 0x0400);
            pck.putAddr(2, respmc);
            pck.msbPutD(8, nonce);
            pck.putSkip(12);
        } else {
            pck.msbPutW(0, 0x0600);
            pck.putSkip(2);
        }
        pck.merge2beg();
        pck.putDefaults();
        conn.send2net(pck);
    }

    public addrType getHwAddr() {
        return addrMac.getRandom();
    }

    public void setFilter(boolean promisc) {
    }

    public state.states getState() {
        return state.states.up;
    }

    public void closeDn() {
        acesIfc.cloneStop();
        lower.connDel(conn);
        conn.setClosing();
    }

    public void flapped() {
    }

    public void setUpper(ifcUp server) {
        upper = server;
        upper.setParent(this);
    }

    public counter getCounter() {
        return cntr;
    }

    public int getMTUsize() {
        return 1400;
    }

    public long getBandwidth() {
        return 8000000;
    }

}
