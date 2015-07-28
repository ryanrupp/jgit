package org.eclipse.jgit.api;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.TreeRevFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * @author rrupp
 *
 */
public class IssueWithTreeTraversal {

	/**
	 * To run this test pass the directory of the repository as an argument.
	 *
	 * This test shows there seems to be an issue with tree traversal I believe
	 * due to some optimizations that take place in {@link TreeRevFilter}.
	 *
	 * Test case is setup as follows and requires this specific branch state of
	 * treeWalkIssue branch. This is reproduced by the following:
	 * <ol>
	 * <li>Make commits on the main branch</li>
	 * <li>Create a new branch off this branch</li>
	 * <li>Make a commit on the new branch that adds a new file</li>
	 * <li>Merge the new branch back into the main branch</li>
	 * <li>Make another commit on the main branch.</li>
	 * </ol>
	 *
	 * This test case then uses the {@link LogCommand} to print out a log
	 * without path filtering (this works). The second time though we use path
	 * filtering and filter in the paths that we modified/added (pom.xml and
	 * added_file.txt in this case). The second test does not print the correct
	 * output, it stops traversal after the second commit missing the first
	 * commit.
	 *
	 * This appears to be due to some optimizations that are in
	 * {@link TreeRevFilter} which ends up getting used when we have a
	 * {@link RevWalk} with a {@link TreeFilter} set on it. Specifically, it
	 * appears when the merge commmit is encountered
	 * 24280cd4a6d8642be7e769fb7031c349e49d98f1, it sees that there's two
	 * parents and tries to reduce them which does two things, it removes the
	 * previous commit on the main branch as a parent
	 * (295ef8e1eb08c6db0f8b283d0bc83fb85f596012) because this will get picked
	 * up when we traverse the merged branch commits parent
	 * (a46f1e78838940e0a704b983404054e3b8b155fa) but it also seems to the
	 * parents then from the second commit on the main branch
	 * (295ef8e1eb08c6db0f8b283d0bc83fb85f596012), see line 239 of
	 * {@link TreeRevFilter}.
	 *
	 * This makes it so that when the RevWalk encounters the second commit
	 * (295ef8e1eb08c6db0f8b283d0bc83fb85f596012) it no longer has any parent
	 * commits associated with it so it never traverse the first commit so it's
	 * missed in the results.
	 *
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		try {
			Git git = Git.open(new File(args[0]));

			callAndPrintRevisions("logCommandWithoutFilterWorks", git.log()
					.setMaxCount(MAX_COUNT));

			callAndPrintRevisions("logCommandWithPathFilterMissingFirstCommit",
					git.log().setMaxCount(MAX_COUNT).addPath("pom.xml")
							.addPath("added_file.txt"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void callAndPrintRevisions(String test, LogCommand logCommand)
			throws NoHeadException, GitAPIException {
		System.out.println("Output for test: " + test);
		for (RevCommit commit : logCommand.call()) {
			System.out.println(commit + " : " + commit.getShortMessage());
		}

		System.out.println();
	}

	private static final int MAX_COUNT = 10;
}
