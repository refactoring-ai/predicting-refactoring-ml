import traceback
import pandas as pd
from configs import SEARCH, N_CV_SEARCH, N_ITER_RANDOM_SEARCH, TEST_SPLIT_SIZE, VALIDATION_DATASETS, TEST
from ml.utils.output import format_results_single_run
from sklearn.metrics import accuracy_score, precision_score, recall_score, confusion_matrix
from sklearn.model_selection import RandomizedSearchCV, StratifiedKFold, GridSearchCV, train_test_split
from ml.pipelines.pipelines import MLPipeline
from ml.preprocessing.preprocessing import retrieve_labelled_instances
from ml.utils.output import format_best_parameters
from utils.date_utils import now
from utils.log import log


def _build_production_model(model_def, best_params, x, y):
    log("Production model build started at %s\n" % now())

    super_model = model_def.model(best_params)
    super_model.fit(x, y)

    return super_model


def _evaluate_model(search, x_train, x_tests, y_train, y_tests):
    log("Test search started at %s\n" % now(), False)
    search.fit(x_train, y_train)
    log(format_best_parameters(search), False)
    best_estimator = search.best_estimator_

    test_scores = {'accuracy': [], 'precision': [], 'recall': [], 'tn': [], 'fp': [], 'fn': [], 'tp': []}
    # Predict unseen results for all validation sets
    for index, x_test in enumerate(x_tests):
        y_pred = best_estimator.predict(x_test)
        y_test = y_tests[index]
        test_scores["accuracy"] += [accuracy_score(y_test, y_pred)]
        test_scores["precision"] += [precision_score(y_test, y_pred)]
        test_scores["recall"] += [recall_score(y_test, y_pred)]
        test_scores["tn"] += [confusion_matrix(y_test, y_pred).ravel()[0]]
        test_scores["fp"] += [confusion_matrix(y_test, y_pred).ravel()[1]]
        test_scores["fn"] += [confusion_matrix(y_test, y_pred).ravel()[2]]
        test_scores["tp"] += [confusion_matrix(y_test, y_pred).ravel()[3]]

    return test_scores


class BinaryClassificationPipeline(MLPipeline):
    """
    Train models for binary classification
    """

    def __init__(self, models_to_run, refactorings, datasets):
        super().__init__(models_to_run, refactorings, datasets)

    def run(self):
        """
        The main method of this pipeline.

        For each combination of dataset, refactoring, and model, it:
        1) Retrieved the labelled instances
        2) See, _run_all_models
        """

        # if there is no datasets, refactorings, and models to run, just stop
        if not self._datasets or not self._refactorings or not self._models_to_run:
            return

        for dataset in self._datasets:
            log("Dataset {}".format(dataset))

            for refactoring in self._refactorings:
                refactoring_name = refactoring.name()
                log("**** Refactoring Type: %s" % refactoring_name)

                # we have two options to select a test set,
                # 1.) Predefined in the database
                if TEST_SPLIT_SIZE < 0 and len(VALIDATION_DATASETS) > 0:
                    train_features, x_train, y_train, scaler = retrieve_labelled_instances(dataset, refactoring, True)
                    # test if any refactorings were found for the given refactoring type
                    if x_train is None:
                        log("Skip model building for refactoring type: " + refactoring.name())
                        continue

                    x_tests, y_tests, dataset_name = [], [], []
                    for validation_dataset in VALIDATION_DATASETS:
                        dataset_name.append(validation_dataset)
                        test_features, x_test, y_test, _ = retrieve_labelled_instances(validation_dataset, refactoring,
                                                                                       False, scaler, train_features)
                        # test if any refactorings were found for the given refactoring type
                        if x_test is None:
                            log("Skip test set %s for refactoring type: %s" % (validation_dataset, refactoring.name()))
                            continue
                        x_tests.append(x_test)
                        y_tests.append(y_test)
                    if len(x_tests) == 0:
                        log("Skip model building for refactoring type: " + refactoring.name())
                        continue

                    # X and Y where already shuffled in the retrieve_labelled_instances function
                    x = pd.concat([x_train] + x_tests)
                    y = pd.concat([y_train] + y_tests)
                    self._run_all_models(refactoring, refactoring_name, dataset, train_features, scaler, x, y, x_train,
                                         x_tests, y_train, y_tests, dataset_name)
                # 2.) random percentage train/ test split
                else:
                    features, x, y, scaler = retrieve_labelled_instances(dataset, refactoring, True)
                    # test if any refactorings were found for the given refactoring type
                    if x is None:
                        log("Skip model building for refactoring type: " + refactoring.name())
                        continue
                    # we split in train and test
                    # (note that we use the same split for all the models)
                    x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=TEST_SPLIT_SIZE, random_state=42)
                    self._run_all_models(refactoring, refactoring_name, dataset, features, scaler, x, y, x_train, [x_test], y_train, [y_test], ["random split"])

    def _run_all_models(self, refactoring, refactoring_name, dataset, features, scaler, x, y, x_train, x_tests, y_train, y_tests, test_names):
        """
        For each model, it:
        1) Performs the hyper parameter search
        2) Performs k-fold cross-validation
        3) Persists evaluation results and the best model
        """
        for model in self._models_to_run:
            model_name = model.name()
            if TEST:
                model_name += " test"
            try:
                log("\nBuilding Model {}".format(model.name()))
                self._start_time()
                test_scores, model_to_save = self._run_single_model(model, x, y, x_train, x_tests, y_train, y_tests)

                # log test scores
                log(format_results_single_run(dataset, refactoring_name, test_names, model_name, test_scores["precision"],
                                            test_scores["recall"], test_scores['accuracy'], test_scores['tn'],
                                            test_scores['fp'], test_scores['fn'], test_scores['tp'],
                                              model_to_save, features))

                # we save the best estimator we had during the search
                model.persist(dataset, refactoring_name, features, model_to_save, scaler)
                self._finish_time(dataset, model, refactoring)
            except Exception as e:
                log("An error occurred while working on refactoring " + refactoring_name + " model " + model.name()
                       + " with datasets: " + str(test_names))
                log(str(e))
                log(str(traceback.format_exc()))

    def _run_single_model(self, model_def, x, y, x_train, x_tests, y_train, y_tests):
        model = model_def.model()

        # perform the search for the best hyper parameters
        param_dist = model_def.params_to_tune()
        search = None

        # choose which search to apply
        if SEARCH == 'randomized':
            search = RandomizedSearchCV(model, param_dist, n_iter=N_ITER_RANDOM_SEARCH, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)
        elif SEARCH == 'grid':
            search = GridSearchCV(model, param_dist, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)

        # Train and test the model
        test_scores = _evaluate_model(search, x_train, x_tests, y_train, y_tests)

        # Run cross validation on whole dataset and safe production ready model
        super_model = _build_production_model(model_def, search.best_params_, x, y)

        # return the scores and the best estimator
        return test_scores, super_model
