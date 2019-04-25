package refactoringml.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LOCUtils {
	public static int countJavaFiles(String clonePath) throws IOException {
		String command = String.format("cloc %s --csv --quiet | grep \"Java\"", clonePath);

		String[] cmd = {
				"/bin/sh",
				"-c",
				command
		};

		Process p = Runtime.getRuntime().exec(cmd);

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

		StringBuilder result = new StringBuilder();

		String s;
		while ((s = stdInput.readLine()) != null) {
			result.append(s);
		}

		int totalLoc = Integer.parseInt(result.toString().trim().split(",")[4]);
		return totalLoc;
	}
}
