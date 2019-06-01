from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import cross_val_score


def run_random_forest(x, y, f):

    #use all available cores by setting n_jobs=-1.
    model = RandomForestClassifier(random_state=42, n_jobs=-1)
    #fit model
    model.fit(x, y)

    #perform 10-fold validation 
    scores = cross_val_score(model, x, y, cv=10)
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores.mean(), scores.std() * 2))
    #show feature importances
    feature_importances_str = ''.join(["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in 
        zip(x.columns.values, model.feature_importances_)])

    precision = cross_val_score(model, x, y, scoring='precision', cv=10)
    recall = cross_val_score(model, x, y, scoring='recall', cv=10)
    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in precision]))
    precision_scores_str += f'\n(Min and max: {precision.min():.2f} and {precision.max():.2f})'
    precision_scores_str += f'\nMean precision: {precision.mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in recall]))
    recall_scores_str += f'\n(Min and max: {recall.min():.2f} and {recall.max():.2f})'
    recall_scores_str += f'\nMean recall: {recall.mean():.2f}'

    # output results to file
    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores.mean(), scores.std() * 2))
    f.write("\nFeature Importances\n")
    f.write(feature_importances_str)
    f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)
    f.write("\n---\n")

    return model

