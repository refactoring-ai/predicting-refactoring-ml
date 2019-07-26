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

        return jsonify({'About:': aboutInformation, 'How to use': 'For more information how to use this api, please visit www.refactoring.io.'})

    def post(self):
        logger.debug("Inside the post method of Recommend")

        data = request.get_json(force=True)
        print(data['version'])

        return jsonify(data)
