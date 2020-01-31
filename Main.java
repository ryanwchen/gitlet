package gitlet;

import java.io.File;

/**
 * Driver class for Gitlet, the miniature version-control system.
 *
 * @author Ryan Chen
 */
public class Main {
    /**
     * Current working directory.
     */
    static final File CWD = new File(".");
    /**
     * Main Folder.
     */
    static final File MAIN_FOLDER = (Utils.join(CWD, ".gitlet"));
    /**
     * Staging area.
     */
    static final File ADD_FOLDER = Utils.join(MAIN_FOLDER, "add");
    /**
     * Folder containg all blobs.
     */
    static final File BLOB_FOLDER = Utils.join(MAIN_FOLDER, "blobs");
    /**
     * File containing serialized story object.
     */
    static final File STORY_FILE = Utils.join(MAIN_FOLDER, "story");
    /**
     * Folder containing all commits.
     */
    static final File COMMIT_FOLDER = Utils.join(MAIN_FOLDER, "commits");

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {
        try {
            basicCheck(args);
            if (args[0].equals("init")) {
                checkNumArgs(args, 1); init();
                return;
            }
            Story story = Story.storyFromFile();
            switch (args[0]) {
            case "add":
                checkNumArgs(args, 2); story.add(args[1]); break;
            case "commit":
                checkNumArgs(args, 2);
                Commit current = story.makeCommit(args[1]);
                story.updateStory(current); current.saveCommit(); break;
            case "rm":
                checkNumArgs(args, 2); story.rm(args[1]); break;
            case "log":
                checkNumArgs(args, 1); story.log(); break;
            case "global-log":
                checkNumArgs(args, 1); story.globalLog(); break;
            case "find":
                checkNumArgs(args, 2); story.find(args[1]); break;
            case "status":
                checkNumArgs(args, 1); story.statusBasic(); break;
            case "checkout":
                story.checkout(args); break;
            case "branch":
                checkNumArgs(args, 2); story.branch(args[1]); break;
            case "rm-branch":
                checkNumArgs(args, 2); story.rmBranch(args[1]); break;
            case "reset":
                checkNumArgs(args, 2); story.reset(args[1]); break;
            case "merge":
                checkNumArgs(args, 2);
                boolean mergeConflict = story.mergeConflict(args[1]);
                Commit cool = story.makeCommit("Merged " + args[1]
                        + " into " + story.getCurrentPointer() + ".");
                if (mergeConflict) {
                    System.out.println("Encountered a merge conflict.");
                }
                String parent2Hash =
                        story.getCommitFromBranch(args[1]).getHash();
                cool.setParent2Hash(parent2Hash); story.updateStory(cool);
                cool.saveCommit(); story.resetMerge(); break;
            default:
                throw new GitletException("No command with that name exists.");
            }
            story.saveStory();
        } catch (GitletException | IllegalArgumentException e) {
            System.out.print(e.getMessage()); System.exit(0);
        }
    }

    /**
     * Basic check for errors.
     *
     * @param args are the arguments from command line.
     */
    public static void basicCheck(String... args) {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        }
        if (!args[0].equals("init") && !MAIN_FOLDER.isDirectory()) {
            throw new GitletException("Not in "
                    + "an initialized Gitlet directory.");
        }
    }

    /**
     * Init function of gitlet.
     */
    public static void init() {
        if (MAIN_FOLDER.isDirectory()) {
            throw new GitletException("A Gitlet version-control system already "
                    + "exists in the current directory.");
        } else {
            MAIN_FOLDER.mkdirs();
            COMMIT_FOLDER.mkdirs();
            ADD_FOLDER.mkdirs();
            BLOB_FOLDER.mkdirs();
            Story story = new Story();
            story.saveStory();
        }
    }

    /**
     * Helper function that checks the number of args is correct.
     *
     * @param args are the args.
     * @param n    is the number being referenced.
     */
    public static void checkNumArgs(String[] args, int n) {
        if (args.length != n) {
            throw new GitletException("Incorrect operands.");
        }
        return;
    }
}
