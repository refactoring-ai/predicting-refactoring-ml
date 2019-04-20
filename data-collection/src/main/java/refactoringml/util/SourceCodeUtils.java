package refactoringml.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

public class SourceCodeUtils {
	public static String removeComments(String sourceCode) {
		PrettyPrinterConfiguration conf = new PrettyPrinterConfiguration();
		conf.setIndentType(PrettyPrinterConfiguration.IndentType.SPACES);
		conf.setPrintComments(false);

		CompilationUnit compilationUnit = StaticJavaParser.parse(sourceCode);

		return compilationUnit.toString(conf);
	}
}
