package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Collections;

import static gitlet.Utils.*;

/** The story of Gitlet. Let there be Gitlet!
 * @author Ryan Chen
 */
public class Story implements Serializable {
    /** Branch to Hash.*/
    private HashMap<String, String> _branchToHash;
    /** Message to Hash.*/
    private HashMap<String, HashSet<String>> _msgToHash;
    /** Commit logs.*/
    private HashSet<String> _commitLogs;
    /** Marked for removal.*/
    private HashSet<String> _toBeRemoved;
    /** All commit hashes.*/
    private HashSet<String> _allCommitHashes;
    /** Current pointer.*/
    private String _currentPointer;
    /** Mark blue helper function for merge.*/
    private HashSet<String> _markedBlue;
    /** Hash ID for LCA.*/
    private String _bestHash;
    /** Distance to LCA.*/
    private int _bestDist;

    /** Constructor of story class.*/
    public Story() {
        Commit first = Commit.EMPTYCOMMIT;
        _branchToHash = new HashMap<>();
        _currentPointer = "master";
        _branchToHash.put("master", first.getHash());
        _msgToHash = new HashMap<>(); _allCommitHashes = new HashSet<>();
        HashSet<String> firstHash = new HashSet<>();
        firstHash.add(first.getHash());
        _allCommitHashes.add(first.getHash());
        _msgToHash.put(first.getMessage(), firstHash);
        _commitLogs = new HashSet<>();
        _commitLogs.add(first.toString());
        _toBeRemoved = new HashSet<>();
        first.saveCommit();
    }

