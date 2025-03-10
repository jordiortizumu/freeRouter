package net.freertr.ifc;

import java.util.List;
import net.freertr.addr.addrMac;
import net.freertr.addr.addrType;
import net.freertr.cfg.cfgInit;
import net.freertr.pack.packHolder;
import net.freertr.pipe.pipeLine;
import net.freertr.pipe.pipeSide;
import net.freertr.user.userHelping;
import net.freertr.util.bits;
import net.freertr.util.cmds;
import net.freertr.util.counter;
import net.freertr.util.logger;
import net.freertr.util.state;

/**
 * connect two interfaces
 *
 * @author matecsaba
 */
public class ifcHairpin {

    /**
     * description of this bridge
     */
    public String description = "";

    /**
     * interface is not ethernet
     */
    public boolean notEther;

    /**
     * buffer size
     */
    public int bufSiz = 65536;

    /**
     * drop probability
     */
    public int randDrop = 0;

    /**
     * duplication probability
     */
    public int randDup = 0;

    /**
     * burstiness probability
     */
    public int randBurstP = 0;

    /**
     * burstiness minimum
     */
    public int randBurstB = 0;

    /**
     * burstiness maximum
     */
    public int randBurstE = 0;

    /**
     * reorder probability
     */
    public int randReord = 0;

    /**
     * delay probability
     */
    public int randDelayP = 0;

    /**
     * delay minimum
     */
    public int randDelayB = 0;

    /**
     * delay maximum
     */
    public int randDelayE = 0;

    private ifcHairpinWorker s1;

    private ifcHairpinWorker s2;

    private pipeLine pip;

    /**
     * create new instance
     */
    public ifcHairpin() {
        s1 = new ifcHairpinWorker(this);
        s2 = new ifcHairpinWorker(this);
        pip = new pipeLine(64 * 1024, true);
        s1.queueRx = pip.getSide();
        s2.queueRx = pip.getSide();
        s1.queueTx = s1.queueRx;
        s2.queueTx = s2.queueRx;
    }

    /**
     * get side one
     *
     * @return interface handler
     */
    public ifcDn getSide1() {
        return s1;
    }

    /**
     * get side two
     *
     * @return interface handler
     */
    public ifcDn getSide2() {
        return s2;
    }

    /**
     * stop this hairpin
     */
    public void stopWork() {
        s1.need2work = false;
        s2.need2work = false;
        pip.setClose();
    }

    /**
     * start this hairpin
     */
    public void startWork() {
        new Thread(s1).start();
        new Thread(s2).start();
    }

    /**
     * get help text
     *
     * @param l storage
     */
    public static void getHelp(userHelping l) {
        l.add(null, "1 2,.   description                 description of this hairpin");
        l.add(null, "2 2,.     [text]                    text describing this hairpin");
        l.add(null, "1 .     ethernet                    specify type of hairpin");
        l.add(null, "1 2     buffer                      specify buffer size");
        l.add(null, "2 .       <num>                     buffer size in bytes");
        l.add(null, ".1 2    random-drop                 specify packet loss probability");
        l.add(null, ".2 .      <num>                     one to this");
        l.add(null, ".1 2    random-burst                specify burstiness probability");
        l.add(null, ".2 3      <num>                     one to this");
        l.add(null, ".3 4        <num>                   minimum time in ms");
        l.add(null, ".4 .          <num>                 maximum time in ms");
        l.add(null, ".1 2    random-duplicate            specify duplication probability");
        l.add(null, ".2 .      <num>                     one to this");
        l.add(null, ".1 2    random-reorder              specify reorder probability");
        l.add(null, ".2 .      <num>                     one to this");
        l.add(null, ".1 2    random-delay                specify delay probability");
        l.add(null, ".2 3      <num>                     one to this");
        l.add(null, ".3 4        <num>                   minimum time in ms");
        l.add(null, ".4 .          <num>                 maximum time in ms");
    }

    /**
     * get configuration
     *
     * @param l storage
     * @param beg beginning
     */
    public void getConfig(List<String> l, String beg) {
        cmds.cfgLine(l, description.length() < 1, cmds.tabulator, "description", description);
        cmds.cfgLine(l, notEther, beg, "ethernet", "");
        l.add(beg + "buffer " + bufSiz);
        l.add(beg + "random-drop " + randDrop);
        l.add(beg + "random-burst " + randBurstP + " " + randBurstB + " " + randBurstE);
        l.add(beg + "random-duplicate " + randDup);
        l.add(beg + "random-reorder " + randReord);
        l.add(beg + "random-delay " + randDelayP + " " + randDelayB + " " + randDelayE);
    }

