package refactoringml.util;

import refactoringml.db.Project;

public class LogUtils {
    //Create a string with relevant information to analyze the error.
    public static String createErrorState(String commitId, Project project) {
        return "\n{commit: " + commitId + " from project: " + project;
    }

    //Create a string with relevant information to analyze the error.
    public static String createRefactoringErrorState(String commitId, Project project, String refactoringSummary) {
        return "\n{refactoring: " + shortSummary(refactoringSummary) + " commit: " + commitId + " from project: " + project;
    }

    public static String createRefactoringErrorState(String commitId, String refactoringSummary) {
        return "\n{refactoring: " + shortSummary(refactoringSummary) + " commit: " + commitId +"}";
    }

    public static String shortSummary(String refactoringSummary) {
        return refactoringSummary.length() <= 20 ? refactoringSummary : refactoringSummary.substring(0, 20);
    }
}