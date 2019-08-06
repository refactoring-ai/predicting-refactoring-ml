import os

import joblib
from imblearn.under_sampling import RandomUnderSampler, ClusterCentroids, NearMiss
import numpy as np
import matplotlib.pyplot as plt
from joblib import load
from keras.models import load_model as keras_load_model

from sklearn.metrics import confusion_matrix
from sklearn.utils.multiclass import unique_labels

from keras_metrics import binary_precision, binary_recall

from configs import BALANCE_DATASET


def load_object(root_folder, obj_descr_type, model_name, dataset, refactoring_name):
    if model_name == 'deep-learning' and obj_descr_type == 'model':
        file_name = root_folder + "/" + obj_descr_type + "-" + model_name + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".h5"
        return keras_load_model(file_name, custom_objects = {"binary_precision": binary_precision(),
                                                             "binary_recall": binary_recall()})
    else:
        file_name = root_folder + "/" + obj_descr_type + "-" + model_name + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        return load(file_name)


def load_model(root_folder, model_name, dataset, refactoring_name):
    return load_object(root_folder, "model", model_name, dataset, refactoring_name)


def load_scaler(root_folder, model_name, dataset, refactoring_name):
    return load_object(root_folder, "scaler", model_name, dataset, refactoring_name)


def save_model(model, model_name, dataset, refactoring_name):
    save_object("model", model, model_name, dataset, refactoring_name)


def save_object(obj_descr_type, obj, model_name, dataset, refactoring_name):
    if model_name == 'deep-learning' and obj_descr_type == 'model':
        file_name = "results/" + obj_descr_type + "-" + model_name + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".h5"
        obj.save(file_name)
    else:
        file_name = "results/" + obj_descr_type + "-" + model_name + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        joblib.dump(obj, file_name)


# more info: https://imbalanced-learn.readthedocs.io/en/stable/under_sampling.html
def perform_under_sampling(x, y):

    if BALANCE_DATASET == 'random':
        rus = RandomUnderSampler(random_state=42)  # 42 is a random number, just to ensure our results are reproducible
        return rus.fit_resample(x, y)

    if BALANCE_DATASET == 'cluster_centroids':
        rus = ClusterCentroids(random_state=42)
        return rus.fit_resample(x, y)

    if BALANCE_DATASET == 'nearmiss':
        rus = NearMiss(version=1)
        return rus.fit_resample(x, y)

    raise Exception("algorithm not found")


def create_persistence_file_name(f, m_refactoring):
    return os.path.splitext(f.name)[0] + '--' + m_refactoring.lower().replace(' ', '-')


def plot_confusion_matrix(y_true, y_pred, classes,
                          normalize=False,
                          title=None,
                          cmap=plt.cm.Blues):
    """
    This function prints and plots the confusion matrix.
    Normalization can be applied by setting `normalize=True`.
    """
    if not title:
        if normalize:
            title = 'Normalized confusion matrix'
        else:
            title = 'Confusion matrix, without normalization'

    # Compute confusion matrix
    cm = confusion_matrix(y_true, y_pred)
    # Only use the labels that appear in the data
    classes = classes[unique_labels(y_true, y_pred)]
    if normalize:
        cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]

    print(cm)

    fig, ax = plt.subplots()
    im = ax.imshow(cm, interpolation='nearest', cmap=cmap)
    ax.figure.colorbar(im, ax=ax)
    # We want to show all ticks...
    ax.set(xticks=np.arange(cm.shape[1]),
           yticks=np.arange(cm.shape[0]),
           # ... and label them with the respective list entries
           xticklabels=classes, yticklabels=classes,
           title=title,
           ylabel='True label',
           xlabel='Predicted label')

    # Rotate the tick labels and set their alignment.
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right",
             rotation_mode="anchor")

    # Loop over data dimensions and create text annotations.
    fmt = '.2f' if normalize else 'd'
    thresh = cm.max() / 2.
    for i in range(cm.shape[0]):
        for j in range(cm.shape[1]):
            ax.text(j, i, format(cm[i, j], fmt),
                    ha="center", va="center",
                    color="white" if cm[i, j] > thresh else "black")
    fig.tight_layout()
    plt.savefig("%s.png" % title)
    return ax
