from flask import jsonify, request
from flask_restful import Resource, reqparse, abort
import logging as logger



class Recommend(Resource):

    def get(self):
        logger.debug("Inside the get method")
        aboutInformation = 'This is an API that explores the effectiveness of machine learning models to predict software refactorings. ' \
                           'More specifically, we make use of six different machine learning algorithms ' \
                           '(i.e., Logistic Regression, Naive Bayes, SVM, Decision Trees, Random Forest, and Deep Neural Network) and ' \
                           'train them in a dataset of more than two million real-world refactorings that happened in 11,149 projects  from the Apache, F-Droid, and GitHub ecosystems. '

        authors = 'Anonymous authors'

        return jsonify({'About:': aboutInformation, 'How to use': 'For more information how to use this api, please visit www.refactoring.io.', 'Authors': authors})

    def post(self):
        logger.debug("Inside the post method of Recommend")

        all_refactoring = ["for class_level_refactoring-->", ["Extract Class",
            "Extract Subclass",
            "Extract Super-class",
            "Extract Interface",
            "Move Class",
            "Rename Class",
            "Move and Rename Class"],
        "for method_level_refactoring-->", ["Extract And Move Method",
            "Extract Method",
            "Inline Method",
            "Move Method",
            "Pull Up Method",
            "Push Down Method",
            "Rename Method"],
        "for variable_level_refactoring-->", ["Extract Variable",
            "Inline Variable",
            "Parameterize Variable",
            "Rename Variable",
            "Rename Parameter",
            "Replace Variable with Attribute",

                           ]
        ]

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
            "Rename Method": "Rename Method"
        }

        variable_level_refactoring = {
            "Extract Variable": "Extract Variable",
            "Inline Variable": "Inline Variable",
            "Parameterize Variable": "Parameterize Variable",
            "Rename Variable": "Rename Variable",
            "Rename Parameter": "Rename Parameter",
            "Replace Variable with Attribute": "Replace Variable with Attribute",
        }

        data = request.get_json(force=True)

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
                                if (len(features) == 47):
                                    # aqui deve-se chamar o modelo para predizer a refatoracao de class_level
                                    print('tem a quantidade de features correta.')
                                else:
                                    message = 'Missing features for class_level_variable. ' \
                                              'Please visit www.refactoring.io '
                                    status_code = 400
                                    return jsonify({'message:': message, 'status_code:': status_code})
                            else:
                                message = 'Missing parameters: {}'.format('features')
                                status_code = 400
                                return jsonify({'message:': message, 'status_code:': status_code})

                        elif (refactoring in method_level_refactoring):
                            print('Esta no metodo  refactoring')
                            if 'features' in data:
                                features = data['features']
                                if (len(features) == 58):
                                    # aqui deve-se chamar o modelo para predizer a refatoracao de class_level
                                    print('tem a quantidade de features correta.')
                                else:
                                    message = 'Missing features for method_level_variable. ' \
                                              'Please visit www.refactoring.io '
                                    status_code = 400
                                    return jsonify({'message:': message, 'status_code:': status_code})
                        elif (refactoring in variable_level_refactoring):
                            if 'features' in data:
                                features = data['features']
                                if (len(features) == 59):
                                    # aqui deve-se chamar o modelo para predizer a refatoracao de class_level
                                    print('tem a quantidade de features correta.')
                                else:
                                    message = 'Missing features for variable_level_variable. ' \
                                              'Please visit www.refactoring.io '
                                    status_code = 400
                                    return jsonify({'message:': message, 'status_code:': status_code})
                        else:
                            message = 'Misspelling the refactoring name it should be: {}'.format(all_refactoring)
                            status_code = 400
                            return jsonify({'message:': message, 'status_code:': status_code})


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