import json


def format_results(dataset, refactoring_name, model_name, precision_scores, recall_scores, accuracy_scores, best_model, features):
    results = ""

    results += "Accuracy: %0.2f (+/- %0.2f)" % (accuracy_scores.mean(), accuracy_scores.std() * 2)

    results += "\nPrecision scores: " + ', '.join(list([f"{e:.2f}" for e in precision_scores]))
    results += f'\n(Min and max: {precision_scores.min():.2f} and {precision_scores.max():.2f})'
    results += f'\nMean precision: {precision_scores.mean():.2f}'
    results += "\nRecall scores: " + ', '.join(list([f"{e:.2f}" for e in recall_scores]))
    results += f'\n(Min and max: {recall_scores.min():.2f} and {recall_scores.max():.2f})'
    results += f'\nMean recall: {recall_scores.mean():.2f}'

    # some models have the 'coef_' attribute, and others have the 'feature_importances_
    # (do not ask me why...)
    if hasattr(best_model, "coef_"):
        results += "\nFeatures:"
        results += (', '.join(str(e) for e in list(features)))
        results += "\nCoefficients:"
        results += "\n" + ''.join(str(e) for e in best_model.coef_.tolist())
    elif hasattr(best_model, "feature_importances_"):
        results += ("Feature Importances: \n" + ''.join(
            ["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in
             zip(features, best_model.feature_importances_)]))
    else:
        results += "(Not possible to collect feature importances)"

    results += f'\nCSV,{dataset},{refactoring_name},{model_name},{precision_scores.mean():.2f},{recall_scores.mean():.2f},{accuracy_scores.mean()}'
    return results


def format_best_parameters(tuned_model):
    best_parameters = tuned_model.best_params_

    results = "Hyperparametrization:\n"
    results += json.dumps(best_parameters, indent=2)

    best_result = tuned_model.best_score_
    results += "\nBest result:\n"
    results += str(best_result)

    return results
