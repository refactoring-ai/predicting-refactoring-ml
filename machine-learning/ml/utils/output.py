import json


def format_results(dataset, refactoring_name, model_name, precision_scores, recall_scores,
                   accuracy_scores, tn, fp, fn, tp, best_model, features):
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

    # summing up the results of the confusion matrix
    total_tn = sum(tn)
    total_fp = sum(fp)
    total_fn = sum(fn)
    total_tp = sum(tp)

    # TODO: print number by number of the confusion matrix
    # (for debugging purposes, we print it in the log already)

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

    results += f'\nCSV,{dataset},{refactoring_name},{model_name},{precision_scores.mean():.2f},{recall_scores.mean():.2f},{accuracy_scores.mean()},{total_tn},{total_fp},{total_fn},{total_tp}'
    results += f'\nCSV2,{dataset},{refactoring_name},{model_name},precision,{precision_scores_str}'
    results += f'\nCSV2,{dataset},{refactoring_name},{model_name},recall,{recall_scores_str}'
    results += f'\nCSV2,{dataset},{refactoring_name},{model_name},accuracy,{accuracy_scores_str}'
    return results


def format_results_single_run(dataset, refactoring_name, model_name, precision, recall, accuracy, tn, fp, fn, tp, best_model, features):
    results = ""

    results += "\nPrecision: %0.2f" % precision
    results += "\nRecall: %0.2f" % recall
    results += "\nAccuracy: %0.2f" % accuracy
    results += "\nConfusion Matrix: tn=%0.2f, fp=%0.2f, fn=%0.2f, tp=%0.2f" % (tn, fp, fn, tp)

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

    results += f'\nCSV,{dataset},{refactoring_name},{model_name},{precision},{recall},{accuracy},{tn},{fp},{fn},{tp}'
    return results


def format_best_parameters(tuned_model):
    best_parameters = tuned_model.best_params_

    results = "Hyperparametrization:\n"
    results += json.dumps(best_parameters, indent=2)

    best_result = tuned_model.best_score_
    results += "\nBest result: "
    results += str(best_result)

    return results
