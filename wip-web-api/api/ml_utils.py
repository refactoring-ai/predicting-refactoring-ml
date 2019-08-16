from joblib import load


def load_object(root_folder, obj_descr_type, model_name, dataset, refactoring_name):
    file_name = root_folder + "/" + obj_descr_type + "-" + model_name + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
    return load(file_name)


def load_model(root_folder, model_name, dataset, refactoring_name):
    return load_object(root_folder, "model", model_name, dataset, refactoring_name)


def load_scaler(root_folder, model_name, dataset, refactoring_name):
    return load_object(root_folder, "scaler", model_name, dataset, refactoring_name)