    /** Returns a commit with message.
     * @param msg is that message.*/
    public Commit makeCommit(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            throw new GitletException("Please enter a commit message.");
        }
        String date = Commit.DATEFORMAT.format(new Date());
        HashMap<String, String> fileNameToFileHash = new HashMap<>();
        Commit parent = this.getCurrentCommit();
        String parentHash = parent.getHash();
        for (Map.Entry<String, String> entry
                : parent.getFileNameToFileHash().entrySet()) {
            fileNameToFileHash.put(entry.getKey(), entry.getValue());
        }
        List<String> stagedFiles = plainFilenamesIn(Main.ADD_FOLDER);
        if (stagedFiles != null) {
            if (stagedFiles.isEmpty() && _toBeRemoved.isEmpty()) {
                throw new GitletException("No changes added to the commit.");
            } else {
                for (String name : stagedFiles) {
                    File inStage = Utils.join(Main.ADD_FOLDER, name);
                    byte[] bytesFile = Utils.readContents(inStage);
                    String hash = sha1(bytesFile);
                    File inBlobs = Utils.join(Main.BLOB_FOLDER, hash);
                    Utils.writeContents(inBlobs, bytesFile);
                    if (fileNameToFileHash.containsKey(name)) {
                        fileNameToFileHash.replace(name, hash);
                    } else {
                        fileNameToFileHash.put(name, hash);
                    }
                    inStage.delete();
                }
            }
        }
        if (!_toBeRemoved.isEmpty()) {
            for (String name : _toBeRemoved) {
                fileNameToFileHash.remove(name);
            }
        }
        return new Commit(msg, date, parentHash, fileNameToFileHash);
    }

    /** Updates story after a change.
     * @param commit is the commit that the created the change.*/
    public void updateStory(Commit commit) {
        _branchToHash.put(_currentPointer, commit.getHash());
        if (_msgToHash.containsKey(commit.getMessage())) {
            HashSet<String> currentHashes =
                    _msgToHash.get(commit.getMessage());
            currentHashes.add(commit.getHash());
            _msgToHash.put(commit.getMessage(), currentHashes);
        } else {
            HashSet<String> currentHashes = new HashSet<>();
            currentHashes.add(commit.getHash());
            _msgToHash.put(commit.getMessage(), currentHashes);
        }
        _commitLogs.add(commit.toString());
        _allCommitHashes.add(commit.getHash());
        _toBeRemoved.clear();
    }

    /** Add function of gitlet.
     * @param nameOfFile is the name of the file being added.*/
    public void add(String nameOfFile) {
        File inCWD = Utils.join(Main.CWD, nameOfFile);
        if (!inCWD.isFile()) {
            throw new GitletException("File does not exist.");
        }
        File inStage = Utils.join(Main.ADD_FOLDER, nameOfFile);
        byte[] contentCWD = Utils.readContents(inCWD);
        Commit currentCommit = getCurrentCommit();
        if (currentCommit.getFileNameToFileHash().get(nameOfFile) != null) {
            if (currentCommit.getFileNameToFileHash().get(nameOfFile).
                    equals(Utils.sha1(contentCWD))) {
                inStage.delete();
            } else {
                Utils.writeContents(inStage, contentCWD);
            }
        } else {
            Utils.writeContents(inStage, contentCWD);
        }
        _toBeRemoved.remove(nameOfFile);
    }

    /** Rm function of gitlet. .
     * @param name is the name of the file removed.*/
    public void rm(String name) {
        File inStage = Utils.join(Main.ADD_FOLDER, name);
        if (!inStage.isFile() && !getCurrentCommit().
                getFileNameToFileHash().containsKey(name)) {
            throw new GitletException("No reason to remove the file.");
        }
        inStage.delete();
        if (getCurrentCommit().getFileNameToFileHash().containsKey(name)) {
            _toBeRemoved.add(name); File inCWD = Utils.join(Main.CWD, name);
            Utils.restrictedDelete(inCWD);
        }
    }

    /** Finds the commit ID with given message.
     * @param msg is the message of the commit.*/
    public void find(String msg) {
        if (!_msgToHash.containsKey(msg)) {
            throw new GitletException("Found no commit with that message.");
        }
        HashSet<String> commitIDs = _msgToHash.get(msg);
        for (String commitID : commitIDs) {
            System.out.println(commitID);
        }
    }

    /** Prints the log of the story.*/
    public void log() {
        for (Commit cur = getCommitFromBranch(_currentPointer);
             cur != null; cur = cur.getParent()) {
            System.out.println(cur.toString());
        }
    }

    /** Prints the global log of the story.*/
    public void globalLog() {
        for (String log : _commitLogs) {
            System.out.println(log);
        }
    }

    /** Adds a branch.
     * @param name is the name of the branch. */
    public void branch(String name) {
        if (_branchToHash.containsKey(name)) {
            throw new GitletException("A branch with that"
                    + " name already exists.");
        }
        _branchToHash.put(name, _branchToHash.get(_currentPointer));
    }

    /** Removes branch.
     * @param name is the name of the branch. */
    public void rmBranch(String name) {
        if (!_branchToHash.containsKey(name)) {
            throw new GitletException("A branch with that name "
                    + "does not exist.");
        } else if (_currentPointer.equals(name)) {
            throw new GitletException("Cannot remove the current branch.");
        }
        _branchToHash.remove(name);
    }

    /** Checkout function of the story.
     * @param args are the arguments from Main. */
    public void checkout(String...args) {
        if (args.length == 3) {
            if (args[1].equals("--")) {
                HashMap<String, String> fileNameToHash =
                        getCurrentCommit().getFileNameToFileHash();
                if (!fileNameToHash.containsKey(args[2])) {
                    throw new GitletException("File does not "
                            + "exist in that commit.");
                }
                String hashBlob = fileNameToHash.get(args[2]);
                Utils.copyFile(Main.CWD, Main.BLOB_FOLDER, hashBlob, args[2]);
            } else {
                throw new GitletException("Incorrect Operands.");
            }
        } else if (args.length == 4) {
            if (args[2].equals("--")) {
                String commitID = findFullHash(args[1]);
                if (commitID == null) {
                    throw new GitletException("No commit with that id exists.");
                }
                Commit current = Commit.fromFile(commitID);
                if (current != null) {
                    HashMap<String, String> fileNameToHash =
                            current.getFileNameToFileHash();
                    if (!fileNameToHash.containsKey(args[3])) {
                        throw new GitletException("File does not "
                                + "exist in that commit.");
                    }
                    String hashBlob = fileNameToHash.get(args[3]);
                    Utils.copyFile(Main.CWD,
                            Main.BLOB_FOLDER, hashBlob, args[3]);
                }
            } else {
                throw new GitletException("Incorrect Operands.");
            }
        } else if (args.length == 2) {
            if (!args[1].isEmpty()) {
                if (!_branchToHash.containsKey(args[1])) {
                    throw new GitletException("No such branch exists.");
                } else if (_currentPointer.equals(args[1])) {
                    throw new GitletException("No need to checkout "
                            + "the current branch.");
                }
                String commitHash = _branchToHash.get(args[1]);
                Commit commitFromBranch = Commit.fromFile(commitHash);
                if (commitFromBranch == null) {
                    throw new GitletException("iip");
                }
                copyAndDelete(commitFromBranch); clearStagingArea();
                _currentPointer = args[1];
                _branchToHash.put(_currentPointer, commitHash);
            } else {
                throw new GitletException("Incorrect Operands.");
            }
        } else {
            throw new GitletException("Incorrect Operands.");
        }
    }

    /** Copies and deletes files in CWD.
     * @param commitFromBranch is the commit we are
     * retrieving files from.*/
    public void copyAndDelete(Commit commitFromBranch) {
        Commit currentCommit = getCurrentCommit();
        if (commitFromBranch != null) {
            for (Map.Entry<String, String> entry
                    :commitFromBranch.getFileNameToFileHash().entrySet()) {
                copyFile(Main.CWD, Main.BLOB_FOLDER,
                        entry.getValue(), entry.getKey());
            }
            for (String nameCurrent : currentCommit.getFileNameToFileHash().
                    keySet()) {
                if (!commitFromBranch.getFileNameToFileHash().
                        containsKey(nameCurrent)) {
                    File fileToDelete = Utils.join(Main.CWD, nameCurrent);
                    fileToDelete.delete();
                }
            }
        }

    }

    /** Helper function that returns the full hash of a commit.
     * @param smallHash is the shortened hash of the commit. */
    public String findFullHash(String smallHash) {
        if (_allCommitHashes.contains(smallHash)) {
            return smallHash;
        }
        for (String value : _allCommitHashes) {
            if (value.substring(0, Math.min(value.length(),
                    smallHash.length())).equals(smallHash)) {
                return value;
            }
        }
        throw new GitletException("No commit with that id exists.");
    }

    /** Clears the staging area. */
    public void clearStagingArea() {
        this._toBeRemoved.clear();
        List<String> names =  plainFilenamesIn(Main.ADD_FOLDER);
        if (names == null) {
            return;
        }
        for (String name : names) {
            File addedFile = Utils.join(Main.ADD_FOLDER, name);
            if (!addedFile.isDirectory()) {
                addedFile.delete();
            }
        }
    }


    /** Checks for Untracked Files in CWD and throws error.
     * @param comID is the commit ID. */
    public void checkCWDForUntrackedFiles(String comID) {
        List<String> names =  plainFilenamesIn(Main.CWD);
        if (names == null) {
            return;
        }
        Commit current = getCurrentCommit();
        List<String> inStage = Utils.plainFilenamesIn(Main.ADD_FOLDER);
        for (String name : names) {
            if (!inStage.contains(name)) {
                if (!current.getFileNameToFileHash().containsKey(name)) {
                    if (Commit.fromFile(comID)
                            .getFileNameToFileHash().containsKey(name)) {
                        throw new GitletException("There is an untracked file"
                                + " in the way; delete it or add it first.");
                    }
                }
            }
        }
    }

    /** Reset command of the story.
     * @param commitID is the commitID being checked-out*/
    public void reset(String commitID) {
        String thisCommitID = findFullHash(commitID);
        checkCWDForUntrackedFiles(thisCommitID);
        if (thisCommitID == null) {
            throw new GitletException("No commit with that id exists.");
        }
        Commit commitFromBranch = Commit.fromFile(thisCommitID);
        copyAndDelete(commitFromBranch);
        clearStagingArea();
        _branchToHash.put(_currentPointer, thisCommitID);
    }

    /** Prints out the status of the story. */
    public void statusBasic() {
        System.out.println("=== Branches ===");
        SortedSet<String> keySet = new TreeSet<>(_branchToHash.keySet());
        for (String key : keySet) {
            if (key.equals(_currentPointer)) {
                System.out.println("*" + key);
            } else {
                System.out.println(key);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List<String> added = Utils.plainFilenamesIn(Main.ADD_FOLDER);
        for (String stagedFile : added) {
            System.out.println(stagedFile);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        List<String> list = new ArrayList<>(_toBeRemoved);
        Collections.sort(list);
        for (String name : list) {
            System.out.println(name);
        }
        System.out.println(); statusExtra();
    }

    /** Prints out the status of the story (part 2). */
    public void statusExtra() {
        List<String> added = Utils.plainFilenamesIn(Main.ADD_FOLDER);
        HashMap<String, String> commitFileToHash =
                getCurrentCommit()
                        .getFileNameToFileHash();
        List<String> fileNamesCWD = Utils.plainFilenamesIn(Main.CWD);
        HashSet<String> allFileNames = new HashSet<>();
        if (commitFileToHash != null) {
            allFileNames.addAll(commitFileToHash.keySet());
        }
        if (fileNamesCWD != null) {
            allFileNames.addAll(fileNamesCWD);
        }
        HashMap<String, String> modFiles = new HashMap<>();
        TreeSet<String> untrackedFiles = new TreeSet<>();
        ArrayList<String> modNames = new ArrayList<>();
        Commit current = getCurrentCommit();
        for (String name : allFileNames) {
            File fileCWD = Utils.join(Main.CWD, name);
            boolean tracked = current.getFileNameToFileHash()
                    .containsKey(name);
            boolean stagedAdd = added.contains(name);
            boolean stagedRemoval = _toBeRemoved.contains(name);
            if (tracked && !stagedAdd && fileCWD.isFile()) {
                if (!Utils.sha1(Utils.readContents(fileCWD)).
                        equals(current.getFileNameToFileHash().get(name))) {
                    modFiles.put(name, " (modified)"); modNames.add(name);
                }
            }
            if (stagedAdd && fileCWD.isFile()) {
                if (!Utils.sha1(Utils.readContents(fileCWD)).
                        equals(Utils.sha1(Utils.readContents
                                (Utils.join(Main.ADD_FOLDER, name))))) {
                    modFiles.put(name, " (modified)");
                    modNames.add(name);
                }
            }
            if (stagedAdd && !fileCWD.isFile()) {
                modFiles.put(name, " (deleted)"); modNames.add(name);
            }
            if (tracked && !stagedRemoval && !fileCWD.isFile()) {
                modFiles.put(name, " (deleted)"); modNames.add(name);
            }
            if (fileCWD.isFile() && !stagedAdd && !tracked) {
                untrackedFiles.add(name);
            }
            if (fileCWD.isFile() && !stagedAdd
                    && stagedRemoval && tracked) {
                untrackedFiles.add(name);
            }
        }
        statusBonus(modNames, modFiles, untrackedFiles);
    }

    /** Prints out the status of the story (part 3).
     * @param modNames is all of the modified names.
     * @param modFiles is all of the modified files.
     * @param untrackedFiles are all untracked. */
    public void statusBonus(ArrayList<String> modNames,
            HashMap<String, String> modFiles, TreeSet<String> untrackedFiles) {
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (int i = 0; i < modNames.size(); i++) {
            String temp = modNames.get(i)
                    + modFiles.get(modNames.get(i));
            System.out.println(temp);
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String file : untrackedFiles) {
            System.out.println(file);
        }
    }

    /** Merge, marking true if there is a merge conflict.
     * @param branch is the given branch.
     * @return true if there is a merge conflict.*/
    public boolean mergeConflict(String branch) {
        checkCWDForUntrackedFiles(getCommitFromBranch(branch).getHash());
        String splitPointCommitHash;
        boolean basic = basicSplitPointCheck(branch);
        if (!basic) {
            splitPointCommitHash = complexSplitPoint(branch);
        } else {
            return false;
        }
        String branchCommitID = _branchToHash.get(branch);
        HashMap<String, String> splitPointFileToHash;
        Commit splitPT = Commit.fromFile(splitPointCommitHash);
        if (splitPT == null) {
            splitPointFileToHash = new HashMap<>();
        } else {
            splitPointFileToHash =
                    splitPT.getFileNameToFileHash();
        }
        HashMap<String, String> currentFileToHash =
                getCurrentCommit().getFileNameToFileHash();
        HashMap<String, String> branchFileToHash =
                getCommitFromBranch(branch).getFileNameToFileHash();
        HashSet<String> allFiles = new HashSet<>();
        allFiles.addAll(currentFileToHash.keySet());
        allFiles.addAll(splitPointFileToHash.keySet());
        allFiles.addAll(branchFileToHash.keySet());
        return mergeConflict2(branchCommitID, allFiles,
                currentFileToHash, branchFileToHash, splitPointFileToHash);
    }

    /** Merge, marking true if there is a merge conflict.
     * @param branchComID is the ID of the branch.
     * @param allFiles contains all files.
     * @param curToHash maps current files to hash.
     * @param brToHash maps branch files to hash.
     * @param spToHash maps splitpoint files to hash.
     * @return true if there is a merge conflict.*/
    public boolean mergeConflict2(String branchComID,
              HashSet<String> allFiles, HashMap<String, String> curToHash,
              HashMap<String, String> brToHash,
              HashMap<String, String> spToHash) {
        boolean doNothing = false; boolean b = false;
        for (String fileName : allFiles) {
            String curID = curToHash.get(fileName); boolean curEmpt = false;
            String brID = brToHash.get(fileName); boolean spEmpt = false;
            String spID = spToHash.get(fileName);
            boolean brEmpt = false; boolean allExist = false;
            if (curID == null) {
                curID = ""; curEmpt = true;
            }
            if (spID == null) {
                spID = ""; spEmpt = true;
            }
            if (brID == null) {
                brID = ""; brEmpt = true;
            }
            if (!curEmpt && !spEmpt && !brEmpt) {
                allExist = true;
            }
            File currentFile = Utils.join(Main.BLOB_FOLDER, curID);
            File branchFile = Utils.join(Main.BLOB_FOLDER, brID);
            if (allExist && !spID.equals(brID)
                    && spID.equals(curID)) {
                checkout("checkout", branchComID, "--", fileName);
                add(fileName);
            } else if (allExist && !spID.equals(curID)
                    && spID.equals(brID)) {
                doNothing = true;
            } else if (!spID.equals(curID)
                    && curID.equals(brID)) {
                doNothing = true;
            } else if (spEmpt && brEmpt && !curEmpt) {
                doNothing = true;
            } else if (spEmpt && !brEmpt && curEmpt) {
                checkout("checkout", branchComID, "--", fileName);
                add(fileName);
            } else if (!spEmpt && brEmpt
                    && !curEmpt && curID.equals(spID)) {
                Utils.restrictedDelete(Utils.join(Main.CWD, fileName));
                _toBeRemoved.add(fileName);
            } else if (!spEmpt && curEmpt
                    && !brEmpt && brID.equals(spID)) {
                doNothing = true;
            } else {
                String curCont = ""; String brCont = "";
                if (!curEmpt) {
                    curCont = Utils.readContentsAsString(currentFile);
                }
                if (!brEmpt) {
                    brCont = Utils.readContentsAsString(branchFile);
                }
                String newContent = "<<<<<<< HEAD\n" + curCont
                        + "=======\n" + brCont + ">>>>>>>\n";
                File file = Utils.join(Main.CWD, fileName); b = true;
                Utils.writeContents(file, newContent); add(fileName);
            }
        }
        return b;
    }

    /** Checks for basic cases of splitpoint for merge.
     * @return true if it is simple to find the splitpoint.
     * @param branch is the given branch.  */
    public boolean basicSplitPointCheck(String branch) {
        List<String> list = Utils.plainFilenamesIn(Main.ADD_FOLDER);
        if (list != null) {
            if (!list.isEmpty() || !_toBeRemoved.isEmpty()) {
                throw new GitletException("You have uncommitted changes.");
            }
        }
        if (!_branchToHash.containsKey(branch)) {
            throw new GitletException("A branch "
                    + "with that name does not exist.");
        }
        Commit currentCommit = getCurrentCommit();
        Commit branchCommit = getCommitFromBranch(branch);
        if (currentCommit.getHash().equals(branchCommit.getHash())) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
        ArrayList<String> currentAncestors = new ArrayList<>();
        ArrayList<String> branchAncestors = new ArrayList<>();
        for (Commit current = getCurrentCommit();
             current != null; current = current.getParent()) {
            currentAncestors.add(current.getHash());
        }
        for (Commit branchCom = getCommitFromBranch(branch);
             branchCom != null; branchCom = branchCom.getParent()) {
            branchAncestors.add(branchCom.getHash());
        }
        if (currentAncestors.containsAll(branchAncestors)) {
            throw new GitletException("Given branch is an ancestor "
                    + "of the current branch.");
        } else if (branchAncestors.containsAll(currentAncestors)) {
            reset(branchCommit.getHash());
            _branchToHash.put(_currentPointer, branchCommit.getHash());
            System.out.println("Current branch fast-forwarded.");
        }
        return false;
    }

    /** Resets merge variables after merging.  */
    public void resetMerge() {
        _markedBlue = null;
        _bestHash = null;
        _bestDist = Integer.MAX_VALUE;
    }

    /** Helper function for merge to find splitPoint beyond basic cases.
     * @return the splitpoint.
     * @param branch is the given branch. */
    public String complexSplitPoint(String branch) {
        Commit branchCommit = getCommitFromBranch(branch);
        _markedBlue = new HashSet<>();
        _bestDist = Integer.MAX_VALUE;
        _bestHash = null;
        Commit current = getCurrentCommit();
        markBlue(current);
        markRed(branchCommit, _bestDist);
        return _bestHash;
    }

    /** Marking Blue during merge.
     * @param reference is the reference commit.*/
    public void markBlue(Commit reference) {
        if (reference == null) {
            return;
        }
        _markedBlue.add(reference.getHash());
        if (reference.getParentHash() != null
                && reference.getParent2Hash() != null) {
            markBlue(reference.getParent());
            markBlue(reference.getParent2());
        } else if (reference.getParentHash() != null) {
            markBlue(reference.getParent());
        } else if (reference.getParent2Hash() != null) {
            markBlue(reference.getParent2());
        }
    }

    /** Marking Red during merge.
     * @param commit is the current commit.
     * @param count is the steps that it is currently at.  */
    public void markRed(Commit commit, int count) {
        if (commit == null) {
            return;
        }
        if (_markedBlue.contains(commit.getHash())) {
            if (count < _bestDist) {
                _bestDist = count;
                _bestHash = commit.getHash();
            }
        }
        markRed(commit.getParent(), count + 1);
        markRed(commit.getParent2(), count + 1);
    }

    /** Returns the current commit. */
    public Commit getCurrentCommit() {
        String commitHash = _branchToHash.get(_currentPointer);
        return Commit.fromFile(commitHash);
    }

    /** Returns commit according to hash.
     * @param branch is the branch commit.*/
    public Commit getCommitFromBranch(String branch) {
        String commitHash = _branchToHash.get(branch);
        if (commitHash == null) {
            throw new GitletException("A branch with that name does not exist");
        }
        return Commit.fromFile(commitHash);
    }

    /** Returns the current story pointer. */
    public String getCurrentPointer() {
        return _currentPointer;
    }

    /** Returns allCommitHashes. */
    public HashSet<String> getAllCommitHashes() {
        return _allCommitHashes;
    }

    /** Returns the msgToHash hashmap. */
    public HashMap<String, HashSet<String>> getMsgToHash() {
        return _msgToHash;
    }

    /** Returns Commit Logs. */
    public HashSet<String> getCommitLogs() {
        return _commitLogs;
    }

    /** Gets story from a file.
     * @returns the full story.*/
    public static Story storyFromFile() {
        return Utils.readObject(Main.STORY_FILE, Story.class);
    }

    /** Saves the story to a file. */
    public void saveStory() {
        Utils.writeObject(Main.STORY_FILE, this);
    }
}
