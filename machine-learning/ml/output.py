import json


def format_results(dataset, refactoring_name, model_name, scores, best_model, features):
    results = ""

    results += "Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2)

    results += "\nPrecision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']]))
    results += f'\n(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})'
    results += f'\nMean precision: {scores["test_precision"].mean():.2f}'
    results += "\nRecall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]]))
    results += f'\n(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})'
    results += f'\nMean recall: {scores["test_recall"].mean():.2f}'

    if hasattr(best_model, "coef_"):
        results += "\nFeatures:"
        results += (', '.join(str(e) for e in list(features)))
        results += "\nCoefficients:"
        results += "\n" + ''.join(str(e) for e in best_model.coef_.tolist())
    else:
        results += ("Feature Importances: \n" + ''.join(
            ["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in
             zip(features, best_model.feature_importances_)]))

    results += f'\nCSV,{dataset},{refactoring_name},{model_name},{scores["test_precision"].mean():.2f},{scores["test_recall"].mean():.2f},{scores["test_accuracy"].mean()}'
    return results


def format_best_parameters(tuned_model):
    best_parameters = tuned_model.best_params_

    results = ""

    results +="Hyperparametrization:\n"
    results += json.dumps(best_parameters, indent=2)

    best_result = tuned_model.best_score_
    results += "\nBest result:\n"
    results += str(best_result)

    return results
