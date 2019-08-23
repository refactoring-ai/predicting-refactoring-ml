import json

from utils.log import log


def print_scores_1(dataset, refactoring_name, model_name, best_model, columns, scores):
    log("Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2))

    log("Precision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']])))
    log(f'(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})')
    log(f'Mean precision: {scores["test_precision"].mean():.2f}')
    log("Recall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]])))
    log(f'(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})')
    log(f'Mean recall: {scores["test_recall"].mean():.2f}')

    # feature importance
    if hasattr(best_model, "coef_"):
        log("Features:")
        log(', '.join(str(e) for e in list(columns)))
        log("Coefficients:\n")
        log(''.join(str(e) for e in best_model.coef_.tolist()))

    log(f'CSV,{dataset},{refactoring_name},{model_name},{scores["test_precision"].mean():.2f},{scores["test_recall"].mean():.2f},{scores["test_accuracy"].mean()}')


def print_scores_2(dataset, refactoring_name, model_name, best_model, columns, scores):
    log("Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2))

    # show feature importances
    log("Feature Importances: \n" + ''.join(
        ["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in
         zip(columns, best_model.feature_importances_)]))

    log("Precision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']])))
    log(f'(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})')
    log(f'Mean precision: {scores["test_precision"].mean():.2f}')
    log("Recall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]])))
    log(f'(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})')
    log(f'Mean recall: {scores["test_recall"].mean():.2f}')

    # output results to file
    log(f'CSV,{dataset},{refactoring_name},{model_name},{scores["test_precision"].mean():.2f},{scores["test_recall"].mean():.2f},{scores["test_accuracy"].mean()}')


def print_best_parameters(tuned_model):
    best_parameters = tuned_model.best_params_
    log("Hyperparametrization:")
    log(json.dumps(best_parameters, indent=2))

    best_result = tuned_model.best_score_
    log("Best result:")
    log(str(best_result))
