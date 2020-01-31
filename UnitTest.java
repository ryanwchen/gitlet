package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Ryan Chen
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    /** First test to get commit message. */
    @Test
    public void testCommitMessage() {
        Commit test = new Commit("hello", Commit.DATEFORMAT.format(new Date(0)),
                null, new HashMap<String, String>());
        assert (test.getMessage().equals("hello"));
    }

    /** Checks if commit msg is the same. */
    @Test
    public void testComMsg1() {
        Commit test = new Commit("good evening",
                Commit.DATEFORMAT.format(new Date(0)),
                null, new HashMap<String, String>());
        assert (test.getMessage().equals("good evening"));
    }

    /** Checks commit message 2 is correct. */
    @Test
    public void testDate() {
        Commit test = new Commit("Atlanta",
                Commit.DATEFORMAT.format(new Date(0)),
                null, new HashMap<String, String>());
        assert (test.getMessage().equals("Atlanta"));
    }

    /** Tests the empty commit. */
    @Test
    public void testEmptyCommit() {
        Commit test = Commit.EMPTYCOMMIT;
        assert (test.getMessage().equals("initial commit"));
    }

    /** Tests setting the 2nd parent hash. */
    @Test
    public void changeParent2Commit() {
        Commit test = Commit.EMPTYCOMMIT;
        test.setParent2Hash("nice");
        assert (test.getParent2Hash().equals("nice"));
    }

    /** Tests setting the 2nd parent hash null. */
    @Test
    public void testParent2CommitHashNull() {
        Commit test = Commit.EMPTYCOMMIT;
        test.setParent2Hash(null);
        assertNull(test.getParent2Hash());
    }

    /** Tests getting an empty parent. */
    @Test
    public void changeParentCommit() {
        Commit test = Commit.EMPTYCOMMIT;
        assertNull(test.getParent());
    }

    /** Tests setting commit message 2. */
    @Test
    public void testCommit2() {
        Commit test = new Commit("cool", Commit.DATEFORMAT.format(new Date(0)),
                null, new HashMap<String, String>());
        assert (test.getMessage().equals("cool"));
    }

    /** Tests setting commit message 3. */
    @Test
    public void testCommit3() {
        Commit test = new Commit("hi", Commit.DATEFORMAT.format(new Date(0)),
                null, new HashMap<String, String>());
        assert (test.getMessage().equals("hi"));
    }

    /** Tests getting a parent. */
    @Test
    public void getParentCommit() {
        Commit test = new Commit("hello", Commit.DATEFORMAT.format(new Date(0)),
                "fdddf", new HashMap<String, String>());
        assert (test.getParentHash().equals("fdddf"));
    }

    /** Tests getting current pointer. */
    @Test
    public void checkCurrentPointer() {
        Commit test = new Commit("hello", Commit.DATEFORMAT.format(new Date(0)),
                "123", new HashMap<String, String>());
        assert (test.getParentHash().equals("123"));
    }

    /** Tests to get First Commit Hash. */
    @Test
    public void checkFirstCommitHash() {
        Commit firstCommit = Commit.EMPTYCOMMIT;
        String emptyCommitID = "6be3acaa81987a291d2d4109897857bcd517a4c0";
        assert (firstCommit.getHash().substring(0,
                emptyCommitID.length()).equals(emptyCommitID));
    }
}

