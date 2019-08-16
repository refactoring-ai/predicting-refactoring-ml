import os

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
    # print("opening file " + file_name)
    f = open(file_name, "r")

    parts_of_file_name = file_name.replace(base_dir, "").split("-")
    dataset = parts_of_file_name[3].replace(".txt", "")

    for line in f:
        if line.startswith("****"):
            current_refactoring = line[5:].strip()
            # print(current_refactoring)

        if line.startswith("- Model:"):
            current_model = line[9:].strip()
            # print(current_model)

        if line.startswith("Precision scores: "):
            current_precision = line[18:].strip()
            print(dataset + "," + current_model + "," + current_refactoring + ",precision," + current_precision)

        elif "Precision scores: " in line:
            current_precision = line[line.index("Precision scores: ")+18:].strip()
            print(dataset + "," + current_model + "," + current_refactoring + ",precision," + current_precision)

        elif line.startswith("Precision: "):
            current_precision = line[11:].strip()
            print(dataset + "," + current_model + "," + current_refactoring + ",precision," + current_precision)

        if line.startswith("Recall scores: "):
            current_recall = line[15:].strip()
            print(dataset + "," + current_model + "," + current_refactoring + ",recall," + current_recall)
        elif line.startswith("Recall: "):
            current_precision = line[8:].strip()
            print(dataset + "," + current_model + "," + current_refactoring + ",recall," + current_precision)



    f.close()





