import json


def format_results(dataset, refactoring_name, model_name, precision_scores, recall_scores, accuracy_scores, best_model, features):
    results = ""

    accuracy_scores_str = ', '.join(list([f"{e:.2f}" for e in accuracy_scores]))
    results += "Accuracy scores: " + accuracy_scores_str
    results += "\nMean Accuracy: %0.2f" % accuracy_scores.mean()

    precision_scores_str = ', '.join(list([f"{e:.2f}" for e in precision_scores]))
    results += "\nPrecision scores: " + precision_scores_str
    results += f'\nMean precision: {precision_scores.mean():.2f}'
    recall_scores_str = ', '.join(list([f"{e:.2f}" for e in recall_scores]))
    results += "\nRecall scores: " + recall_scores_str
    results += f'\nMean recall: {recall_scores.mean():.2f}\n'

    # some models have the 'coef_' attribute, and others have the 'feature_importances_
    # (do not ask me why...)
    if hasattr(best_model, "coef_"):
        results += "\nFeatures:"
        results += (', '.join(str(e) for e in list(features)))
        results += "\nCoefficients:"
        results += "\n" + ''.join(str(e) for e in best_model.coef_.tolist())
    elif hasattr(best_model, "feature_importances_"):
        results += ("\nFeature Importances: \n" + ''.join(
            ["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in
             zip(features, best_model.feature_importances_)]))
    else:
        results += "\n(Not possible to collect feature importances)"

    results += f'\nCSV,{dataset},{refactoring_name},{model_name},{precision_scores.mean():.2f},{recall_scores.mean():.2f},{accuracy_scores.mean()}'
    results += f'\nCSV2,{dataset},{refactoring_name},{model_name},precision,{precision_scores_str}'
    results += f'\nCSV2,{dataset},{refactoring_name},{model_name},recall,{recall_scores_str}'
    results += f'\nCSV2,{dataset},{refactoring_name},{model_name},accuracy,{accuracy_scores_str}'
    return results


def format_best_parameters(tuned_model):
    best_parameters = tuned_model.best_params_

    results = "Hyperparametrization:\n"
    results += json.dumps(best_parameters, indent=2)

    best_result = tuned_model.best_score_
    results += "\nBest result: "
    results += str(best_result)

    return results
