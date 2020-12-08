package user;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import pipe.pipeShell;
import pipe.pipeSide;
import tab.tabGen;
import util.bits;
import util.cmds;

/**
 * process image creation
 *
 * @author matecsaba
 */
public class userImage {

    private pipeSide pip;

    private String tempDir = "../binDsk";

    private String downDir = "../binDwn";

    private String imgName = "../binImg/rtr";

    private int downMode = 1;

    private String arch = "amd64";

    private String mirror = "http://deb.debian.org/debian/";

    private userImageList allPkgs = new userImageList();

    private userImageList missing = new userImageList();

    private userImageList selected = new userImageList();

    private userImageList forbidden = new userImageList();

    private int exec(String s) {
        pip.linePut("!" + s + ".");
        pipeShell sh = pipeShell.exec(pip, "sh -c", s, false, true);
        sh.waitFor();
        return sh.resultNum();
    }

    private boolean delete(String s) {
        return exec("rm -rf " + s) != 0;
    }

    private boolean download(String url, String fil) {
        switch (downMode) {
            case 2:
                if (new File(fil).exists()) {
                    return false;
                }
                break;
            case 1:
                break;
            case 0:
                return false;
        }
        delete(fil);
        return exec("wget -O " + fil + " " + url) != 0;
    }

    private boolean readUpCatalog(String dist, String pool) {
        String cat1 = tempDir + "/" + pool + ".txt";
        String cat2 = downDir + "/" + arch + "--" + dist + "-" + pool + ".xz";
        delete(cat1);
        download(mirror + "dists/" + dist + "/" + pool + "/binary-" + arch + "/Packages.xz", cat2);
        exec("cp " + cat2 + " " + cat1 + ".xz");
        exec("xz -d " + cat1 + ".xz");
        List<String> res = bits.txt2buf(cat1);
        if (res == null) {
            return true;
        }
        userImageNtry pkg = new userImageNtry();
        for (int cnt = 0; cnt < res.size(); cnt++) {
            String a = res.get(cnt).trim();
            int i = a.indexOf(":");
            if (i < 1) {
                continue;
            }
            String b = a.substring(i + 1, a.length()).trim();
            a = a.substring(0, i).trim().toLowerCase();
            if (a.equals("package")) {
                allPkgs.del(pkg);
                allPkgs.update(pkg);
                pkg = new userImageNtry();
                pkg.name = b;
                continue;
            }
            if (a.equals("depends")) {
                pkg.addDepends(b);
                continue;
            }
            if (a.equals("pre-depends")) {
                pkg.addDepends(b);
                continue;
            }
            if (a.equals("filename")) {
                pkg.file = b;
                continue;
            }
            if (a.equals("version")) {
                pkg.vers = b;
                continue;
            }
            if (a.equals("size")) {
                pkg.size = bits.str2num(b);
                continue;
            }
        }
        delete(cat1);
        return false;
    }

    private void selectOnePackage(int level, String nam, String by) {
        nam = nam.trim();
        if (nam.length() < 1) {
            return;
        }
        if (forbidden.startsWith(nam) != null) {
            return;
        }
        userImageNtry pkt = allPkgs.find(nam);
        if (pkt == null) {
            missing.add(nam);
            return;
        }
        pkt.added = by;
        if (selected.update(pkt)) {
            return;
        }
        for (int i = 0; i < pkt.depend.size(); i++) {
            selectOnePackage(level + 1, pkt.depend.get(i), nam);
        }
    }

    private void downAllFiles() {
        for (int i = 0; i < selected.size(); i++) {
            userImageNtry pkg = selected.get(i);
            download(mirror + pkg.file, downDir + "/" + arch + "-" + pkg.name + ".deb");
        }
    }

    private void install(String name) {
        exec("dpkg-deb -x " + name + " " + tempDir + "/");
    }

    private void instAllFiles() {
        for (int i = 0; i < selected.size(); i++) {
            userImageNtry pkg = selected.get(i);
            install(downDir + "/" + arch + "-" + pkg.name + ".deb");
        }
    }

