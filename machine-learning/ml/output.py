import json

from log import log


def print_scores_1(dataset, refactoring_name, model_name, best_model, columns, scores):
    accuracy_str = "Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2)

    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']]))
    precision_scores_str += f'(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})'
    precision_scores_str += f'Mean precision: {scores["test_precision"].mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]]))
    recall_scores_str += f'(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})'
    recall_scores_str += f'Mean recall: {scores["test_recall"].mean():.2f}'

    print(accuracy_str)
    log(accuracy_str)
    log(precision_scores_str)
    log(recall_scores_str)

    # feature importance
    if hasattr(best_model, "coef_"):
        log("Features:")
        log(', '.join(str(e) for e in list(columns)))
        log("Coefficients:\n")
        log(''.join(str(e) for e in best_model.coef_.tolist()))

    log(f'CSV,{dataset},{refactoring_name},{model_name},{scores["test_precision"].mean():.2f},{scores["test_recall"].mean():.2f},{scores["test_accuracy"].mean()}')


def print_scores_2(dataset, refactoring_name, model_name, best_model, columns, scores):
    print(scores)
    accuracy_str = "Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2)
    print(accuracy_str)

    # show feature importances
    feature_importances_str = "Feature Importances: \n" + ''.join(
        ["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in
         zip(columns, best_model.feature_importances_)])
    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']]))
    precision_scores_str += f'(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})'
    precision_scores_str += f'Mean precision: {scores["test_precision"].mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]]))
    recall_scores_str += f'(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})'
    recall_scores_str += f'Mean recall: {scores["test_recall"].mean():.2f}'

    # output results to file
    log(accuracy_str)
    log(feature_importances_str)
    log(precision_scores_str)
    log(recall_scores_str)

    log(f'CSV,{dataset},{refactoring_name},{model_name},{scores["test_precision"].mean():.2f},{scores["test_recall"].mean():.2f},{scores["test_accuracy"].mean()}')


def print_best_parameters(f, tuned_model):
    best_parameters = tuned_model.best_params_
    log("Hyperparametrization:")
    log(json.dumps(best_parameters, indent=2))

    best_result = tuned_model.best_score_
    log("Best result:")
    log(str(best_result))
