import csv
from pathlib import Path


def save_results(model_name, refactoring_name, ids, y_test, y_pred):
    file_exists = Path('results/result_file.csv').is_file()

    # Assuming the predictions are aligned with the metadata
    with open('results/result_file.csv', mode='a+', newline='\n') as result_file:
        result_writer = csv.writer(result_file, delimiter=",", quoting=csv.QUOTE_MINIMAL)

        if file_exists is False:
            result_writer.writerow(['Classifier', 'RefactorName', 'Refactoring', 'Prediction', 'id'])

        for id, (idx, value) in enumerate(ids.items()):
            result_writer.writerow([model_name, refactoring_name, str(y_test.loc[idx]), str(y_pred[id]), str(value)])