    /**
     * do the work
     *
     * @param cmd command to do
     */
    public void doer(cmds cmd) {
        pip = cmd.pipe;
        List<String> res = bits.txt2buf(cmd.word());
        if (res == null) {
            cmd.error("no such file");
            return;
        }
        missing.setSorting(true);
        selected.setSorting(true);
        forbidden.setSorting(true);
        for (int cnt = 0; cnt < res.size(); cnt++) {
            String s = res.get(cnt);
            s = s.replaceAll("%tmp%", tempDir);
            s = s.replaceAll("%dwn%", downDir);
            s = s.replaceAll("%img%", imgName);
            s = s.replaceAll("%arch%", arch);
            s = s.replaceAll("%%", "%");
            s += "#";
            int i = s.indexOf("#");
            s = s.substring(0, i).trim();
            s += " ";
            i = s.indexOf(" ");
            String a = s.substring(0, i).trim().toLowerCase();
            s = s.substring(i, s.length()).trim();
            if (a.length() < 1) {
                continue;
            }
            if (a.equals("exec")) {
                exec(s);
                continue;
            }
            cmd.error("--> " + a + " " + s + " <--");
            if (a.equals("include")) {
                cmds c = new cmds("", s);
                c.pipe = pip;
                doer(c);
                continue;
            }
            if (a.equals("download")) {
                downMode = bits.str2num(s);
                continue;
            }
            if (a.equals("arch")) {
                arch = s;
                continue;
            }
            if (a.equals("temp")) {
                tempDir = s;
                continue;
            }
            if (a.equals("exit")) {
                break;
            }
            if (a.equals("mirror")) {
                mirror = s;
                continue;
            }
            if (a.equals("catalog-read")) {
                i = s.indexOf(" ");
                a = s.substring(0, i).trim().trim();
                s = s.substring(i, s.length()).trim();
                cmd.error("reading " + s + " of " + a + " list");
                if (readUpCatalog(a, s)) {
                    cmd.error("failed");
                }
                continue;
            }
            if (a.equals("catalog-sort")) {
                cmd.error("sorting " + allPkgs.size() + " entries");
                allPkgs.setSorting(true);
                continue;
            }
            if (a.equals("select-one")) {
                selectOnePackage(0, s, s);
                continue;
            }
            if (a.equals("select-dis")) {
                forbidden.add(s);
                continue;
            }
            if (a.equals("select-del")) {
                selected.del(s);
                continue;
            }
            if (a.equals("select-lst")) {
                for (i = 0; i < selected.size(); i++) {
                    cmd.error("" + selected.get(i));
                }
                continue;
            }
            if (a.equals("select-sum")) {
                cmd.error("");
                cmd.error("forbidden: " + forbidden);
                cmd.error("");
                cmd.error("selected: " + selected);
                cmd.error("");
                cmd.error("missing: " + missing);
                cmd.error("");
                continue;
            }
            if (a.equals("package-down")) {
                downAllFiles();
                continue;
            }
            if (a.equals("package-inst")) {
                instAllFiles();
                continue;
            }
            if (a.equals("del-ifdn")) {
                if (downMode == 1) {
                    delete(s);
                }
                continue;
            }
            if (a.equals("del-alw")) {
                delete(s);
                continue;
            }
            cmd.error("unknown command: " + a + " " + s);
        }
    }

}

class userImageList {

    private final tabGen<userImageNtry> lst = new tabGen<userImageNtry>();

    private boolean needSorting = false;

    public void setSorting(boolean sorted) {
        needSorting = sorted;
    }

    public userImageNtry startsWith(String a) {
        userImageNtry pkg;
        for (int i = 0; i < lst.size(); i++) {
            pkg = lst.get(i);
            if (a.startsWith(pkg.name)) {
                return pkg;
            }
        }
        return null;
    }

    public userImageNtry find(String a) {
        userImageNtry pkg = new userImageNtry();
        pkg.name = a;
        return lst.find(pkg);
    }

    public userImageNtry del(userImageNtry pkg) {
        pkg.name = pkg.name.trim();
        return lst.del(pkg);
    }

    public userImageNtry del(String a) {
        userImageNtry pkg = new userImageNtry();
        pkg.name = a;
        return lst.del(pkg);
    }

    public userImageNtry add(String a) {
        userImageNtry pkg = new userImageNtry();
        pkg.name = a;
        update(pkg);
        return pkg;
    }

    public boolean update(userImageNtry pkg) {
        pkg.name = pkg.name.trim();
        return lst.add(pkg) != null;
    }

    public userImageNtry get(int i) {
        return lst.get(i);
    }

    public int size() {
        return lst.size();
    }

    public String toString() {
        String s = "";
        long o = 0;
        for (int i = 0; i < lst.size(); i++) {
            userImageNtry pkg = lst.get(i);
            o += pkg.size;
            s += " " + pkg.name;
        }
        s += " - " + o / 1024 + " kb";
        return s.substring(1, s.length());
    }

}

class userImageNtry implements Comparator<userImageNtry> {

    public String name = "";

    public String added = "";

    public String file = "";

    public String vers = "";

    public int size = 0;

    public List<String> depend = new ArrayList<String>();

    public int level;

    public int compare(userImageNtry o1, userImageNtry o2) {
        return o1.name.compareTo(o2.name);
    }

    public String toString() {
        String s = name + " " + vers + " " + added + " " + file + " " + size;
        for (int i = 0; i < depend.size(); i++) {
            s += " " + depend.get(i);
        }
        return s;
    }

    public void addDepends(String s) {
        s += ",";
        for (;;) {
            int i = s.indexOf(",");
            int o = s.indexOf("|");
            if (i < 0) {
                break;
            }
            if ((o >= 0) && (o < i)) {
                i = o;
            }
            String a = s.substring(0, i).trim();
            s = s.substring(i + 1, s.length());
            i = a.indexOf("(");
            if (i >= 0) {
                a = a.substring(0, i);
            }
            depend.add(a.trim());
        }
    }

}
