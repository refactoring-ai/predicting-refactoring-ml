import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RepoCleaner {
    private static String androidReposRaw = "C:/Users/jange/Desktop/predicting-refactoring-ml/data-collection/android_repos.txt";
    private static String githubReposRaw = "C:/Users/jange/Desktop/predicting-refactoring-ml/data-collection/project_list_2.csv";

    private static String androidReposOut = "C:/Users/jange/Desktop/predicting-refactoring-ml/data-collection/android_repos_cleaned.csv";
    private static String githubReposCleaned = "C:/Users/jange/Desktop/predicting-refactoring-ml/data-collection/project_list_2_cleaned.csv";

    private static String githubReposOldIn = "C:/Users/jange/Desktop/predicting-refactoring-ml/data-collection/project_list_cleaned_noDup.csv";
    private static String githubReposOut = "C:/Users/jange/Desktop/predicting-refactoring-ml/data-collection/project_list_2_cleaned_noDup.csv";

    public static void main(String[] args) throws IOException {
        writeAndRead(androidReposRaw,androidReposOut, RepoCleaner::extractRepoUrlAndroid);
        writeAndRead(githubReposRaw,githubReposCleaned, RepoCleaner::extractRepoUrlGithub);
        removeDuplicates();
    }

    /*
    Remove all duplicate repositories from the githubRepos
     */
    private static void removeDuplicates() throws IOException {
        List<String> androidRepos = FileUtils.readLines(new File(androidReposOut));
        List<String> githubRepos = FileUtils.readLines(new File(githubReposCleaned));
        List<String> githubReposOld = FileUtils.readLines(new File(githubReposOldIn));

        int githubReposCount = githubRepos.size();
        githubRepos.removeAll(androidRepos);
        System.out.println("Removed " + (githubReposCount -  githubRepos.size()) + " duplicate repositories from the github repository list.");

        githubReposCount = githubRepos.size();
        githubRepos.removeAll(githubReposOld);
        System.out.println("Removed " + (githubReposCount -  githubRepos.size()) + " duplicate repositories from the github repository list.");

        writeCSV(githubReposOut, githubRepos);
    }

    private static void writeAndRead(String fileIn, String fileOut,  Function<String[], String> urlParser) throws IOException {
        List<String> parsed = parseCSV(fileIn, urlParser);
        System.out.println("Parsed a total of " + parsed.size() + " repositories.");

        parsed.stream().distinct().collect(Collectors.toList());
        System.out.println("Wrote a total of " + parsed.size() + " repositories to disk.");
        writeCSV(fileOut, parsed);
    }

    private static List<String> parseCSV(String filePath, Function<String[], String> urlParser) throws IOException {
        List<String> lines = FileUtils.readLines(new File(filePath));
        List<String> parsed = new ArrayList();
        for(String line : lines) {
            try {
                String[] values = line.split(",");
                String repoName = values[2];
                String repoURL = urlParser.apply(values);
                String host = "github";
                parsed.add(repoName + "," + repoURL + "," + host);
            } catch (ArrayIndexOutOfBoundsException e){
                System.out.println("Failed to parse the line: " + line);
            }
        }

        return parsed;
    }

    private static String extractRepoUrlGithub(String[] values){
        return values[3].replace("api.", "").replace("repos/","");
    }

    private static String extractRepoUrlAndroid(String[] values){
        return "https://github.com/" + values[2] + "/" + values[3];
    }

    private static void writeCSV(String filePath, List<String> content) throws IOException {
        for(String line : content) {
            refactoringml.util.FileUtils.appendToFile(new File(filePath), line + "\n");
        }
    }
}