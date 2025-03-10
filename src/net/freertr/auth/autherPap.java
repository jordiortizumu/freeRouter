package net.freertr.auth;

import net.freertr.pack.packHolder;
import net.freertr.util.bits;

/**
 * password authentication protocol (rfc1334)
 *
 * @author matecsaba
 */
public class autherPap extends autherDoer {

    /**
     * ppp name
     */
    public final static String pppName = "pap";

    /**
     * ppp control type
     */
    public final static int pppCtrl = 0xc023;

    /**
     * Authenticate Request
     */
    public final static int codeReq = 1;

    /**
     * Authenticate Ack
     */
    public final static int codeAck = 2;

    /**
     * Authenticate Nak
     */
    public final static int codeNak = 3;

    private int sentId;

    /**
     * the constructor
     *
     * @param prnt parent
     */
    public autherPap(authenDown prnt) {
        parent = prnt;
    }

    /**
     * convert code to string
     *
     * @param i code
     * @return string
     */
    public static String code2str(int i) {
        switch (i) {
            case codeReq:
                return "authReq";
            case codeAck:
                return "authAck";
            case codeNak:
                return "authNak";
            default:
                return "unknown=" + i;
        }
    }

    protected void authenRecv(packHolder pck, int code, int id) {
        autherPapMsg msg = new autherPapMsg();
        msg.code = code;
        msg.id = id;
        if (msg.parsePack(pck)) {
            return;
        }
        if (!working) {
            return;
        }
        working = false;
        parent.recvAuthPack("" + msg);
        switch (code) {
            case codeReq:
                if (isClient()) {
                    return;
                }
                result = authenRem.authUserPass(msg.user, msg.pass);
                if (result.result == authResult.authSuccessful) {
                    msg.code = codeAck;
                } else {
                    msg.code = codeNak;
                }
                msg.user = code2str(msg.code);
                msg.createPack(pck);
                parent.sendAuthPack(pck, pppCtrl, msg.code, msg.id, "" + msg);
                break;
            case codeAck:
                if (!isClient()) {
                    return;
                }
                result = new authResult(authenRem, authResult.authSuccessful, sentUser, sentPass);
                break;
            case codeNak:
                if (!isClient()) {
                    return;
                }
                result = new authResult(authenRem, authResult.authBadUserPass, sentUser, sentPass);
                break;
            default:
                working = true;
                break;
        }
    }

    protected void authenSend(packHolder pck) {
        if (!working) {
            return;
        }
        if (!isClient()) {
            return;
        }
        sentId = bits.randomB();
        autherPapMsg msg = new autherPapMsg();
        msg.code = codeReq;
        msg.id = sentId;
        msg.user = sentUser;
        msg.pass = sentPass;
        msg.createPack(pck);
        parent.sendAuthPack(pck, pppCtrl, msg.code, msg.id, "" + msg);
        return;
    }

}

class autherPapMsg {

    public int code;

    public int id;

    public String user;

    public String pass;

    private String getStr(packHolder pck) {
        int i = pck.getByte(0);
        pck.getSkip(1);
        String s = pck.getAsciiZ(0, i, 0);
        pck.getSkip(i);
        return s;
    }

    private void putStr(packHolder pck, String s) {
        pck.putByte(0, s.length());
        pck.putSkip(1);
        pck.putAsciiZ(0, s.length(), s, 0);
        pck.putSkip(s.length());
    }

    public boolean parsePack(packHolder pck) {
        user = getStr(pck);
        if (code == autherPap.codeReq) {
            pass = getStr(pck);
        }
        return false;
    }

    public void createPack(packHolder pck) {
        pck.clear();
        putStr(pck, user);
        if (code == autherPap.codeReq) {
            putStr(pck, pass);
        }
        pck.merge2beg();
    }

    public String toString() {
        return "cod=" + autherPap.code2str(code) + " id=" + id + " user=" + user;
    }

}
