import csv
from pathlib import Path


def save_results(predictions, model_name, refactoring_name, metadata):
    file_exists = Path('results/result_file.csv').is_file()

    # Assuming the predictions are aligned with the metadata
    with open('results/result_file.csv', mode='a+', newline='\n') as result_file:
        result_writer = csv.writer(result_file, delimiter=",", quoting=csv.QUOTE_MINIMAL)

        if file_exists is False:
            result_writer.writerow(['Refactoring', 'Classifier', 'RefactorName', 'ClassName', 'CommitId', 'GitUrl'])

        for id, (idx, row) in enumerate(metadata.iterrows()):
            result_writer.writerow([str(predictions[id]), str(model_name), str(refactoring_name)] + row.to_list())
