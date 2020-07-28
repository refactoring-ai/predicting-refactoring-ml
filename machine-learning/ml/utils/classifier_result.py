import csv
from pathlib import Path


def save_results(predictions, model_name, refactoring_name, metadata):
    file_exists = Path('result_file.csv').is_file()

    # Assuming the predictions are aligned with the metadata
    with open('result_file.csv', mode='a+') as result_file:
        result_writer = csv.writer(result_file, delimiter=',')

        if file_exists is False:
            result_writer.writerow(['Refactoring', 'Classifier', 'RefactorName', 'ClassName', 'MethodName',
                                    'VariableName', 'CommitId', 'GitUrl'])

        for prediction in predictions:
            for idx, row in metadata.iterrows():
                result_writer.writerow([prediction, model_name, refactoring_name] + row)
