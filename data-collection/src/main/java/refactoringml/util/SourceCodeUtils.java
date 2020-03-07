package refactoringml.util;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.PrettyPrinterConfiguration;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import java.io.IOException;
import static refactoringml.util.JGitUtils.readFileFromGit;

public class SourceCodeUtils {
	//Get the clean source code for the given file for the given commit from the repository
	public static String getCleanSourceCode(Repository repo, RevCommit commit, String filepath) throws IOException {
		String rawSourceCode = readFileFromGit(repo, commit, filepath);

		try {
			return SourceCodeUtils.removeComments(rawSourceCode);
		} catch (ParseProblemException e){
			//we reached the start of the repo, without finding a parsable version of the class
			if(commit.getParentCount() == 0)
				return "";

			return getCleanSourceCode(repo, commit.getParent(0), filepath);
		}
	}

	public static String removeComments(String sourceCode) {
		PrettyPrinterConfiguration conf = new PrettyPrinterConfiguration();
		conf.setIndentType(PrettyPrinterConfiguration.IndentType.SPACES);
		conf.setPrintComments(false);

		CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);
		return compilationUnit.toString(conf);
	}

	public static  boolean nonClassFile(String fileName) {
		return fileName.equals("/dev/null");
	}
}