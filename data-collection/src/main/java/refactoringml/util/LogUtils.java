package refactoringml.util;

import refactoringml.db.Project;

public class LogUtils {
    //Create a string with relevant information to analyze the error.
    public static String createErrorState(String commitId, Project project) {
        return "\n{commit: " + commitId + " from project: " + project;
    }

    //Create a string with relevant information to analyze the error.
    public static String createRefactoringErrorState(String commitId, Project project, String refactoringSummary) {
        return "\n{refactoring: " + refactoringSummary + " commit: " + commitId + " from project: " + project;
    }
}