    /**
     * do configuration
     *
     * @param cmd command
     */
    public void doConfig(cmds cmd) {
        String s = cmd.word();
        if (s.equals("description")) {
            description = cmd.getRemaining();
            return;
        }
        if (s.equals("ethernet")) {
            notEther = false;
            return;
        }
        if (s.equals("buffer")) {
            bufSiz = bits.str2num(cmd.word());
            pipeLine old = pip;
            pip = new pipeLine(bufSiz, true);
            s1.queueRx = pip.getSide();
            s2.queueRx = pip.getSide();
            s1.queueTx = s1.queueRx;
            s2.queueTx = s2.queueRx;
            old.setClose();
            return;
        }
        if (s.equals("random-drop")) {
            randDrop = bits.str2num(cmd.word());
            return;
        }
        if (s.equals("random-burst")) {
            randBurstP = bits.str2num(cmd.word());
            randBurstB = bits.str2num(cmd.word());
            randBurstE = bits.str2num(cmd.word());
            return;
        }
        if (s.equals("random-duplicate")) {
            randDup = bits.str2num(cmd.word());
            return;
        }
        if (s.equals("random-reorder")) {
            randReord = bits.str2num(cmd.word());
            return;
        }
        if (s.equals("random-delay")) {
            randDelayP = bits.str2num(cmd.word());
            randDelayB = bits.str2num(cmd.word());
            randDelayE = bits.str2num(cmd.word());
            return;
        }
        if (!s.equals("no")) {
            cmd.badCmd();
            return;
        }
        s = cmd.word();
        if (s.equals("description")) {
            description = "";
            return;
        }
        if (s.equals("ethernet")) {
            notEther = true;
            return;
        }
        if (s.equals("random-drop")) {
            randDrop = 0;
            return;
        }
        if (s.equals("random-burst")) {
            randBurstP = 0;
            randBurstB = 0;
            randBurstE = 0;
            return;
        }
        if (s.equals("random-duplicate")) {
            randDup = 0;
            return;
        }
        if (s.equals("random-reorder")) {
            randReord = 0;
            return;
        }
        if (s.equals("random-delay")) {
            randDelayP = 0;
            randDelayB = 0;
            randDelayE = 0;
            return;
        }
        cmd.badCmd();
    }

}

class ifcHairpinWorker implements ifcDn, Runnable {

    public boolean need2work = true;

    public final ifcHairpin parent;

    public pipeSide queueRx;

    public pipeSide queueTx;

    private counter cntr = new counter();

    public addrType hwaddr = addrMac.getRandom();

    private ifcUp upper = new ifcNull();

    public ifcHairpinWorker(ifcHairpin lower) {
        parent = lower;
    }

    public counter getCounter() {
        return cntr;
    }

    public state.states getState() {
        return state.states.up;
    }

    public void setFilter(boolean promisc) {
    }

    public addrType getHwAddr() {
        return hwaddr;
    }

    public void setUpper(ifcUp server) {
        upper = server;
        upper.setParent(this);
    }

    public void closeDn() {
    }

    public void flapped() {
    }

    public void sendPack(packHolder pck) {
        if (cfgInit.booting) {
            return;
        }
        pck.merge2beg();
        if (!parent.notEther) {
            ifcEther.createETHheader(pck, false);
        }
        byte[] buf = pck.getCopy();
        queueTx.nonBlockPut(buf, 0, buf.length);
    }

    public int getMTUsize() {
        return 1500;
    }

    public long getBandwidth() {
        return 100000000;
    }

    public void doWork() {
        packHolder pck = new packHolder(true, true);
        byte[] buf = new byte[packHolder.maxHead];
        for (;;) {
            if (!need2work) {
                break;
            }
            if (parent.randBurstP > 0) {
                if (bits.random(0, parent.randBurstP) == 0) {
                    bits.sleep(bits.random(parent.randBurstB, parent.randBurstE));
                }
            }
            int i = queueRx.blockingGet(buf, 0, buf.length);
            if (i < 0) {
                continue;
            }
            if (parent.randDrop > 0) {
                if (bits.random(0, parent.randDrop) == 0) {
                    continue;
                }
            }
            buf2pck(buf, pck, i);
            if (parent.randDup > 0) {
                if (bits.random(0, parent.randDup) == 0) {
                    upper.recvPack(pck.copyBytes(true, true));
                }
            }
            if (parent.randReord > 0) {
                if (bits.random(0, parent.randReord) == 0) {
                    i = queueRx.blockingGet(buf, 0, buf.length);
                    if (i < 0) {
                        continue;
                    }
                    packHolder pck2 = new packHolder(true, true);
                    buf2pck(buf, pck2, i);
                    upper.recvPack(pck2);
                }
            }
            if (parent.randDelayP > 0) {
                if (bits.random(0, parent.randDelayP) == 0) {
                    ifcDelay.recvPack(bits.random(parent.randDelayB, parent.randDelayE), upper, pck);
                    continue;
                }
            }
            upper.recvPack(pck);
        }
    }

    private void buf2pck(byte[] buf, packHolder pck, int len) {
        pck.clear();
        pck.putCopy(buf, 0, 0, len);
        pck.putSkip(len);
        pck.merge2beg();
        if (parent.notEther) {
            return;
        }
        ifcEther.parseETHheader(pck, false);
    }

    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            logger.exception(e);
        }
    }

}
