package refactoringml.util;

import com.github.mauricioaniche.ck.util.SourceCodeLineCounter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static refactoringml.util.FileUtils.IsTestFile;

public class Counter {
	public static CounterResult countProductionAndTestFiles(String srcPath) {
		String[] allFiles = FileUtils.getAllJavaFiles(srcPath);

		List<String> productionFiles = Arrays.stream(allFiles).filter(x -> !IsTestFile(x)).collect(Collectors.toList());
		List<String> testFiles = Arrays.stream(allFiles).filter(x -> IsTestFile(x)).collect(Collectors.toList());

		Long productionLoc = productionFiles.stream().map(x -> countLines(x)).reduce(0L, (a, b) -> a + b);
		Long testLoc = testFiles.stream().map(x -> countLines(x)).reduce(0L, (a, b) -> a + b);

		return new CounterResult(productionFiles.size(), testFiles.size(), productionLoc, testLoc);
	}

	private static long countLines(String x) {
		try {
			return (long) SourceCodeLineCounter.getNumberOfLines(new BufferedReader(new FileReader(x)));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static class CounterResult {
		private long qtyOfProductionFiles;
		private long qtyOfTestFiles;

		private long locProductionFiles;
		private long locTestFiles;

		public CounterResult(long qtyOfProductionFiles, long qtyOfTestFiles, long locProductionFiles, long locTestFiles) {
			this.qtyOfProductionFiles = qtyOfProductionFiles;
			this.qtyOfTestFiles = qtyOfTestFiles;
			this.locProductionFiles = locProductionFiles;
			this.locTestFiles = locTestFiles;
		}

		public long getQtyOfProductionFiles() {
			return qtyOfProductionFiles;
		}

		public long getQtyOfTestFiles() {
			return qtyOfTestFiles;
		}

		public long getLocProductionFiles() {
			return locProductionFiles;
		}

		public long getLocTestFiles() {
			return locTestFiles;
		}
	}
}