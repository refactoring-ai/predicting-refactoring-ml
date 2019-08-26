# parses the feature importance results of all log files
import os

line_list_of_features = False
line_coefficients = False
line_list_of_features_2 = False
list_of_features = []
features_with_coefs = []

top10 = dict()
top5 = dict()
top3 = dict()
top1 = dict()


def plus_one(list, key):
    if not key in list:
        list[key] = 0

    list[key] = list[key] + 1


def magic_count():
    sorted_x = sorted(features_with_coefs.items(), key=lambda kv: kv[1], reverse=True)

    counter = 1
    for key_xy in sorted_x:
        key = key_xy[0]
        if counter == 1:
            plus_one(top1, key)

        if 1 <= counter <= 5:
            plus_one(top5, key)

        if 1 <= counter <= 10:
            plus_one(top10, key)

        counter = counter + 1
        if counter > 10:
            break


def print_csv(p):
    sorted_x = sorted(p.items(), key=lambda kv: kv[1], reverse=True)

    for x in sorted_x:
        print(x[0] + "\t" + str(x[1]))



# ---------
# main
base_dir = "/Users/mauricioaniche/Desktop/drive2/rq1-raw-logs/"


def find_files(base_dir, pattern):
    files = []
    for r, d, f in os.walk(base_dir):
        for file in f:
            if pattern in file:
                files.append(os.path.join(r, file))

    return files


list_of_files_for_processing = find_files(base_dir, "variable-level")

for file_name in list_of_files_for_processing:
    print("opening file " + file_name)
    f = open(file_name, "r")

    for line in f:

        if line_list_of_features:
            features = line.split(", ")
            list_of_features = []

            for feature in features:
                list_of_features.append(feature.strip())

            line_list_of_features = False

        elif line_coefficients:

            coefs = line.replace("[", "").replace("]", "").split(", ")

            features_with_coefs = dict()
            for i in range(0, len(list_of_features)):
                features_with_coefs[list_of_features[i]] = abs(float(coefs[i]));

            magic_count()

            line_list_of_features = False
            line_coefficients = False

        elif line_list_of_features_2:
            if line.strip() == '':
                line_list_of_features_2 = False
                magic_count()
            else:
                r = line.split(":")
                features_with_coefs[r[0].strip()] = float(r[1].strip())

        elif line.startswith('Features:'):
            line_list_of_features = True

        elif line.startswith("Feature Importances:"):
            features_with_coefs = dict()
            line_list_of_features_2 = True

        elif line.startswith('Coefficients:'):
            line_coefficients = True

    f.close()


print("-- top 1")
print_csv(top1)
print("-- top 5")
print_csv(top5)
print("-- top 10")
print_csv(top10)