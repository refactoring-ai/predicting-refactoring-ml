package refactoringml;

import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import org.eclipse.jgit.api.Git;
import org.junit.Ignore;
import org.junit.Test;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import refactoringml.util.RefactoringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RefactoringMinerPlaygroundTest {

	@Test @Ignore
	public void x() throws IOException {
		String commit = "b610707cd072f07efb816074a4844bb1b31e482c";

		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

		Git git = Git.open(new File("/Users/mauricioaniche/Desktop/commons-lang"));


		miner.detectAtCommit(git.getRepository(), commit, new RefactoringHandler() {
			@Override
			public boolean skipCommit(String commitId) {
				return super.skipCommit(commitId);
			}

			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {

				System.out.println(refactorings);

				for (Refactoring refactoring : refactorings) {
					RenameVariableRefactoring r = (RenameVariableRefactoring) refactoring;

					System.out.println(RefactoringUtils.fullMethodName(r.getOperationBefore()));
				}
			}
		});
	}
}
