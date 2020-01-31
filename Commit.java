package gitlet;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

/** Commit class of Gitlet.
 * @author Ryan Chen
 */
public class Commit implements Serializable {
    /** Date format.*/
    static final SimpleDateFormat DATEFORMAT =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
    /** Basic date for empty commit.*/
    static final Commit EMPTYCOMMIT =
            new Commit("initial commit", DATEFORMAT.format(new Date(0)),
            null, new HashMap<String, String>());
    /** Message of commit.*/
    private String _message;
    /** Date of commit.*/
    private String _date;
    /** Parent hash of commit.*/
    private String _parentHash;
    /** 2nd Parent hash of commit.*/
    private String _parent2Hash;
    /** File name to file hash.*/
    private HashMap<String, String> _fileNameToFileHash;
    /** Hash of commit.*/
    private String _commitHash;

    /** The constructor for the Commit class.
     * @param msg is the message of the Commit.
     * @param parentHash is the parent hash.
     * @param date is the date the commit was created.
     * @param fileNameToHash is the file name to hash. */
    public Commit(String msg, String date, String parentHash,
                  HashMap<String, String> fileNameToHash) {
        _message = msg;
        _date = date;
        _parentHash = parentHash;
        _parent2Hash = null;
        _fileNameToFileHash = fileNameToHash;
        if (_fileNameToFileHash.isEmpty()) {
            _commitHash = Utils.sha1(_message + _date);
        } else {
            List<String> keyList =
                    new ArrayList<>(_fileNameToFileHash.keySet());
            Collections.sort(keyList);
            String nameToFile = "";
            for (String key : keyList) {
                nameToFile += "key: " + key + "value: "
                        + _fileNameToFileHash.get(key);
            }
            _commitHash = Utils.sha1(_date
                    + _message + _parentHash + nameToFile);
        }
    }

    /** Returns the fileNameToHash. */
    public HashMap<String, String> getFileNameToFileHash() {
        if (_fileNameToFileHash == null) {
            return null;
        }
        return _fileNameToFileHash;
    }

    /** Returns the commit hash. */
    public String getHash() {
        return _commitHash;
    }

    /** Returns the commit from its hash.
     * @param hash is the hash of the commit.*/
    public static Commit fromFile(String hash) {
        File commitFile = Utils.join(Main.COMMIT_FOLDER, hash);
        if (!commitFile.exists()) {
            return null;
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Saves the commit object to file.*/
    public void saveCommit() {
        Utils.writeObject(Utils.join(Main.COMMIT_FOLDER,
                this._commitHash), this);
    }

    /** Returns the parent of the commit. */
    public Commit getParent() {
        String parentHash = this.getParentHash();
        if (parentHash == null) {
            return null;
        }
        return fromFile(parentHash);
    }

    /** Returns the hash of the parent of the commit. */
    public String getParentHash() {
        return _parentHash;
    }

    /** Returns the second parent of the commit. */
    public Commit getParent2() {
        String parent2Hash = this.getParent2Hash();
        if (parent2Hash == null) {
            return null;
        }
        return fromFile(parent2Hash);
    }

    /** Returns the hash of the second parent of the commit. */
    public String getParent2Hash() {
        return _parent2Hash;
    }

    /** Sets the hash of the second parent of the commit.
     * @param parent2Hash is the ID being set. */
    public void setParent2Hash(String parent2Hash) {
        this._parent2Hash = parent2Hash;
    }

    /** Returns the message of the commit. */
    public String getMessage() {
        return _message;
    }

    @Override
    public String toString() {
        if (_parent2Hash != null && _parentHash != null) {
            String shortID1 = _parentHash.substring(0, 6);
            String shortID2 = _parent2Hash.substring(0, 6);
            return String.format("===%ncommit %s%nMerge: %s %s%nDate: %s%n%s%n",
                    _commitHash, shortID1, shortID2, _date, _message);
        }
        return String.format("===%ncommit %s%nDate: %s%n%s%n",
                _commitHash, _date, _message);
    }
}
