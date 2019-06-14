import json


def print_scores_1(scores, best_model, columns, f):
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2))

    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']]))
    precision_scores_str += f'\n(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})'
    precision_scores_str += f'\nMean precision: {scores["test_precision"].mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]]))
    recall_scores_str += f'\n(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})'
    recall_scores_str += f'\nMean recall: {scores["test_recall"].mean():.2f}'

    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2))
    f.write("\nFeatures:\n")
    f.write(', '.join(str(e) for e in list(columns)))
    f.write("\n")
    f.write("Coefficients:\n")
    f.write(''.join(str(e) for e in best_model.coef_.tolist()))
    f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)


def print_scores_2(best_model, columns, f, scores):
    print(scores)
    accuracy_str = "Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2)
    print(accuracy_str)
    # show feature importances
    feature_importances_str = "Feature Importances: " + ''.join(
        ["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in
         zip(columns, best_model.feature_importances_)])
    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']]))
    precision_scores_str += f'\n(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})'
    precision_scores_str += f'\nMean precision: {scores["test_precision"].mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]]))
    recall_scores_str += f'\n(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})'
    recall_scores_str += f'\nMean recall: {scores["test_recall"].mean():.2f}'
    # output results to file
    f.write(accuracy_str)
    f.write("\n")
    f.write(feature_importances_str)
    f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)


def print_best_parameters(f, tuned_model):
    best_parameters = tuned_model.best_params_
    f.write("\nHyperparametrization:\n")
    f.write("Best parameters:\n")
    f.write(json.dumps(best_parameters, indent=2))
    print("Best parameters: " + json.dumps(best_parameters, indent=2))
    best_result = tuned_model.best_score_
    print("Best result: " + str(best_result))
    f.write("Best result:\n")
    f.write(str(best_result))
