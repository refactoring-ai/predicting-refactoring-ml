import logging as logger
import os
import numpy
from flask import jsonify, request
from flask_restful import Resource, reqparse, abort
from joblib import load



class Recommend(Resource):

    def __init__(self, root_path):
        self.predictors = {}
        self.scalers = {}

        curr_dir = os.path.dirname(os.path.abspath(__file__))
        root_path = curr_dir + "/" + root_path 
        models_name = os.listdir(root_path + "/models")
        scalers_name = os.listdir(root_path + "/scalers")

        for m in models_name:
            if m != '.DS_Store':
                self.predictors[m] = load(root_path + "/models/" + m)
        for s in scalers_name:
            if s != '.DS_Store':
                self.scalers[s] = load(root_path + "/scalers/" + s)


    def predict(self, model, dataset, refactoring_name, features, level):
        key_model = "model-{}--{}.joblib".format(model, refactoring_name)
        key_scaler = "scaler-{}--{}.joblib".format(model, refactoring_name)
        features =  self.scalers[key_scaler].transform([features])
        
        return self.predictors[key_model].predict(features)


    def get(self):
        logger.debug("Inside the get method")
        aboutInformation = 'This is an API that explores the effectiveness of machine learning models to predict software refactorings. ' \
                           'More specifically, we make use of six different machine learning algorithms ' \
                           '(i.e., Logistic Regression, Naive Bayes, SVM, Decision Trees, Random Forest, and Deep Neural Network) and ' \
                           'train them in a dataset of more than two million real-world refactorings that happened in 11,149 projects  from the Apache, F-Droid, and GitHub ecosystems. '

        authors = 'Anonymous authors'

        return jsonify({'About:': aboutInformation, 'How to use': 'For more information how to use this api, please visit www.refactoring.io.', 'Authors': authors})

    def validate_data(self, data):
        """ 
        Validate input data
      
        Returns: 
        version: string
        dataset: string
        refactoring: string
        model: string
        features: dict
        level: string
        """

        logger.debug("Validating sent data")

        class_level_refactoring = {
            "Extract Class": "Extract Class",
            "Extract Subclass": "Extract Subclass",
            "Extract Super-class": "Extract Super-class",
            "Extract Interface": "Extract Interface",
            "Move Class": "Move Class",
            "Rename Class": "Rename Class",
            "Move and Rename Class": "Move and Rename Class"
        }

        method_level_refactoring = {
            "Extract And Move Method": "Extract And Move Method",
            "Extract Method": "Extract Method",
            "Inline Method": "Inline Method",
            "Move Method": "Move Method",
            "Pull Up Method": "Pull Up Method",
            "Push Down Method": "Push Down Method",
            "RenameMethod": "Rename Method"
        }

        variable_level_refactoring = {
            "Extract Variable": "Extract Variable",
            "Inline Variable": "Inline Variable",
            "Parameterize Variable": "Parameterize Variable",
            "Rename Variable": "Rename Variable",
            "Rename Parameter": "Rename Parameter",
            "Replace Variable with Attribute": "Replace Variable with Attribute",
        }

        class_order = ["classAnonymousClassesQty", "classAssignmentsQty", "classCbo", "classComparisonsQty", "classLambdasQty", "classLcom", "classLoc", "classLoopQty", "classMathOperationsQty", "classMaxNestedBlocks", "classNosi", "classNumberOfAbstractMethods", "classNumberOfDefaultFields", "classNumberOfDefaultMethods", "classNumberOfFields", "classNumberOfFinalFields", "classNumberOfFinalMethods", "classNumberOfMethods", "classNumberOfPrivateFields", "classNumberOfPrivateMethods", "classNumberOfProtectedFields", "classNumberOfProtectedMethods", "classNumberOfPublicFields", "classNumberOfPublicMethods", "classNumberOfStaticFields", "classNumberOfStaticMethods", "classNumberOfSynchronizedFields", "classNumberOfSynchronizedMethods", "classNumbersQty", "classParenthesizedExpsQty", "classReturnQty", "classRfc", "classStringLiteralsQty", "classSubClassesQty", "classTryCatchQty", "classUniqueWordsQty", "classVariablesQty", "classWmc", "authorOwnership", "bugFixCount", "linesAdded", "linesDeleted", "qtyMajorAuthors", "qtyMinorAuthors", "qtyOfAuthors", "qtyOfCommits", "refactoringsInvolved"]
        method_order = ["classAnonymousClassesQty", "classAssignmentsQty", "classCbo", "classComparisonsQty", "classLambdasQty", "classLcom", "classLoc", "classLoopQty", "classMathOperationsQty", "classMaxNestedBlocks", "classNosi", "classNumberOfAbstractMethods", "classNumberOfDefaultFields", "classNumberOfDefaultMethods", "classNumberOfFields", "classNumberOfFinalFields", "classNumberOfFinalMethods", "classNumberOfMethods", "classNumberOfPrivateFields", "classNumberOfPrivateMethods", "classNumberOfProtectedFields", "classNumberOfProtectedMethods", "classNumberOfPublicFields", "classNumberOfPublicMethods", "classNumberOfStaticFields", "classNumberOfStaticMethods", "classNumberOfSynchronizedFields", "classNumberOfSynchronizedMethods", "classNumbersQty", "classParenthesizedExpsQty", "classReturnQty", "classRfc", "classStringLiteralsQty", "classSubClassesQty", "classTryCatchQty", "classUniqueWordsQty", "classVariablesQty", "classWmc", "methodAnonymousClassesQty", "methodAssignmentsQty", "methodCbo", "methodComparisonsQty", "methodLambdasQty", "methodLoc", "methodLoopQty", "methodMathOperationsQty", "methodMaxNestedBlocks", "methodNumbersQty", "methodParametersQty", "methodParenthesizedExpsQty", "methodReturnQty", "methodRfc", "methodStringLiteralsQty", "methodSubClassesQty", "methodTryCatchQty", "methodUniqueWordsQty", "methodVariablesQty", "methodWmc"]
        variable_order = ["classAnonymousClassesQty", "classAssignmentsQty", "classCbo", "classComparisonsQty", "classLambdasQty", "classLcom", "classLoc", "classLoopQty", "classMathOperationsQty", "classMaxNestedBlocks", "classNosi", "classNumberOfAbstractMethods", "classNumberOfDefaultFields", "classNumberOfDefaultMethods", "classNumberOfFields", "classNumberOfFinalFields", "classNumberOfFinalMethods", "classNumberOfMethods", "classNumberOfPrivateFields", "classNumberOfPrivateMethods", "classNumberOfProtectedFields", "classNumberOfProtectedMethods", "classNumberOfPublicFields", "classNumberOfPublicMethods", "classNumberOfStaticFields", "classNumberOfStaticMethods", "classNumberOfSynchronizedFields", "classNumberOfSynchronizedMethods", "classNumbersQty", "classParenthesizedExpsQty", "classReturnQty", "classRfc", "classStringLiteralsQty", "classSubClassesQty", "classTryCatchQty", "classUniqueWordsQty", "classVariablesQty", "classWmc", "methodAnonymousClassesQty", "methodAssignmentsQty", "methodCbo", "methodComparisonsQty", "methodLambdasQty", "methodLoc", "methodLoopQty", "methodMathOperationsQty", "methodMaxNestedBlocks", "methodNumbersQty", "methodParametersQty", "methodParenthesizedExpsQty", "methodReturnQty", "methodRfc", "methodStringLiteralsQty", "methodSubClassesQty", "methodTryCatchQty", "methodUniqueWordsQty", "methodVariablesQty", "methodWmc", "variableAppearances"]
        
        features_list = []
        missing_features = []

        if 'version' in data:
            version = data['version']
            if 'dataset' in data:
                dataset = data['dataset']
                if 'refactoring' in data:
                    refactoring = data['refactoring']
                    if 'model' in data:
                        model = data['model']
                        if (refactoring in class_level_refactoring):
                            if 'features' in data:
                                features = data['features']
                                for mo in class_order:
                                    v = features.get(mo, None)
                                    if v is None:
                                        missing_features.append(mo)
                                    else:
                                        features_list.append(v)

                                if len(missing_features) > 0:
                                    message = 'Missing features: {}' \
                                              ' Please visit www.refactoring.io '.format(', '.join(missing_features))
                                    status_code = 400
                                    return jsonify({'message:': message, 'status_code:': status_code})

                                return [version, dataset, refactoring, model, numpy.asarray(features_list), "method"]
                            else:
                                message = 'Missing parameters: {}'.format('features')
                                status_code = 400
                                return jsonify({'message:': message, 'status_code:': status_code})

                        elif (refactoring in method_level_refactoring):
                            if 'features' in data:
                                features = data['features']
                                for mo in method_order:
                                    v = features.get(mo, None)
                                    if v is None:
                                        missing_features.append(mo)
                                    else:
                                        features_list.append(v)

                                if len(missing_features) > 0:
                                    message = 'Missing features: {}' \
                                              ' Please visit www.refactoring.io '.format(', '.join(missing_features))
                                    status_code = 400
                                    return jsonify({'message:': message, 'status_code:': status_code})

                                return [version, dataset, refactoring, model, numpy.asarray(features_list), "method"]
                        elif (refactoring in variable_level_refactoring):
                            if 'features' in data:
                                features = data['features']
                                for mo in variable_order:
                                    v = features.get(mo, None)
                                    if v is None:
                                        missing_features.append(mo)
                                    else:
                                        features_list.append(v)

                                if len(missing_features) > 0:
                                    message = 'Missing features: {}' \
                                              ' Please visit www.refactoring.io '.format(', '.join(missing_features))
                                    status_code = 400
                                    return jsonify({'message:': message, 'status_code:': status_code})
                                return [version, dataset, refactoring, model, numpy.asarray(features_list), "method"]
                    else:
                        message = 'Missing parameters: {}'.format('model')
                        status_code = 400
                        return jsonify({'message:': message, 'status_code:': status_code})
                else:
                    message = 'Missing parameters: {}'.format('refactoring')
                    status_code = 400
                    return jsonify({'message:': message, 'status_code:': status_code})
            else:
                message = 'Missing parameters: {}'.format('dataset')
                status_code = 400
                return jsonify({'message:': message, 'status_code:': status_code})

        else:
            message = 'Missing parameters: {}'.format('version')
            status_code = 400
            return jsonify({'message:' : message, 'status_code:' : status_code})


    def post(self):
        logger.debug("Inside the post method of Recommend")

        data = request.get_json(force=True)

        return_validation = self.validate_data(data)
        if not isinstance(return_validation, list):
            return return_validation

        version, dataset, refactoring, model, features, level = return_validation
        prediction = self.predict(model, dataset, refactoring, features, level)[0]

        response = {"status_code": 200}
        response["recommended"] = True if prediction == 1 else False

        return response
        