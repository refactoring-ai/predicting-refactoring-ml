from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_val_score


def run_logistic_regression(x, y, f):

    model = LogisticRegression()
    model.fit(x, y)

    # apply 10-fold validation
    scores = cross_val_score(model, x, y, cv=10)
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores.mean(), scores.std() * 2))

    precision = cross_val_score(model, x, y, scoring='precision', cv=10)
    recall = cross_val_score(model, x, y, scoring='recall', cv=10)
    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in precision]))
    precision_scores_str += f'\n(Min and max: {precision.min():.2f} and {precision.max():.2f})'
    precision_scores_str += f'\nMean precision: {precision.mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in recall]))
    recall_scores_str += f'\n(Min and max: {recall.min():.2f} and {recall.max():.2f})'
    recall_scores_str += f'\nMean recall: {recall.mean():.2f}'

    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores.mean(), scores.std() * 2))
    f.write("\nFeatures:\n")
    f.write(', '.join(str(e) for e in list(x.columns.values)))
    f.write("\n")
    f.write("Coefficients:\n")
    f.write(''.join(str(e) for e in model.coef_.tolist()))
    f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)

    return model
