package net.freertr.serv;

import java.util.Comparator;
import java.util.List;
import net.freertr.addr.addrIP;
import net.freertr.cfg.cfgAll;
import net.freertr.cfg.cfgBrdg;
import net.freertr.clnt.clntEtherIp;
import net.freertr.ifc.ifcBridgeIfc;
import net.freertr.ip.ipFwd;
import net.freertr.ip.ipFwdIface;
import net.freertr.ip.ipPrt;
import net.freertr.pack.packHolder;
import net.freertr.pipe.pipeSide;
import net.freertr.prt.prtGenConn;
import net.freertr.tab.tabGen;
import net.freertr.user.userFilter;
import net.freertr.user.userFormat;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.counter;
import net.freertr.util.state;

/**
 * etherip (rfc3378) server
 *
 * @author matecsaba
 */
public class servEtherIp extends servGeneric implements ipPrt {

    /**
     * create instance
     */
    public servEtherIp() {
    }

    /**
     * interface to use
     */
    public cfgBrdg brdgIfc;

    /**
     * physical interface
     */
    public boolean physInt = false;

    /**
     * list of connections
     */
    public tabGen<servEtherIpConn> conns = new tabGen<servEtherIpConn>();

    /**
     * timeout
     */
    public int timeout = 60000;

    /**
     * counter
     */
    public counter cntr;

    /**
     * defaults text
     */
    public final static String[] defaultL = {
        "server etherip .*! port " + clntEtherIp.prot,
        "server etherip .*! protocol " + proto2string(protoAllDgrm),
        "server etherip .*! no physical-interface",
        "server etherip .*! timeout 60000"
    };

    /**
     * defaults filter
     */
    public static tabGen<userFilter> defaultF;

    public tabGen<userFilter> srvDefFlt() {
        return defaultF;
    }

    public void srvShRun(String beg, List<String> l, int filter) {
        if (brdgIfc == null) {
            l.add(beg + "no bridge");
        } else {
            l.add(beg + "bridge " + brdgIfc.name);
        }
        cmds.cfgLine(l, !physInt, beg, "physical-interface", "");
        l.add(beg + "timeout " + timeout);
    }

    public boolean srvCfgStr(cmds cmd) {
        String s = cmd.word();
        if (s.equals("timeout")) {
            timeout = bits.str2num(cmd.word());
            return false;
        }
        if (s.equals("bridge")) {
            brdgIfc = cfgAll.brdgFind(cmd.word(), false);
            if (brdgIfc == null) {
                cmd.error("no such bridge group");
                return false;
            }
            return false;
        }
        if (s.equals("physical-interface")) {
            physInt = true;
            return false;
        }
        if (!s.equals("no")) {
            return true;
        }
        s = cmd.word();
        if (s.equals("bridge")) {
            brdgIfc = null;
            return false;
        }
        if (s.equals("physical-interface")) {
            physInt = false;
            return false;
        }
        return true;
    }

    public void srvHelp(userHelping l) {
        l.add(null, "1 2  bridge                       set interface to clone");
        l.add(null, "2 .    <num>                      number of bridge");
        l.add(null, "1 2  timeout                      timeout of client");
        l.add(null, "2 .    <num>                      milliseconds");
        l.add(null, "1 .  physical-interface           adding as physical to bridge");
    }

    public String srvName() {
        return "etherip";
    }

    public int srvPort() {
        return clntEtherIp.prot;
    }

    public int srvProto() {
        return protoAllDgrm;
    }

    public boolean srvInit() {
        return genRawStart(this, 0);
    }

    public boolean srvDeinit() {
        return genRawStop(this, 0);
    }

    public boolean srvAccept(pipeSide pipe, prtGenConn id) {
        return true;
    }

    /**
     * get protocol number
     *
     * @return number
     */
    public int getProtoNum() {
        return clntEtherIp.prot;
    }

    public String toString() {
        return "etherip";
    }

    /**
     * close interface
     *
     * @param iface interface
     */
    public void closeUp(ipFwdIface iface) {
    }

    /**
     * set state
     *
     * @param iface interface
     * @param stat state
     */
    public void setState(ipFwdIface iface, state.states stat) {
    }

    /**
     * received packet
     *
     * @param rxIfc interface
     * @param pck packet
     */
    public void recvPack(ipFwdIface rxIfc, packHolder pck) {
        servEtherIpConn ntry = new servEtherIpConn(rxIfc, pck.IPsrc, this);
        servEtherIpConn old = conns.add(ntry);
        if (old != null) {
            old.doRecv(pck);
            return;
        }
        if (brdgIfc == null) {
            return;
        }
        if (srvCheckAccept(rxIfc, pck)) {
            return;
        }
        ntry.doStartup();
        ntry.doRecv(pck);
    }

    /**
     * alert packet
     *
     * @param rxIfc interface
     * @param pck packet
     * @return false on success, true on error
     */
    public boolean alertPack(ipFwdIface rxIfc, packHolder pck) {
        return true;
    }

    /**
     * error packet
     *
     * @param err error code
     * @param rtr address
     * @param rxIfc interface
     * @param pck packet
     */
    public void errorPack(counter.reasons err, addrIP rtr, ipFwdIface rxIfc, packHolder pck) {
    }

    /**
     * get counter
     *
     * @return counter
     */
    public counter getCounter() {
        return cntr;
    }

    /**
     * get show
     *
     * @return result
     */
    public userFormat getShow() {
        userFormat res = new userFormat("|", "addr|iface|for|since");
        for (int i = 0; i < conns.size(); i++) {
            servEtherIpConn ntry = conns.get(i);
            if (ntry == null) {
                continue;
            }
            res.add(ntry.peer + "|" + ntry.brdgIfc.getIfcName() + "|" + bits.timePast(ntry.created) + "|" + bits.time2str(cfgAll.timeZoneName, ntry.created + cfgAll.timeServerOffset, 3));
        }
        return res;
    }

}

class servEtherIpConn implements Runnable, Comparator<servEtherIpConn> {

    public servEtherIp lower;

    public ipFwd fwdCor;

    public ipFwdIface iface;

    public addrIP peer;

    public clntEtherIp worker;

    public ifcBridgeIfc brdgIfc;

    public boolean seenPack;

    public long created;

    public servEtherIpConn(ipFwdIface ifc, addrIP adr, servEtherIp parent) {
        iface = ifc;
        peer = adr.copyBytes();
        lower = parent;
        fwdCor = lower.srvVrf.getFwd(peer);
        worker = new clntEtherIp();
    }

    public int compare(servEtherIpConn o1, servEtherIpConn o2) {
        int i = o1.iface.compare(o1.iface, o2.iface);
        if (i != 0) {
            return i;
        }
        return o1.peer.compare(o1.peer, o2.peer);
    }

    public void doStartup() {
        worker.setEndpoints(fwdCor, iface, peer);
        brdgIfc = lower.brdgIfc.bridgeHed.newIface(lower.physInt, true, false);
        worker.setUpper(brdgIfc);
        created = bits.getTime();
        new Thread(this).start();
    }

    public void doRecv(packHolder pck) {
        seenPack = true;
        worker.recvPack(iface, pck);
    }

    public void run() {
        for (;;) {
            bits.sleep(lower.timeout);
            if (!seenPack) {
                break;
            }
            seenPack = false;
        }
        brdgIfc.closeUp();
        worker.closeDn();
        lower.conns.del(this);
    }

}
